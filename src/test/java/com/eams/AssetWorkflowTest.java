package com.eams;

import com.eams.entity.Employee;
import com.eams.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The full asset lifecycle, end to end, through the real REST API.
 * Main focus: review item C4 (asset status must never get out of sync).
 */
class AssetWorkflowTest extends IntegrationTestBase {

    @Test
    void fullLifecycleKeepsAssetStatusConsistent() throws Exception {
        String admin = login(createUser("ADMIN"));
        String manager = login(createUser("MANAGER"));
        User techUser = createUser("TECHNICIAN");
        String tech = login(techUser);

        User aliceUser = createUser("EMPLOYEE");
        Employee alice = createEmployeeProfile(aliceUser);
        String aliceToken = login(aliceUser);
        User bobUser = createUser("EMPLOYEE");
        createEmployeeProfile(bobUser);
        String bobToken = login(bobUser);

        long categoryId = createCategory(admin);
        long assetId = createAsset(admin, categoryId, 1200);
        assertStatus(admin, assetId, "AVAILABLE");

        // --- both employees request the same asset ---
        long aliceRequest = id(doPost(aliceToken, "/api/requests",
                "{\"assetId\":" + assetId + ",\"reason\":\"Month-end closing\",\"priority\":\"HIGH\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assetCode").exists()));
        long bobRequest = id(doPost(bobToken, "/api/requests",
                "{\"assetId\":" + assetId + ",\"reason\":\"Design work\"}")
                .andExpect(status().isCreated()));

        // duplicate open request by the same employee is refused
        doPost(aliceToken, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"again\"}")
                .andExpect(status().isConflict());

        // employees cannot see each other's requests
        doGet(bobToken, "/api/requests/" + aliceRequest).andExpect(status().isForbidden());

