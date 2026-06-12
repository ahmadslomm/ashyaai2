# 🎙️ Social Audio App - تطبيق اجتماعي صوتي متكامل

## نظرة عامة على الرؤية

تحويل TUILiveKit إلى **منصة بث صوتي اجتماعية متكاملة** تنافس تطبيقات Bigo Live و Tami، مع الحفاظ على استقرار البنية الأساسية.

---

## 📊 المعمارية الكلية للتطبيق

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                               │
├─────────────────────────────────────────────────────────────┤
│ Activities & Fragments                                        │
│ ├── HomeActivity (Dashboard)                                  │
│ ├── RoomListActivity (غرف الصوت)                             │
│ ├── ProfileActivity (الملف الشخصي)                           │
│ ├── RoomDetailActivity (داخل الغرفة)                         │
│ ├── PKBattleActivity (معارك PK)                              │
│ └── RankingsActivity (اللوحات)                               │
├─────────────────────────────────────────────────────────────┤
│                    Features Layer                             │
├─────────────────────────────────────────────────────────────┤
│ Services (Singletons)                                         │
│ ├── UserService (الملفات الشخصية)                            │
│ ├── FollowService (المتابعة والأصدقاء)                       │
│ ├── GiftService (الهدايا المحسّنة)                           │
│ ├── PKService (منافسات PK)                                   │
│ ├── RankingService (اللوحات)                                 │
│ ├── RoleService (الأدوار والأذونات)                          │
│ ├── NotificationService (الإشعارات)                           │
│ ├── VIPService (VIP - موجود)                                 │
│ └── WalletService (المحفظة - موجودة)                         │
├─────────────────────────────────────────────────────────────┤
│                  Repository Layer                             │
├─────────────────────────────────────────────────────────────┤
│ Firebase Repositories                                         │
│ ├── UserRepository                                            │
│ ├── FollowRepository                                          │
│ ├── GiftRepository                                            │
│ ├── PKRepository                                              │
│ ├── RankingRepository                                         │
│ └── RoleRepository                                            │
├─────────────────────────────────────────────────────────────┤
│                    Firebase Layer                             │
├─────────────────────────────────────────────────────────────┤
│ Firestore Collections                                         │
│ ├── users/{userId}/profile                                    │
│ ├── users/{userId}/followers                                  │
│ ├── users/{userId}/following                                  │
│ ├── rooms/{roomId}/roles/{userId}                             │
│ ├── rooms/{roomId}/gift_rankings                              │
│ ├── gifts/{giftId}                                            │
│ ├── pk_matches/{pkId}                                         │
│ └── notifications/{notificationId}                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔄 سير العمليات الأساسية

### 1️⃣ سير العملية: إرسال هدية مع PK

```
المستخدم يضغط "إرسال هدية"
    ↓
GiftSendDialog يعرض الهدايا المتاحة
    ↓
اختيار هدية + عدد
    ↓
WalletService يتحقق من الرصيد
    (مع تطبيق خصم VIP إن وجد)
    ↓
[تصحيح: يتم خصم العملات أولاً]
WalletService.deductCoins()
    ↓
GiftService.sendGift()
    ├── إرسال الهدية عبر TRTC/Firestore
    ├── عرض تأثيرات بصرية وصوتية
    └── إضافة نقطة للـ PK (إن كانت PK نشطة)
    ↓
PKService.addPKScore()
    ├── تحديث نقاط الفريق
    ├── تحديث اللوحة الموازنة
    └── إرسال notifications للاعبين
    ↓
RankingService.updateRankings()
    ├── تحديث أكثر المرسلين
    ├── تحديث أكثر المستقبلين
    └── تحديث rankings في Firebase
```

### 2️⃣ سير العملية: المتابعة والأصدقاء

