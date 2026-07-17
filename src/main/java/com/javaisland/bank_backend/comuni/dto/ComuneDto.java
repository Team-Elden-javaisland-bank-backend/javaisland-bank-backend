package com.javaisland.bank_backend.comuni.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComuneDto {
    private String nome;
    private String provincia;
    private String codiceCatastale;
}
