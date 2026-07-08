package com.javaisland.bank_backend.card;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardIssueDTO {

    @NotNull(message = "L'ID del conto è obbligatorio")
    private Long accountId;

    @NotBlank(message = "Il nome del titolare è obbligatorio")
    @Size(max = 150, message = "Il nome del titolare non può superare i 150 caratteri")
    private String holderName;

    @NotBlank(message = "Il tipo di carta è obbligatorio")
    private String cardType;
}