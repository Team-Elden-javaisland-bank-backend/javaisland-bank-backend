package com.javaisland.bank_backend.request;

import com.javaisland.bank_backend.card.Card;
import com.javaisland.bank_backend.card.CardRepository;
import com.javaisland.bank_backend.card.CardStatusRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RequestService {

    private final BankRequestRepository bankRequestRepository;
    private final LoanRepository loanRepository;
    private final CardRepository cardRepository;
    private final CardStatusRepository cardStatusRepository;

    public RequestService(BankRequestRepository bankRequestRepository,
                          LoanRepository loanRepository,
                          CardRepository cardRepository,
                          CardStatusRepository cardStatusRepository) {
        this.bankRequestRepository = bankRequestRepository;
        this.loanRepository = loanRepository;
        this.cardRepository = cardRepository;
        this.cardStatusRepository = cardStatusRepository;
    }

    @Transactional
    public BankRequest createRequest(Long userId, String description) {
        BankRequest request = new BankRequest();
        request.setUserId(userId);
        request.setDescription(description);
        request.setStatus("PENDING");
        return bankRequestRepository.save(request);
    }

    @Transactional
    public BankRequest createAccountClosureRequest(Long userId, Long accountId) {
        if (accountId == null) {
            throw new ApiBankException("L'ID del conto corrente è obbligatorio per la chiusura.");
        }
        BankRequest request = new BankRequest();
        request.setUserId(userId);
        request.setStatus("PENDING");
        request.setDescription("RICHIESTA_CHIUSURA_CONTO | ID Conto: " + accountId);
        return bankRequestRepository.save(request);
    }

    @Transactional
    public BankRequest createCardBlockRequest(Long userId, Long cardId) {
        if (cardId == null) {
            throw new ApiBankException("L'ID della carta è obbligatorio per il blocco.");
        }
        BankRequest request = new BankRequest();
        request.setUserId(userId);
        request.setStatus("PENDING");
        request.setDescription("RICHIESTA_BLOCCO_CARTA | ID Carta: " + cardId);
        return bankRequestRepository.save(request);
    }

    @Transactional
    public Loan applyForLoan(Long userId, BigDecimal amount, BigDecimal annualRate, Integer months, Long accountId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || months <= 0) {
            throw new ApiBankException("Importo e durata del finanziamento devono essere maggiori di zero.");
        }

        // 🧠 Calcolo della rata allineato alla tua logica precedente
        BigDecimal years = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal totalInterest = amount.multiply(annualRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)).multiply(years);
        BigDecimal totalToRepay = amount.add(totalInterest);
        BigDecimal monthlyInstallment = totalToRepay.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        Loan loan = new Loan();
        loan.setUserId(userId);
        // 📌 Modificati i setter con i nomi reali della tua entità Loan!
        loan.setLoanAmount(amount);
        loan.setInterestRate(annualRate);
        loan.setDurationMonths(months);
        loan.setMonthlyInstallment(monthlyInstallment);
        loan.setStatus("PENDING");
        loan.setAccountId(accountId);

        return loanRepository.save(loan);
    }

    @Transactional
    public BankRequest reviewRequest(Long requestId, Long employeeId, String status, String rejectionReason) {
        BankRequest request = bankRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata"));

        request.setStatus(status);
        request.setReviewedByUserId(employeeId);
        if ("REJECTED".equals(status)) {
            request.setRejectionReason(rejectionReason);
        }
        return bankRequestRepository.save(request);
    }

    @Transactional
    public BankRequest processBankRequest(ProcessRequestDTO dto) {
        BankRequest request = bankRequestRepository.findById(dto.getRequestId())
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("Questa richiesta è già stata elaborata");
        }

        request.setStatus(dto.getStatus());
        request.setReviewedByUserId(dto.getEmployeeId());

        if ("REJECTED".equals(dto.getStatus())) {
            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new ApiBankException("Il motivo del rifiuto è obbligatorio per lo stato REJECTED");
            }
            request.setRejectionReason(dto.getRejectionReason());
            return bankRequestRepository.save(request);
        }

        if ("COMPLETED".equals(dto.getStatus())) {
            executeApprovedAction(request);
        }

        return bankRequestRepository.save(request);
    }

    private void executeApprovedAction(BankRequest request) {
        String desc = request.getDescription();

        if (desc != null && desc.startsWith("RICHIESTA_BLOCCO_CARTA")) {
            String[] parts = desc.split(": ");
            if (parts.length < 2) {
                throw new ApiBankException("Formato descrizione richiesta non valido");
            }
            Long cardId = Long.parseLong(parts[1].trim());

            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new ApiBankException("Carta non trovata per il blocco automatico"));

            var blockedStatus = cardStatusRepository.findByStatusName("BLOCKED")
                    .orElseThrow(() -> new ApiBankException("Stato carta BLOCKED non configurato."));
            card.setStatus(blockedStatus);
            cardRepository.save(card);
        }
    }
}