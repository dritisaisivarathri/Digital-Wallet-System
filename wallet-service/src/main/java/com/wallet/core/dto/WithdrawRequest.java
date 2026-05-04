package com.wallet.core.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class WithdrawRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Minimum withdrawal amount is 0.01")
    private BigDecimal amount;

    private String notes;

    public WithdrawRequest() {}

    public WithdrawRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
