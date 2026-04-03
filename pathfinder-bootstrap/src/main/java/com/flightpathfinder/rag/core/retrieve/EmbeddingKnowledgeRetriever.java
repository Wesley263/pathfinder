package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.infra.ai.embedding.EmbeddingService;
import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Embedding-backed KB retriever with deterministic fallback to the catalog scorer.
 */
@Service
@Primary
public class EmbeddingKnowledgeRetriever implements KnowledgeRetriever {

    private final DefaultKnowledgeRetriever fallbackKnowledgeRetriever;
    private final EmbeddingService embeddingService;
    private final Map<String, List<Float>> documentEmbeddingCache = new ConcurrentHashMap<>();

    public EmbeddingKnowledgeRetriever(DefaultKnowledgeRetriever fallbackKnowledgeRetriever,
                                       EmbeddingService embeddingService) {
        this.fallbackKnowledgeRetriever = fallbackKnowledgeRetriever;
        this.embeddingService = embeddingService;
    }

    @Override
    public KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult) {
        IntentSplitResult safeIntentSplitResult = intentSplitResult == null ? IntentSplitResult.empty() : intentSplitResult;
        if (safeIntentSplitResult.kbIntents().isEmpty()) {
            return fallbackKnowledgeRetriever.retrieve(rewriteResult, safeIntentSplitResult);
        }

        try {
            Map<String, List<Object>> catalog = loadCatalog();
            if (catalog.isEmpty()) {
                return fallbackKnowledgeRetriever.retrieve(rewriteResult, safeIntentSplitResult);
            }

            List<KbRetrievalItem> items = new ArrayList<>();
            for (ResolvedIntent kbIntent : safeIntentSplitResult.kbIntents()) {
                items.addAll(retrieveForIntent(catalog, rewriteResult, kbIntent));
            }
            if (items.isEmpty()) {
                return fallbackKnowledgeRetriever.retrieve(rewriteResult, safeIntentSplitResult);
            }
            List<KbRetrievalItem> deduplicated = deduplicate(items);
            return new KbContext(
                    "SUCCESS",
                    buildSummary(deduplicated),
                    safeIntentSplitResult.kbIntents(),
                    deduplicated,
                    false,
                    "");
        } catch (Exception exception) {
            return fallbackKnowledgeRetriever.retrieve(rewriteResult, safeIntentSplitResult);
        }
    }

    private List<KbRetrievalItem> retrieveForIntent(Map<String, List<Object>> catalog,
                                                    RewriteResult rewriteResult,
                                                    ResolvedIntent kbIntent) throws Exception {
        String collectionName = kbIntent.kbCollectionName();
        if (collectionName == null || collectionName.isBlank()) {
            return List.of();
        }
        List<Object> documents = catalog.getOrDefault(collectionName, List.of());
        if (documents.isEmpty()) {
            return List.of();
        }

        String query = resolveQuery(rewriteResult, kbIntent);
        List<Float> queryEmbedding = embeddingService.embed(query);
        if (queryEmbedding.isEmpty()) {
            return List.of();
        }

        int limit = kbIntent.kbTopK() == null ? 3 : Math.max(1, kbIntent.kbTopK());
        List<ScoredItem> scoredItems = new ArrayList<>();
        for (Object document : documents) {
            String documentId = readString(document, "documentId");
            List<Float> documentEmbedding = documentEmbeddingCache.computeIfAbsent(documentId, ignored -> embedDocument(document));
            double vectorScore = cosineSimilarity(queryEmbedding, documentEmbedding);
            double lexicalScore = lexicalScore(query, kbIntent, document);
            double finalScore = clamp((vectorScore * 0.72D) + (lexicalScore * 0.28D));
            scoredItems.add(new ScoredItem(document, finalScore));
        }

        List<KbRetrievalItem> rankedItems = new ArrayList<>();
        int rank = 1;
        for (ScoredItem scoredItem : scoredItems.stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .toList()) {
            rankedItems.add(toItem(kbIntent, scoredItem.document(), scoredItem.score(), rank++));
        }
        return rankedItems;
    }

    private List<Float> embedDocument(Object document) {
        try {
            String text = readString(document, "title") + "\n"
                    + readString(document, "content") + "\n"
                    + String.join(", ", readStringList(document, "tags"));
            return embeddingService.embed(text);
        } catch (Exception exception) {
            return List.of();
        }
    }

    private KbRetrievalItem toItem(ResolvedIntent kbIntent, Object document, double score, int rank) {
        try {
            return new KbRetrievalItem(
                    kbIntent.intentId(),
                    kbIntent.intentName(),
                    kbIntent.question(),
                    readString(document, "collectionName"),
                    readString(document, "topic"),
                    readString(document, "source"),
                    readString(document, "documentId"),
                    readString(document, "title"),
                    readString(document, "content"),
                    score,
                    rank);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to map KB document", exception);
        }
    }

    private List<KbRetrievalItem> deduplicate(List<KbRetrievalItem> items) {
        Map<String, KbRetrievalItem> deduplicated = new LinkedHashMap<>();
        for (KbRetrievalItem item : items) {
            String key = item.intentId() + "|" + item.documentId();
            KbRetrievalItem existing = deduplicated.get(key);
            if (existing == null || item.score() > existing.score()) {
                deduplicated.put(key, item);
            }
        }
        return List.copyOf(deduplicated.values());
    }

    private String buildSummary(List<KbRetrievalItem> items) {
        Map<String, Long> countsByCollection = new LinkedHashMap<>();
        for (KbRetrievalItem item : items) {
            countsByCollection.merge(item.collectionName(), 1L, Long::sum);
        }
        return countsByCollection.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .reduce((left, right) -> left + " | " + right)
                .orElse("no KB entries matched");
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Object>> loadCatalog() throws Exception {
        Field catalogField = DefaultKnowledgeRetriever.class.getDeclaredField("KNOWLEDGE_CATALOG");
        catalogField.setAccessible(true);
        Map<String, List<Object>> catalog = (Map<String, List<Object>>) catalogField.get(null);
        return catalog == null ? Map.of() : catalog;
    }

    private String resolveQuery(RewriteResult rewriteResult, ResolvedIntent kbIntent) {
        if (kbIntent.question() != null && !kbIntent.question().isBlank()) {
            return kbIntent.question();
        }
        return rewriteResult == null ? "" : rewriteResult.routingQuestion();
    }

    private double lexicalScore(String query, ResolvedIntent kbIntent, Object document) throws Exception {
        String normalizedQuery = normalize(query);
        double score = 0.52D;
        if (Objects.equals(readString(document, "collectionName"), kbIntent.kbCollectionName())) {
            score += 0.10D;
        }
        if (normalizedQuery.contains(normalize(readString(document, "topic")))) {
            score += 0.08D;
        }
        for (String tag : readStringList(document, "tags")) {
            if (normalizedQuery.contains(normalize(tag))) {
                score += 0.04D;
            }
        }
        if (normalizedQuery.contains(normalize(readString(document, "title")))) {
            score += 0.06D;
        }
        if (normalizedQuery.contains(normalize(kbIntent.intentName()))) {
            score += 0.04D;
        }
        return clamp(score);
    }

    private double cosineSimilarity(List<Float> left, List<Float> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0.0D;
        }
        double dot = 0.0D;
        double leftNorm = 0.0D;
        double rightNorm = 0.0D;
        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0.0D || rightNorm == 0.0D) {
            return 0.0D;
        }
        double cosine = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        return clamp((cosine + 1.0D) / 2.0D);
    }

    private String readString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        Object value = method.invoke(target);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringList(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        Object value = method.invoke(target);
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (Object item : values) {
            if (item != null) {
                strings.add(String.valueOf(item));
            }
        }
        return List.copyOf(strings);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double clamp(double rawScore) {
        double bounded = Math.max(0.0D, Math.min(0.99D, rawScore));
        return BigDecimal.valueOf(bounded).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private record ScoredItem(Object document, double score) {
    }
}