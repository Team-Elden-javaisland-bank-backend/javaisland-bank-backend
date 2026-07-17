package com.javaisland.bank_backend.user.repository;

import com.javaisland.bank_backend.user.model.UserPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserPinRepository extends JpaRepository<UserPin, Long> {

    Optional<UserPin> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
