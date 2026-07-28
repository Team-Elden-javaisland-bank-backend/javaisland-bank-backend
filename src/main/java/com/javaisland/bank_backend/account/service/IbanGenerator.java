package com.javaisland.bank_backend.account.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class IbanGenerator {

    private static final String COUNTRY_CODE = "IT";
    private static final String ABI = "05428";
    private static final String CAB = "11101";
    private static final int ACCOUNT_NUMBER_LENGTH = 12;
    private static final int MAX_GENERATION_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a valid Italian IBAN in the format:
     * ITkkC AAAAA BBBBB CCCCCCCCCCCC
     * (27 characters total)
     */
    public String generate() {
        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            String accountPart = generateAccountNumber();
            String ibanBody = ABI + CAB + accountPart;
            char cin = calculateCin(ibanBody);
            String partialIban = COUNTRY_CODE + "00" + cin + ibanBody;
            String checkDigits = calculateCheckDigits(partialIban);
            String iban = COUNTRY_CODE + checkDigits + cin + ibanBody;

            if (iban.length() == 27) {
                return iban;
            }
        }
        throw new RuntimeException("Failed to generate valid IBAN after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * 12-digit random account number
     */
    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder(ACCOUNT_NUMBER_LENGTH);
        for (int i = 0; i < ACCOUNT_NUMBER_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * CIN (Check Individual Number) calculation per Italian banking standard.
     *
     * Input: 22-char string = ABI(5) + CAB(5) + AccountNumber(12)
     *
     * For each position (1-indexed):
     * - ODD positions: use lookup table
     * - EVEN positions: double the digit, sum individual digits if >= 10
     *
     * CIN = (10 - (sum % 10)) % 10, mapped to letter: 0=A, 1=B, ..., 9=J
     */
    private char calculateCin(String ibanBody) {
        int[] oddLookup = {
            1, 0, 5, 7, 9, 13, 15, 17, 19, 21,
            1, 0, 5, 7, 9, 13, 15, 17, 19, 21,
            2, 4, 18, 20, 11, 3, 6, 8, 12, 14,
            16, 10, 22, 25, 24, 23
        };

        int sum = 0;
        for (int i = 0; i < ibanBody.length(); i++) {
            char ch = ibanBody.charAt(i);
            int value;

            if (Character.isDigit(ch)) {
                value = ch - '0';
            } else if (Character.isLetter(ch)) {
                value = Character.toUpperCase(ch) - 'A';
            } else {
                throw new IllegalArgumentException("Invalid character in IBAN body: " + ch);
            }

            if ((i + 1) % 2 != 0) {
                // Odd position: lookup table
                sum += oddLookup[value];
            } else {
                // Even position: double and sum digits
                int doubled = value * 2;
                sum += (doubled / 10) + (doubled % 10);
            }
        }

        int cinDigit = (10 - (sum % 10)) % 10;
        return (char) ('A' + cinDigit);
    }

    /**
     * IBAN check digits calculation per ISO 13616.
     *
     * 1. Move country code + check digits to end: body + "IT00"
     * 2. Replace letters: A=10, B=11, ..., Z=35
     * 3. Compute mod 97
     * 4. Check digits = 98 - (result mod 97), zero-padded to 2 digits
     */
    private String calculateCheckDigits(String partialIban) {
        // Move "IT00" to end: strip "IT00" from front, append it
        String numericPart = partialIban.substring(4) + partialIban.substring(0, 4);

        StringBuilder numeric = new StringBuilder();
        for (char ch : numericPart.toCharArray()) {
            if (Character.isDigit(ch)) {
                numeric.append(ch);
            } else if (Character.isLetter(ch)) {
                numeric.append(Character.getNumericValue(ch));
            } else {
                throw new IllegalArgumentException("Invalid character in IBAN: " + ch);
            }
        }

        // Mod 97 on the big number
        int remainder = 0;
        for (int i = 0; i < numeric.length(); i++) {
            remainder = (remainder * 10 + (numeric.charAt(i) - '0')) % 97;
        }

        int checkDigits = 98 - remainder;
        return String.format("%02d", checkDigits);
    }
}
