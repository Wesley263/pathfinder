package com.flightpathfinder.infra.ai.token;

import org.springframework.stereotype.Component;
/**
 * 说明。
 */

@Component
public class HeuristicTokenCounterService implements TokenCounterService {

    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}


