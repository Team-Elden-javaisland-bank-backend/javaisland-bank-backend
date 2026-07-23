package com.javaisland.bank_backend.user.repository;

import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    @Query("SELECT u FROM User u JOIN FETCH u.status WHERE u.keycloakId = :keycloakId")
    Optional<User> findByKeycloakIdWithStatus(@Param("keycloakId") String keycloakId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByStatus(UserStatus status);

    List<User> findByRoleTypeOrderByFirstNameAscLastNameAsc(RoleType roleType);

    long countByRoleType(RoleType roleType);

    long countByStatus(UserStatus status);
}