package com.flightpathfinder.infra.ai.token;
/**
 * 用于 Token 估算的能力接口。
 */
public interface TokenCounterService {

    int estimateTokens(String text);
}



