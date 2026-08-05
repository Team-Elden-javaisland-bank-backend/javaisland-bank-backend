package com.javaisland.bank_backend.card.model;

import com.javaisland.bank_backend.card.model.CardStatus;
import com.javaisland.bank_backend.card.model.CardType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", length = 16)
    private String cardNumber;

    @Column(name = "card_number_hash", unique = true, length = 64)
    private String cardNumberHash;

    @Column(name = "holder_name", nullable = false, length = 150)
    private String holderName;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "cvv", length = 3)
    private String cvv;

    @Column(name = "card_number_enc", length = 300)
    private String cardNumberEnc;

    @Column(name = "cvv_enc", length = 150)
    private String cvvEnc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private CardStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_type_id", nullable = false)
    private CardType cardType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;
}