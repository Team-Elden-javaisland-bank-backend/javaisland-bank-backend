package com.javaisland.bank_backend.user.repository;

import com.javaisland.bank_backend.user.model.PasswordChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordChangeRequestRepository extends JpaRepository<PasswordChangeRequest, Long> {

    List<PasswordChangeRequest> findByStatusOrderByCreatedAtDesc(String status);

    Optional<PasswordChangeRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<PasswordChangeRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
}
