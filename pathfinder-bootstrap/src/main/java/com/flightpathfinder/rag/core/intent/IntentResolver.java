package com.flightpathfinder.rag.core.intent;

import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 改写结果到最终分流结果的解析边界。
 *
 * <p>resolver 位于 classifier 之上，因为只有它能同时看到所有子问题的分类结果，
 * 并对外给出统一的 KB/MCP/SYSTEM 分流语义。</p>
 */
public interface IntentResolver {

    /**
     * 把改写结果解析成完整的意图视图。
     *
     * @param rewriteResult 第一阶段改写输出
     * @return 包含子问题级打分和汇总分流结果的解析对象
     */
    IntentResolution resolve(RewriteResult rewriteResult);
}