```
المستخدم يضغط "متابعة" على حساب آخر
    ↓
FollowService.followUser(targetUserId)
    ├── إضافة في Firestore: users/{myId}/following/{targetId}
    └── إضافة عكسي: users/{targetId}/followers/{myId}
    ↓
LiveData تُبدّث الـ UI فوراً
    ├── زر المتابعة يتحول إلى "يتم المتابعة"
    └── صورة المستخدم تُضاف لقائمة الأصدقاء
    ↓
NotificationService.sendNotification()
    └── إرسال إخطار للمستخدم: "فلان يتابعك"
    ↓
ActivityService.recordActivity()
    └── تسجيل في activity feed
```

### 3️⃣ سير العملية: بدء معركة PK

```
مالك الغرفة يضغط "بدء PK"
    ↓
PKDialog يعرض غرف متاحة للتحدي
    ↓
اختيار غرفة + مدة المعركة
    ↓
PKService.createPKMatch()
    ├── حفظ PKMatch في Firestore
    ├── إرسال دعوة للغرفة الأخرى
    └── بدء عداد زمني
    ↓
غرفتا PK يبدآن بتسجيل الهدايا
    ├── كل هدية = نقاط (حسب النوع)
    └── تحديث فوري في score board
    ↓
عند انتهاء الوقت
    ├── PKService.finishPKMatch()
    ├── حساب الفائز
    ├── عرض celebration animation
    └── إرسال rewards/badges
```

---

## 🏗️ مرحلة 1: الأساسيات (الأسابيع 1-2)

### A. نظام الملفات الشخصية (User Profiles)

#### البنية:
```java
// UserProfile.java - نموذج البيانات
public class UserProfile {
    public String userId;
    public String nickname;
    public String avatarUrl;
    public String bio;
    public String status;              // "online", "offline", "in_room"
    public long followersCount;
    public long followingCount;
    public long coinsBalance;
    public VipLevel vipLevel;
    public long createdAt;
    public long updatedAt;
    public List<String> badges;        // شارات خاصة
}

// OnlineStatus.java - حالة المستخدم
public enum OnlineStatus {
    OFFLINE,
    ONLINE,
    IN_ROOM_VOICE,
    IN_STREAM_VIDEO,
    IN_PK_BATTLE
}
```

#### Firebase:
```
users/{userId}/profile
├── nickname: "Ali123"
├── avatarUrl: "https://..."
├── bio: "Music lover 🎵"
├── status: "in_room_voice"
├── vipLevel: 3
├── followersCount: 1250
├── followingCount: 320
└── badges: ["first_gift", "top_sender_week"]
```

#### Services:
```java
// UserProfileService.java
class UserProfileService {
    getInstance(): UserProfileService
    
    // الحصول على الملف الشخصي
    getUserProfile(userId): LiveData<UserProfile>
    
    // تحديث الملف الشخصي
    updateProfile(profile, callback): void
    
    // تحديث حالة المستخدم
    updateStatus(status): void
    
    // رفع صورة شخصية
    uploadAvatar(imageFile, callback): void
    
    // تسجيل نشاط (دخول غرفة، إرسال هدية، إلخ)
    recordActivity(activityType, roomId): void
}

// UserProfileRepository.java
class UserProfileRepository {
    getInstance(): UserProfileRepository
    
    getProfileFromFirestore(userId): Task<UserProfile>
    updateProfileInFirestore(profile): Task<Void>
    uploadAvatarImage(file, userId): Task<Uri>
    listenToProfileUpdates(userId, liveData): void
}
```

