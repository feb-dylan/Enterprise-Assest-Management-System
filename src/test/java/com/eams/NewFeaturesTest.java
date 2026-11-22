package com.eams;

import com.eams.entity.User;
import com.eams.service.ScheduledAlertsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uploads, invitations / password reset, QR scan, labels, notifications,
 * insights, stock-take audits, AI endpoints when no API key is configured.
 */
class NewFeaturesTest extends IntegrationTestBase {

    /** Smallest valid PNG header bytes (magic number + padding). */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Autowired
    private ScheduledAlertsService scheduledAlertsService;

    @Test
    void imageUploadChecksRealFileContentNotTheClaimedType() throws Exception {
        String admin = login(createUser("ADMIN"));
        long assetId = createAsset(admin, createCategory(admin), 500);

        // HTML pretending to be a PNG -> rejected
        MockMultipartFile fake = new MockMultipartFile("file", "evil.html", "image/png",
                "<script>alert(1)</script>".getBytes());
        mvc.perform(multipart("/api/assets/" + assetId + "/image").file(fake)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());

        // real PNG with a misleading name -> stored as .png
        MockMultipartFile png = new MockMultipartFile("file", "photo.html", "text/html", PNG);
        mvc.perform(multipart("/api/assets/" + assetId + "/image").file(png)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("/uploads/assets/")))
                .andExpect(content().string(containsString(".png")));
    }

    @Test
    void damagePhotosAreStoredAndAiIsReportedDisabledWithoutKey() throws Exception {
        String admin = login(createUser("ADMIN"));
        String manager = login(createUser("MANAGER"));
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long assetId = createAsset(admin, createCategory(admin), 700);
        long req = id(doPost(emp, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"work\"}"));
        doPut(manager, "/api/requests/" + req + "/approve", null);
        doPut(admin, "/api/requests/" + req + "/assign", null);
        long damageId = id(doPost(emp, "/api/damage", "{\"assetId\":" + assetId + ",\"description\":\"cracked\"}"));

        mvc.perform(multipart("/api/damage/" + damageId + "/photos")
                        .file(new MockMultipartFile("files", "a.png", "image/png", PNG))
                        .file(new MockMultipartFile("files", "b.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + emp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrls.length()").value(2))
                .andExpect(jsonPath("$.aiStatus").value("DISABLED"));

        // manual AI triage without an API key -> 503 with a clear message
        doPost(admin, "/api/damage/" + damageId + "/ai-triage", null)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("OPENROUTER_API_KEY")));
    }

    @Test
    void assistantReportsDisabledWithoutKey() throws Exception {
        String admin = login(createUser("ADMIN"));
        doGet(admin, "/api/assistant/status")
                .andExpect(jsonPath("$.assistantEnabled").value(false));
        doPost(admin, "/api/assistant/ask", "{\"question\":\"How many laptops?\"}")
                .andExpect(status().isServiceUnavailable());
        // employees cannot use the assistant at all
        doPost(login(createUser("EMPLOYEE")), "/api/assistant/ask", "{\"question\":\"hi\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteFlowSetsPasswordAndLinkIsSingleUse() throws Exception {
        String admin = login(createUser("ADMIN"));
        String email = unique("newtech") + "@test.local";

        String link = json(doPost(admin, "/api/users/invite",
                "{\"email\":\"" + email + "\",\"role\":\"TECHNICIAN\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("TECHNICIAN")), "$.inviteLink");
        String token = link.substring(link.indexOf("token=") + 6, link.indexOf("&invite"));

        String body = "{\"token\":\"" + token + "\",\"newPassword\":\"Welcome2026!\"}";
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Welcome2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"));
    }

    @Test
    void forgotPasswordNeverRevealsWhetherEmailExists() throws Exception {
        String a = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + createUser("EMPLOYEE").getEmail() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String b = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody-" + unique("x") + "@test.local\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void adminCannotDisableThemselvesButCanDisableOthers() throws Exception {
        User adminUser = createUser("ADMIN");
        String admin = login(adminUser);
        User other = createUser("EMPLOYEE");

        doPatch(admin, "/api/users/" + adminUser.getId(), "{\"enabled\":false}")
                .andExpect(status().isBadRequest());
        doPatch(admin, "/api/users/" + other.getId(), "{\"enabled\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void qrScanShowsActionsForTheCurrentUser() throws Exception {
        String admin = login(createUser("ADMIN"));
        long assetId = createAsset(admin, createCategory(admin), 400);
        String code = assetCode(admin, assetId);

        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);

        doGet(login(empUser), "/api/assets/code/" + code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedActions", hasItem("REQUEST_ASSET")))
                .andExpect(jsonPath("$.heldByMe").value(false));
        doGet(admin, "/api/assets/code/" + code)
                .andExpect(jsonPath("$.allowedActions", hasItem("PRINT_LABEL")));

        // QR code image + label sheet
        doGet(admin, "/api/assets/" + assetId + "/qr-code")
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        doGet(admin, "/api/assets/labels/pdf?ids=" + assetId)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void notificationsAreCreatedForWorkflowEvents() throws Exception {
        String admin = login(createUser("ADMIN"));
        User managerUser = createUser("MANAGER");
        String manager = login(managerUser);
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long assetId = createAsset(admin, createCategory(admin), 400);
        long req = id(doPost(emp, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"work\"}"));

        List<String> managerTypes = json(doGet(manager, "/api/notifications"), "$[*].type");
        assertThat(managerTypes).contains("REQUEST_CREATED");

        doPut(manager, "/api/requests/" + req + "/approve", null);
        doGet(emp, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(1));
        doPut(emp, "/api/notifications/read-all", null).andExpect(status().isOk());
        doGet(emp, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void warrantyExpiringAssetsAreListedAndAlertsAreNotRepeated() throws Exception {
        String admin = login(createUser("ADMIN"));
        long cat = createCategory(admin);
        String code = unique("WAR");
        doPost(admin, "/api/assets", """
                {"assetCode":"%s","name":"Projector","categoryId":%d,"purchasePrice":500,
                 "purchaseDate":"2024-01-01","warrantyExpiryDate":"%s"}
                """.formatted(code, cat, java.time.LocalDate.now().plusDays(10)))
                .andExpect(status().isCreated());

        doGet(admin, "/api/assets/warranty-expiring?days=30")
                .andExpect(jsonPath("$[*].assetCode", hasItem(code)));

        int first = scheduledAlertsService.warrantyAlerts();
        int second = scheduledAlertsService.warrantyAlerts();
        assertThat(first).isGreaterThan(0);
        assertThat(second).isZero();
    }

    @Test
    void insightsEndpointsWork() throws Exception {
        String admin = login(createUser("ADMIN"));
        createAsset(admin, createCategory(admin), 1500);

        doGet(admin, "/api/insights/summary")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets").exists())
                .andExpect(jsonPath("$.assetsByCategory").isArray());
        doGet(admin, "/api/insights/replacement-candidates").andExpect(status().isOk());
        doGet(admin, "/api/insights/idle-assets?days=0").andExpect(status().isOk());
        doGet(login(createUser("EMPLOYEE")), "/api/insights/summary").andExpect(status().isForbidden());
    }

    @Test
    void stockTakeFindsMissingAndMisplacedAssets() throws Exception {
        String admin = login(createUser("ADMIN"));
        String tech = login(createUser("TECHNICIAN"));
        long cat = createCategory(admin);
        String location = unique("Room");

        String[] codes = new String[3];
        for (int i = 0; i < 3; i++) {
            codes[i] = unique("STK");
            doPost(admin, "/api/assets", """
                    {"assetCode":"%s","name":"Chair","categoryId":%d,"location":"%s"}
                    """.formatted(codes[i], cat, location)).andExpect(status().isCreated());
        }

        long campaign = id(doPost(admin, "/api/audits",
                "{\"name\":\"Q4 audit\",\"location\":\"" + location + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expected").value(3)));

        doPost(tech, "/api/audits/" + campaign + "/scans", "{\"assetCode\":\"" + codes[0] + "\"}")
                .andExpect(jsonPath("$.result").value("FOUND"));
        doPost(tech, "/api/audits/" + campaign + "/scans",
                "{\"assetCode\":\"" + codes[1] + "\",\"scannedLocation\":\"Basement\"}")
                .andExpect(jsonPath("$.result").value("WRONG_LOCATION"));

        doPost(admin, "/api/audits/" + campaign + "/close", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(1))
                .andExpect(jsonPath("$.wrongLocation").value(1))
                .andExpect(jsonPath("$.missing").value(1))
                .andExpect(jsonPath("$.missingAssets[0].assetCode").value(codes[2]));

        // closed campaigns cannot be scanned
        doPost(tech, "/api/audits/" + campaign + "/scans", "{\"assetCode\":\"" + codes[2] + "\"}")
                .andExpect(status().isBadRequest());

        doGet(admin, "/api/audit-log?limit=20").andExpect(status().isOk());
    }

    @Test
    void retireAssetClosesOpenRequests() throws Exception {
        String admin = login(createUser("ADMIN"));
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long assetId = createAsset(admin, createCategory(admin), 300);
        long req = id(doPost(emp, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"work\"}"));

        doPut(admin, "/api/assets/" + assetId + "/retire", "{\"disposalMethod\":\"DONATE\",\"note\":\"School\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"))
                .andExpect(jsonPath("$.disposalMethod").value("DONATE"));

        doGet(emp, "/api/requests/" + req)
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Asset was retired"));
    }
}
