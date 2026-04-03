package com.flightpathfinder.rag.core.retrieve;

import com.flightpathfinder.rag.core.intent.IntentSplitResult;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * First-pass KB retriever backed by a small in-process catalog.
 *
 * <p>This implementation is intentionally lightweight. The current goal is to preserve KB
 * retrieval boundaries and result semantics before introducing a heavier retriever.</p>
 */
@Service
public class DefaultKnowledgeRetriever implements KnowledgeRetriever {

    private static final Map<String, List<KnowledgeDocument>> KNOWLEDGE_CATALOG = Map.of(
            "policy_edge_cases", List.of(
                    new KnowledgeDocument(
                            "policy-001",
                            "日本短期入境与过境注意事项",
                            "中国护照前往日本通常需要按当前政策提前确认签证或有效入境资格，过境场景要额外核对是否真正入境以及联程衔接条件。",
                            "policy_edge_cases",
                            "visa-policy",
                            "kb://policy_edge_cases/japan-transit",
                            List.of("日本", "签证", "入境", "过境", "联程")),
                    new KnowledgeDocument(
                            "policy-002",
                            "韩国过境与停留边界提醒",
                            "韩国过境是否可临时停留，要结合护照、签证、目的地资格和停留时长综合判断，边界场景不能只看单一关键词。",
                            "policy_edge_cases",
                            "transit-policy",
                            "kb://policy_edge_cases/korea-transit",
                            List.of("韩国", "过境", "停留", "免签", "边界"))),
            "travel_experience", List.of(
                    new KnowledgeDocument(
                            "experience-001",
                            "首尔转机体验常见节奏",
                            "首尔大型枢纽机场的转机体验通常取决于入境与否、航站楼切换和安检排队，国际转国际需要重点预留衔接缓冲。",
                            "travel_experience",
                            "transfer-experience",
                            "kb://travel_experience/seoul-transfer",
                            List.of("首尔", "转机", "衔接", "安检", "航站楼")),
                    new KnowledgeDocument(
                            "experience-002",
                            "成田机场转机流程提醒",
                            "成田机场转机要关注到达航站楼、是否需要提取行李以及再次值机，夜间落地时服务窗口开放时间也很关键。",
                            "travel_experience",
                            "airport-process",
                            "kb://travel_experience/narita-transfer",
                            List.of("成田", "转机", "行李", "值机", "流程"))),
            "faq_and_tips", List.of(
                    new KnowledgeDocument(
                            "tips-001",
                            "国际航班值机与到场建议",
                            "国际航班通常建议更早到达机场，具体提前量要结合机场规模、航司值机截止时间和是否托运行李判断。",
                            "faq_and_tips",
                            "check-in-tips",
                            "kb://faq_and_tips/checkin",
                            List.of("国际航班", "提前多久", "值机", "托运", "建议")),
                    new KnowledgeDocument(
                            "tips-002",
                            "廉航出行准备要点",
                            "廉航出行前要重点核对随身行李尺寸、托运行李规则、值机方式和附加收费项，避免在机场临时补差价。",
                            "faq_and_tips",
                            "budget-travel",
                            "kb://faq_and_tips/budget-airline",
                            List.of("廉航", "行李", "收费", "出行建议", "避坑"))));

    /**
     * Retrieves KB entries for the KB intents selected by stage one.
     *
     * @param rewriteResult rewritten question used as retrieval text
     * @param intentSplitResult split result containing KB intents
     * @return KB context with success, empty, or error semantics
     */
    @Override
    public KbContext retrieve(RewriteResult rewriteResult, IntentSplitResult intentSplitResult) {
        IntentSplitResult safeIntentSplitResult = intentSplitResult == null ? IntentSplitResult.empty() : intentSplitResult;
        List<ResolvedIntent> kbIntents = safeIntentSplitResult.kbIntents();
        if (kbIntents.isEmpty()) {
            return KbContext.empty("no KB intents matched in the current split result", List.of());
        }

        try {
            List<KbRetrievalItem> items = new ArrayList<>();
            for (ResolvedIntent kbIntent : kbIntents) {
                items.addAll(retrieveForIntent(rewriteResult, kbIntent));
            }

            if (items.isEmpty()) {
                return KbContext.empty("no KB entries matched the current KB intents", kbIntents);
            }

            return new KbContext(
                    "SUCCESS",
                    buildSummary(items),
                    kbIntents,
                    deduplicate(items),
                    false,
                    "");
        } catch (Exception exception) {
            return KbContext.error("KB retrieval failed", kbIntents, exception.getMessage());
        }
    }

    private List<KbRetrievalItem> retrieveForIntent(RewriteResult rewriteResult, ResolvedIntent kbIntent) {
        String collectionName = kbIntent.kbCollectionName();
        if (collectionName == null || collectionName.isBlank()) {
            return List.of();
        }

        List<KnowledgeDocument> documents = KNOWLEDGE_CATALOG.getOrDefault(collectionName, List.of());
        if (documents.isEmpty()) {
            return List.of();
        }

        String query = resolveQuery(rewriteResult, kbIntent);
        AtomicInteger rankCounter = new AtomicInteger(1);
        int limit = kbIntent.kbTopK() == null ? 3 : Math.max(1, kbIntent.kbTopK());

        // Ranking is collection-local on purpose so a future retriever can replace this toy
        // catalog without changing the external KB context contract.
        return documents.stream()
                .map(document -> new ScoredDocument(document, score(query, kbIntent, document)))
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(limit)
                .map(scoredDocument -> toItem(kbIntent, scoredDocument.document(), scoredDocument.score(), rankCounter.getAndIncrement()))
                .toList();
    }

    private KbRetrievalItem toItem(ResolvedIntent kbIntent, KnowledgeDocument document, double score, int rank) {
        return new KbRetrievalItem(
                kbIntent.intentId(),
                kbIntent.intentName(),
                kbIntent.question(),
                document.collectionName(),
                document.topic(),
                document.source(),
                document.documentId(),
                document.title(),
                document.content(),
                score,
                rank);
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

    private double score(String query, ResolvedIntent kbIntent, KnowledgeDocument document) {
        String normalizedQuery = normalize(query);
        double score = 0.55D;
        if (document.collectionName().equals(kbIntent.kbCollectionName())) {
            score += 0.10D;
        }
        if (normalizedQuery.contains(normalize(document.topic()))) {
            score += 0.08D;
        }
        int tagHits = 0;
        for (String tag : document.tags()) {
            if (normalizedQuery.contains(normalize(tag))) {
                tagHits++;
            }
        }
        score += Math.min(0.20D, tagHits * 0.05D);
        if (normalizedQuery.contains(normalize(document.title()))) {
            score += 0.06D;
        }
        if (normalizedQuery.contains(normalize(kbIntent.intentName()))) {
            score += 0.04D;
        }
        return BigDecimal.valueOf(Math.min(score, 0.98D))
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String resolveQuery(RewriteResult rewriteResult, ResolvedIntent kbIntent) {
        if (kbIntent.question() != null && !kbIntent.question().isBlank()) {
            return kbIntent.question();
        }
        return rewriteResult == null ? "" : rewriteResult.routingQuestion();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record KnowledgeDocument(
            String documentId,
            String title,
            String content,
            String collectionName,
            String topic,
            String source,
            List<String> tags) {
    }

    private record ScoredDocument(KnowledgeDocument document, double score) {
    }
}
