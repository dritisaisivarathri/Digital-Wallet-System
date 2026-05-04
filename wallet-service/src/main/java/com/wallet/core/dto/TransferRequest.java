package com.wallet.core.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public class TransferRequest {
    @NotNull(message = "Enter correct user ID. Recipient user ID is required.")
    private UUID targetUserId;

    @NotNull(message = "Enter correct amount. Amount is required.")
    @Positive(message = "Enter correct amount. Amount must be greater than 0.")
    private BigDecimal amount;
    private String notes;

    public TransferRequest() {}

    public TransferRequest(UUID targetUserId, BigDecimal amount, String notes) {
        this.targetUserId = targetUserId;
        this.amount = amount;
        this.notes = notes;
    }

    public UUID getTargetUserId() { return targetUserId; }
    public void setTargetUserId(UUID targetUserId) { this.targetUserId = targetUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
