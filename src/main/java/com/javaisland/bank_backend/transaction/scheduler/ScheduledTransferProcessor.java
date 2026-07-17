package com.javaisland.bank_backend.transaction.scheduler;

import com.javaisland.bank_backend.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferProcessor {

    private final TransactionService transactionService;

    @Scheduled(cron = "0 * * * * *")
    public void processPendingTransfers() {
        log.debug("Checking for pending transfers...");
        transactionService.executePendingTransfers();
    }
}