#### الـ UI:
```kotlin
// ProfileFragment.kt
class ProfileFragment : Fragment() {
    private val userService = UserProfileService.getInstance()
    private var userProfile: UserProfile? = null
    
    fun observeProfile(userId: String) {
        userService.getUserProfile(userId).observe(viewLifecycleOwner) { profile ->
            userProfile = profile
            updateUI(profile)
        }
    }
    
    fun updateUI(profile: UserProfile) {
        // عرض الصورة
        profileImage.loadImage(profile.avatarUrl)
        
        // عرض الاسم
        nickname.text = profile.nickname
        
        // عرض عدد المتابعين
        followersCount.text = "${profile.followersCount} متابع"
        
        // عرض الحالة
        statusBadge.text = getStatusText(profile.status)
        statusBadge.color = getStatusColor(profile.status)
        
        // عرض شارات VIP
        if (profile.vipLevel > 0) {
            vipBadge.show(profile.vipLevel)
        }
    }
}

// UserProfileCard.kt - مكون مُعاد الاستخدام
@Composable
fun UserProfileCard(userId: String) {
    val userService = UserProfileService.getInstance()
    val profile by userService.getUserProfile(userId).observeAsState()
    
    profile?.let {
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(url = it.avatarUrl, size = 48.dp)
            Column {
                Text(it.nickname, style = MaterialTheme.typography.bodyMedium)
                Row {
                    OnlineStatusBadge(it.status)
                    if (it.vipLevel > 0) VipBadge(it.vipLevel)
                }
            }
        }
    }
}
```

---

### B. نظام المتابعة والأصدقاء (Follow System)

#### البنية:
```java
// FollowRelation.java
public class FollowRelation {
    public String followerId;
    public String followingId;
    public long followedAt;
    public String status;  // "active", "blocked"
}
```

#### Firebase:
```
users/{myUserId}/following/{targetUserId}
├── followedAt: 1718064000000
└── status: "active"

users/{targetUserId}/followers/{myUserId}
├── followedAt: 1718064000000
└── status: "active"
```

#### Services:
```java
// FollowService.java
class FollowService {
    getInstance(): FollowService
    
    // متابعة مستخدم
    followUser(targetUserId): Task<Void>
    
    // إلغاء المتابعة
    unfollowUser(targetUserId): Task<Void>
    
    // الحصول على قائمة المتابعين
    getFollowers(userId): LiveData<List<UserProfile>>
    
    // الحصول على قائمة من يتابعهم المستخدم
    getFollowing(userId): LiveData<List<UserProfile>>
    
    // فحص هل يتابع المستخدم الآخر
    isFollowing(targetUserId): LiveData<Boolean>
}

// FollowRepository.java
class FollowRepository {
    getInstance(): FollowRepository
    
    addFollowRelation(followerId, followingId): Task<Void>
    removeFollowRelation(followerId, followingId): Task<Void>
    getFollowersFromFirestore(userId): Task<List<String>>
    getFollowingFromFirestore(userId): Task<List<String>>
    listenToFollowingList(userId, liveData): void
}
```

#### الـ UI:
```kotlin
// FollowButton.kt
@Composable
fun FollowButton(userId: String, targetUserId: String) {
    val followService = FollowService.getInstance()
    val isFollowing by followService.isFollowing(targetUserId).observeAsState(false)
    
    Button(
        onClick = {
            if (isFollowing) {
                followService.unfollowUser(targetUserId)
            } else {
                followService.followUser(targetUserId)
            }
        }
    ) {
        Text(if (isFollowing) "يتم المتابعة" else "متابعة")
    }
}

// FriendsListFragment.kt
class FriendsListFragment : Fragment() {
    private val followService = FollowService.getInstance()
    
    fun observeFriendsList() {
        val myUserId = TUILogin.getUserId()
        followService.getFollowing(myUserId).observe(viewLifecycleOwner) { friends ->
            updateFriendsList(friends)
        }
    }
}
```

---

### C. نظام الأدوار والإدارة (Room Roles)

#### البنية:
```java
// RoomRole.java
public enum RoomRole {
    OWNER(4),          // مالك الغرفة
    ADMIN(3),          // مسؤول الغرفة
    MODERATOR(2),      // مشرف
    MEMBER(1);         // عضو عادي
    
    public int level;
}

// RoomPermission.java
public enum RoomPermission {
    KICK_USER,         // إخراج مستخدم
    MUTE_USER,         // كتم صوت
    MANAGE_SEAT,       // إدارة المقاعد
    MANAGE_ADMIN,      // إدارة المسؤولين
    CHANGE_SETTINGS,   // تغيير الإعدادات
    START_PK,          // بدء معركة PK
    SEND_ANNOUNCEMENT; // إرسال إعلان
}
```

