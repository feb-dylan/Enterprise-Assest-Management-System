package com.eams.repository;

import com.eams.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByUserId(Long userId);

    // Check whether a department has employees
    boolean existsByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Employee> findByEmployeeCode(String employeeCode);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Employee> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Employee> findByUserEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"user", "department"})
    Optional<Employee> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Employee> findAll(Pageable pageable);

    // Find employees belonging to a specific department
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Employee> findByDepartmentId(
            Long departmentId,
            Pageable pageable
    );

    // Employee search: first name, last name, employee code, or full name
    @Query("""
        SELECT e
        FROM Employee e
        WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(CONCAT(e.firstName, ' ', e.lastName))
              LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    @EntityGraph(attributePaths = {"user", "department"})
    Page<Employee> searchEmployees(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}