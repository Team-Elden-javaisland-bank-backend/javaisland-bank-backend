package com.javaisland.bank_backend.request;

import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RequestService {

    private final BankRequestRepository requestRepository;
    private final LoanRepository loanRepository;

    public RequestService(BankRequestRepository requestRepository, LoanRepository loanRepository) {
        this.requestRepository = requestRepository;
        this.loanRepository = loanRepository;
    }

    // 🧠 1. CREA UNA RICHIESTA BUROCRATICA GENERICA
    public BankRequest createRequest(Long userId, String description) {
        BankRequest request = new BankRequest();
        request.setUserId(userId);
        request.setDescription(description);
        request.setStatus("PENDING");
        return requestRepository.save(request);
    }

    // 🧠 2. APPROVAZIONE O RIFIUTO DELLA RICHIESTA DA PARTE DI UN DIPENDENTE
    public BankRequest reviewRequest(Long requestId, Long employeeId, String status, String rejectionReason) {
        BankRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta burocratica non trovata."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("Questa richiesta è già stata elaborata.");
        }

        request.setStatus(status);
        request.setReviewedByUserId(employeeId);

        if ("REJECTED".equals(status)) {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new ApiBankException("È obbligatorio inserire il motivo del rifiuto.");
            }
            request.setRejectionReason(rejectionReason);
        }

        return requestRepository.save(request);
    }

    // 🧠 3. CALCOLO RATA E RICHIESTA FINANZIAMENTO (LOAN)
    public Loan applyForLoan(Long userId, BigDecimal amount, BigDecimal annualRate, Integer months) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || months <= 0) {
            throw new ApiBankException("Importo e durata del finanziamento devono essere maggiori di zero.");
        }

        // Calcolo della rata mensile con tasso d'interesse semplice (Quota Capitale + Quota Interessi)
        // Formula: (Importo + (Importo * Tasso * Anni)) / Mesi
        BigDecimal years = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal totalInterest = amount.multiply(annualRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)).multiply(years);
        BigDecimal totalToRepay = amount.add(totalInterest);
        BigDecimal monthlyInstallment = totalToRepay.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setLoanAmount(amount);
        loan.setInterestRate(annualRate);
        loan.setDurationMonths(months);
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setStatus("PENDING");

        return loanRepository.save(loan);
    }
}