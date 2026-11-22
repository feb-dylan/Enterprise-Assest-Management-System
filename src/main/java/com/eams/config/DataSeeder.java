package com.eams.config;

import com.eams.entity.Asset;
import com.eams.entity.AssetAssignment;
import com.eams.entity.AssetRequest;
import com.eams.entity.AssetStatus;
import com.eams.entity.AssignmentStatus;
import com.eams.entity.Category;
import com.eams.entity.DamageReport;
import com.eams.entity.DamageStatus;
import com.eams.entity.Department;
import com.eams.entity.Employee;
import com.eams.entity.Maintenance;
import com.eams.entity.MaintenanceStatus;
import com.eams.entity.RequestPriority;
import com.eams.entity.RequestStatus;
import com.eams.entity.Role;
import com.eams.entity.User;
import com.eams.repository.AssetAssignmentRepository;
import com.eams.repository.AssetRepository;
import com.eams.repository.AssetRequestRepository;
import com.eams.repository.CategoryRepository;
import com.eams.repository.DamageReportRepository;
import com.eams.repository.DepartmentRepository;
import com.eams.repository.EmployeeRepository;
import com.eams.repository.MaintenanceRepository;
import com.eams.repository.RoleRepository;
import com.eams.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1. Always: makes sure the four roles exist (previously they had to be
 *    inserted into the database by hand).
 * 2. Only when SEED_DEMO_DATA=true and the demo admin does not exist yet:
 *    loads a realistic demo data set (users for every role, departments,
 *    categories, ~30 assets and 18 months of history) so dashboards,
 *    timelines and health scores look real in a demo.
 *
 * Demo logins (password = SEED_DEMO_PASSWORD, default Password123!):
 *   admin@eams.demo, manager@eams.demo, tech@eams.demo,
 *   alice@eams.demo, bob@eams.demo, chan@eams.demo
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final List<String> ROLES = List.of("ADMIN", "MANAGER", "EMPLOYEE", "TECHNICIAN");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetRequestRepository requestRepository;
    private final DamageReportRepository damageReportRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String role : ROLES) {
            if (roleRepository.findByName(role).isEmpty()) {
                roleRepository.save(new Role(role));
                log.info("Created missing role {}", role);
            }
        }

        if (properties.seed() != null && properties.seed().demoData()
                && !userRepository.existsByEmailIgnoreCase("admin@eams.demo")) {
            seedDemoData();
        }
    }

    // =========================================================
    // DEMO DATA
    // =========================================================

    private final Map<String, Department> departments = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();

    private void seedDemoData() {
        log.info("Seeding demo data ...");
        LocalDate today = LocalDate.now();

        // ----- departments -----
        for (String[] d : new String[][]{
                {"IT", "Information technology and support"},
                {"Finance", "Accounting and budgeting"},
                {"Human Resources", "People and hiring"},
                {"Operations", "Logistics and facilities"},
                {"Marketing", "Brand and communications"}}) {
            departments.put(d[0], departmentRepository.findByName(d[0])
                    .orElseGet(() -> departmentRepository.save(new Department(d[0], d[1]))));
        }

        // ----- categories (name, lifespan months) -----
        for (Object[] c : new Object[][]{
                {"Laptop", 48}, {"Monitor", 72}, {"Printer", 60}, {"Phone", 36},
                {"Projector", 60}, {"Network Equipment", 72}, {"Furniture", 120}, {"Vehicle", 96}}) {
            String name = (String) c[0];
            Category category = categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Category n = new Category();
                n.setName(name);
                n.setDescription(name + " assets");
                return n;
            });
            category.setExpectedLifespanMonths((Integer) c[1]);
            categories.put(name, categoryRepository.save(category));
        }

        // ----- users + employees -----
        User admin = user("admin@eams.demo", "ADMIN");
        User manager = user("manager@eams.demo", "MANAGER");
        User tech = user("tech@eams.demo", "TECHNICIAN");
        Employee alice = employee(user("alice@eams.demo", "EMPLOYEE"), "EMP-001", "Alice", "Tan", "Finance", "Accountant");
        Employee bob = employee(user("bob@eams.demo", "EMPLOYEE"), "EMP-002", "Bob", "Lin", "Marketing", "Designer");
        Employee chan = employee(user("chan@eams.demo", "EMPLOYEE"), "EMP-003", "Chan", "Myat", "IT", "Developer");
        employee(manager, "MGR-001", "Mya", "Thandar", "Operations", "Operations Manager");
        employee(tech, "TEC-001", "Ko", "Zaw", "IT", "Field Technician");

        // ----- assets -----
        Asset lapOld1 = asset("LAP-0001", "Dell Latitude 5490", "Laptop", 1180, today.minusMonths(56), today.minusMonths(20), "HQ Floor 3 - Finance", "Dell");
        Asset lapOld2 = asset("LAP-0002", "HP EliteBook 840 G5", "Laptop", 1250, today.minusMonths(50), today.minusMonths(14), "HQ Floor 2 - Marketing", "HP");
        Asset lap3 = asset("LAP-0003", "Lenovo ThinkPad T14", "Laptop", 1320, today.minusMonths(26), today.plusDays(12), "HQ Floor 3 - IT", "Lenovo");
        Asset lap4 = asset("LAP-0004", "MacBook Pro 14", "Laptop", 2100, today.minusMonths(14), today.plusMonths(10), "HQ Floor 2 - Marketing", "Apple");
        Asset lap5 = asset("LAP-0005", "Dell XPS 13", "Laptop", 1450, today.minusMonths(8), today.plusMonths(16), "Store Room A", "Dell");
        Asset lap6 = asset("LAP-0006", "Lenovo ThinkPad X1 Carbon", "Laptop", 1890, today.minusMonths(3), today.plusMonths(33), "Store Room A", "Lenovo");
        Asset lap7 = asset("LAP-0007", "HP ProBook 450", "Laptop", 890, today.minusMonths(30), today.plusDays(25), "Store Room A", "HP");
        Asset mon1 = asset("MON-0001", "Dell UltraSharp 27", "Monitor", 420, today.minusMonths(40), today.minusMonths(4), "HQ Floor 3 - Finance", "Dell");
        Asset mon2 = asset("MON-0002", "LG 34\" UltraWide", "Monitor", 610, today.minusMonths(18), today.plusMonths(18), "HQ Floor 2 - Marketing", "LG");
        asset("MON-0003", "Samsung 24\" FHD", "Monitor", 180, today.minusMonths(6), today.plusMonths(30), "Store Room A", "Samsung");
        asset("MON-0004", "Dell P2422H", "Monitor", 230, today.minusMonths(11), today.plusMonths(25), "Store Room A", "Dell");
        Asset prn1 = asset("PRN-0001", "HP LaserJet Pro M404", "Printer", 380, today.minusMonths(62), today.minusMonths(26), "HQ Floor 3 - Print Room", "HP");
        asset("PRN-0002", "Canon imageRUNNER 2630", "Printer", 2600, today.minusMonths(20), today.plusDays(20), "HQ Floor 2 - Print Room", "Canon");
        Asset phn1 = asset("PHN-0001", "iPhone 14", "Phone", 899, today.minusMonths(20), today.minusMonths(8), "HQ Floor 3 - IT", "Apple");
        asset("PHN-0002", "Samsung Galaxy S23", "Phone", 799, today.minusMonths(9), today.plusMonths(15), "Store Room A", "Samsung");
        asset("PHN-0003", "Google Pixel 8", "Phone", 699, today.minusMonths(2), today.plusMonths(22), "Store Room A", "Google");
        asset("PRJ-0001", "Epson EB-W51 Projector", "Projector", 540, today.minusMonths(34), today.plusDays(5), "Meeting Room 1", "Epson");
        asset("PRJ-0002", "BenQ MH560 Projector", "Projector", 610, today.minusMonths(4), today.plusMonths(32), "Meeting Room 2", "BenQ");
        asset("NET-0001", "Cisco Catalyst 9200 Switch", "Network Equipment", 3200, today.minusMonths(28), today.plusMonths(8), "Server Room", "Cisco");
        asset("NET-0002", "Ubiquiti UniFi AP Pro", "Network Equipment", 180, today.minusMonths(15), today.plusMonths(9), "HQ Floor 2", "Ubiquiti");
        asset("NET-0003", "Fortinet FortiGate 60F", "Network Equipment", 950, today.minusMonths(22), today.plusDays(28), "Server Room", "Fortinet");
        asset("FUR-0001", "Ergonomic Office Chair", "Furniture", 320, today.minusMonths(24), null, "HQ Floor 3", "Herman Miller");
        asset("FUR-0002", "Standing Desk", "Furniture", 650, today.minusMonths(12), null, "HQ Floor 3", "Flexispot");
        asset("FUR-0003", "Conference Table", "Furniture", 1400, today.minusMonths(48), null, "Meeting Room 1", "IKEA");
        asset("VEH-0001", "Toyota Hiace Van", "Vehicle", 32000, today.minusMonths(44), today.plusMonths(4), "Parking B1", "Toyota");
        asset("VEH-0002", "Honda Wave 110 Motorbike", "Vehicle", 1500, today.minusMonths(10), today.plusMonths(14), "Parking B1", "Honda");
        Asset lap8 = asset("LAP-0008", "Asus ExpertBook B5", "Laptop", 1100, today.minusMonths(16), today.plusMonths(8), "HQ Floor 3 - IT", "Asus");
        asset("LAP-0009", "Acer TravelMate P2", "Laptop", 780, today.minusMonths(38), today.minusMonths(2), "Store Room A", "Acer");
        asset("MON-0005", "BenQ PD2700U", "Monitor", 520, today.minusMonths(5), today.plusMonths(31), "Store Room A", "BenQ");
        asset("PHN-0004", "iPhone 15", "Phone", 999, today.minusMonths(1), today.plusMonths(23), "Store Room A", "Apple");

        // ----- assignment history -----
        // Old laptop: first held by Bob (returned), now held by Alice -> a "reassignment"
        assignment(lapOld1, bob, admin, today.minusMonths(54), today.minusMonths(30), "Upgraded to a newer model");
        assignment(lapOld1, alice, admin, today.minusMonths(29), null, null);
        assignment(lapOld2, bob, admin, today.minusMonths(48), null, null);
        assignment(lap3, chan, admin, today.minusMonths(25), null, null);
        assignment(lap4, bob, admin, today.minusMonths(13), today.minusMonths(2), "Project finished");
        assignment(mon1, alice, admin, today.minusMonths(39), null, null);
        assignment(mon2, bob, admin, today.minusMonths(17), null, null);
        assignment(phn1, chan, admin, today.minusMonths(19), null, null);
        assignment(lap8, chan, admin, today.minusMonths(15), null, null);

        // ----- repairs: the old laptops are expensive to keep (replacement candidates) -----
        repair(lapOld1, alice, tech, "Battery swelling, laptop does not close", today.minusMonths(20), 180, "Replaced battery");
        repair(lapOld1, alice, tech, "Keyboard keys not responding", today.minusMonths(11), 140, "Replaced keyboard");
        repair(lapOld1, alice, tech, "Screen flickering", today.minusMonths(4), 320, "Replaced display panel");
        repair(lapOld2, bob, tech, "Hinge cracked", today.minusMonths(15), 160, "Replaced hinge");
        repair(lapOld2, bob, tech, "Fan noise and overheating", today.minusMonths(6), 210, "Replaced fan, new thermal paste");
        repair(prn1, chan, tech, "Paper jam, broken roller", today.minusMonths(9), 95, "Replaced pickup roller");
        repair(mon1, alice, tech, "Dead pixels in corner", today.minusMonths(7), 60, "Panel recalibrated");

        // ----- open damage: phone reported, laptop under repair -----
        DamageReport phoneDamage = new DamageReport();
        phoneDamage.setAsset(phn1);
        phoneDamage.setEmployee(chan);
        phoneDamage.setDescription("Dropped the phone, the screen is cracked across the top");
        phoneDamage.setStatus(DamageStatus.REPORTED);
        phoneDamage.setReportedDate(LocalDateTime.now().minusDays(1));
        damageReportRepository.save(phoneDamage);
        phn1.setStatus(AssetStatus.DAMAGED);

        DamageReport lapDamage = new DamageReport();
        lapDamage.setAsset(lap8);
        lapDamage.setEmployee(chan);
        lapDamage.setDescription("Coffee spilled on the keyboard, some keys stick");
        lapDamage.setStatus(DamageStatus.REPAIRING);
        lapDamage.setReportedDate(LocalDateTime.now().minusDays(4));
        damageReportRepository.save(lapDamage);
        Maintenance openJob = new Maintenance();
        openJob.setAsset(lap8);
        openJob.setDamageReport(lapDamage);
        openJob.setTechnicianUser(tech);
        openJob.setTechnician(tech.getEmail());
        openJob.setDescription("Clean liquid damage, replace keyboard if needed");
        openJob.setStartDate(today.minusDays(3));
        openJob.setStatus(MaintenanceStatus.IN_PROGRESS);
        maintenanceRepository.save(openJob);
        lap8.setStatus(AssetStatus.MAINTENANCE);

        // ----- requests -----
        request(alice, lap6, null, "My laptop keeps breaking - need a reliable replacement for month-end closing",
                RequestStatus.PENDING, RequestPriority.HIGH, null, null);
        request(bob, null, categories.get("Monitor"), "Second monitor for design work",
                RequestStatus.PENDING, RequestPriority.NORMAL, null, null);
        request(chan, lap5, null, "Spare laptop for testing deployments",
                RequestStatus.APPROVED, RequestPriority.NORMAL, manager, null);
        request(bob, lap7, null, "Laptop for a new intern",
                RequestStatus.REJECTED, RequestPriority.LOW, manager, "Intern will use a shared laptop");

        assetRepository.saveAll(List.of(phn1, lap8));
        log.info("Demo data loaded. Log in as admin@eams.demo / manager@eams.demo / tech@eams.demo / "
                + "alice@eams.demo (password: SEED_DEMO_PASSWORD)");
    }

    // ---------------------------------------------------------

    private User user(String email, String role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(properties.seed().demoPassword()))
                        .role(roleRepository.findByName(role).orElseThrow())
                        .enabled(true)
                        .emailVerified(true)
                        .build()));
    }

    private Employee employee(User user, String code, String first, String last,
                              String department, String position) {
        return employeeRepository.findByUserId(user.getId()).orElseGet(() -> {
            Employee e = new Employee(code, first, last, departments.get(department));
            e.setUser(user);
            e.setPosition(position);
            e.setPhone("+95 9 " + (400000000 + Math.abs(code.hashCode() % 99999999)));
            e.setHireDate(LocalDate.now().minusYears(2));
            return employeeRepository.save(e);
        });
    }

    private Asset asset(String code, String name, String category, int price,
                        LocalDate purchased, LocalDate warranty, String location, String vendor) {
        Asset a = new Asset();
        a.setAssetCode(code);
        a.setName(name);
        a.setDescription(name);
        a.setSerialNumber("SN-" + code.replace("-", ""));
        a.setCategory(categories.get(category));
        a.setPurchasePrice(BigDecimal.valueOf(price));
        a.setPurchaseDate(purchased);
        a.setWarrantyExpiryDate(warranty);
        a.setLocation(location);
        a.setVendor(vendor);
        a.setStatus(AssetStatus.AVAILABLE);
        return assetRepository.save(a);
    }

    private void assignment(Asset asset, Employee employee, User admin,
                            LocalDate assigned, LocalDate returned, String note) {
        AssetAssignment a = new AssetAssignment(asset, employee, assigned, admin);
        if (returned != null) {
            a.setStatus(AssignmentStatus.RETURNED);
            a.setReturnedDate(returned);
            a.setReturnNote(note);
        } else {
            asset.setStatus(AssetStatus.ASSIGNED);
            assetRepository.save(asset);
        }
        assignmentRepository.save(a);
    }

    private void repair(Asset asset, Employee reporter, User tech, String problem,
                        LocalDate when, int cost, String fix) {
        DamageReport d = new DamageReport();
        d.setAsset(asset);
        d.setEmployee(reporter);
        d.setDescription(problem);
        d.setStatus(DamageStatus.RESOLVED);
        d.setResolutionNote(fix);
        d.setReportedDate(when.atTime(10, 0));
        damageReportRepository.save(d);

        Maintenance m = new Maintenance();
        m.setAsset(asset);
        m.setDamageReport(d);
        m.setTechnicianUser(tech);
        m.setTechnician(tech.getEmail());
        m.setDescription(fix);
        m.setStartDate(when.plusDays(1));
        m.setEndDate(when.plusDays(4));
        m.setRepairCost(BigDecimal.valueOf(cost));
        m.setStatus(MaintenanceStatus.COMPLETED);
        maintenanceRepository.save(m);
    }

    private void request(Employee employee, Asset asset, Category category, String reason,
                         RequestStatus status, RequestPriority priority, User decidedBy, String rejection) {
        AssetRequest r = new AssetRequest(employee, asset, reason);
        if (category != null) {
            r.setCategory(category);
        }
        r.setPriority(priority);
        r.setStatus(status);
        if (decidedBy != null) {
            r.setApprovedBy(decidedBy);
            r.setApprovedAt(LocalDateTime.now().minusDays(status == RequestStatus.APPROVED ? 1 : 5));
        }
        r.setRejectionReason(rejection);
        requestRepository.save(r);
    }
}
