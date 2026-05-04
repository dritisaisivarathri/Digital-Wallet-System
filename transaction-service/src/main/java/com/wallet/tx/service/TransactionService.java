package com.wallet.tx.service;

import com.wallet.tx.entity.LedgerEntry;
import com.wallet.tx.entity.Transaction;
import com.wallet.tx.repository.LedgerEntryRepository;
import com.wallet.tx.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @KafkaListener(topics = "wallet.topup.success", groupId = "transaction-group")
    @Transactional
    public void handleTopUp(Map<String, Object> event) {
        log.info("Received Top-Up Event: {}", event);
        try {
            processTopUpEvent(event);
        } catch (Exception e) {
            log.error("Error processing top-up event: {}", event, e);
        }
    }

    @KafkaListener(topics = "wallet.transfer.completed", groupId = "transaction-group")
    @Transactional
    public void handleTransfer(Map<String, Object> event) {
        log.info("Received Transfer Event: {}", event);
        try {
            processTransferEvent(event);
        } catch (Exception e) {
            log.error("Error processing transfer event: {}", event, e);
        }
    }

    @KafkaListener(topics = "wallet.withdraw.success", groupId = "transaction-group")
    @Transactional
    public void handleWithdraw(Map<String, Object> event) {
        log.info("Received Withdrawal Event: {}", event);
        try {
            syncWithdraw(event);
        } catch (Exception e) {
            log.error("Error processing withdrawal event: {}", event, e);
        }
    }

    @Transactional
    public boolean syncTopUp(Map<String, Object> event) {
        return processTopUpEvent(event);
    }

    @Transactional
    public boolean syncTransfer(Map<String, Object> event) {
        return processTransferEvent(event);
    }

    @Transactional
    public boolean syncWithdraw(Map<String, Object> event) {
        return processWithdrawEvent(event);
    }

    private boolean processTopUpEvent(Map<String, Object> event) {
        log.info("Processing top-up event: {}", event);
        UUID userId = UUID.fromString(event.get("userId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        UUID txId = UUID.fromString(event.get("transactionId").toString());
        if (transactionRepository.existsById(txId)) {
            log.warn("Skipping duplicate top-up transaction {}", txId);
            return false;
        }

        String method = event.get("paymentMethod") != null ? event.get("paymentMethod").toString() : "unknown";
        String subType = event.get("subType") != null ? event.get("subType").toString() : "TOPUP";
        String notes = event.get("notes") != null ? event.get("notes").toString() : "";
        boolean rewardCashback = "REWARD_CASHBACK".equalsIgnoreCase(subType);

        Transaction tx = new Transaction();
        tx.setId(txId);
        tx.setToUserId(userId);
        tx.setAmount(amount);
        tx.setType(rewardCashback ? "REWARD_CASHBACK" : "TOPUP");
        tx.setStatus("COMPLETED");
        tx.setReferenceNotes(
                rewardCashback
                        ? (notes.isBlank() ? "Reward cashback credited" : notes)
                        : "Topup via " + method
        );
        transactionRepository.saveAndFlush(tx);

        LedgerEntry credit = new LedgerEntry();
        credit.setAccountId(userId);
        credit.setTransactionId(txId);
        credit.setType("CREDIT");
        credit.setAmount(amount);
        credit.setDescription(rewardCashback ? "Reward cashback" : "Wallet Top-up");
        ledgerEntryRepository.saveAndFlush(credit);

        log.info("Successfully processed top-up for user: {}", userId);
        return true;
    }

    private boolean processTransferEvent(Map<String, Object> event) {
        UUID fromUserId = UUID.fromString(event.get("fromUserId").toString());
        UUID toUserId = UUID.fromString(event.get("toUserId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        UUID txId = UUID.fromString(event.get("transactionId").toString());
        if (transactionRepository.existsById(txId)) {
            log.info("Skipping duplicate transfer transaction {}", txId);
            return false;
        }

        String notes = event.get("notes") != null ? event.get("notes").toString() : "";

        Transaction tx = new Transaction();
        tx.setId(txId);
        tx.setFromUserId(fromUserId);
        tx.setToUserId(toUserId);
        tx.setAmount(amount);
        tx.setType("TRANSFER");
        tx.setStatus("COMPLETED");
        tx.setReferenceNotes(notes);
        transactionRepository.saveAndFlush(tx);

        log.info("Processing transfer debit for user: {} amount: {}", fromUserId, amount);
        LedgerEntry debit = new LedgerEntry();
        debit.setAccountId(fromUserId);
        debit.setTransactionId(txId);
        debit.setType("DEBIT");
        debit.setAmount(amount);
        debit.setDescription("Transfer to " + toUserId);
        ledgerEntryRepository.saveAndFlush(debit);

        log.info("Processing transfer credit for user: {} amount: {}", toUserId, amount);
        LedgerEntry credit = new LedgerEntry();
        credit.setAccountId(toUserId);
        credit.setTransactionId(txId);
        credit.setType("CREDIT");
        credit.setAmount(amount);
        credit.setDescription("Transfer from " + fromUserId);
        ledgerEntryRepository.saveAndFlush(credit);

        log.info("Transfer ledger entries completed for transaction: {}", txId);

        log.info("Successfully processed transfer from {} to {}", fromUserId, toUserId);
        return true;
    }

    private boolean processWithdrawEvent(Map<String, Object> event) {
        UUID userId = UUID.fromString(event.get("userId").toString());
        BigDecimal amount = new BigDecimal(event.get("amount").toString());
        UUID txId = UUID.fromString(event.get("transactionId").toString());
        if (transactionRepository.existsById(txId)) {
            log.info("Skipping duplicate withdrawal transaction {}", txId);
            return false;
        }

        String notes = event.get("notes") != null ? event.get("notes").toString() : "";

        Transaction tx = new Transaction();
        tx.setId(txId);
        tx.setFromUserId(userId);
        tx.setAmount(amount);
        tx.setType("WITHDRAWAL");
        tx.setStatus("COMPLETED");
        tx.setReferenceNotes(notes.isBlank() ? "Wallet withdrawal" : notes);
        transactionRepository.saveAndFlush(tx);

        LedgerEntry debit = new LedgerEntry();
        debit.setAccountId(userId);
        debit.setTransactionId(txId);
        debit.setType("DEBIT");
        debit.setAmount(amount);
        debit.setDescription("Wallet withdrawal");
        ledgerEntryRepository.saveAndFlush(debit);

        log.info("Successfully processed withdrawal for user: {}", userId);
        return true;
    }
}
