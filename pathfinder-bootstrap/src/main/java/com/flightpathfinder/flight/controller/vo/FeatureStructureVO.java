package com.flightpathfinder.flight.controller.vo;

import java.util.List;

public record FeatureStructureVO(String feature, String module, String status, List<String> boundaries) {
}

