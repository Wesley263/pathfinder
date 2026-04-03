package com.flightpathfinder.rag.core.rewrite;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermNormalizer {

    private static final Pattern ASCII_ONLY_PATTERN = Pattern.compile("^[\\p{ASCII}]+$");

    private final List<TermMapping> mappings;

    public TermNormalizer() {
        this(defaultMappings());
    }

    public TermNormalizer(List<TermMapping> mappings) {
        this.mappings = List.copyOf((mappings == null ? List.<TermMapping>of() : mappings).stream()
                .sorted(Comparator.comparingInt(TermMapping::priority).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (TermMapping mapping) -> mapping.sourceTerm().length()).reversed()))
                .toList());
    }

    public String normalize(String text) {
        String normalizedText = text == null ? "" : text.trim();
        for (TermMapping mapping : mappings) {
            normalizedText = applyMapping(normalizedText, mapping);
        }
        return normalizedText;
    }

    private static List<TermMapping> defaultMappings() {
        return List.of(
                new TermMapping("上海浦东国际机场", "PVG", 100),
                new TermMapping("浦东国际机场", "PVG", 95),
                new TermMapping("浦东机场", "PVG", 94),
                new TermMapping("浦东", "PVG", 90),
                new TermMapping("上海虹桥国际机场", "SHA", 100),
                new TermMapping("虹桥国际机场", "SHA", 95),
                new TermMapping("虹桥机场", "SHA", 94),
                new TermMapping("虹桥", "SHA", 90),
                new TermMapping("北京首都国际机场", "PEK", 100),
                new TermMapping("首都机场", "PEK", 94),
                new TermMapping("北京大兴国际机场", "PKX", 100),
                new TermMapping("大兴国际机场", "PKX", 95),
                new TermMapping("大兴机场", "PKX", 94),
                new TermMapping("上海", "SHA", 70),
                new TermMapping("北京", "BJS", 70),
                new TermMapping("伦敦", "LON", 70),
                new TermMapping("东京", "TYO", 70),
                new TermMapping("大阪", "OSA", 70),
                new TermMapping("首尔", "SEL", 70),
                new TermMapping("巴黎", "PAR", 70),
                new TermMapping("纽约", "NYC", 70),
                new TermMapping("洛杉矶", "LAX", 70),
                new TermMapping("新加坡", "SIN", 70),
                new TermMapping("曼谷", "BKK", 70),
                new TermMapping("中国国际航空", "CA", 90),
                new TermMapping("国航", "CA", 85),
                new TermMapping("中国东方航空", "MU", 90),
                new TermMapping("东航", "MU", 85),
                new TermMapping("中国南方航空", "CZ", 90),
                new TermMapping("南航", "CZ", 85),
                new TermMapping("海南航空", "HU", 90),
                new TermMapping("海航", "HU", 85),
                new TermMapping("转机", "中转", 80),
                new TermMapping("转乘", "中转", 80)
        );
    }

    private String applyMapping(String text, TermMapping mapping) {
        if (text.isBlank() || mapping.sourceTerm().isBlank()) {
            return text;
        }

        if (ASCII_ONLY_PATTERN.matcher(mapping.sourceTerm()).matches()) {
            Pattern pattern = Pattern.compile(Pattern.quote(mapping.sourceTerm()),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            return pattern.matcher(text).replaceAll(Matcher.quoteReplacement(mapping.targetTerm()));
        }
        return text.replace(mapping.sourceTerm(), mapping.targetTerm());
    }

    public record TermMapping(String sourceTerm, String targetTerm, int priority) {
    }
}
