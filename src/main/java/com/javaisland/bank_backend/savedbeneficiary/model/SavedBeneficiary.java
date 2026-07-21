package com.javaisland.bank_backend.savedbeneficiary.model;

import com.javaisland.bank_backend.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavedBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "beneficiary_name", nullable = false, length = 150)
    private String beneficiaryName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
