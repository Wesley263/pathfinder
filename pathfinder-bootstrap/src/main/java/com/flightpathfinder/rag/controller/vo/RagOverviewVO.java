package com.flightpathfinder.rag.controller.vo;

import java.util.List;

public record RagOverviewVO(String feature, String module, String status, int mcpToolCount, List<String> boundaries) {
}
