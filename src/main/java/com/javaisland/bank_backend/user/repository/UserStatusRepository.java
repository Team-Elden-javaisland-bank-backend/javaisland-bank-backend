package com.javaisland.bank_backend.user.repository;

import com.javaisland.bank_backend.user.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserStatusRepository extends JpaRepository<UserStatus, Integer> {
    Optional<UserStatus> findByUserStatus(String userStatus);
}
