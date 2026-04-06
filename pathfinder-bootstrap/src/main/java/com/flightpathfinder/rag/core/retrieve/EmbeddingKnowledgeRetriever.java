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
 * 基于 embedding 的 KB 检索器。
 *
 * <p>它在内置目录检索器之上增加向量相似度能力，但仍保留确定性 fallback。
 * 这样 embedding 服务不可用时，KB 分支仍能维持稳定可运行。</p>
 */
@Service
@Primary
public class EmbeddingKnowledgeRetriever implements KnowledgeRetriever {

    /** 目录式 KB 检索器兜底实现。 */
    private final DefaultKnowledgeRetriever fallbackKnowledgeRetriever;
    /** embedding 向量服务。 */
    private final EmbeddingService embeddingService;
    /** 文档 embedding 缓存，避免每次检索都重复向量化相同文档。 */
    private final Map<String, List<Float>> documentEmbeddingCache = new ConcurrentHashMap<>();

    /**
     * 构造 embedding 检索器。
     *
     * @param fallbackKnowledgeRetriever 目录式兜底检索器
     * @param embeddingService embedding 向量服务
     */
    public EmbeddingKnowledgeRetriever(DefaultKnowledgeRetriever fallbackKnowledgeRetriever,
                                       EmbeddingService embeddingService) {
        this.fallbackKnowledgeRetriever = fallbackKnowledgeRetriever;
        this.embeddingService = embeddingService;
    }

    /**
     * 基于 embedding 执行 KB 检索。
     *
     * @param rewriteResult 改写结果
     * @param intentSplitResult 分流结果
     * @return embedding 检索结果；无法执行时回落到目录式检索结果
     */
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

    /**
     * 针对单个 KB intent 执行 embedding 检索。
     *
     * @param catalog 当前知识目录
     * @param rewriteResult 改写结果
     * @param kbIntent 当前 KB intent
     * @return 当前 intent 命中的条目列表
     * @throws Exception 当反射读取文档字段失败时抛出异常
     */
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

    /**
     * 为单条文档生成 embedding。
     *
     * @param document 文档对象
     * @return 文档 embedding；失败时返回空列表
     */
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

    /**
     * 把反射读取到的文档对象转换为统一 KB 条目。
     *
     * @param kbIntent 当前 KB intent
     * @param document 原始文档对象
     * @param score 文档得分
     * @param rank 排名
     * @return 标准化 KB 条目
     */
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

    /**
     * 对条目按 intentId + documentId 去重。
     *
     * @param items 原始条目列表
     * @return 去重后的条目列表
     */
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

    /**
     * 生成 KB 检索摘要。
     *
     * @param items 检索条目列表
     * @return 按集合统计的摘要文本
     */
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
    /**
     * 通过反射读取目录式检索器中的知识目录。
     *
     * <p>这里刻意复用兜底检索器已有的目录数据，避免为第一版 embedding 检索再维护一份平行目录。</p>
     *
     * @return 当前知识目录
     * @throws Exception 当反射读取失败时抛出异常
     */
    private Map<String, List<Object>> loadCatalog() throws Exception {
        Field catalogField = DefaultKnowledgeRetriever.class.getDeclaredField("KNOWLEDGE_CATALOG");
        catalogField.setAccessible(true);
        Map<String, List<Object>> catalog = (Map<String, List<Object>>) catalogField.get(null);
        return catalog == null ? Map.of() : catalog;
    }

    /**
     * 决定本次检索应使用的问题文本。
     *
     * @param rewriteResult 改写结果
     * @param kbIntent 当前 KB intent
     * @return 优先使用子问题文本，否则退回改写主问题
     */
    private String resolveQuery(RewriteResult rewriteResult, ResolvedIntent kbIntent) {
        if (kbIntent.question() != null && !kbIntent.question().isBlank()) {
            return kbIntent.question();
        }
        return rewriteResult == null ? "" : rewriteResult.routingQuestion();
    }

    /**
     * 计算词法得分，用于与向量相似度混合。
     *
     * @param query 当前检索问题
     * @param kbIntent 当前 KB intent
     * @param document 待评分文档
     * @return 词法得分
     * @throws Exception 当文档字段读取失败时抛出异常
     */
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

    /**
     * 计算两个向量的余弦相似度。
     *
     * @param left 左侧向量
     * @param right 右侧向量
     * @return 归一化后的相似度分数
     */
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

    /**
     * 通过无参方法反射读取字符串字段。
     *
     * @param target 目标对象
     * @param methodName 访问器方法名
     * @return 读取到的字符串值
     * @throws Exception 当反射调用失败时抛出异常
     */
    private String readString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        Object value = method.invoke(target);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    /**
     * 通过无参方法反射读取字符串列表字段。
     *
     * @param target 目标对象
     * @param methodName 访问器方法名
     * @return 读取到的字符串列表
     * @throws Exception 当反射调用失败时抛出异常
     */
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

    /**
     * 统一文本比较格式。
     *
     * @param value 原始文本
     * @return 归一化后的比较文本
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 裁剪并规范化得分范围。
     *
     * @param rawScore 原始分数
     * @return 保留四位小数的分数
     */
    private double clamp(double rawScore) {
        double bounded = Math.max(0.0D, Math.min(0.99D, rawScore));
        return BigDecimal.valueOf(bounded).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 文档得分结果。
     *
     * @param document 候选文档
     * @param score 文档分数
     */
    private record ScoredItem(Object document, double score) {
    }
}