#### Firebase:
```
rooms/{roomId}/roles/{userId}
├── role: "admin"
├── assignedAt: 1718064000000
└── assignedBy: "ownerUserId"
```

#### Services:
```java
// RoleService.java
class RoleService {
    getInstance(): RoleService
    
    // الحصول على دور المستخدم في الغرفة
    getUserRoleInRoom(roomId, userId): LiveData<RoomRole>
    
    // فحص إذا كان لديه صلاحية معينة
    hasPermission(roomId, userId, permission): LiveData<Boolean>
    
    // تعيين دور جديد
    assignRole(roomId, targetUserId, newRole): Task<Void>
    
    // إخراج مستخدم
    kickUser(roomId, userId, reason): Task<Void>
    
    // كتم صوت
    muteUser(roomId, userId, duration): Task<Void>
}

// RoleRepository.java
class RoleRepository {
    getInstance(): RoleRepository
    
    getRoleFromFirestore(roomId, userId): Task<RoomRole>
    setRoleInFirestore(roomId, userId, role): Task<Void>
    deleteRoleFromFirestore(roomId, userId): Task<Void>
    listenToRoomRoles(roomId, liveData): void
}
```

#### التكامل مع الـ UI:
```kotlin
// RoomActionMenu.kt - قائمة الخيارات داخل الغرفة
@Composable
fun RoomActionMenu(userId: String, roomId: String) {
    val roleService = RoleService.getInstance()
    val canKickUser by roleService.hasPermission(
        roomId, 
        TUILogin.getUserId(), 
        RoomPermission.KICK_USER
    ).observeAsState(false)
    
    if (canKickUser) {
        MenuItem(
            text = "إخراج المستخدم",
            onClick = {
                roleService.kickUser(roomId, userId, "Kicked by admin")
            }
        )
    }
}
```

---

## 🎁 مرحلة 2: نظام الهدايا المحسّن (الأسابيع 3-4)

### A. نموذج الهدايا المحسّن

#### البنية:
```java
// GiftRarity.java
public enum GiftRarity {
    COMMON(1),         // شائعة - بدون تأثيرات
    RARE(2),           // نادرة - تأثيرات عادية
    EPIC(3),           // ملحمية - تأثيرات قوية
    LEGENDARY(4),      // أسطورية - تأثيرات فاخرة
    VIP_EXCLUSIVE(5);  // حصرية VIP فقط
    
    public int animationLevel;
}

// Gift.java - نموذج محسّن
public class Gift {
    public String giftId;
    public String name;
    public String description;
    public int coins;              // سعر الهدية
    public GiftRarity rarity;
    public String svgaUrl;         // رسم متحرك
    public String soundUrl;        // مقطع صوت
    public boolean vipOnly;
    public List<String> tags;      // فئات: "romantic", "celebration", etc
    public long createdAt;
    public int animationDuration;  // بالميلي ثانية
    public List<ParticleEffect> particles;
}

// ParticleEffect.java
public class ParticleEffect {
    public String type;            // "sparkles", "confetti", "hearts"
    public String color;
    public int count;
    public int duration;
}
```

#### Firebase:
```
gifts/{giftId}
├── name: "🌹 Rose"
├── coins: 5
├── rarity: "COMMON"
├── svgaUrl: "gs://bucket/gifts/rose.svga"
├── soundUrl: "gs://bucket/sounds/gift_sound.mp3"
├── vipOnly: false
├── animationDuration: 2000
└── particles: [
    {
      "type": "sparkles",
      "color": "#FFB6C1",
      "count": 50,
      "duration": 1500
    }
]
```

### B. التأثيرات البصرية والصوتية

