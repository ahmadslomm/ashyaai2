# Coins Wallet Economy Implementation Summary

## Overview
A complete coins wallet economy system has been implemented for the Android TUILiveKit application, featuring Firebase-backed persistence, real-time synchronization, VIP discount integration, and gift purchase gating.

## Components Implemented

### 1. **Wallet Data Models** (`features/wallet/model/`)
- **WalletInfo.java**: Represents user wallet state
  - Fields: userId, coinBalance, updatedAt
  - Method: `hasEnough(long cost)` for transaction validation

- **WalletTransaction.java**: Represents transaction history entries
  - Fields: transactionId, userId, type, amount, balanceAfter, createdAt, description
  - Tracks all coin movements

- **TransactionType.java**: Enum for transaction categorization
  - Earning: EARN_REWARD, EARN_DAILY_LOGIN, EARN_GIFT_RECEIVED, etc.
  - Spending: SPEND_GIFT, SPEND_PREMIUM_FEATURE, SPEND_ROOM_ENTRY, etc.
  - Transfers and refunds

### 2. **Firebase Repository Layer** (`features/wallet/repository/WalletRepository.java`)
- Singleton pattern for Firestore access
- Firestore Collections:
  - `users_wallet/{userId}` - Wallet documents with coinBalance and updatedAt
  - `users_wallet/{userId}/wallet_transactions/{transactionId}` - Transaction history
- Methods:
  - `getWalletInfoLiveData(userId)` - Real-time wallet updates via LiveData
  - `fetchWalletInfo(userId, listener)` - One-time wallet fetch
  - `updateWalletBalance(userId, newBalance, listener)` - Write balance to Firestore
  - `addTransaction(userId, type, amount, balanceAfter, description, listener)` - Log transaction
  - `fetchTransactionHistory(userId, limit, listener)` - Query transaction history
  - `unsubscribeWalletUpdates(userId)` - Clean up listeners
- Real-time synchronization with Firestore snapshot listeners
- Automatic observer management and cleanup

### 3. **Business Logic Service Layer** (`features/wallet/service/WalletService.java`)
- Singleton pattern for high-level wallet operations
- Methods:
  - `getWalletInfo(userId)` - Returns cached LiveData of wallet
  - `addCoins(userId, amount, type, description, listener)` - Increase balance
  - `deductCoins(userId, amount, type, description, listener)` - Decrease balance with validation
  - `fetchTransactionHistory(userId, limit, listener)` - Get transaction records
  - `clearCache(userId)` - Cleanup wallet observers
- Local caching with automatic Firestore synchronization
- Proper observer lifecycle management to prevent memory leaks

### 4. **Gift Purchase Integration** (`component/giftaccess/GiftSendDialog.java`)
**Key Features:**
- Real-time wallet balance display in gift send dialog
- VIP discount application:
  - Formula: `finalCost = Math.round(giftCost * (1.0f - vipLevel.level * 0.02f))`
  - Level 1: 2% discount, Level 5: 10% discount
- Transaction validation before gift send
- Two-step gift send process:
  1. Deduct coins from wallet
  2. On success, send gift through gift service
- Error handling with user-friendly toast messages
- Observer lifecycle: attach on dialog creation, remove on dismiss

### 5. **User Info Display** (`features/audiencecontainer/view/userinfo/UserInfoDialog.java`)
- Display user's coin balance in user information popup
- Real-time balance updates via LiveData observer
- Proper attachment/detachment of wallet observer in dialog lifecycle

### 6. **Firebase Initialization** (`component/login/LiveKitInitializer.java`)
- `FirebaseApp.initializeApp(getContext())` called in room engine login
- Initializes Firebase SDK when user logs in
- Safe to call multiple times (Firebase handles duplicate init)
- **Note**: Requires `google-services.json` in app module for production setup

## File Structure
```
Android/tuilivekit/src/main/java/com/trtc/uikit/livekit/
├── features/wallet/
│   ├── model/
│   │   ├── WalletInfo.java
│   │   ├── WalletTransaction.java
│   │   └── TransactionType.java
│   ├── repository/
│   │   └── WalletRepository.java
│   └── service/
│       └── WalletService.java
├── component/giftaccess/
│   └── GiftSendDialog.java (MODIFIED)
├── features/audiencecontainer/view/userinfo/
│   └── UserInfoDialog.java (MODIFIED)
└── component/login/
    └── LiveKitInitializer.java (MODIFIED)
```

