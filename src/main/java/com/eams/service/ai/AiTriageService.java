package com.eams.service.ai;

import com.eams.entity.AiTriageStatus;
import com.eams.entity.DamagePhoto;
import com.eams.entity.DamageReport;
import com.eams.repository.DamageReportRepository;
import com.eams.service.FileStorageService;
import com.eams.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AI damage triage: a vision model (via OpenRouter) looks at the damage photo(s)
 * plus the employee's description and suggests type, severity, action, cost range
 * and safety risk. A technician still decides; the AI only suggests.
 *
 * Runs in a background thread after the photo upload is committed.
 * Free models do not reliably support strict JSON mode, so the prompt asks for
 * JSON and the answer is parsed defensively (every field validated).
 */
@Service
public class AiTriageService {

    private static final Logger log = LoggerFactory.getLogger(AiTriageService.class);
    private static final int MAX_IMAGES = 3;

    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> ACTIONS = Set.of("REPAIR", "REPLACE", "INSPECT", "NO_ACTION");

    private static final String SYSTEM_PROMPT = """
            You assess photos of damaged company equipment (laptops, monitors, printers,
            phones, furniture, vehicles, network gear) for an asset-management system.
            A technician will review your assessment, so be specific and practical.

            Severity: LOW = cosmetic only, works normally; MEDIUM = partly impaired, usable
            with workarounds; HIGH = core function broken or unusable; CRITICAL = unusable
            AND a safety risk (exposed wiring, swollen battery, sharp broken glass, burn
            marks, leaking fluid).

            Suggested action: REPAIR when a repair is reasonable, REPLACE when repair is
            unlikely to be worth it, INSPECT when the photo is unclear, NO_ACTION when no
            real damage is visible. If the photo does not show the described damage, say
            so in the summary and choose INSPECT.

            Reply with ONLY one JSON object, no markdown, exactly these keys:
            {"damageType": "SHORT_UPPER_SNAKE_CASE e.g. CRACKED_SCREEN",
             "severity": "LOW|MEDIUM|HIGH|CRITICAL",
             "suggestedAction": "REPAIR|REPLACE|INSPECT|NO_ACTION",
             "summary": "one or two sentences for a technician",
             "estimatedCostMin": number (USD),
             "estimatedCostMax": number (USD),
             "safetyRisk": true|false}
            """;

    /** Validated result. */
    public record TriageResult(String damageType, String severity, String suggestedAction,
                               String summary, BigDecimal costMin, BigDecimal costMax,
                               boolean safetyRisk) {}

    private final OpenRouterClient ai;
    private final DamageReportRepository damageReportRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final TransactionTemplate tx;
    private final JsonMapper json = JsonMapper.builder().build();