#### GiftAnimationManager:
```java
// GiftAnimationManager.java
class GiftAnimationManager {
    getInstance(): GiftAnimationManager
    
    // تشغيل تأثير الهدية
    playGiftAnimation(
        containerView: ViewGroup,
        gift: Gift,
        callback: OnAnimationComplete
    ): void
    
    // تشغيل تأثيرات الجزيئات
    playParticleEffect(
        containerView: ViewGroup,
        effect: ParticleEffect,
        duration: long
    ): void
    
    // تشغيل صوت الهدية
    playGiftSound(gift: Gift): void
    
    // إيقاف التأثيرات
    stopAllAnimations(): void
}

// GiftAnimationController.kt
class GiftAnimationController(private val view: ViewGroup) {
    
    fun playSequentialGifts(gifts: List<Gift>) {
        gifts.forEachIndexed { index, gift ->
            Handler(Looper.getMainLooper()).postDelayed({
                playGift(gift)
            }, index * 500L)  // تأخير 500ms بين كل هدية
        }
    }
    
    private fun playGift(gift: Gift) {
        // تشغيل الرسم المتحرك SVGA
        if (gift.rarity.ordinal >= GiftRarity.RARE.ordinal) {
            val svgaView = SVGAPlayer()
            view.addView(svgaView)
            svgaView.startAnimation(gift.svgaUrl)
        }
        
        // تشغيل تأثيرات الجزيئات
        gift.particles.forEach { effect ->
            playParticleEffect(effect, gift.animationDuration.toLong())
        }
        
        // تشغيل الصوت
        AudioManager.play(gift.soundUrl, volume = 0.8f)
    }
}
```

### C. لوحات المتصدرين

#### البنية:
```java
// GiftRankEntry.java
public class GiftRankEntry {
    public String userId;
    public String userName;
    public String avatarUrl;
    public long totalCoins;
    public int giftCount;
    public int rank;
    public VipLevel vipLevel;
    public long lastUpdated;
}

// RankingService.java
class RankingService {
    getInstance(): RankingService
    
    // أكثر المرسلين للهدايا
    getTopGiftSenders(
        roomId: String,
        timeRange: TimeRange,  // TODAY, WEEK, MONTH, ALL_TIME
        limit: Int = 10
    ): LiveData<List<GiftRankEntry>>
    
    // أكثر المستقبلين للهدايا
    getTopGiftReceivers(
        roomId: String,
        timeRange: TimeRange,
        limit: Int = 10
    ): LiveData<List<GiftRankEntry>>
    
    // تحديث الترتيب (يُستدعى بعد كل هدية)
    updateRankings(roomId: String, senderId: String, amount: Long): Task<Void>
}

// RankingRepository.java
class RankingRepository {
    getInstance(): RankingRepository
    
    getRankingsFromFirestore(roomId: String, timeRange: String): Task<List<GiftRankEntry>>
    updateRankingInFirestore(roomId: String, userId: String, amount: Long): Task<Void>
    listenToRankingsUpdates(roomId: String, liveData: MutableLiveData): void
}
```

#### Firebase:
```
rooms/{roomId}/gift_rankings/{timeRange}/{rank}
├── userId: "user123"
├── userName: "Ahmed"
├── avatarUrl: "..."
├── totalCoins: 50000
├── giftCount: 234
└── vipLevel: 3

Firestore Index:
├── Collection: rooms/{roomId}/gift_rankings/{timeRange}
├── Fields: totalCoins (Descending)
└── Auto-updates every time gift is sent
```

