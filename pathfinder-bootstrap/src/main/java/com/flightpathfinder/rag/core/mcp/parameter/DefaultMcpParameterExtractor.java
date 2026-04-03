package com.flightpathfinder.rag.core.mcp.parameter;

import com.flightpathfinder.framework.protocol.mcp.McpToolDescriptor;
import com.flightpathfinder.rag.core.intent.ResolvedIntent;
import com.flightpathfinder.rag.core.rewrite.RewriteResult;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultMcpParameterExtractor implements McpParameterExtractor {

    private static final String GRAPH_PATH_TOOL_ID = "graph.path.search";
    private static final String FLIGHT_SEARCH_TOOL_ID = "flight.search";
    private static final String PRICE_LOOKUP_TOOL_ID = "price.lookup";
    private static final String VISA_CHECK_TOOL_ID = "visa.check";
    private static final String CITY_COST_TOOL_ID = "city.cost";
    private static final String RISK_EVALUATE_TOOL_ID = "risk.evaluate";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern IATA_CODE_PATTERN = Pattern.compile("\\b([A-Z]{3})\\b");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("\\b([A-Z]{2})\\b");
    private static final Pattern FROM_TO_PATTERN = Pattern.compile(
            "(?:从|FROM)\\s*([A-Z]{3})\\s*(?:出发)?\\s*(?:到|飞到|飞往|前往|TO)\\s*([A-Z]{3})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ARROW_ROUTE_PATTERN = Pattern.compile(
            "\\b([A-Z]{3})\\b\\s*(?:->|=>|到|飞到|飞往|前往|TO)\\s*\\b([A-Z]{3})\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CSV_PAIR_PATTERN = Pattern.compile("\\b([A-Z]{3})\\b\\s*,\\s*\\b([A-Z]{3})\\b");
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:预算|预算在|预算不超过|预算上限|不超过|低于|少于|控制在)\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RMB_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:元|人民币|rmb)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STOPOVER_PATTERN = Pattern.compile(
            "(?:停留|中转停留|转机停留|stopoverDays(?:=|\\s*))\\s*(\\d+)\\s*(?:天|d|days?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MAX_SEGMENTS_PATTERN = Pattern.compile(
            "(?:maxSegments(?:=|\\s*)|最多|至多|不超过)\\s*(\\d+)\\s*(?:段|航段)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TRANSFER_COUNT_PATTERN = Pattern.compile("(?:最多|至多|不超过)\\s*(\\d+)\\s*次中转");
    private static final Pattern TOP_K_PATTERN = Pattern.compile(
            "(?:topK(?:=|\\s*)|top\\s*|前\\s*)(\\d{1,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FLEXIBILITY_PATTERN = Pattern.compile(
            "(?:前后|灵活|浮动|flexibilityDays(?:=|\\s*)|flex(?:=|\\s*))\\s*(\\d+)\\s*(?:天|d|days?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern STAY_DAYS_PATTERN = Pattern.compile(
            "(?:停留|待|呆|stayDays(?:=|\\s*)|stay(?:=|\\s*))\\s*(\\d+)\\s*(?:天|d|days?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GENERIC_DAY_COUNT_PATTERN = Pattern.compile("(\\d+)\\s*(?:天|day|days)");
    private static final Pattern PASSPORT_COUNTRY_CODE_PATTERN = Pattern.compile(
            "\\b([A-Z]{2})\\b\\s*(?:passport|护照)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern AIRLINE_PAIR_PATTERN = Pattern.compile(
            "\\b([A-Z0-9]{2})\\b\\s*(?:/|,|->|到|转|接|和|与|&)\\s*\\b([A-Z0-9]{2})\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern AIRLINE_CODE_PATTERN = Pattern.compile("\\b([A-Z0-9]{2})\\b");
    private static final Pattern BUFFER_HOURS_PATTERN = Pattern.compile(
            "(?:缓冲时间|中转时间|衔接时间|bufferHours(?:=|\\s*)|buffer(?:=|\\s*))\\s*(\\d+(?:\\.\\d+)?)\\s*(?:小时|h|hours?)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GENERIC_HOUR_COUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:小时|h|hours)");
    private static final Pattern HUB_AIRPORT_PATTERN = Pattern.compile(
            "(?:在|于|经|via|through)\\s*([A-Z]{3})\\s*(?:中转|转机|衔接)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b");
    private static final Pattern CHINESE_DATE_PATTERN = Pattern.compile("(?:(20\\d{2})年)?\\s*(\\d{1,2})月\\s*(\\d{1,2})日");
    private static final Set<String> IGNORED_IATA_LIKE_TOKENS = Set.of("TOP", "RMB", "USD", "DAY", "AND", "THE", "MAX");
    private static final Set<String> IGNORED_COUNTRY_CODE_TOKENS = Set.of("TO", "OR", "IN", "ON", "BY", "AT", "OF", "NO", "HI");
    private static final Set<String> IGNORED_AIRLINE_CODE_TOKENS = Set.of("TO", "OR", "IN", "ON", "BY", "AT", "OF", "NO", "HI", "CN", "JP", "KR", "TH", "SG", "GB", "FR", "US", "AU", "AE");
    private static final Map<String, String> METRO_TO_AIRPORT = Map.of(
            "BJS", "PEK",
            "LON", "LHR",
            "NYC", "JFK",
            "OSA", "KIX",
            "PAR", "CDG",
            "SEL", "ICN",
            "SHA", "PVG",
            "TYO", "NRT"
    );
    private static final Map<String, String> COUNTRY_NAME_TO_ISO = Map.ofEntries(
            Map.entry("中国", "CN"),
            Map.entry("china", "CN"),
            Map.entry("日本", "JP"),
            Map.entry("japan", "JP"),
            Map.entry("韩国", "KR"),
            Map.entry("南韩", "KR"),
            Map.entry("korea", "KR"),
            Map.entry("south korea", "KR"),
            Map.entry("新加坡", "SG"),
            Map.entry("singapore", "SG"),
            Map.entry("泰国", "TH"),
            Map.entry("thailand", "TH"),
            Map.entry("菲律宾", "PH"),
            Map.entry("菲律賓", "PH"),
            Map.entry("philippines", "PH"),
            Map.entry("阿联酋", "AE"),
            Map.entry("阿联酋迪拜", "AE"),
            Map.entry("uae", "AE"),
            Map.entry("united arab emirates", "AE"),
            Map.entry("英国", "GB"),
            Map.entry("英國", "GB"),
            Map.entry("uk", "GB"),
            Map.entry("united kingdom", "GB"),
            Map.entry("britain", "GB"),
            Map.entry("法国", "FR"),
            Map.entry("法國", "FR"),
            Map.entry("france", "FR"),
            Map.entry("澳大利亚", "AU"),
            Map.entry("澳洲", "AU"),
            Map.entry("australia", "AU"),
            Map.entry("美国", "US"),
            Map.entry("美國", "US"),
            Map.entry("usa", "US"),
            Map.entry("united states", "US"));
    private static final Map<String, String> CITY_NAME_TO_IATA = Map.ofEntries(
            Map.entry("上海", "PVG"),
            Map.entry("shanghai", "PVG"),
            Map.entry("北京", "PEK"),
            Map.entry("beijing", "PEK"),
            Map.entry("东京", "NRT"),
            Map.entry("東京", "NRT"),
            Map.entry("tokyo", "NRT"),
            Map.entry("大阪", "KIX"),
            Map.entry("osaka", "KIX"),
            Map.entry("首尔", "ICN"),
            Map.entry("首爾", "ICN"),
            Map.entry("seoul", "ICN"),
            Map.entry("曼谷", "BKK"),
            Map.entry("bangkok", "BKK"),
            Map.entry("新加坡", "SIN"),
            Map.entry("singapore", "SIN"),
            Map.entry("吉隆坡", "KUL"),
            Map.entry("kuala lumpur", "KUL"),
            Map.entry("洛杉矶", "LAX"),
            Map.entry("los angeles", "LAX"),
            Map.entry("纽约", "JFK"),
            Map.entry("紐約", "JFK"),
            Map.entry("new york", "JFK"),
            Map.entry("伦敦", "LHR"),
            Map.entry("倫敦", "LHR"),
            Map.entry("london", "LHR"),
            Map.entry("巴黎", "CDG"),
            Map.entry("paris", "CDG"),
            Map.entry("法兰克福", "FRA"),
            Map.entry("法蘭克福", "FRA"),
            Map.entry("frankfurt", "FRA"),
            Map.entry("悉尼", "SYD"),
            Map.entry("sydney", "SYD"),
            Map.entry("迪拜", "DXB"),
            Map.entry("dubai", "DXB"));
    private static final Map<String, String> AIRLINE_NAME_TO_CODE = Map.ofEntries(
            Map.entry("国航", "CA"),
            Map.entry("中国国航", "CA"),
            Map.entry("air china", "CA"),
            Map.entry("东航", "MU"),
            Map.entry("东方航空", "MU"),
            Map.entry("china eastern", "MU"),
            Map.entry("南航", "CZ"),
            Map.entry("南方航空", "CZ"),
            Map.entry("china southern", "CZ"),
            Map.entry("春秋", "9C"),
            Map.entry("spring airlines", "9C"),
            Map.entry("ana", "NH"),
            Map.entry("全日空", "NH"),
            Map.entry("日航", "JL"),
            Map.entry("japan airlines", "JL"),
            Map.entry("jal", "JL"),
            Map.entry("大韩", "KE"),
            Map.entry("korean air", "KE"),
            Map.entry("韩亚", "OZ"),
            Map.entry("asiana", "OZ"),
            Map.entry("新航", "SQ"),
            Map.entry("新加坡航空", "SQ"),
            Map.entry("singapore airlines", "SQ"),
            Map.entry("汉莎", "LH"),
            Map.entry("lufthansa", "LH"),
            Map.entry("英航", "BA"),
            Map.entry("british airways", "BA"),
            Map.entry("法航", "AF"),
            Map.entry("air france", "AF"),
            Map.entry("美联航", "UA"),
            Map.entry("united airlines", "UA"),
            Map.entry("达美", "DL"),
            Map.entry("delta", "DL"),
            Map.entry("美国航空", "AA"),
            Map.entry("american airlines", "AA"));

    private final Clock clock;

    public DefaultMcpParameterExtractor() {
        this.clock = Clock.systemDefaultZone();
    }

    @Override
    public McpParameterExtractionResult extract(RewriteResult rewriteResult,
                                                ResolvedIntent resolvedIntent,
                                                McpToolDescriptor toolDescriptor) {
        if (resolvedIntent == null || toolDescriptor == null) {
            return new McpParameterExtractionResult(
                    "",
                    Map.of(),
                    List.of(),
                    "INVALID_INPUT",
                    "resolved intent or tool descriptor is missing");
        }

        String toolId = toolDescriptor.toolId();
        if (!GRAPH_PATH_TOOL_ID.equals(toolId)
                && !FLIGHT_SEARCH_TOOL_ID.equals(toolId)
                && !PRICE_LOOKUP_TOOL_ID.equals(toolId)
                && !VISA_CHECK_TOOL_ID.equals(toolId)
                && !CITY_COST_TOOL_ID.equals(toolId)
                && !RISK_EVALUATE_TOOL_ID.equals(toolId)) {
            return new McpParameterExtractionResult(
                    toolId,
                    Map.of(),
                    List.of(),
                    "UNSUPPORTED_TOOL",
                    "only graph.path.search, flight.search, price.lookup, visa.check, city.cost and risk.evaluate are supported in the current MCP execution stage");
        }

        String extractionSource = resolveExtractionSource(rewriteResult, resolvedIntent);
        LinkedHashMap<String, Object> rawParameters = switch (toolId) {
            case GRAPH_PATH_TOOL_ID -> extractGraphPathParameters(extractionSource);
            case FLIGHT_SEARCH_TOOL_ID -> extractFlightSearchParameters(extractionSource);
            case PRICE_LOOKUP_TOOL_ID -> extractPriceLookupParameters(extractionSource);
            case VISA_CHECK_TOOL_ID -> extractVisaCheckParameters(extractionSource);
            case CITY_COST_TOOL_ID -> extractCityCostParameters(extractionSource);
            case RISK_EVALUATE_TOOL_ID -> extractRiskEvaluateParameters(extractionSource);
            default -> new LinkedHashMap<>();
        };

        return validateAgainstSchema(toolDescriptor, rawParameters);
    }

    private LinkedHashMap<String, Object> extractGraphPathParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        extractPrimaryRoutePair(extractionSource).ifPresent(route -> {
            rawParameters.put("origin", route.origin());
            rawParameters.put("destination", route.destination());
        });
        extractBudget(extractionSource).ifPresent(value -> rawParameters.put("maxBudget", value));
        extractStopoverDays(extractionSource).ifPresent(value -> rawParameters.put("stopoverDays", value));
        extractMaxSegments(extractionSource).ifPresent(value -> rawParameters.put("maxSegments", value));
        extractTopK(extractionSource).ifPresent(value -> rawParameters.put("topK", value));
        return rawParameters;
    }

    private LinkedHashMap<String, Object> extractFlightSearchParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        extractPrimaryRoutePair(extractionSource).ifPresent(route -> {
            rawParameters.put("origin", route.origin());
            rawParameters.put("destination", route.destination());
        });
        extractDate(extractionSource).ifPresent(value -> rawParameters.put("date", value));
        extractFlexibilityDays(extractionSource).ifPresent(value -> rawParameters.put("flexibilityDays", value));
        extractTopK(extractionSource).ifPresent(value -> rawParameters.put("topK", value));
        return rawParameters;
    }

    private LinkedHashMap<String, Object> extractPriceLookupParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        List<RoutePair> routePairs = extractAllRoutePairs(extractionSource);
        if (!routePairs.isEmpty()) {
            rawParameters.put("cityPairs", joinRoutePairs(routePairs));
        }
        extractDate(extractionSource).ifPresent(value -> rawParameters.put("date", value));
        return rawParameters;
    }

    private LinkedHashMap<String, Object> extractVisaCheckParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        String passportCountry = extractPassportCountry(extractionSource).orElse("CN");
        boolean questionMentionsPassport = extractionSource.contains("护照")
                || extractionSource.toLowerCase(Locale.ROOT).contains("passport");
        List<String> countryCodes = extractCountryCodes(extractionSource).stream()
                .filter(code -> !(questionMentionsPassport && code.equals(passportCountry)))
                .toList();
        if (!countryCodes.isEmpty()) {
            rawParameters.put("countryCodes", String.join(",", countryCodes));
        }
        extractStayDays(extractionSource).ifPresent(value -> rawParameters.put("stayDays", value));
        rawParameters.put("passportCountry", passportCountry);
        return rawParameters;
    }

    private LinkedHashMap<String, Object> extractCityCostParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        List<String> iataCodes = extractCityCostCodes(extractionSource);
        if (!iataCodes.isEmpty()) {
            rawParameters.put("iataCodes", String.join(",", iataCodes));
        }
        return rawParameters;
    }

    private LinkedHashMap<String, Object> extractRiskEvaluateParameters(String extractionSource) {
        LinkedHashMap<String, Object> rawParameters = new LinkedHashMap<>();
        extractRiskHubAirport(extractionSource).ifPresent(value -> rawParameters.put("hubAirport", value));
        List<String> airlineCodes = extractRiskAirlineCodes(extractionSource);
        if (!airlineCodes.isEmpty()) {
            rawParameters.put("firstAirline", airlineCodes.getFirst());
        }
        if (airlineCodes.size() >= 2) {
            rawParameters.put("secondAirline", airlineCodes.get(1));
        }
        extractBufferHours(extractionSource).ifPresent(value -> rawParameters.put("bufferHours", value));
        return rawParameters;
    }

    @SuppressWarnings("unchecked")
    private McpParameterExtractionResult validateAgainstSchema(McpToolDescriptor toolDescriptor, Map<String, Object> extractedParameters) {
        Map<String, Object> inputSchema = toolDescriptor.inputSchema() == null ? Map.of() : toolDescriptor.inputSchema();
        List<String> requiredFields = inputSchema.get("required") instanceof List<?> required
                ? required.stream().map(String::valueOf).toList()
                : List.of();
        Map<String, Object> properties = inputSchema.get("properties") instanceof Map<?, ?> propertySource
                ? (Map<String, Object>) propertySource
                : Map.of();

        LinkedHashMap<String, Object> validatedParameters = new LinkedHashMap<>();
        List<String> missingRequiredFields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String parameterName = entry.getKey();
            Map<String, Object> propertySchema = entry.getValue() instanceof Map<?, ?> schema
                    ? (Map<String, Object>) schema
                    : Map.of();
            Object normalizedValue = normalizeValue(toolDescriptor.toolId(), parameterName, extractedParameters.get(parameterName), propertySchema);
            if (normalizedValue == null && propertySchema.containsKey("default")) {
                normalizedValue = normalizeValue(toolDescriptor.toolId(), parameterName, propertySchema.get("default"), propertySchema);
            }

            if (normalizedValue != null) {
                validatedParameters.put(parameterName, normalizedValue);
            } else if (requiredFields.contains(parameterName)) {
                missingRequiredFields.add(parameterName);
            }
        }

        if (!missingRequiredFields.isEmpty()) {
            return new McpParameterExtractionResult(
                    toolDescriptor.toolId(),
                    validatedParameters,
                    missingRequiredFields,
                    "MISSING_REQUIRED",
                    "missing required MCP parameters: " + String.join(", ", missingRequiredFields));
        }

        String validationError = validateBusinessRules(toolDescriptor.toolId(), validatedParameters);
        if (!validationError.isBlank()) {
            return new McpParameterExtractionResult(
                    toolDescriptor.toolId(),
                    validatedParameters,
                    List.of(),
                    "INVALID_PARAMETER",
                    validationError);
        }

        return new McpParameterExtractionResult(toolDescriptor.toolId(), validatedParameters, List.of(), "READY", "");
    }

    private Object normalizeValue(String toolId, String parameterName, Object rawValue, Map<String, Object> propertySchema) {
        if (rawValue == null) {
            return null;
        }
        String type = String.valueOf(propertySchema.getOrDefault("type", "string"));
        return switch (type) {
            case "string" -> normalizeString(toolId, parameterName, rawValue);
            case "integer" -> normalizeInteger(rawValue);
            case "number" -> normalizeNumber(rawValue);
            case "boolean" -> normalizeBoolean(rawValue);
            default -> rawValue;
        };
    }

    private String normalizeString(String toolId, String parameterName, Object rawValue) {
        String value = String.valueOf(rawValue).trim();
        if (value.isBlank()) {
            return null;
        }
        return switch (parameterName) {
            case "origin", "destination" -> normalizeAirportCode(value, allowsMetroFallback(toolId));
            case "hubAirport" -> normalizeAirportCode(value, allowsMetroFallback(toolId));
            case "firstAirline", "secondAirline" -> normalizeAirlineCode(value);
            case "date" -> normalizeDateValue(value);
            case "cityPairs" -> normalizeCityPairs(value);
            case "iataCodes" -> normalizeIataCodes(value);
            case "passportCountry" -> normalizeCountryCode(value);
            case "countryCodes" -> normalizeCountryCodes(value);
            default -> value;
        };
    }

    private Integer normalizeInteger(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        try {
            String value = String.valueOf(rawValue).trim();
            return value.isBlank() ? null : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Double normalizeNumber(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String value = String.valueOf(rawValue).trim();
            return value.isBlank() ? null : Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Boolean normalizeBoolean(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String value = String.valueOf(rawValue).trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return null;
        }
        return switch (value) {
            case "true", "yes" -> Boolean.TRUE;
            case "false", "no" -> Boolean.FALSE;
            default -> null;
        };
    }

    private String validateBusinessRules(String toolId, Map<String, Object> parameters) {
        String origin = stringValue(parameters.get("origin"));
        String destination = stringValue(parameters.get("destination"));
        if (!origin.isBlank() && !destination.isBlank() && origin.equals(destination)) {
            return "origin and destination cannot be the same";
        }

        return switch (toolId) {
            case GRAPH_PATH_TOOL_ID -> validateGraphPathRules(parameters);
            case FLIGHT_SEARCH_TOOL_ID -> validateFlightSearchRules(parameters);
            case PRICE_LOOKUP_TOOL_ID -> validatePriceLookupRules(parameters);
            case VISA_CHECK_TOOL_ID -> validateVisaCheckRules(parameters);
            case CITY_COST_TOOL_ID -> validateCityCostRules(parameters);
            case RISK_EVALUATE_TOOL_ID -> validateRiskEvaluateRules(parameters);
            default -> "";
        };
    }

    private String validateGraphPathRules(Map<String, Object> parameters) {
        Double maxBudget = numberValue(parameters.get("maxBudget"));
        Integer stopoverDays = integerValue(parameters.get("stopoverDays"));
        Integer maxSegments = integerValue(parameters.get("maxSegments"));
        Integer topK = integerValue(parameters.get("topK"));

        if (maxBudget != null && maxBudget <= 0D) {
            return "maxBudget must be greater than 0";
        }
        if (stopoverDays != null && stopoverDays < 0) {
            return "stopoverDays cannot be negative";
        }
        if (maxSegments != null && (maxSegments < 1 || maxSegments > 5)) {
            return "maxSegments must be between 1 and 5";
        }
        if (topK != null && (topK < 1 || topK > 20)) {
            return "topK must be between 1 and 20";
        }
        return "";
    }

    private String validateFlightSearchRules(Map<String, Object> parameters) {
        String date = stringValue(parameters.get("date"));
        Integer flexibilityDays = integerValue(parameters.get("flexibilityDays"));
        Integer topK = integerValue(parameters.get("topK"));

        if (!isValidIsoDate(date)) {
            return "date must be in yyyy-MM-dd format";
        }
        if (flexibilityDays != null && (flexibilityDays < 0 || flexibilityDays > 7)) {
            return "flexibilityDays must be between 0 and 7";
        }
        if (topK != null && (topK < 1 || topK > 20)) {
            return "topK must be between 1 and 20";
        }
        return "";
    }

    private String validatePriceLookupRules(Map<String, Object> parameters) {
        String cityPairs = stringValue(parameters.get("cityPairs"));
        String date = stringValue(parameters.get("date"));
        List<RoutePair> routePairs = parseDelimitedRoutePairs(cityPairs);
        if (routePairs.isEmpty()) {
            return "cityPairs must use ORIGIN,DESTINATION;ORIGIN,DESTINATION format";
        }
        if (!isValidIsoDate(date)) {
            return "date must be in yyyy-MM-dd format";
        }
        boolean hasInvalidRoutePair = routePairs.stream().anyMatch(pair -> pair.origin().equals(pair.destination()));
        if (hasInvalidRoutePair) {
            return "cityPairs cannot contain the same origin and destination";
        }
        return "";
    }

    private String validateVisaCheckRules(Map<String, Object> parameters) {
        String countryCodes = stringValue(parameters.get("countryCodes"));
        String passportCountry = stringValue(parameters.get("passportCountry"));
        Integer stayDays = integerValue(parameters.get("stayDays"));
        List<String> countries = parseDelimitedCountryCodes(countryCodes);
        if (countries.isEmpty()) {
            return "countryCodes must contain at least one 2-letter ISO country code";
        }
        if (passportCountry.isBlank() || !passportCountry.matches("^[A-Z]{2}$")) {
            return "passportCountry must be a 2-letter ISO country code";
        }
        if (stayDays != null && stayDays < 0) {
            return "stayDays cannot be negative";
        }
        return "";
    }

    private String validateCityCostRules(Map<String, Object> parameters) {
        String iataCodes = stringValue(parameters.get("iataCodes"));
        List<String> codes = parseDelimitedIataCodes(iataCodes);
        if (codes.isEmpty()) {
            return "iataCodes must contain at least one 3-letter city or airport code";
        }
        return "";
    }

    private String validateRiskEvaluateRules(Map<String, Object> parameters) {
        String hubAirport = stringValue(parameters.get("hubAirport"));
        String firstAirline = stringValue(parameters.get("firstAirline"));
        String secondAirline = stringValue(parameters.get("secondAirline"));
        Double bufferHours = numberValue(parameters.get("bufferHours"));
        if (hubAirport.isBlank() || !hubAirport.matches("^[A-Z]{3}$")) {
            return "hubAirport must be a 3-letter airport code";
        }
        if (firstAirline.isBlank() || !firstAirline.matches("^[A-Z0-9]{2}$")) {
            return "firstAirline must be a 2-character airline code";
        }
        if (secondAirline.isBlank() || !secondAirline.matches("^[A-Z0-9]{2}$")) {
            return "secondAirline must be a 2-character airline code";
        }
        if (bufferHours == null || bufferHours <= 0D) {
            return "bufferHours must be greater than 0";
        }
        return "";
    }

    private Optional<RoutePair> extractPrimaryRoutePair(String question) {
        List<RoutePair> routePairs = extractAllRoutePairs(question);
        return routePairs.isEmpty() ? Optional.empty() : Optional.of(routePairs.getFirst());
    }

    private List<RoutePair> extractAllRoutePairs(String question) {
        String normalizedQuestion = question == null ? "" : question.trim().toUpperCase(Locale.ROOT);
        LinkedHashSet<RoutePair> routePairs = new LinkedHashSet<>();
        collectRoutePairs(routePairs, FROM_TO_PATTERN, normalizedQuestion);
        collectRoutePairs(routePairs, ARROW_ROUTE_PATTERN, normalizedQuestion);
        collectRoutePairs(routePairs, CSV_PAIR_PATTERN, normalizedQuestion);

        if (!routePairs.isEmpty()) {
            return List.copyOf(routePairs);
        }

        List<String> codes = extractAirportCodes(normalizedQuestion);
        if (codes.size() >= 4 && codes.size() % 2 == 0) {
            for (int index = 0; index < codes.size(); index += 2) {
                addRoutePair(routePairs, codes.get(index), codes.get(index + 1));
            }
            return List.copyOf(routePairs);
        }

        if (codes.size() == 3 && looksLikeComparisonQuestion(normalizedQuestion)) {
            addRoutePair(routePairs, codes.get(0), codes.get(1));
            addRoutePair(routePairs, codes.get(0), codes.get(2));
            return List.copyOf(routePairs);
        }

        if (codes.size() >= 2) {
            addRoutePair(routePairs, codes.getFirst(), codes.get(1));
        }
        return List.copyOf(routePairs);
    }

    private void collectRoutePairs(Set<RoutePair> routePairs, Pattern pattern, String question) {
        Matcher matcher = pattern.matcher(question);
        while (matcher.find()) {
            addRoutePair(routePairs, matcher.group(1), matcher.group(2));
        }
    }

    private void addRoutePair(Set<RoutePair> routePairs, String rawOrigin, String rawDestination) {
        String origin = normalizeAirportCode(rawOrigin, true);
        String destination = normalizeAirportCode(rawDestination, true);
        if (!origin.isBlank() && !destination.isBlank()) {
            routePairs.add(new RoutePair(origin, destination));
        }
    }

    private List<String> extractAirportCodes(String question) {
        return IATA_CODE_PATTERN.matcher(question)
                .results()
                .map(result -> normalizeAirportCode(result.group(1), true))
                .filter(code -> !code.isBlank())
                .filter(code -> !IGNORED_IATA_LIKE_TOKENS.contains(code))
                .distinct()
                .toList();
    }

    private List<String> extractCountryCodes(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        LinkedHashSet<String> countryCodes = new LinkedHashSet<>();
        String upperQuestion = normalizedQuestion.toUpperCase(Locale.ROOT);
        COUNTRY_CODE_PATTERN.matcher(upperQuestion)
                .results()
                .map(result -> normalizeCountryCode(result.group(1)))
                .filter(code -> !code.isBlank())
                .filter(code -> !IGNORED_COUNTRY_CODE_TOKENS.contains(code))
                .forEach(countryCodes::add);

        String lowerQuestion = normalizedQuestion.toLowerCase(Locale.ROOT);
        COUNTRY_NAME_TO_ISO.entrySet().stream()
                .filter(entry -> lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .forEach(countryCodes::add);

        return List.copyOf(countryCodes);
    }

    private List<String> extractCityCostCodes(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        LinkedHashSet<String> cityCodes = new LinkedHashSet<>(extractAirportCodes(normalizedQuestion.toUpperCase(Locale.ROOT)));
        String lowerQuestion = normalizedQuestion.toLowerCase(Locale.ROOT);
        CITY_NAME_TO_IATA.entrySet().stream()
                .filter(entry -> lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .forEach(cityCodes::add);
        return List.copyOf(cityCodes);
    }

    private Optional<String> extractRiskHubAirport(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        Matcher hubMatcher = HUB_AIRPORT_PATTERN.matcher(normalizedQuestion.toUpperCase(Locale.ROOT));
        if (hubMatcher.find()) {
            String normalizedCode = normalizeAirportCode(hubMatcher.group(1), true);
            if (!normalizedCode.isBlank()) {
                return Optional.of(normalizedCode);
            }
        }

        List<String> airportCodes = extractAirportCodes(normalizedQuestion.toUpperCase(Locale.ROOT));
        if (looksLikeRiskQuestion(normalizedQuestion) && airportCodes.size() >= 3) {
            return Optional.of(airportCodes.get(1));
        }
        if (!airportCodes.isEmpty()) {
            return Optional.of(airportCodes.getFirst());
        }

        String lowerQuestion = normalizedQuestion.toLowerCase(Locale.ROOT);
        return CITY_NAME_TO_IATA.entrySet().stream()
                .filter(entry -> lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private List<String> extractRiskAirlineCodes(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        LinkedHashSet<String> airlineCodes = new LinkedHashSet<>();
        String lowerQuestion = normalizedQuestion.toLowerCase(Locale.ROOT);
        AIRLINE_NAME_TO_CODE.entrySet().stream()
                .filter(entry -> lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .forEach(airlineCodes::add);

        Matcher pairMatcher = AIRLINE_PAIR_PATTERN.matcher(normalizedQuestion.toUpperCase(Locale.ROOT));
        while (pairMatcher.find()) {
            addAirlineCode(airlineCodes, pairMatcher.group(1));
            addAirlineCode(airlineCodes, pairMatcher.group(2));
        }

        Matcher airlineMatcher = AIRLINE_CODE_PATTERN.matcher(normalizedQuestion.toUpperCase(Locale.ROOT));
        while (airlineMatcher.find()) {
            addAirlineCode(airlineCodes, airlineMatcher.group(1));
        }

        return airlineCodes.stream().limit(2).toList();
    }

    private Optional<Double> extractBufferHours(String question) {
        Matcher bufferMatcher = BUFFER_HOURS_PATTERN.matcher(question);
        if (bufferMatcher.find()) {
            return parseDouble(bufferMatcher.group(1));
        }

        if (looksLikeRiskQuestion(question)) {
            Matcher genericMatcher = GENERIC_HOUR_COUNT_PATTERN.matcher(question.toLowerCase(Locale.ROOT));
            if (genericMatcher.find()) {
                return parseDouble(genericMatcher.group(1));
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> extractStayDays(String question) {
        Matcher stayMatcher = STAY_DAYS_PATTERN.matcher(question);
        if (stayMatcher.find()) {
            return parseInteger(stayMatcher.group(1));
        }

        String lowerQuestion = question.toLowerCase(Locale.ROOT);
        if (lowerQuestion.contains("签证") || lowerQuestion.contains("visa")) {
            Matcher genericMatcher = GENERIC_DAY_COUNT_PATTERN.matcher(question);
            if (genericMatcher.find()) {
                return parseInteger(genericMatcher.group(1));
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractPassportCountry(String question) {
        Matcher codeMatcher = PASSPORT_COUNTRY_CODE_PATTERN.matcher(question.toUpperCase(Locale.ROOT));
        if (codeMatcher.find()) {
            String normalized = normalizeCountryCode(codeMatcher.group(1));
            if (!normalized.isBlank()) {
                return Optional.of(normalized);
            }
        }

        String lowerQuestion = question.toLowerCase(Locale.ROOT);
        if (lowerQuestion.contains("中国护照") || lowerQuestion.contains("china passport") || lowerQuestion.contains("cn passport")) {
            return Optional.of("CN");
        }
        return COUNTRY_NAME_TO_ISO.entrySet().stream()
                .filter(entry -> lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT) + "护照")
                        || lowerQuestion.contains(entry.getKey().toLowerCase(Locale.ROOT) + " passport"))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private Optional<Double> extractBudget(String question) {
        Matcher budgetMatcher = BUDGET_PATTERN.matcher(question);
        if (budgetMatcher.find()) {
            return parseDouble(budgetMatcher.group(1));
        }
        Matcher rmbMatcher = RMB_PATTERN.matcher(question);
        if (rmbMatcher.find()) {
            return parseDouble(rmbMatcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<Integer> extractStopoverDays(String question) {
        Matcher matcher = STOPOVER_PATTERN.matcher(question);
        return matcher.find() ? parseInteger(matcher.group(1)) : Optional.empty();
    }

    private Optional<Integer> extractMaxSegments(String question) {
        if (question.contains("直飞")) {
            return Optional.of(1);
        }

        Matcher segmentMatcher = MAX_SEGMENTS_PATTERN.matcher(question);
        if (segmentMatcher.find()) {
            return parseInteger(segmentMatcher.group(1));
        }

        Matcher transferMatcher = TRANSFER_COUNT_PATTERN.matcher(question);
        if (transferMatcher.find()) {
            return parseInteger(transferMatcher.group(1)).map(transferCount -> transferCount + 1);
        }
        return Optional.empty();
    }

    private Optional<Integer> extractTopK(String question) {
        Matcher matcher = TOP_K_PATTERN.matcher(question);
        return matcher.find() ? parseInteger(matcher.group(1)) : Optional.empty();
    }

    private Optional<Integer> extractFlexibilityDays(String question) {
        Matcher matcher = FLEXIBILITY_PATTERN.matcher(question);
        return matcher.find() ? parseInteger(matcher.group(1)) : Optional.empty();
    }

    private Optional<String> extractDate(String question) {
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(question);
        if (isoMatcher.find()) {
            return formatDate(isoMatcher.group(1), isoMatcher.group(2), isoMatcher.group(3));
        }

        Matcher chineseMatcher = CHINESE_DATE_PATTERN.matcher(question);
        if (chineseMatcher.find()) {
            String year = chineseMatcher.group(1);
            String month = chineseMatcher.group(2);
            String day = chineseMatcher.group(3);
            if (year != null && !year.isBlank()) {
                return formatDate(year, month, day);
            }
            return inferCurrentYearDate(month, day);
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(clock);
        if (normalizedQuestion.contains("后天") || normalizedQuestion.contains("day after tomorrow")) {
            return Optional.of(today.plusDays(2).format(ISO_DATE_FORMATTER));
        }
        if (normalizedQuestion.contains("明天") || normalizedQuestion.contains("tomorrow")) {
            return Optional.of(today.plusDays(1).format(ISO_DATE_FORMATTER));
        }
        if (normalizedQuestion.contains("今天") || normalizedQuestion.contains("today")) {
            return Optional.of(today.format(ISO_DATE_FORMATTER));
        }
        return Optional.empty();
    }

    private Optional<String> formatDate(String year, String month, String day) {
        try {
            LocalDate date = LocalDate.of(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    Integer.parseInt(day));
            return Optional.of(date.format(ISO_DATE_FORMATTER));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<String> inferCurrentYearDate(String month, String day) {
        try {
            LocalDate today = LocalDate.now(clock);
            LocalDate candidate = LocalDate.of(today.getYear(), Integer.parseInt(month), Integer.parseInt(day));
            if (candidate.isBefore(today.minusDays(1))) {
                candidate = candidate.plusYears(1);
            }
            return Optional.of(candidate.format(ISO_DATE_FORMATTER));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<Double> parseDouble(String rawValue) {
        try {
            return Optional.of(Double.parseDouble(rawValue.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Optional<Integer> parseInteger(String rawValue) {
        try {
            return Optional.of(Integer.parseInt(rawValue.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String resolveExtractionSource(RewriteResult rewriteResult, ResolvedIntent resolvedIntent) {
        if (resolvedIntent.question() != null && !resolvedIntent.question().isBlank()) {
            return resolvedIntent.question().trim();
        }
        return rewriteResult == null ? "" : rewriteResult.routingQuestion();
    }

    private boolean allowsMetroFallback(String toolId) {
        return GRAPH_PATH_TOOL_ID.equals(toolId)
                || FLIGHT_SEARCH_TOOL_ID.equals(toolId)
                || PRICE_LOOKUP_TOOL_ID.equals(toolId)
                || CITY_COST_TOOL_ID.equals(toolId)
                || RISK_EVALUATE_TOOL_ID.equals(toolId);
    }

    private String normalizeAirportCode(String rawCode, boolean allowMetroFallback) {
        String normalizedCode = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.length() != 3) {
            return "";
        }
        return allowMetroFallback ? METRO_TO_AIRPORT.getOrDefault(normalizedCode, normalizedCode) : normalizedCode;
    }

    private String normalizeAirlineCode(String rawCode) {
        String normalizedCode = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.matches("^[A-Z0-9]{2}$")) {
            return normalizedCode;
        }
        String mapped = AIRLINE_NAME_TO_CODE.get(normalizedCode.toLowerCase(Locale.ROOT));
        return mapped == null ? "" : mapped;
    }

    private String normalizeDateValue(String rawValue) {
        Optional<String> normalized = extractDate(rawValue);
        return normalized.orElseGet(() -> {
            try {
                return LocalDate.parse(rawValue, ISO_DATE_FORMATTER).format(ISO_DATE_FORMATTER);
            } catch (DateTimeParseException exception) {
                return rawValue;
            }
        });
    }

    private String normalizeCityPairs(String rawValue) {
        List<RoutePair> routePairs = parseDelimitedRoutePairs(rawValue);
        return routePairs.isEmpty() ? rawValue : joinRoutePairs(routePairs);
    }

    private String normalizeIataCodes(String rawValue) {
        List<String> codes = parseDelimitedIataCodes(rawValue);
        return codes.isEmpty() ? rawValue : String.join(",", codes);
    }

    private String normalizeCountryCode(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() == 2) {
            return normalized;
        }
        String mapped = COUNTRY_NAME_TO_ISO.get(normalized.toLowerCase(Locale.ROOT));
        return mapped == null ? "" : mapped;
    }

    private String normalizeCountryCodes(String rawValue) {
        List<String> codes = parseDelimitedCountryCodes(rawValue);
        return codes.isEmpty() ? rawValue : String.join(",", codes);
    }

    private List<RoutePair> parseDelimitedRoutePairs(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        LinkedHashSet<RoutePair> routePairs = new LinkedHashSet<>();
        for (String candidatePair : rawValue.split(";")) {
            String[] parts = candidatePair.trim().split(",");
            if (parts.length != 2) {
                continue;
            }
            String origin = normalizeAirportCode(parts[0], true);
            String destination = normalizeAirportCode(parts[1], true);
            if (!origin.isBlank() && !destination.isBlank()) {
                routePairs.add(new RoutePair(origin, destination));
            }
        }
        return List.copyOf(routePairs);
    }

    private List<String> parseDelimitedCountryCodes(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> countryCodes = new LinkedHashSet<>();
        for (String token : rawValue.split("[,;\\s]+")) {
            String normalized = normalizeCountryCode(token);
            if (!normalized.isBlank() && !IGNORED_COUNTRY_CODE_TOKENS.contains(normalized)) {
                countryCodes.add(normalized);
            }
        }
        return List.copyOf(countryCodes);
    }

    private List<String> parseDelimitedIataCodes(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> iataCodes = new LinkedHashSet<>();
        for (String token : rawValue.split("[,;\\s]+")) {
            String normalized = normalizeAirportCode(token, true);
            if (!normalized.isBlank() && !IGNORED_IATA_LIKE_TOKENS.contains(normalized)) {
                iataCodes.add(normalized);
                continue;
            }
            String mapped = CITY_NAME_TO_IATA.get(token.trim().toLowerCase(Locale.ROOT));
            if (mapped != null && !mapped.isBlank()) {
                iataCodes.add(mapped);
            }
        }
        return List.copyOf(iataCodes);
    }

    private String joinRoutePairs(List<RoutePair> routePairs) {
        return routePairs.stream()
                .map(pair -> pair.origin() + "," + pair.destination())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private boolean looksLikeComparisonQuestion(String normalizedQuestion) {
        String lowerQuestion = normalizedQuestion.toLowerCase(Locale.ROOT);
        return lowerQuestion.contains("比价")
                || lowerQuestion.contains("比较")
                || lowerQuestion.contains("对比")
                || lowerQuestion.contains("哪个便宜")
                || lowerQuestion.contains("更便宜");
    }

    private boolean looksLikeRiskQuestion(String question) {
        String lowerQuestion = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return lowerQuestion.contains("风险")
                || lowerQuestion.contains("中转")
                || lowerQuestion.contains("转机")
                || lowerQuestion.contains("衔接")
                || lowerQuestion.contains("赶得上")
                || lowerQuestion.contains("稳不稳")
                || lowerQuestion.contains("buffer")
                || lowerQuestion.contains("risk");
    }

    private void addAirlineCode(Set<String> airlineCodes, String rawCode) {
        String normalizedCode = normalizeAirlineCode(rawCode);
        if (!normalizedCode.isBlank() && !IGNORED_AIRLINE_CODE_TOKENS.contains(normalizedCode)) {
            airlineCodes.add(normalizedCode);
        }
    }

    private boolean isValidIsoDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(rawDate, ISO_DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Double numberValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private record RoutePair(String origin, String destination) {
    }
}
