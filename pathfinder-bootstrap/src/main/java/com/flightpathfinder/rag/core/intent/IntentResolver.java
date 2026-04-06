package com.flightpathfinder.rag.core.intent;

import com.flightpathfinder.rag.core.rewrite.RewriteResult;

/**
 * 改写结果到最终分流结果的解析边界。
 *
 * 负责把改写后的问题映射为可执行的意图分流视图。
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