#### الـ UI:
```kotlin
// RankingsFragment.kt
class RankingsFragment : Fragment() {
    private val rankingService = RankingService.getInstance()
    
    fun observeRankings(roomId: String) {
        rankingService.getTopGiftSenders(
            roomId,
            TimeRange.TODAY,
            limit = 20
        ).observe(viewLifecycleOwner) { rankings ->
            updateRankingsList(rankings)
        }
    }
    
    fun updateRankingsList(rankings: List<GiftRankEntry>) {
        rankingsRecyclerView.adapter = RankingsAdapter(rankings).apply {
            setOnItemClickListener { entry ->
                navigateToUserProfile(entry.userId)
            }
        }
    }
}

// RankingItemView.kt - عنصر الترتيب
@Composable
fun RankingItem(entry: GiftRankEntry, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(
                if (rank <= 3) {
                    getMedalColor(rank)
                } else {
                    Color.Transparent
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الميدالية أو الترتيب
        if (rank <= 3) {
            Text(
                getMedalEmoji(rank),
                fontSize = 24.sp,
                modifier = Modifier.width(40.dp)
            )
        } else {
            Text(
                "#$rank",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(40.dp)
            )
        }
        
        // صورة المستخدم
        Image(
            url = entry.avatarUrl,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.userName, fontWeight = FontWeight.Bold)
            Text("${entry.totalCoins} عملة", style = MaterialTheme.typography.bodySmall)
        }
        
        // شارة VIP
        if (entry.vipLevel > 0) {
            VipBadgeSmall(entry.vipLevel)
        }
    }
}
```

---

## ⚔️ مرحلة 3: نظام المنافسات PK (الأسابيع 5-6)

### A. نموذج المنافسة

#### البنية:
```java
// PKMatch.java
public class PKMatch {
    public String pkId;
    public String room1Id;
    public String room2Id;
    public String owner1UserId;
    public String owner2UserId;
    public long startTime;
    public long endTime;
    public long duration;                // بالثواني
    public PKStatus status;              // "active", "finished"
    public long room1Score;
    public long room2Score;
    public String winnerId;
    public List<PKScoreEvent> scoreHistory;
}

// PKScoreEvent.java - تسجيل كل نقطة
public class PKScoreEvent {
    public String pkId;
    public String giftId;
    public String senderId;
    public String senderRoomId;
    public long points;
    public long timestamp;
    public String giftName;
}

// PKStatus.java
public enum PKStatus {
    PENDING,           // في انتظار قبول الدعوة
    ACTIVE,            // جاري
    FINISHED,          // انتهى
    CANCELLED
}
```

#### Firebase:
```
pk_matches/{pkId}
├── room1Id: "room_abc123"
├── room2Id: "room_xyz789"
├── owner1UserId: "user1"
├── owner2UserId: "user2"
├── startTime: 1718064000000
├── endTime: 1718064300000
├── duration: 300 (ثانية = 5 دقائق)
├── status: "active"
├── room1Score: 1250
├── room2Score: 950
└── scoreHistory: [{...}, {...}]

pk_matches/{pkId}/score_events/{eventId}
├── giftId: "gift_rose"
├── senderId: "user123"
├── senderRoomId: "room_abc123"
├── points: 100
└── timestamp: 1718064120000
```

### B. خدمة PK

#### Services:
```java
// PKService.java
class PKService {
    getInstance(): PKService
    
    // بدء معركة PK
    startPKBattle(
        initiatorRoomId: String,
        targetRoomId: String,
        durationSeconds: Int,
        callback: OnPKStarted
    ): Task<String>  // يرجع pkId
    
    // قبول دعوة PK
    acceptPKBattle(pkId: String): Task<Void>
    
    // رفض دعوة PK
    rejectPKBattle(pkId: String): Task<Void>
    
    // إضافة نقاط عند إرسال هدية
    addPKScore(
        pkId: String,
        roomId: String,
        gift: Gift,
        senderId: String
    ): Task<Void>
    
    // الحصول على حالة المعركة الحالية
    getPKMatch(pkId: String): LiveData<PKMatch>
    
    // إنهاء المعركة
    finishPKBattle(pkId: String): Task<Void>
    
    // الحصول على معارك PK النشطة
    getActivePKMatches(): LiveData<List<PKMatch>>
}

// PKRepository.java
class PKRepository {
    getInstance(): PKRepository
    
    createPKMatchInFirestore(match: PKMatch): Task<String>
    updatePKScoreInFirestore(pkId: String, room1Score: Long, room2Score: Long): Task<Void>
    addScoreEventInFirestore(event: PKScoreEvent): Task<Void>
    getPKMatchFromFirestore(pkId: String): Task<PKMatch>
    listenToPKUpdates(pkId: String, liveData: MutableLiveData): void
    updatePKStatus(pkId: String, status: PKStatus): Task<Void>
}
```

