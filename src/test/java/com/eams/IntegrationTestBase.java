package com.eams;

import com.eams.entity.Department;
import com.eams.entity.Employee;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.repository.DepartmentRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full Spring Boot app + in-memory H2 database (profile "test"),
 * real security filter chain, real JWTs.
 * Every test uses unique emails/codes, so tests do not depend on each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Password123!";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RoleRepository roleRepository;
    @Autowired
    protected EmployeeRepository employeeRepository;
    @Autowired
    protected DepartmentRepository departmentRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Test user with a role. Returns their email. */
    protected User createUser(String role) {
        Role r = roleRepository.findByName(role).orElseThrow();
        return userRepository.save(User.builder()
                .email(unique(role.toLowerCase()) + "@test.local")
                .password(passwordEncoder.encode(PASSWORD))
                .role(r)
                .enabled(true)
                .emailVerified(true)
                .build());
    }

    protected Employee createEmployeeProfile(User user) {
        Department dept = departmentRepository.save(new Department(unique("Dept"), "test"));
        Employee e = new Employee(unique("EMP"), "Test", user.getEmail().substring(0, 6), dept);
        e.setUser(user);
        return employeeRepository.save(e);
    }

    protected String login(User user) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    // ---------- request helpers ----------

    protected ResultActions doGet(String token, String url) throws Exception {
        return mvc.perform(auth(get(url), token));
    }

    protected ResultActions doPost(String token, String url, String json) throws Exception {
        return mvc.perform(auth(post(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json == null ? "{}" : json));
    }

    protected ResultActions doPut(String token, String url, String json) throws Exception {
        return mvc.perform(auth(put(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json == null ? "{}" : json));
    }

    protected ResultActions doPatch(String token, String url, String json) throws Exception {
        return mvc.perform(auth(patch(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions doDelete(String token, String url) throws Exception {
        return mvc.perform(auth(delete(url), token));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return token == null ? b : b.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static <T> T json(ResultActions actions, String path) throws Exception {
        return JsonPath.read(actions.andReturn().getResponse().getContentAsString(), path);
    }

    protected static long id(ResultActions actions) throws Exception {
        Number n = json(actions, "$.id");
        return n.longValue();
    }

    // ---------- domain helpers ----------

    protected long createCategory(String adminToken) throws Exception {
        return id(doPost(adminToken, "/api/categories",
                "{\"name\":\"" + unique("Laptop") + "\",\"expectedLifespanMonths\":48}")
                .andExpect(status().isCreated()));
    }

    protected long createAsset(String adminToken, long categoryId, int price) throws Exception {
        String code = unique("AST");
        return id(doPost(adminToken, "/api/assets", """
                {"assetCode":"%s","name":"Test Laptop","categoryId":%d,
                 "purchasePrice":%d,"purchaseDate":"2024-01-15","location":"HQ Floor 3"}
                """.formatted(code, categoryId, price))
                .andExpect(status().isCreated()));
    }

    protected String assetCode(String token, long assetId) throws Exception {
        return json(doGet(token, "/api/assets/" + assetId).andExpect(status().isOk()), "$.assetCode");
    }
}
