package com.eams.repository;

import com.eams.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "role")
    List<User> findByRoleName(String roleName);

    @EntityGraph(attributePaths = "role")
    List<User> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);
}
