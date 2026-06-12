package com.trtc.uikit.livekit.features.wallet.service;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.trtc.uikit.livekit.features.wallet.model.TransactionType;
import com.trtc.uikit.livekit.features.wallet.model.WalletInfo;
import com.trtc.uikit.livekit.features.wallet.model.WalletTransaction;
import com.trtc.uikit.livekit.features.wallet.repository.WalletRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WalletService - High-level wallet operations and business rules.
 */
public class WalletService {
    private static final String TAG = "WalletService";

    private static WalletService instance;
    private final WalletRepository repository;
    private final Map<String, MutableLiveData<WalletInfo>> walletCache;
    private final Map<String, Observer<WalletInfo>> repositoryObservers;

    private WalletService() {
        this.repository = WalletRepository.getInstance();
        this.walletCache = new ConcurrentHashMap<>();
        this.repositoryObservers = new ConcurrentHashMap<>();
    }

    public static synchronized WalletService getInstance() {
        if (instance == null) {
            instance = new WalletService();
        }
        return instance;
    }

    public LiveData<WalletInfo> getWalletInfo(String userId) {
        if (!walletCache.containsKey(userId)) {
            MutableLiveData<WalletInfo> liveData = new MutableLiveData<>(new WalletInfo(userId, 0, System.currentTimeMillis()));
            walletCache.put(userId, liveData);
            Observer<WalletInfo> repositoryObserver = walletInfo -> {
                if (walletInfo != null) {
                    liveData.setValue(walletInfo);
                }
            };
            repositoryObservers.put(userId, repositoryObserver);
            repository.getWalletInfoLiveData(userId).observeForever(repositoryObserver);
        }
        return walletCache.get(userId);
    }

    public void fetchWalletInfo(String userId, WalletRepository.OnWalletFetchListener listener) {
        repository.fetchWalletInfo(userId, listener);
    }

    public void addCoins(String userId, long amount, TransactionType type, String description, WalletRepository.OnWalletOperationListener listener) {
        if (amount <= 0) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Amount must be positive"));
            }
            return;
        }

        repository.fetchWalletInfo(userId, new WalletRepository.OnWalletFetchListener() {
            @Override
            public void onSuccess(WalletInfo walletInfo) {
                long newBalance = walletInfo.coinBalance + amount;
                repository.updateWalletBalance(userId, newBalance, new WalletRepository.OnWalletOperationListener() {
                    @Override
                    public void onSuccess() {
                        repository.addTransaction(userId, type, amount, newBalance, description, listener);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (listener != null) {
                            listener.onFailure(e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (listener != null) {
                    listener.onFailure(e);
                }
            }
        });
    }

    public void deductCoins(String userId, long amount, TransactionType type, String description, WalletRepository.OnWalletOperationListener listener) {
        if (amount <= 0) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("Amount must be positive"));
            }
            return;
        }

        repository.fetchWalletInfo(userId, new WalletRepository.OnWalletFetchListener() {
            @Override
            public void onSuccess(WalletInfo walletInfo) {
                if (walletInfo.coinBalance < amount) {
                    if (listener != null) {
                        listener.onFailure(new IllegalArgumentException("Insufficient coins"));
                    }
                    return;
                }
                long newBalance = walletInfo.coinBalance - amount;
                repository.updateWalletBalance(userId, newBalance, new WalletRepository.OnWalletOperationListener() {
                    @Override
                    public void onSuccess() {
                        repository.addTransaction(userId, type, -amount, newBalance, description, listener);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (listener != null) {
                            listener.onFailure(e);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (listener != null) {
                    listener.onFailure(e);
                }
            }
        });
    }

    public void fetchTransactionHistory(String userId, long limit, WalletRepository.OnTransactionHistoryListener listener) {
        repository.fetchTransactionHistory(userId, limit, listener);
    }

    public void clearCache(String userId) {
        if (repositoryObservers.containsKey(userId)) {
            Observer<WalletInfo> observer = repositoryObservers.remove(userId);
            if (observer != null) {
                repository.getWalletInfoLiveData(userId).removeObserver(observer);
            }
        }
        walletCache.remove(userId);
        repository.unsubscribeWalletUpdates(userId);
    }
}
