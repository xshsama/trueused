package com.xsh.trueused.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xsh.trueused.entity.Role;
import com.xsh.trueused.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
