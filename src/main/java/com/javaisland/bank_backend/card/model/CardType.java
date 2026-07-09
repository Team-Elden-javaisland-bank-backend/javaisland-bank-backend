package com.javaisland.bank_backend.card.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", unique = true, nullable = false, length = 30)
    private String typeName;
}
