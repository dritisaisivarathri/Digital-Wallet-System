package com.wallet.rewards.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletCreditRequest {
    private UUID userId;
    private BigDecimal amount;
    private String source;
    private String note;

    public WalletCreditRequest() {
    }

    public WalletCreditRequest(UUID userId, BigDecimal amount, String source, String note) {
        this.userId = userId;
        this.amount = amount;
        this.source = source;
        this.note = note;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
