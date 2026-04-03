package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import com.flightpathfinder.mcp.graph.model.RestoredFlightGraph;
import com.flightpathfinder.mcp.graph.model.RestoredGraphNode;
import java.util.Comparator;
import java.util.List;

/**
 * Final weighted ranker for Pareto-selected candidate paths.
 *
 * <p>This stage comes after Pareto filtering on purpose: Pareto keeps multiple "good in
 * different ways" candidates alive, then weighted ranking chooses the final top-K.</p>
 */
final class GraphPathWeightedRanker {

    /**
     * Ranks the selected candidate set and returns the final top-K paths.
     */
    List<RestoredCandidatePath> rank(RestoredFlightGraph graph,
                                     GraphPathParetoSelection selection,
                                     GraphPathScoringProfile profile,
                                     int topK) {
        List<RestoredCandidatePath> candidates = selection.selectedCandidates();
        if (candidates.isEmpty()) {
            return List.of();
        }

        ScoreRange priceRange = ScoreRange.ofDoubles(candidates.stream()
                .mapToDouble(RestoredCandidatePath::totalPriceCny)
                .toArray());
        ScoreRange durationRange = ScoreRange.ofInts(candidates.stream()
                .mapToInt(RestoredCandidatePath::totalDurationMinutes)
                .toArray());
        ScoreRange reliabilityRange = ScoreRange.ofDoubles(candidates.stream()
                .mapToDouble(RestoredCandidatePath::averageOnTimeRate)
                .toArray());
        ScoreRange transferRange = ScoreRange.ofInts(candidates.stream()
                .mapToInt(RestoredCandidatePath::transferCount)
                .toArray());
        ScoreRange stopRange = ScoreRange.ofInts(candidates.stream()
                .mapToInt(RestoredCandidatePath::totalStops)
                .toArray());
        ScoreRange competitionRange = ScoreRange.ofDoubles(candidates.stream()
                .mapToDouble(RestoredCandidatePath::averageCompetitionCount)
                .toArray());
        ScoreRange layerRange = new ScoreRange(1.0, Math.max(1.0, selection.maxLayer()));

        // Normalizing each metric over the current candidate set keeps one large-magnitude
        // field such as price from drowning out the rest of the weighted profile.
        return candidates.stream()
                .map(path -> new RankedPath(path, score(
                        graph,
                        path,
                        selection.layerOf(path),
                        priceRange,
                        durationRange,
                        reliabilityRange,
                        transferRange,
                        stopRange,
                        competitionRange,
                        profile,
                        layerRange)))
                .sorted(Comparator.comparingDouble(RankedPath::score).reversed()
                        .thenComparing((RankedPath rankedPath) -> rankedPath.path().totalPriceCny())
                        .thenComparing(rankedPath -> rankedPath.path().totalDurationMinutes())
                        .thenComparing(rankedPath -> rankedPath.path().transferCount()))
                .limit(topK)
                .map(RankedPath::path)
                .toList();
    }

    private double score(RestoredFlightGraph graph,
                         RestoredCandidatePath path,
                         int paretoLayer,
                         ScoreRange priceRange,
                         ScoreRange durationRange,
                         ScoreRange reliabilityRange,
                         ScoreRange transferRange,
                         ScoreRange stopRange,
                         ScoreRange competitionRange,
                         GraphPathScoringProfile profile,
                         ScoreRange layerRange) {
        double priceScore = priceRange.normalizeLowerIsBetter(path.totalPriceCny());
        double durationScore = durationRange.normalizeLowerIsBetter(path.totalDurationMinutes());
        double reliabilityScore = reliabilityRange.normalizeHigherIsBetter(path.averageOnTimeRate());
        double transferScore = transferRange.normalizeLowerIsBetter(path.transferCount());
        double stopScore = stopRange.normalizeLowerIsBetter(path.totalStops());
        double baggageScore = path.baggageIncluded() ? 1.0 : 0.0;
        double competitionScore = competitionRange.normalizeHigherIsBetter(path.averageCompetitionCount());
        double detourScore = detourEfficiency(graph, path);
        double layerScore = layerRange.normalizeLowerIsBetter(paretoLayer);

        return priceScore * profile.priceWeight()
                + durationScore * profile.durationWeight()
                + reliabilityScore * profile.reliabilityWeight()
                + transferScore * profile.transferWeight()
                + stopScore * profile.stopWeight()
                + baggageScore * profile.baggageWeight()
                + detourScore * profile.detourWeight()
                + competitionScore * profile.competitionWeight()
                + layerScore * profile.paretoLayerWeight();
    }

    private double detourEfficiency(RestoredFlightGraph graph, RestoredCandidatePath path) {
        if (path.legs().isEmpty()) {
            return 0.0;
        }
        RestoredGraphNode origin = graph.getNode(path.legs().get(0).origin());
        RestoredGraphNode destination = graph.getNode(path.legs().get(path.legs().size() - 1).destination());
        if (origin == null || destination == null || path.totalDistanceKm() <= 0.0) {
            return 0.5;
        }
        double directDistance = haversineKm(
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude());
        if (directDistance <= 0.0) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, directDistance / path.totalDistanceKm()));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record RankedPath(RestoredCandidatePath path, double score) {
    }

    private record ScoreRange(double min, double max) {

        static ScoreRange ofDoubles(double[] values) {
            if (values.length == 0) {
                return new ScoreRange(0.0, 1.0);
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (double value : values) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new ScoreRange(min, max);
        }

        static ScoreRange ofInts(int[] values) {
            if (values.length == 0) {
                return new ScoreRange(0.0, 1.0);
            }
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int value : values) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return new ScoreRange(min, max);
        }

        double normalizeLowerIsBetter(double value) {
            if (max <= min) {
                return 1.0;
            }
            return 1.0 - ((value - min) / (max - min));
        }

        double normalizeHigherIsBetter(double value) {
            if (max <= min) {
                return 1.0;
            }
            return (value - min) / (max - min);
        }
    }
}
