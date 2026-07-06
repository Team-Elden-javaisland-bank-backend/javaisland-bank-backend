package com.javaisland.bank_backend.card;

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

    @Column(name = "card_number", unique = true, nullable = false, length = 16)
    private String cardNumber;

    @Column(name = "holder_name", nullable = false, length = 150)
    private String holderName;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false, length = 3)
    private String cvv;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_name", length = 30)
    private CardStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_name", nullable = false, length = 30)
    private CardType cardType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;
}