package com.trtc.uikit.livekit.features.wallet.model;

import com.google.gson.annotations.SerializedName;

/**
 * WalletTransaction - Represents a wallet transaction entry.
 */
public class WalletTransaction {
    @SerializedName("transactionId")
    public String transactionId;

    @SerializedName("userId")
    public String userId;

    @SerializedName("type")
    public TransactionType type;

    @SerializedName("amount")
    public long amount;

    @SerializedName("balanceAfter")
    public long balanceAfter;

    @SerializedName("createdAt")
    public long createdAt;

    @SerializedName("description")
    public String description;

    public WalletTransaction() {
    }

    public WalletTransaction(String transactionId, String userId, TransactionType type, long amount, long balanceAfter, long createdAt, String description) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
        this.description = description;
    }

    @Override
    public String toString() {
        return "WalletTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", createdAt=" + createdAt +
                ", description='" + description + '\'' +
                '}';
    }
}
