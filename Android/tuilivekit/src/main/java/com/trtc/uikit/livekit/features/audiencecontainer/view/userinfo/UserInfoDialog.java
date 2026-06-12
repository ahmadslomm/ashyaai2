package com.trtc.uikit.livekit.features.audiencecontainer.view.userinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import com.tencent.cloud.tuikit.engine.room.TUIRoomDefine;
import com.tencent.imsdk.v2.V2TIMFollowInfo;
import com.tencent.imsdk.v2.V2TIMManager;
import com.tencent.imsdk.v2.V2TIMValueCallback;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.util.ToastUtil;
import com.trtc.tuikit.common.imageloader.ImageLoader;
import com.trtc.tuikit.common.ui.PopupDialog;
import com.trtc.uikit.livekit.R;
import com.trtc.uikit.livekit.common.LiveKitLogger;
import com.trtc.uikit.livekit.features.audiencecontainer.manager.AudienceManager;
import com.trtc.uikit.livekit.features.wallet.model.WalletInfo;
import com.trtc.uikit.livekit.features.wallet.service.WalletService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressLint("ViewConstructor")
public class UserInfoDialog extends PopupDialog {
    private static final LiveKitLogger LOGGER = LiveKitLogger.getLiveStreamLogger("UserInfoDialog");

    private final Context                    mContext;
    private       Button                     mButtonFollow;
    private       TextView                   mTextUserName;
    private final AudienceManager            mAudienceManager;
    private       TextView                   mTextUserId;
    private       ImageView                  mImageAvatar;
    private       TextView                   mTextFans;
    private       TextView                   mTextWalletBalance;
    private       TUIRoomDefine.SeatFullInfo mUserInfo;

    private final Observer<WalletInfo> mWalletObserver = walletInfo -> {
        if (walletInfo == null) {
            return;
        }
        if (mTextWalletBalance != null) {
            mTextWalletBalance.setText(getContext().getString(R.string.wallet_balance_text, walletInfo.coinBalance));
        }
    };

    private final Observer<Set<String>> mFollowingUserObserver = this::onFollowingUserChanged;

    public UserInfoDialog(Context context, AudienceManager audienceManager) {
        super(context);
        mContext = context;
        mAudienceManager = audienceManager;
        initView();
    }

    public void init(TUIRoomDefine.SeatFullInfo userInfo) {
        mUserInfo = userInfo;
        mAudienceManager.getUserManager().checkFollowUser(userInfo.userId);
        updateView();
    }

    public void init(TUIRoomDefine.UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        if (mUserInfo == null) {
            mUserInfo = new TUIRoomDefine.SeatFullInfo();
        }
        mUserInfo.userId = userInfo.userId;
        mUserInfo.userName = userInfo.userName;
        mUserInfo.userAvatar = userInfo.avatarUrl;
        mAudienceManager.getUserManager().checkFollowUser(userInfo.userId);
        updateView();
    }

    private void addObserver() {
        mAudienceManager.getUserState().followingUserList.observeForever(mFollowingUserObserver);
    }

    private void removeObserver() {
        mAudienceManager.getUserState().followingUserList.removeObserver(mFollowingUserObserver);
    }

    private void initView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.livekit_user_info, null);
        bindViewId(view);
        updateView();
        setView(view);
    }

    private void bindViewId(View view) {
        mButtonFollow = view.findViewById(R.id.btn_follow);
        mTextUserName = view.findViewById(R.id.tv_anchor_name);
        mTextUserId = view.findViewById(R.id.tv_user_id);
        mTextWalletBalance = view.findViewById(R.id.tv_wallet_balance);
        mImageAvatar = view.findViewById(R.id.iv_avatar);
        mTextFans = view.findViewById(R.id.tv_fans);
    }

    private void observeWalletBalance() {
        if (mTextWalletBalance == null || mUserInfo == null || TextUtils.isEmpty(mUserInfo.userId)) {
            return;
        }
        WalletService.getInstance().getWalletInfo(mUserInfo.userId).observeForever(mWalletObserver);
    }

    private void unobserveWalletBalance() {
        if (mUserInfo == null || TextUtils.isEmpty(mUserInfo.userId)) {
            return;
        }
        WalletService.getInstance().getWalletInfo(mUserInfo.userId).removeObserver(mWalletObserver);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        addObserver();
        getFansNumber();
        observeWalletBalance();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeObserver();
        unobserveWalletBalance();
    }

    @SuppressLint("SetTextI18n")
    private void updateView() {
        if (mUserInfo == null) {
            return;
        }
        if (TextUtils.isEmpty(mUserInfo.userId)) {
            return;
        }
        mTextUserName.setText(TextUtils.isEmpty(mUserInfo.userName) ? mUserInfo.userId : mUserInfo.userName);
        mTextUserId.setText("UserId:" + mUserInfo.userId);
        String avatarUrl = mUserInfo.userAvatar;
        if (TextUtils.isEmpty(avatarUrl)) {
            mImageAvatar.setImageResource(R.drawable.livekit_ic_avatar);
        } else {
            ImageLoader.load(mContext, mImageAvatar, avatarUrl, R.drawable.livekit_ic_avatar);
        }

        refreshFollowButton();
        mButtonFollow.setOnClickListener(this::onFollowButtonClick);
    }

    private void getFansNumber() {
        if (mUserInfo == null) {
            return;
        }
        if (TextUtils.isEmpty(mUserInfo.userId)) {
            return;
        }
        List<String> userIDList = new ArrayList<>();
        userIDList.add(mUserInfo.userId);
        V2TIMManager.getFriendshipManager().getUserFollowInfo(userIDList,
                new V2TIMValueCallback<List<V2TIMFollowInfo>>() {
                    @Override
                    public void onSuccess(List<V2TIMFollowInfo> v2TIMFollowInfos) {
                        if (v2TIMFollowInfos != null && !v2TIMFollowInfos.isEmpty()) {
                            V2TIMFollowInfo result = v2TIMFollowInfos.get(0);
                            if (result != null) {
                                mTextFans.setText(String.valueOf(result.getFollowersCount()));
                            }
                        }
                    }

                    @Override
                    public void onError(int code, String desc) {
                        LOGGER.error("UserInfoDialog" + " getUserFollowInfo failed:errorCode:" + "message:" + desc);
                        ToastUtil.toastShortMessage(code + "," + desc);
                    }
                });
    }

    private void refreshFollowButton() {
        if (mAudienceManager.getUserState().followingUserList.getValue().contains(mUserInfo.userId)) {
            mButtonFollow.setText(R.string.common_unfollow_anchor);
            mButtonFollow.setBackgroundResource(R.drawable.livekit_user_info_detail_button_unfollow);
        } else {
            mButtonFollow.setText(R.string.common_follow_anchor);
            mButtonFollow.setBackgroundResource(R.drawable.livekit_user_info_button_follow);
        }
        getFansNumber();
    }

    private void onFollowingUserChanged(Set<String> followUsers) {
        if (mUserInfo == null) {
            return;
        }
        if (TextUtils.isEmpty(mUserInfo.userId)) {
            return;
        }
        refreshFollowButton();
    }

    private void onFollowButtonClick(View view) {
        if (mAudienceManager.getUserState().followingUserList.getValue().contains(mUserInfo.userId)) {
            mAudienceManager.getUserManager().unfollowUser(mUserInfo.userId);
        } else {
            mAudienceManager.getUserManager().followUser(mUserInfo.userId);
        }
    }
}
