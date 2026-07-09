package com.javaisland.bank_backend.card.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name", unique = true, nullable = false, length = 30)
    private String statusName;
}
