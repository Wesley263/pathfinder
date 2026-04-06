package com.flightpathfinder.infra.ai.token;
/**
 * token 估算服务。
 */
public interface TokenCounterService {

    /**
     * 估算文本对应的 token 数量。
     *
     * @param text 输入文本
     * @return 估算 token 数
     */
    int estimateTokens(String text);
}



