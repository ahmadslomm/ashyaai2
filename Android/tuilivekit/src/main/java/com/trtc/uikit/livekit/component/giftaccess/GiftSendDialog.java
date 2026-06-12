package com.trtc.uikit.livekit.component.giftaccess;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.trtc.uikit.livekit.R;
import com.trtc.uikit.livekit.component.gift.GiftListView;
import com.trtc.uikit.livekit.features.vip.manager.VipManager;
import com.trtc.uikit.livekit.features.vip.model.VipLevel;
import com.trtc.uikit.livekit.features.wallet.model.WalletInfo;
import com.trtc.uikit.livekit.features.wallet.repository.WalletRepository;
import com.trtc.uikit.livekit.features.wallet.service.WalletService;
import com.trtc.uikit.livekit.features.wallet.model.TransactionType;

import io.trtc.tuikit.atomicxcore.api.Gift;

public final class GiftSendDialog extends BottomSheetDialog implements GiftListView.OnSendGiftListener {

    public static final String TAG = "GiftSendDialog";

    public String mRoomId;
    public String mOwnerId;
    public String mOwnerName;
    public String mOwnerAvatarUrl;

    private TextView mWalletBalanceView;
    private TextView mWalletAddCoinsButton;
    private WalletInfo mCurrentWalletInfo;
    private LiveData<VipLevel> mVipLevelLiveData;
    private LiveData<WalletInfo> mWalletInfoLiveData;
    private final Observer<WalletInfo> mWalletObserver = walletInfo -> {
        if (walletInfo == null) {
            return;
        }
        mCurrentWalletInfo = walletInfo;
        updateWalletBalanceView(walletInfo.coinBalance);
    };

    public GiftSendDialog(Context context, String roomId, String ownerId, String ownerName, String ownerAvatarUrl) {
        super(context);
        setContentView(R.layout.gift_layout_send_dialog_panel);
        mRoomId = roomId;
        mOwnerId = ownerId;
        mOwnerName = ownerName;
        mOwnerAvatarUrl = ownerAvatarUrl;
        init();
    }

    private void init() {
        GiftListView mGiftListView = findViewById(R.id.gift_list_view);
        if (mGiftListView != null) {
            mGiftListView.init(mRoomId);
            mGiftListView.setListener(this);
        }

        mWalletBalanceView = findViewById(R.id.tv_wallet_balance);
        mWalletAddCoinsButton = findViewById(R.id.tv_wallet_add_coins);
        if (mWalletAddCoinsButton != null) {
            mWalletAddCoinsButton.setOnClickListener(v -> ToastUtil.toastShortMessage("Please add coins in the wallet flow."));
        }

        initWalletInfo();
        setCanceledOnTouchOutside(true);
    }

    private void initWalletInfo() {
        if (mWalletBalanceView == null) {
            return;
        }
        String userId = TUILogin.getUserId();
        mWalletInfoLiveData = WalletService.getInstance().getWalletInfo(userId);
        if (mWalletInfoLiveData != null) {
            mWalletInfoLiveData.observeForever(mWalletObserver);
        }
        mVipLevelLiveData = VipManager.getInstance().getVipLevel(userId);
    }

    private void updateWalletBalanceView(long balance) {
        if (mWalletBalanceView == null) {
            return;
        }
        mWalletBalanceView.setText(getContext().getString(R.string.wallet_balance_text, balance));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View bottomSheet = findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(com.trtc.tuikit.common.R.color.common_design_bottom_sheet_color);
        }
    }

    @Override
    public void dismiss() {
        if (mWalletInfoLiveData != null) {
            mWalletInfoLiveData.removeObserver(mWalletObserver);
        }
        super.dismiss();
    }

    @Override
    public void onSendGift(GiftListView view, Gift gift, int count) {
        long giftCost = gift.coins * count;
        String userId = TUILogin.getUserId();

        long finalCost = giftCost;
        VipLevel vipLevel = VipLevel.NONE;
        if (mVipLevelLiveData != null && mVipLevelLiveData.getValue() != null) {
            vipLevel = mVipLevelLiveData.getValue();
        }
        if (vipLevel != null && vipLevel.isVip()) {
            float discount = 1.0f - (vipLevel.level * 0.02f);
            finalCost = Math.max(0, Math.round(giftCost * discount));
        }

        if (mCurrentWalletInfo == null || !mCurrentWalletInfo.hasEnough(finalCost)) {
            ToastUtil.toastShortMessage(getContext().getString(R.string.wallet_insufficient_coins));
            return;
        }

        WalletService.getInstance().deductCoins(userId, finalCost, TransactionType.SPEND_GIFT,
                "Send gift " + gift.name + " x" + count,
                new WalletRepository.OnWalletOperationListener() {
                    @Override
                    public void onSuccess() {
                        view.sendGift(gift, count);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        ToastUtil.toastShortMessage(e == null ? "Failed to send gift" : e.getMessage());
                    }
                });
    }
}



