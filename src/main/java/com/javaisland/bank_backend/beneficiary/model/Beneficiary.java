package com.javaisland.bank_backend.beneficiary.model;

import com.javaisland.bank_backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "destination_account_number"})
})
@Getter
@Setter
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "destination_account_number", nullable = false, length = 50)
    private String destinationAccountNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
