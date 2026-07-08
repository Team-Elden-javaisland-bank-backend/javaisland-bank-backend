package com.javaisland.bank_backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Integer> {
    Optional<UserStatus> findByStatusName(String statusName);
}
