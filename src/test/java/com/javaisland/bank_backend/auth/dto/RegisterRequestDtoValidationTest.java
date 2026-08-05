package com.javaisland.bank_backend.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) factory.close();
    }

    private RegisterRequestDto validBase() {
        return RegisterRequestDto.builder()
                .firstName("Mario")
                .lastName("Rossi")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("mario@test.com")
                .password("Aa1!xxxx")
                .gender("M")
                .profession("Impiegato")
                .fiscalCode("RSSMRA90A01H501U")
                .phone("+393331234567")
                .residence("Via Roma 1, 00100")
                .birthPlace("Roma")
                .birthProvince("RM")
                .build();
    }

    private Set<String> violationsOnPhone(String phone) {
        RegisterRequestDto dto = validBase();
        dto.setPhone(phone);
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void validMobile_333() {
        assertTrue(violationsOnPhone("+393331234567").isEmpty());
    }

    @Test
    void validMobile_347() {
        assertTrue(violationsOnPhone("+393471234567").isEmpty());
    }

    @Test
    void validMobile_9digit() {
        assertTrue(violationsOnPhone("+39320123456").isEmpty());
    }

    @Test
    void invalid_notStartingWith3() {
        assertFalse(violationsOnPhone("+391234567890").isEmpty());
    }

    @Test
    void invalid_tooShort() {
        assertFalse(violationsOnPhone("+3933312").isEmpty());
    }

    @Test
    void invalid_tooLong() {
        assertFalse(violationsOnPhone("+39333123456789").isEmpty());
    }

    @Test
    void invalid_missingPrefix() {
        assertFalse(violationsOnPhone("3331234567").isEmpty());
    }

    @Test
    void invalid_letters() {
        assertFalse(violationsOnPhone("+39abc3331234").isEmpty());
    }

    @Test
    void invalid_empty() {
        assertFalse(violationsOnPhone("").isEmpty());
    }

    @Test
    void invalid_blank() {
        assertFalse(violationsOnPhone("   ").isEmpty());
    }
}
