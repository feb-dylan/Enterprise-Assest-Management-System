package com.eams;

import com.eams.entity.Employee;
import com.eams.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Review items C1, C2, C3, H1, H2, H6.
 */
class SecurityAndErrorHandlingTest extends IntegrationTestBase {

    // ---------------- C3: error responses ----------------

    @Test
    void notFoundReturns404WithJsonMessage() throws Exception {
        String admin = login(createUser("ADMIN"));
        doGet(admin, "/api/assets/999999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Asset not found")));
    }

    @Test
    void validationErrorReturns400WithStringMessageAndFieldMap() throws Exception {
        String admin = login(createUser("ADMIN"));
        doPost(admin, "/api/categories", "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", instanceOf(String.class)))
                .andExpect(jsonPath("$.messages.name").exists());
    }

    @Test
    void categoryDescriptionLongerThanColumnIsRejectedNotA500() throws Exception {
        String admin = login(createUser("ADMIN"));
        doPost(admin, "/api/categories",
                "{\"name\":\"" + unique("Cat") + "\",\"description\":\"" + "x".repeat(300) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordIs401NotA500() throws Exception {
        User user = createUser("EMPLOYEE");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void invalidEnumInPathIs400() throws Exception {
        String admin = login(createUser("ADMIN"));
        doGet(admin, "/api/assets/status/BROKEN").andExpect(status().isBadRequest());
    }

    // ---------------- authentication ----------------

    @Test
    void noTokenGives401Json() throws Exception {
        doGet(null, "/api/assets")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("log in")));
    }

    @Test
    void unknownEndpointIsDeniedByDefault() throws Exception {
        String admin = login(createUser("ADMIN"));
        doGet(admin, "/api/some-new-endpoint").andExpect(status().isForbidden());
    }

    @Test
    void disabledUserTokenStopsWorkingImmediately() throws Exception {   // H1
        User user = createUser("EMPLOYEE");
        String token = login(user);
        doGet(token, "/api/auth/me").andExpect(status().isOk());

        user.setEnabled(false);
        userRepository.save(user);

        doGet(token, "/api/auth/me").andExpect(status().isUnauthorized());
    }

    @Test
    void oldTokenIsRevokedAfterPasswordChange() throws Exception {
        User user = createUser("EMPLOYEE");
        String oldToken = login(user);

        String newToken = json(doPut(oldToken, "/api/auth/change-password",
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPassword456!\"}")
                .andExpect(status().isOk()), "$.token");

        doGet(oldToken, "/api/auth/me").andExpect(status().isUnauthorized());
        doGet(newToken, "/api/auth/me").andExpect(status().isOk());
    }

    @Test
    void loginIsLockedAfterRepeatedFailures() throws Exception {   // H6
        User user = createUser("EMPLOYEE");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"bad\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void disabledAccountWithWrongPasswordLooksLikeBadCredentials() throws Exception {   // H2
        User user = createUser("EMPLOYEE");
        user.setEnabled(false);
        userRepository.save(user);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- C2: sub-paths are protected ----------------

    @Test
    void employeeCannotUploadAssetImage() throws Exception {
        String admin = login(createUser("ADMIN"));
        long assetId = createAsset(admin, createCategory(admin), 1000);
        String employee = login(createUser("EMPLOYEE"));

        mvc.perform(multipart("/api/assets/" + assetId + "/image")
                        .file("file", new byte[]{1, 2, 3})
                        .header("Authorization", "Bearer " + employee))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotListEmployeesThroughDepartmentEndpoint() throws Exception {
        User admin = createUser("ADMIN");
        Employee someone = createEmployeeProfile(createUser("EMPLOYEE"));
        String employee = login(createUser("EMPLOYEE"));

        doGet(employee, "/api/departments/" + someone.getDepartment().getId() + "/employees")
                .andExpect(status().isForbidden());
        doGet(login(admin), "/api/departments/" + someone.getDepartment().getId() + "/employees")
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotCreateAsset() throws Exception {
        String employee = login(createUser("EMPLOYEE"));
        doPost(employee, "/api/assets", "{}").andExpect(status().isForbidden());
    }

    // ---------------- C1: cannot act as someone else ----------------

    @Test
    void employeeCannotReadAnotherEmployeesRequestsOrDashboard() throws Exception {
        User aliceUser = createUser("EMPLOYEE");
        createEmployeeProfile(aliceUser);
        Employee bob = createEmployeeProfile(createUser("EMPLOYEE"));
        String alice = login(aliceUser);

        doGet(alice, "/api/requests/my/" + bob.getId()).andExpect(status().isForbidden());
        doGet(alice, "/api/requests/assignments/my/" + bob.getId()).andExpect(status().isForbidden());
        doGet(alice, "/api/damage/my/" + bob.getId()).andExpect(status().isForbidden());
        doGet(alice, "/api/dashboard/employee/" + bob.getId()).andExpect(status().isForbidden());

        // her own data via the new id-free URLs
        doGet(alice, "/api/requests/my").andExpect(status().isOk());
        doGet(alice, "/api/dashboard/employee").andExpect(status().isOk());
    }

    @Test
    void requestIsAlwaysCreatedForTheLoggedInEmployee() throws Exception {
        String admin = login(createUser("ADMIN"));
        long assetId = createAsset(admin, createCategory(admin), 900);

        User aliceUser = createUser("EMPLOYEE");
        Employee alice = createEmployeeProfile(aliceUser);
        Employee bob = createEmployeeProfile(createUser("EMPLOYEE"));

        // Alice tries to file the request "as Bob"
        doPost(login(aliceUser), "/api/requests",
                "{\"employeeId\":" + bob.getId() + ",\"assetId\":" + assetId + ",\"reason\":\"need it\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(alice.getId().intValue()));
    }

    @Test
    void managerIdInUrlIsIgnoredAndRealManagerIsRecorded() throws Exception {
        String admin = login(createUser("ADMIN"));
        long assetId = createAsset(admin, createCategory(admin), 900);
        User employeeUser = createUser("EMPLOYEE");
        createEmployeeProfile(employeeUser);
        long requestId = id(doPost(login(employeeUser), "/api/requests",
                "{\"assetId\":" + assetId + ",\"reason\":\"need it\"}"));

        User manager = createUser("MANAGER");
        User otherManager = createUser("MANAGER");

        doPut(login(manager), "/api/requests/" + requestId + "/approve/" + otherManager.getId(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedBy").value(manager.getId().intValue()));
    }
}