## UI Changes
- **Gift Send Dialog**: Added wallet balance display and "Add coins" button with placeholder
- **User Info Dialog**: Added wallet balance display
- **String Resources**: Added wallet-related strings (Balance, Add coins, Insufficient coins, etc.)

## Configuration Requirements

### 1. Gradle Dependencies
Already added to `Android/tuilivekit/build.gradle`:
```gradle
implementation platform('com.google.firebase:firebase-bom:32.2.0')
implementation 'com.google.firebase:firebase-firestore'
```

### 2. Firebase Project Setup (Required for Production)
1. Create Firebase project in [Google Firebase Console](https://console.firebase.google.com/)
2. Create Firestore database
3. Add `google-services.json` to `Android/app/src/` directory
4. Add Google Services plugin to `Android/app/build.gradle`:
   ```gradle
   plugins {
       id 'com.google.gms.google-services'
   }
   ```
5. Add plugin classpath to `Android/build.gradle`:
   ```gradle
   classpath 'com.google.gms:google-services:4.3.15'
   ```
6. **Firestore Rules** (Recommended):
   ```firestore
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users_wallet/{userId} {
         allow read: if request.auth.uid == userId;
         allow write: if request.auth.uid == userId;
       }
       match /users_wallet/{userId}/wallet_transactions/{transaction} {
         allow read: if request.auth.uid == userId;
         allow write: if request.auth.uid == userId;
       }
     }
   }
   ```

## Real-Time Features
1. **Balance Synchronization**: Wallet balance updates in real-time across all app screens
2. **Transaction Logging**: All gift purchases are logged with timestamp and description
3. **Multi-Screen Updates**: Changes to wallet propagate to gift dialog and user info screens
4. **Listener Management**: Automatic cleanup prevents memory leaks

## Gift Purchase Flow
```
User clicks "Send Gift"
    ↓
GiftSendDialog.onSendGift() called
    ↓
Calculate VIP discount (if applicable)
    ↓
Check if wallet has enough coins
    ↓
Call WalletService.deductCoins()
    ↓
WalletRepository updates Firestore balance
    ↓
Add transaction record to Firestore
    ↓
On success: Call GiftListView.sendGift()
    ↓
GiftStore processes gift send
```

## Implementation Checklist
- ✅ Wallet data models (WalletInfo, WalletTransaction, TransactionType)
- ✅ Firebase Firestore integration (WalletRepository)
- ✅ Service layer with caching (WalletService)
- ✅ Gift purchase gating with wallet validation
- ✅ VIP discount application to gift costs
- ✅ Transaction history logging
- ✅ Real-time balance updates via LiveData
- ✅ Firebase initialization in app startup
- ✅ UI displays (gift dialog, user info)
- ✅ String resources and localization
- ✅ Observer lifecycle management
- ⚠️ **Pending**: `google-services.json` configuration (user's Firebase project)
- ⚠️ **Pending**: Wallet top-up UI/flow (placeholder only)
- ⚠️ **Pending**: Transaction history UI screen

## Testing Recommendations
1. **Basic Balance**: Create user with test coins in Firebase, verify display in dialogs
2. **Gift Purchase**: Send gift, verify balance decreases correctly
3. **VIP Discount**: Grant VIP status, verify discounted cost calculated
4. **Transaction Log**: Send multiple gifts, verify transaction history in Firestore
5. **Real-Time Sync**: Modify balance in Firebase console, verify UI updates live
6. **Error Handling**: Attempt purchase with insufficient coins, verify error message

## Notes
- **Firebase Initialization**: Currently called on room engine login. For app-wide wallet features, consider initializing Firebase in Application.onCreate()
- **Observer Memory**: WalletService properly unsubscribes observers to prevent leaks
- **Firestore Structure**: All data stored under `users_wallet` collection for easy querying and security rules
- **VIP Integration**: Works seamlessly with existing VipManager and VipRepository
- **Backward Compatibility**: Changes to existing files are additive; no existing functionality removed
