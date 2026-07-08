package com.javaisland.bank_backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleTypeRepository extends JpaRepository<RoleType, Integer> {
    Optional<RoleType> findByRoleName(String roleName);
}
