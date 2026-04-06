package com.flightpathfinder.rag.core.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
/**
 * 用于覆盖目标行为的断言并防止回归。
 */
class TermNormalizerTest {

    @Test
    void normalize_shouldApplyDefaultMappingsAndPriority() {
        TermNormalizer normalizer = new TermNormalizer();

        String normalized = normalizer.normalize("上海浦东国际机场 转机 到 北京");

        assertEquals("PVG 中转 到 BJS", normalized);
    }

    @Test
    void normalize_shouldUseCaseInsensitiveReplacementForAsciiMapping() {
        TermNormalizer normalizer = new TermNormalizer(List.of(
                new TermNormalizer.TermMapping("flight", "航班", 100)));

        String normalized = normalizer.normalize("Find FLIGHT info");

        assertEquals("Find 航班 info", normalized);
    }

    @Test
    void normalize_shouldPreferLongerSourceWhenPrioritySame() {
        TermNormalizer normalizer = new TermNormalizer(List.of(
                new TermNormalizer.TermMapping("a", "X", 10),
                new TermNormalizer.TermMapping("ab", "Y", 10)));

        String normalized = normalizer.normalize("ab a");

        assertEquals("Y X", normalized);
    }

    @Test
    void normalize_shouldHandleNullAndTrimInput() {
        TermNormalizer normalizer = new TermNormalizer();

        assertEquals("", normalizer.normalize(null));
        assertEquals("SHA", normalizer.normalize("  上海  "));
    }
}



