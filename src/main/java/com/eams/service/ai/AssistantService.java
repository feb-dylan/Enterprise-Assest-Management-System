package com.eams.service.ai;

import com.eams.dto.response.Responses.AssetHealthResponse;
import com.eams.dto.response.Responses.AssistantResponse;
import com.eams.dto.response.Responses.TimelineEvent;
import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetStatus;
import com.eams.entity.Employee;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import com.eams.exception.ServiceUnavailableException;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.service.DtoMapper;
import com.eams.service.InsightsService;
import com.eams.service.TimelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * "Ask EAMS" - natural-language questions about the asset data (via OpenRouter).
 *
 * The model never writes SQL. It can only call the fixed, READ-ONLY tools below,
 * which run our own repository/service code. The endpoint is restricted to
 * ADMIN and MANAGER, who are allowed to see all asset data anyway.
 *
 * Tool-calling loop: send the question, run any tools the model asks for,
 * send the results back, repeat until it answers.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    /** Each turn is one API request; free OpenRouter models allow ~50 requests/day. */
    private static final int MAX_TURNS = 6;
    private static final int MAX_ROWS = 50;

    private static final String SYSTEM_PROMPT = """
            You are "Ask EAMS", the assistant inside an Enterprise Asset Management System.
            You answer questions from administrators and managers about the organisation's
            physical assets: inventory, who holds what, damage, repairs, costs, warranties,
            and which assets should be replaced.

            Use the tools to look up facts; never invent assets, people, numbers or dates.
            If the tools return nothing relevant, say so plainly. When a question is ambiguous,
            make a sensible assumption and state it in one short sentence.

            Answer in a few sentences. When you list more than three items, use a compact
            markdown table. Money is in US dollars. Mention asset codes so the reader can
            find the asset. Today's date is %s.
            """;

    private final OpenRouterClient ai;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final TimelineService timelineService;
    private final InsightsService insightsService;
    private final TransactionTemplate readTx;
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

    public AssistantService(OpenRouterClient ai,
                            AssetRepository assetRepository,
                            AssetAssignmentRepository assignmentRepository,
                            MaintenanceRepository maintenanceRepository,
                            TimelineService timelineService,
                            InsightsService insightsService,
                            PlatformTransactionManager transactionManager) {
        this.ai = ai;
        this.assetRepository = assetRepository;
        this.assignmentRepository = assignmentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.timelineService = timelineService;
        this.insightsService = insightsService;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
    }

    public boolean isEnabled() {
        return ai.isEnabled();
    }

    // =========================================================
    // THE LOOP
    // =========================================================

    public AssistantResponse ask(String question) {
        if (!ai.isEnabled()) {
            throw new ServiceUnavailableException(
                    "The AI assistant is not configured (set OPENROUTER_API_KEY and AI_MODEL on the server)");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", String.format(SYSTEM_PROMPT, LocalDate.now())));
        messages.add(Map.of("role", "user", "content", question));

        List<String> toolsUsed = new ArrayList<>();

        try {
            for (int turn = 0; turn < MAX_TURNS; turn++) {
                // "tools" must be sent on every request, including follow-ups
                OpenRouterClient.ChatResult result = ai.chat(ai.chatModel(), messages, TOOLS, 4000);

                if (result.toolCalls().isEmpty()) {
                    String answer = result.content() == null ? "" : result.content().trim();
                    if ("length".equals(result.finishReason())) {
                        answer += "\n\n_(Answer was cut short.)_";
                    }
                    if ("content_filter".equals(result.finishReason()) && answer.isBlank()) {
                        answer = "I can't help with that request. Try asking about assets, repairs, "
                                + "warranties or costs.";
                    }
                    return new AssistantResponse(answer.isBlank()
                            ? "I could not find an answer to that." : answer, toolsUsed, true);
                }

                // Keep the model's turn (with its tool_calls) in the conversation, unchanged.
                messages.add(result.assistantMessage());

                // Run every requested tool and send each result back.
                for (OpenRouterClient.ToolCall call : result.toolCalls()) {
                    toolsUsed.add(call.name());
                    Map<String, Object> toolMessage = new LinkedHashMap<>();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", call.id());
                    toolMessage.put("content", runTool(call));
                    messages.add(toolMessage);
                }
            }
        } catch (OpenRouterClient.AiException e) {
            log.warn("Assistant call failed: HTTP {} {}", e.status(), e.getMessage());
            String message = e.status() == 429
                    ? "The free AI quota is used up for now. Please try again later."
                    : "The AI assistant is temporarily unavailable. Please try again.";
            throw new ServiceUnavailableException(message);
        }

        return new AssistantResponse(
                "That question needed too many lookups. Try asking something more specific.",
                toolsUsed, true);
    }

    // =========================================================
    // TOOLS
    // =========================================================

    private static final List<Map<String, Object>> TOOLS = List.of(
            tool("search_assets",
                    "Find assets. All filters are optional and combined with AND. Returns code, name, "
                            + "category, status, location, current holder and their department, "
                            + "purchase price and warranty expiry date.",
                    Map.of(
                            "query", Map.of("type", "string",
                                    "description", "Text matched against asset name, code, serial number or location"),
                            "status", Map.of("type", "string",
                                    "enum", List.of("AVAILABLE", "ASSIGNED", "DAMAGED", "MAINTENANCE", "RETIRED")),
                            "category", Map.of("type", "string",
                                    "description", "Category name, e.g. Laptop (partial match)"),
                            "department", Map.of("type", "string",
                                    "description", "Department of the employee currently holding the asset (partial match)"),
                            "warranty_expiring_within_days", Map.of("type", "integer",
                                    "description", "Only assets whose warranty ends within this many days from today")),
                    List.of()),
            tool("asset_history",
                    "Full lifecycle history of one asset: registration, requests, assignments, "
                            + "damage reports, repairs and status changes, oldest first.",
                    Map.of("asset_code", Map.of("type", "string", "description", "Asset code, e.g. LAP-0042")),
                    List.of("asset_code")),
            tool("repair_cost_summary",
                    "Total repair cost and number of repairs, grouped by category, asset or month. "
                            + "Cancelled jobs are excluded. Dates filter on the repair start date.",
                    Map.of(
                            "group_by", Map.of("type", "string", "enum", List.of("category", "asset", "month")),
                            "from", Map.of("type", "string", "description", "Start date, YYYY-MM-DD (optional)"),
                            "to", Map.of("type", "string", "description", "End date, YYYY-MM-DD (optional)")),
                    List.of("group_by")),
            tool("count_assets",
                    "Count assets grouped by status, category or department of the current holder.",
                    Map.of("group_by", Map.of("type", "string",
                            "enum", List.of("status", "category", "department"))),
                    List.of("group_by")),
            tool("replacement_candidates",
                    "Assets the health score recommends replacing (old, often repaired, or repairs "
                            + "costing more than half the purchase price), worst first, with the reasons.",
                    Map.of("limit", Map.of("type", "integer", "description", "Maximum rows (default 10)")),
                    List.of())
    );

    /** OpenAI-compatible tool definition: {"type":"function","function":{name, description, parameters}} */
    private static Map<String, Object> tool(String name, String description,
                                            Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        return Map.of("type", "function", "function", function);
    }

    /** Runs one tool and returns its result as a JSON string (or an error text). */
    @SuppressWarnings("unchecked")
    private String runTool(OpenRouterClient.ToolCall call) {
        try {
            Map<String, Object> input = call.arguments() == null || call.arguments().isBlank()
                    ? Map.of()
                    : json.readValue(call.arguments(), Map.class);
            Map<String, Object> args = input != null ? input : Map.of();
            Object output = readTx.execute(status -> switch (call.name()) {
                case "search_assets" -> searchAssets(args);
                case "asset_history" -> assetHistory(args);
                case "repair_cost_summary" -> repairCostSummary(args);
                case "count_assets" -> countAssets(args);
                case "replacement_candidates" -> replacementCandidates(args);
                default -> throw new IllegalArgumentException("Unknown tool: " + call.name());
            });
            return json.writeValueAsString(output);
        } catch (Exception e) {
            log.debug("Tool {} failed: {}", call.name(), e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ---------- tool implementations (read-only) ----------

    private Object searchAssets(Map<String, Object> args) {
        String query = str(args, "query");
        AssetStatus status = enumValue(AssetStatus.class, str(args, "status"));
        String category = lower(str(args, "category"));
        String department = lower(str(args, "department"));
        Integer warrantyDays = integer(args, "warranty_expiring_within_days");

        Map<Long, Employee> holders = holders();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0;
        for (Asset a : assetRepository.filter(status, null, query)) {
            Employee holder = holders.get(a.getId());
            if (category != null && (a.getCategory() == null
                    || !a.getCategory().getName().toLowerCase(Locale.ROOT).contains(category))) {
                continue;
            }
            if (department != null && (holder == null || holder.getDepartment() == null
                    || !holder.getDepartment().getName().toLowerCase(Locale.ROOT).contains(department))) {
                continue;
            }
            if (warrantyDays != null && (a.getWarrantyExpiryDate() == null
                    || a.getWarrantyExpiryDate().isBefore(today)
                    || a.getWarrantyExpiryDate().isAfter(today.plusDays(warrantyDays)))) {
                continue;
            }
            total++;
            if (rows.size() < MAX_ROWS) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", a.getAssetCode());
                row.put("name", a.getName());
                row.put("category", a.getCategory() != null ? a.getCategory().getName() : null);
                row.put("status", a.getStatus());
                row.put("location", a.getLocation());
                row.put("holder", DtoMapper.fullName(holder));
                row.put("holder_department", holder != null && holder.getDepartment() != null
                        ? holder.getDepartment().getName() : null);
                row.put("purchase_price", a.getPurchasePrice());
                row.put("warranty_expiry", a.getWarrantyExpiryDate());
                rows.add(row);
            }
        }
        return Map.of("total_matches", total, "shown", rows.size(), "assets", rows);
    }

    private Object assetHistory(Map<String, Object> args) {
        String code = str(args, "asset_code");
        if (code == null) {
            throw new IllegalArgumentException("asset_code is required");
        }
        Asset asset = assetRepository.findByAssetCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("No asset with code " + code));
        List<TimelineEvent> events = timelineService.forAsset(asset.getId());
        List<TimelineEvent> lastEvents = events.size() > 40
                ? events.subList(events.size() - 40, events.size())
                : events;
        return Map.of(
                "asset", Map.of("code", asset.getAssetCode(), "name", asset.getName(),
                        "status", asset.getStatus()),
                "events", lastEvents.stream().map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("at", e.at());
                    m.put("what", e.title());
                    m.put("details", e.description());
                    m.put("by", e.actor());
                    return m;
                }).toList());
    }

    private Object repairCostSummary(Map<String, Object> args) {
        String groupBy = str(args, "group_by") == null ? "category" : str(args, "group_by");
        LocalDate from = date(args, "from");
        LocalDate to = date(args, "to");

        Map<String, BigDecimal> cost = new TreeMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (Maintenance m : maintenanceRepository.findAll()) {
            if (m.getStatus() == MaintenanceStatus.CANCELLED) {
                continue;
            }
            LocalDate start = m.getStartDate();
            if ((from != null && start.isBefore(from)) || (to != null && start.isAfter(to))) {
                continue;
            }
            String key = switch (groupBy) {
                case "asset" -> m.getAsset().getAssetCode() + " " + m.getAsset().getName();
                case "month" -> start.getYear() + "-" + String.format("%02d", start.getMonthValue());
                default -> m.getAsset().getCategory() != null
                        ? m.getAsset().getCategory().getName() : "Uncategorised";
            };
            cost.merge(key, m.getRepairCost() != null ? m.getRepairCost() : BigDecimal.ZERO, BigDecimal::add);
            count.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> rows = cost.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(MAX_ROWS)
                .map(e -> Map.<String, Object>of("group", e.getKey(),
                        "total_cost", e.getValue(), "repairs", count.get(e.getKey())))
                .toList();
        BigDecimal total = cost.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("group_by", groupBy, "total_cost", total, "rows", rows);
    }

    private Object countAssets(Map<String, Object> args) {
        String groupBy = str(args, "group_by") == null ? "status" : str(args, "group_by");
        Map<Long, Employee> holders = "department".equals(groupBy) ? holders() : Map.of();

        Map<String, Long> counts = new TreeMap<>();
        for (Asset a : assetRepository.findAll()) {
            String key = switch (groupBy) {
                case "category" -> a.getCategory() != null ? a.getCategory().getName() : "Uncategorised";
                case "department" -> {
                    Employee holder = holders.get(a.getId());
                    yield holder != null && holder.getDepartment() != null
                            ? holder.getDepartment().getName() : "(not assigned)";
                }
                default -> a.getStatus().name();
            };
            counts.merge(key, 1L, Long::sum);
        }
        return Map.of("group_by", groupBy, "counts", counts);
    }

    private Object replacementCandidates(Map<String, Object> args) {
        Integer limit = integer(args, "limit");
        List<AssetHealthResponse> list = insightsService.replacementCandidates(limit != null ? limit : 10);
        return list.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", h.assetCode());
            m.put("name", h.assetName());
            m.put("category", h.categoryName());
            m.put("health_score", h.score());
            m.put("age_years", h.ageYears());
            m.put("repairs", h.repairCount());
            m.put("total_repair_cost", h.totalRepairCost());
            m.put("purchase_price", h.purchasePrice());
            m.put("reasons", h.reasons());
            return m;
        }).toList();
    }

    // ---------- helpers ----------

    /** assetId -> employee currently holding it */
    private Map<Long, Employee> holders() {
        Map<Long, Employee> map = new HashMap<>();
        assignmentRepository.findAll().stream()
                .filter(a -> AssetAssignmentRepository.HOLDING.contains(a.getStatus()))
                .sorted(Comparator.comparing(AssetAssignment::getId))
                .forEach(a -> map.put(a.getAsset().getId(), a.getEmployee()));
        return map;
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    private static Integer integer(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v instanceof Number n) {
            return Math.max(1, Math.min(n.intValue(), 3650));
        }
        if (v instanceof String s && !s.isBlank()) {
            return Math.max(1, Math.min(Integer.parseInt(s.trim()), 3650));
        }
        return null;
    }

    private static LocalDate date(Map<String, Object> args, String key) {
        String s = str(args, key);
        if (s == null) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(key + " must be YYYY-MM-DD");
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName() + ": " + value);
        }
    }
}