### C. واجهة PK

#### الـ UI:
```kotlin
// PKBattleFragment.kt
class PKBattleFragment : Fragment() {
    private val pkService = PKService.getInstance()
    private var currentPKId: String? = null
    private var timerTask: ScheduledFuture<*>? = null
    
    fun startPKMode(targetRoomId: String) {
        val myRoomId = RoomManager.getCurrentRoomId()
        
        pkService.startPKBattle(
            myRoomId,
            targetRoomId,
            durationSeconds = 300,  // 5 دقائق
            callback = object : OnPKStarted {
                override fun onSuccess(pkId: String) {
                    currentPKId = pkId
                    observePKMatch(pkId)
                    startTimer()
                }
                
                override fun onError(error: String) {
                    showError(error)
                }
            }
        )
    }
    
    fun observePKMatch(pkId: String) {
        pkService.getPKMatch(pkId).observe(viewLifecycleOwner) { match ->
            updatePKScoreBoard(match)
        }
    }
    
    fun updatePKScoreBoard(match: PKMatch) {
        // عرض الغرفة الأولى والثانية
        room1Name.text = getRoomName(match.room1Id)
        room2Name.text = getRoomName(match.room2Id)
        
        // عرض النقاط
        room1Score.text = "${match.room1Score}"
        room2Score.text = "${match.room2Score}"
        
        // عرض مؤشر الفوز (الأطول يتحرك بسرعة)
        val total = match.room1Score + match.room2Score
        if (total > 0) {
            val percentage = (match.room1Score.toFloat() / total * 100).toInt()
            progressBar.setProgress(percentage)
        }
        
        // تأثير بصري عند تحديث النقاط
        animateScoreUpdate(match)
    }
    
    private fun startTimer() {
        val timerStart = System.currentTimeMillis()
        val duration = 300000L  // 5 دقائق
        
        timerTask = ScheduledExecutorService.scheduleAtFixedRate({
            val elapsed = System.currentTimeMillis() - timerStart
            val remaining = (duration - elapsed) / 1000
            
            if (remaining <= 0) {
                endPKBattle()
            } else {
                updateTimerDisplay(remaining)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }
    
    private fun endPKBattle() {
        currentPKId?.let {
            pkService.finishPKBattle(it)
            showPKResults()
        }
        timerTask?.cancel()
    }
    
    fun showPKResults() {
        // عرض الفائز والجوائز
        val winner = if (room1Score > room2Score) room1Name else room2Name
        showCelebration(winner)
    }
}

// PKScoreBoardView.kt - عنصر واجهة اللوحة الموازنة
@Composable
fun PKScoreBoard(match: PKMatch) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Black.copy(alpha = 0.7f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // الفريق 1
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(getRoomName(match.room1Id), color = Color.White, fontWeight = FontWeight.Bold)
            Text("${match.room1Score}", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        
        // الفاصل
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(60.dp)
                .background(Color.Gray)
        )
        
        // الفريق 2
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(getRoomName(match.room2Id), color = Color.White, fontWeight = FontWeight.Bold)
            Text("${match.room2Score}", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
    
    // شريط التقدم
    LinearProgressIndicator(
        progress = match.room1Score.toFloat() / (match.room1Score + match.room2Score).coerceAtLeast(1),
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = Color.Green,
        backgroundColor = Color.Red
    )
}

// PKResultsDialog.kt
@Composable
fun PKResultsDialog(match: PKMatch, onDismiss: () -> Unit) {
    val isRoom1Won = match.room1Score > match.room2Score
    val winnerName = if (isRoom1Won) getRoomName(match.room1Id) else getRoomName(match.room2Id)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتهت المعركة! 🏆") },
        text = {
            Column {
                Text("الفائز: $winnerName", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("الفريق 1: ${match.room1Score}", fontSize = 16.sp)
                Text("الفريق 2: ${match.room2Score}", fontSize = 16.sp)
            }
        },
        buttons = {
            Button(onClick = onDismiss) {
                Text("استمرار")
            }
        }
    )
}
```