    public AiTriageService(OpenRouterClient ai,
                           DamageReportRepository damageReportRepository,
                           FileStorageService fileStorageService,
                           NotificationService notificationService,
                           PlatformTransactionManager transactionManager) {
        this.ai = ai;
        this.damageReportRepository = damageReportRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public boolean isEnabled() {
        return ai.isEnabled();
    }

    /** Fire-and-forget: analyse the report in the background. */
    @Async
    public void analyzeAsync(Long damageReportId) {
        analyze(damageReportId);
    }

    public void analyze(Long damageReportId) {
        if (!ai.isEnabled()) {
            save(damageReportId, r -> r.setAiStatus(AiTriageStatus.DISABLED));
            return;
        }

        // 1. Load what we need (short transaction)
        Input input = tx.execute(status -> loadInput(damageReportId));
        if (input == null || input.images().isEmpty()) {
            log.info("Damage report {} has no readable photo - skipping AI triage", damageReportId);
            return;
        }

        // 2. Call the model (outside any DB transaction)
        try {
            TriageResult result = callModel(input);
            save(damageReportId, r -> {
                r.setAiStatus(AiTriageStatus.COMPLETED);
                r.setAiDamageType(result.damageType());
                r.setAiSeverity(result.severity());
                r.setAiSuggestedAction(result.suggestedAction());
                r.setAiSummary(result.summary());
                r.setAiCostMin(result.costMin());
                r.setAiCostMax(result.costMax());
                r.setAiSafetyRisk(result.safetyRisk());
                r.setAiAnalyzedAt(LocalDateTime.now());

                if ("CRITICAL".equals(result.severity()) || result.safetyRisk()) {
                    notificationService.notifyRole("TECHNICIAN", "DAMAGE_CRITICAL",
                            "Critical damage reported",
                            r.getAsset().getAssetCode() + " - " + r.getAsset().getName()
                                    + ": " + result.summary(),
                            "/damage/" + r.getId());
                }
            });
            log.info("AI triage completed for damage report {}: {}", damageReportId, result.severity());
        } catch (OpenRouterClient.AiException e) {
            log.warn("AI triage failed for damage report {}: HTTP {} {}",
                    damageReportId, e.status(), e.getMessage());
            save(damageReportId, r -> r.setAiStatus(AiTriageStatus.FAILED));
        } catch (Exception e) {
            log.warn("AI triage failed for damage report {}: {}", damageReportId, e.getMessage());
            save(damageReportId, r -> r.setAiStatus(AiTriageStatus.FAILED));
        }
    }

    // ---------------------------------------------------------

    private record Image(String dataUrl) {}

    private record Input(String assetName, String categoryName, String description, List<Image> images) {}

    private Input loadInput(Long id) {
        DamageReport report = damageReportRepository.findById(id).orElse(null);
        if (report == null) {
            return null;
        }
        List<Image> images = new ArrayList<>();
        for (DamagePhoto photo : report.getPhotos()) {
            if (images.size() >= MAX_IMAGES) {
                break;
            }
            Path path = fileStorageService.resolveUrl(photo.getUrl());
            if (path == null || !Files.isRegularFile(path)) {
                continue;
            }
            try {
                String mime = photo.getContentType() != null ? photo.getContentType() : "image/jpeg";
                images.add(new Image("data:" + mime + ";base64,"
                        + Base64.getEncoder().encodeToString(Files.readAllBytes(path))));
            } catch (Exception e) {
                log.warn("Could not read photo {}: {}", photo.getUrl(), e.getMessage());
            }
        }
        if (!images.isEmpty()) {
            report.setAiStatus(AiTriageStatus.PENDING);
        }
        return new Input(
                report.getAsset().getName(),
                report.getAsset().getCategory() != null ? report.getAsset().getCategory().getName() : "Unknown",
                report.getDescription(),
                images);
    }

    private TriageResult callModel(Input input) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text",
                "Asset: " + input.assetName() + " (category: " + input.categoryName() + ")\n"
                        + "Employee's description of the damage:\n" + input.description()));
        for (Image image : input.images()) {
            content.add(Map.of("type", "image_url", "image_url", Map.of("url", image.dataUrl())));
        }

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", content));

        OpenRouterClient.ChatResult result = ai.chat(ai.visionModel(), messages, null, 1000);
        if (result.content() == null || result.content().isBlank()) {
            throw new IllegalStateException("The model returned an empty assessment");
        }
        return parse(result.content());
    }

    /** Extracts the JSON object from the reply and validates every field. */
    @SuppressWarnings("unchecked")
    TriageResult parse(String reply) {
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("The model did not return JSON");
        }
        Map<String, Object> m = json.readValue(reply.substring(start, end + 1), Map.class);

        String severity = upper(m.get("severity"));
        if (!SEVERITIES.contains(severity)) {
            throw new IllegalStateException("Invalid severity: " + severity);
        }
        String action = upper(m.get("suggestedAction"));
        if (!ACTIONS.contains(action)) {
            action = "INSPECT";
        }

        BigDecimal min = money(m.get("estimatedCostMin"));
        BigDecimal max = money(m.get("estimatedCostMax"));
        if (min.compareTo(max) > 0) {
            BigDecimal t = min;
            min = max;
            max = t;
        }

        Object risk = m.get("safetyRisk");
        boolean safetyRisk = risk instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(risk));

        String damageType = upper(m.get("damageType"));
        return new TriageResult(
                truncate(damageType == null ? "UNKNOWN" : damageType.replaceAll("[^A-Z0-9_]", "_"), 50),
                severity,
                action,
                truncate(m.get("summary") == null ? "" : String.valueOf(m.get("summary")).trim(), 1000),
                min, max, safetyRisk);
    }

    private void save(Long id, java.util.function.Consumer<DamageReport> change) {
        tx.executeWithoutResult(status -> damageReportRepository.findById(id).ifPresent(change));
    }

    private static String upper(Object value) {
        return value == null ? null : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private static BigDecimal money(Object value) {
        try {
            double d = value instanceof Number n ? n.doubleValue()
                    : Double.parseDouble(String.valueOf(value).replaceAll("[^0-9.]", ""));
            return BigDecimal.valueOf(Math.max(0, d)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2);
        }
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
