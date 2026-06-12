package com.trtc.uikit.livekit.features.wallet.model;

import com.google.gson.annotations.SerializedName;

/**
 * WalletInfo - Represents a user's coin wallet.
 */
public class WalletInfo {
    @SerializedName("userId")
    public String userId;

    @SerializedName("coinBalance")
    public long coinBalance = 0;

    @SerializedName("updatedAt")
    public long updatedAt = 0;

    public WalletInfo() {
    }

    public WalletInfo(String userId, long coinBalance, long updatedAt) {
        this.userId = userId;
        this.coinBalance = coinBalance;
        this.updatedAt = updatedAt;
    }

    /**
     * Check if the wallet has enough coins for a cost.
     */
    public boolean hasEnough(long cost) {
        return coinBalance >= cost;
    }

    @Override
    public String toString() {
        return "WalletInfo{" +
                "userId='" + userId + '\'' +
                ", coinBalance=" + coinBalance +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