---

## 🔔 مرحلة 4: نظام الإشعارات (الأسبوع 7)

```java
// NotificationService.java
class NotificationService {
    getInstance(): NotificationService
    
    // إرسال إخطار
    sendNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType,
        data: Map<String, String>
    ): Task<Void>
    
    // الحصول على الإخطارات الحديثة
    getNotifications(limit: Int): LiveData<List<AppNotification>>
    
    // حذف إخطار
    deleteNotification(notificationId: String): Task<Void>
    
    // تعليم كمقروء
    markAsRead(notificationId: String): Task<Void>
}

// NotificationType.java
enum NotificationType {
    NEW_FOLLOWER,
    GIFT_RECEIVED,
    FRIEND_ONLINE,
    PK_INVITE,
    ROOM_STARTED,
    ACHIEVEMENT_EARNED
}
```

---

## 📊 Database Schema النهائي

### Firestore Collections:

```
├── users/
│   ├── {userId}/
│   │   ├── profile/ (basic info)
│   │   ├── followers/ (collection)
│   │   ├── following/ (collection)
│   │   ├── wallet/ (balance)
│   │   ├── wallet_transactions/ (collection)
│   │   ├── vip/ (VIP info)
│   │   ├── activities/ (collection)
│   │   └── notifications/ (collection)
│
├── rooms/
│   ├── {roomId}/
│   │   ├── info/ (room name, owner, etc)
│   │   ├── roles/ (collection - admin roles)
│   │   ├── gift_rankings/ (collection)
│   │   └── participants/ (collection)
│
├── gifts/
│   └── {giftId}/ (gift info, animations, sounds)
│
└── pk_matches/
    └── {pkId}/
        ├── match info
        └── score_events/ (collection)
```

---

## 🎯 الخطوات التالية

### Week 1-2: Phase 1
- [ ] تطوير User Profiles
- [ ] تطوير Follow System
- [ ] تطوير Room Roles

### Week 3-4: Phase 2
- [ ] تحسين نظام الهدايا
- [ ] تطوير Gift Animations
- [ ] تطوير Rankings

### Week 5-6: Phase 3
- [ ] تطوير PK System
- [ ] واجهات PK
- [ ] اختبار المنافسات

### Week 7: Phase 4
- [ ] تطوير Notifications
- [ ] Firebase Cloud Messaging
- [ ] Push notifications

---

## ✅ معايير النجاح

- ✅ جميع الأنظمة تعمل مع بعضها بسلاسة
- ✅ تحديثات فورية عبر Firebase
- ✅ واجهات سلسة وجذابة
- ✅ أداء عالي بدون تأخير
- ✅ تطبيق جاهز للإنتاج
- ✅ توافقي مع TUILiveKit الأصلي

---

## 📝 الملاحظات المهمة

1. **عدم كسر الأساسيات**: نحافظ على جميع وظائف TUILiveKit الأصلية
2. **الأداء**: استخدام Pagination و Caching
3. **الأمان**: Firestore security rules محكمة
4. **توسيع المستقبل**: بنية معمارية تسمح بإضافة ميزات جديدة
5. **التوثيق**: توثيق شامل لكل مكون

---

**آخر تحديث**: 2026-06-11
**الحالة**: جاهز للبدء بالمرحلة الأولى
