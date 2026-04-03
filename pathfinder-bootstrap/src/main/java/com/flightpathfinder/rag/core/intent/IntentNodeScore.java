package com.flightpathfinder.rag.core.intent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record IntentNodeScore(IntentNode node, double score) {

    public IntentNodeScore {
        node = Objects.requireNonNull(node, "node cannot be null");
        score = clamp(score);
    }

    private static double clamp(double rawScore) {
        double bounded = Math.max(0.0D, Math.min(1.0D, rawScore));
        return BigDecimal.valueOf(bounded).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