        // --- manager approves Alice, admin assigns ---
        doPut(manager, "/api/requests/" + aliceRequest + "/approve", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        long assignmentId = id(doPut(admin, "/api/requests/" + aliceRequest + "/assign", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").exists()));
        assertStatus(admin, assetId, "ASSIGNED");

        // Bob's competing request was closed automatically
        doGet(bobToken, "/api/requests/" + bobRequest)
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason", containsString("another employee")));

        // --- damage: only the holder may report it ---
        doPost(bobToken, "/api/damage",
                "{\"assetId\":" + assetId + ",\"description\":\"broken\"}")
                .andExpect(status().isForbidden());
        long damageId = id(doPost(aliceToken, "/api/damage",
                "{\"employeeId\":999,\"assetId\":" + assetId + ",\"description\":\"Screen cracked\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(alice.getId().intValue())));
        assertStatus(admin, assetId, "DAMAGED");

        // --- technician starts the repair ---
        doPut(tech, "/api/damage/" + damageId, "{\"status\":\"REPAIRING\"}").andExpect(status().isOk());
        assertStatus(admin, assetId, "MAINTENANCE");

        long maintenanceId = id(doPost(tech, "/api/maintenance", """
                {"assetId":%d,"description":"Replace display","startDate":"2026-01-10"}
                """.formatted(assetId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technicianUserId").value(techUser.getId().intValue())));

        // cannot resolve the damage while the repair job is still open
        doPut(tech, "/api/damage/" + damageId, "{\"status\":\"RESOLVED\"}").andExpect(status().isConflict());

        // invalid maintenance transition is refused
        doPut(tech, "/api/maintenance/" + maintenanceId, "{\"status\":\"SCHEDULED\"}")
                .andExpect(status().isOk());   // same status = no transition

        // --- repair completed: damage resolved, asset goes BACK TO ALICE (not AVAILABLE!) ---
        doPut(tech, "/api/maintenance/" + maintenanceId,
                "{\"status\":\"COMPLETED\",\"repairCost\":150}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate").exists());
        doGet(admin, "/api/damage/" + damageId).andExpect(jsonPath("$.status").value("RESOLVED"));
        assertStatus(admin, assetId, "ASSIGNED");

        // completed is final
        doPut(tech, "/api/maintenance/" + maintenanceId, "{\"status\":\"IN_PROGRESS\"}")
                .andExpect(status().isBadRequest());

        // --- admin cannot set workflow statuses by hand ---
        String code = assetCode(admin, assetId);
        doPut(admin, "/api/assets/" + assetId, """
                {"assetCode":"%s","name":"Test Laptop","categoryId":%d,"status":"AVAILABLE"}
                """.formatted(code, categoryId))
                .andExpect(status().isBadRequest());

        // --- cannot delete or retire an asset someone holds ---
        doDelete(admin, "/api/assets/" + assetId).andExpect(status().isConflict());

        // --- return ---
        doPut(aliceToken, "/api/requests/assignments/" + assignmentId + "/return-request",
                "{\"returnNote\":\"Leaving the project\"}")
                .andExpect(jsonPath("$.status").value("RETURN_REQUESTED"));
        assertStatus(admin, assetId, "ASSIGNED");
        doPut(admin, "/api/requests/assignments/" + assignmentId + "/return", "{}")
                .andExpect(jsonPath("$.status").value("RETURNED"));
        assertStatus(admin, assetId, "AVAILABLE");

        // --- timeline + health ---
        List<String> types = json(doGet(admin, "/api/assets/" + assetId + "/timeline")
                .andExpect(status().isOk()), "$[*].type");
        assertThat(types).contains("REGISTERED", "REQUESTED", "REQUEST_APPROVED", "ASSIGNED",
                "DAMAGE_REPORTED", "MAINTENANCE_COMPLETED", "RETURNED", "STATUS_CHANGED");

        doGet(admin, "/api/assets/" + assetId + "/health")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repairCount").value(1))
                .andExpect(jsonPath("$.totalRepairCost").value(150.0));

        // --- delete with history = soft delete (retired) ---
        doDelete(admin, "/api/assets/" + assetId)
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(content.getResponse().getContentAsString())
                        .contains("retired instead of deleted"));
        assertStatus(admin, assetId, "RETIRED");

        // --- dashboards use counts ---
        doGet(admin, "/api/dashboard/admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets", greaterThan(0)));
        doGet(aliceToken, "/api/dashboard/employee")
                .andExpect(jsonPath("$.returnedAssignments").value(1));
    }

    @Test
    void cancelledMaintenanceDoesNotLeaveAssetStuckInMaintenance() throws Exception {
        String admin = login(createUser("ADMIN"));
        String manager = login(createUser("MANAGER"));
        String tech = login(createUser("TECHNICIAN"));
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long assetId = createAsset(admin, createCategory(admin), 800);
        long req = id(doPost(emp, "/api/requests", "{\"assetId\":" + assetId + ",\"reason\":\"work\"}"));
        doPut(manager, "/api/requests/" + req + "/approve", null).andExpect(status().isOk());
        doPut(admin, "/api/requests/" + req + "/assign", null).andExpect(status().isOk());

        long damageId = id(doPost(emp, "/api/damage", "{\"assetId\":" + assetId + ",\"description\":\"fan noise\"}"));
        doPut(tech, "/api/damage/" + damageId, "{\"status\":\"REPAIRING\"}").andExpect(status().isOk());
        long jobId = id(doPost(tech, "/api/maintenance",
                "{\"assetId\":" + assetId + ",\"description\":\"check fan\",\"startDate\":\"2026-01-01\"}"));
        assertStatus(admin, assetId, "MAINTENANCE");

        doPut(tech, "/api/maintenance/" + jobId, "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());

        // damage goes back to review -> asset is DAMAGED, not stuck in MAINTENANCE
        doGet(admin, "/api/damage/" + damageId).andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        assertStatus(admin, assetId, "DAMAGED");

        // resolving without repair puts it back with the employee
        doPut(tech, "/api/damage/" + damageId, "{\"status\":\"RESOLVED\"}").andExpect(status().isOk());
        assertStatus(admin, assetId, "ASSIGNED");
    }

    @Test
    void categoryRequestLetsAdminPickTheUnit() throws Exception {
        String admin = login(createUser("ADMIN"));
        String manager = login(createUser("MANAGER"));
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        String emp = login(empUser);

        long laptops = createCategory(admin);
        long otherCategory = createCategory(admin);
        long laptop = createAsset(admin, laptops, 1000);
        long otherAsset = createAsset(admin, otherCategory, 300);

        long req = id(doPost(emp, "/api/requests",
                "{\"categoryId\":" + laptops + ",\"reason\":\"Any laptop is fine\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").doesNotExist()));
        doPut(manager, "/api/requests/" + req + "/approve", null).andExpect(status().isOk());

        doPut(admin, "/api/requests/" + req + "/assign", null).andExpect(status().isBadRequest());
        doPut(admin, "/api/requests/" + req + "/assign?assetId=" + otherAsset, null)
                .andExpect(status().isBadRequest());
        doPut(admin, "/api/requests/" + req + "/assign?assetId=" + laptop, null)
                .andExpect(status().isOk());
        assertStatus(admin, laptop, "ASSIGNED");
    }

    @Test
    void requestNeedsAssetOrCategory() throws Exception {
        User empUser = createUser("EMPLOYEE");
        createEmployeeProfile(empUser);
        doPost(login(empUser), "/api/requests", "{\"reason\":\"something\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("assetId or categoryId")));
    }

    private void assertStatus(String token, long assetId, String expected) throws Exception {
        doGet(token, "/api/assets/" + assetId).andExpect(jsonPath("$.status").value(expected));
    }
}
