package com.trtc.uikit.livekit.features.wallet.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.trtc.uikit.livekit.features.wallet.model.TransactionType;
import com.trtc.uikit.livekit.features.wallet.model.WalletInfo;
import com.trtc.uikit.livekit.features.wallet.model.WalletTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WalletRepository - Handles Firebase-backed wallet operations.
 */
public class WalletRepository {
    private static final String TAG = "WalletRepository";
    private static final String WALLET_COLLECTION = "users_wallet";
    private static final String TRANSACTIONS_COLLECTION = "wallet_transactions";

    private static WalletRepository instance;
    private final FirebaseFirestore firestore;
    private final Map<String, MutableLiveData<WalletInfo>> walletLiveDataCache;
    private final Map<String, ListenerRegistration> listenerRegistrations;

    private WalletRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.walletLiveDataCache = new ConcurrentHashMap<>();
        this.listenerRegistrations = new ConcurrentHashMap<>();
    }

    public static synchronized WalletRepository getInstance() {
        if (instance == null) {
            instance = new WalletRepository();
        }
        return instance;
    }

    public LiveData<WalletInfo> getWalletInfoLiveData(String userId) {
        if (!walletLiveDataCache.containsKey(userId)) {
            MutableLiveData<WalletInfo> liveData = new MutableLiveData<>(new WalletInfo(userId, 0, System.currentTimeMillis()));
            walletLiveDataCache.put(userId, liveData);
            listenToWalletUpdates(userId, liveData);
        }
        return walletLiveDataCache.get(userId);
    }

    private void listenToWalletUpdates(String userId, MutableLiveData<WalletInfo> liveData) {
        if (listenerRegistrations.containsKey(userId)) {
            listenerRegistrations.get(userId).remove();
        }

        ListenerRegistration registration = firestore
                .collection(WALLET_COLLECTION)
                .document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to wallet updates for " + userId, error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        WalletInfo walletInfo = snapshot.toObject(WalletInfo.class);
                        if (walletInfo != null) {
                            walletInfo.userId = userId;
                            liveData.setValue(walletInfo);
                            return;
                        }
                    }
                    liveData.setValue(new WalletInfo(userId, 0, System.currentTimeMillis()));
                });

        listenerRegistrations.put(userId, registration);
    }

    public void fetchWalletInfo(String userId, OnWalletFetchListener listener) {
        firestore.collection(WALLET_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        WalletInfo walletInfo = snapshot.toObject(WalletInfo.class);
                        if (walletInfo != null) {
                            walletInfo.userId = userId;
                            listener.onSuccess(walletInfo);
                            return;
                        }
                    }
                    listener.onSuccess(new WalletInfo(userId, 0, System.currentTimeMillis()));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching wallet info for " + userId, e);
                    listener.onFailure(e);
                });
    }

    public void updateWalletBalance(String userId, long newBalance, OnWalletOperationListener listener) {
        if (newBalance < 0) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Balance cannot be negative"));
            }
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("coinBalance", newBalance);
        data.put("updatedAt", System.currentTimeMillis());

        firestore.collection(WALLET_COLLECTION)
                .document(userId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating wallet balance for " + userId, e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    public void addTransaction(String userId, TransactionType type, long amount, long balanceAfter, String description, OnWalletOperationListener listener) {
        String transactionId = UUID.randomUUID().toString();
        WalletTransaction transaction = new WalletTransaction(transactionId, userId, type, amount, balanceAfter, System.currentTimeMillis(), description);
        firestore.collection(WALLET_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .document(transactionId)
                .set(transaction)
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding wallet transaction for " + userId, e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    public void fetchTransactionHistory(String userId, long limit, OnTransactionHistoryListener listener) {
        firestore.collection(WALLET_COLLECTION)
                .document(userId)
                .collection(TRANSACTIONS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WalletTransaction> transactions = new ArrayList<>();
                    for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                        WalletTransaction transaction = snapshot.toObject(WalletTransaction.class);
                        if (transaction != null) {
                            transactions.add(transaction);
                        }
                    }
                    listener.onSuccess(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching transaction history for " + userId, e);
                    listener.onFailure(e);
                });
    }

    public void unsubscribeWalletUpdates(String userId) {
        if (listenerRegistrations.containsKey(userId)) {
            listenerRegistrations.get(userId).remove();
            listenerRegistrations.remove(userId);
        }
        walletLiveDataCache.remove(userId);
    }

    public void cleanup() {
        for (ListenerRegistration registration : listenerRegistrations.values()) {
            registration.remove();
        }
        listenerRegistrations.clear();
        walletLiveDataCache.clear();
    }

    public interface OnWalletFetchListener {
        void onSuccess(WalletInfo walletInfo);
        void onFailure(Exception e);
    }

    public interface OnWalletOperationListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnTransactionHistoryListener {
        void onSuccess(List<WalletTransaction> transactions);
        void onFailure(Exception e);
    }
}
