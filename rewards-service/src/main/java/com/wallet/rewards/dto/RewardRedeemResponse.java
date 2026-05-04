package com.wallet.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RewardRedeemResponse {
    private UUID rewardId;
    private String rewardName;
    private String rewardType;
    private int pointsSpent;
    private int remainingPoints;
    private int remainingStock;
    private BigDecimal cashbackCredited;
    private BigDecimal walletBalance;
    private String message;
    private LocalDateTime redeemedAt;

    public UUID getRewardId() {
        return rewardId;
    }

    public void setRewardId(UUID rewardId) {
        this.rewardId = rewardId;
    }

    public String getRewardName() {
        return rewardName;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public int getPointsSpent() {
        return pointsSpent;
    }

    public void setPointsSpent(int pointsSpent) {
        this.pointsSpent = pointsSpent;
    }

    public int getRemainingPoints() {
        return remainingPoints;
    }

    public void setRemainingPoints(int remainingPoints) {
        this.remainingPoints = remainingPoints;
    }

    public int getRemainingStock() {
        return remainingStock;
    }

    public void setRemainingStock(int remainingStock) {
        this.remainingStock = remainingStock;
    }

    public BigDecimal getCashbackCredited() {
        return cashbackCredited;
    }

    public void setCashbackCredited(BigDecimal cashbackCredited) {
        this.cashbackCredited = cashbackCredited;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }
}
