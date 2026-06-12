# HƯỚNG DẪN TÍCH HỢP SỰ KIỆN QUẢNG CÁO & THEO DÕI HÀNH TRÌNH ĐỘC LẬP FIREBASE (KOTLIN)

Bộ SDK thu nhỏ này giúp nhà phát triển tích hợp cơ chế theo dõi hành trình người dùng (Screen Drop-offs, Exit Screen Rate) và vòng đời hiển thị quảng cáo (Load/Show, eCPM Decay, Ad Errors) trên Android Kotlin hoàn toàn độc lập với Google Firebase.

---

## 📂 1. Cấu Trúc Module SDK `LivescoreTrackingSDKKotlin`

Trong thư mục này bao gồm các file mã nguồn mẫu cấu hình sẵn:
1. `LiveScoreApiService.kt`: Giao thức Retrofit API gọi các endpoint log sự kiện.
2. `LiveScoreHttpClient.kt`: Builder tạo OkHttpClient tự động đính kèm headers nhận diện phiên bản.
3. `ScreenTracker.kt`: Trình quản lý session và log di chuyển màn hình kèm thời gian lưu lại (duration).
4. `AdSessionTracker.kt`: Bộ đếm số lần hiển thị (impression index) của quảng cáo trong session.
5. `GlobalCrashHandler.kt`: Trình bắt Exception toàn cục (crash logs) để đánh giá độ ổn định (Stability Index).

---

## 🏗️ 2. Hướng Dẫn Thiết Lập Retrofit API Client

Sử dụng thư viện Retrofit và OkHttpClient để thiết lập API Service:

```kotlin
import com.cscmobiapp.livescore.analytics.LiveScoreHttpClient
import com.cscmobiapp.livescore.analytics.LiveScoreApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 1. Tạo HttpClient kèm Header Interceptor
val okHttpClient = LiveScoreHttpClient.createClient(
    packageName = "com.cscmobiapp.livescore", // x-param1
    versionCode = "102"                       // x-param2
)

// 2. Tạo Retrofit instance
val retrofit = Retrofit.Builder()
    .baseUrl("http://vps-cua-ban.com/livescore/") // Thay bằng URL API Server
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

// 3. Khởi tạo ApiService dùng chung trong App
val liveScoreApiService = retrofit.create(LiveScoreApiService::class.java)
```

---

## 🚀 3. Tích Hợp Chi Tiết Từng Tính Năng

### 📌 3.1. Theo Dõi Chuyển Màn & Drop-off (Log Screen View)
* **Mục tiêu:** Đo lường tỷ lệ rớt màn hình qua từng bước và phát hiện màn hình thoát ứng dụng (Exit Screen Rate).
* **API:** `POST /api/v1/users/log-screen-view`

#### Cách gọi hàm:
Gọi `ScreenTracker.trackScreenView` trong hàm `onResume()` của các `Activity` hoặc `Fragment` trong dự án.

```kotlin
override fun onResume() {
    super.onResume()
    
    // Tự động log màn hình hiện tại user vừa mở
    // SDK tự tính toán thời gian ở lại (duration) của màn hình TRƯỚC ĐÓ và gửi log ngầm.
    ScreenTracker.trackScreenView(
        apiService = liveScoreApiService,
        deviceId = getAndroidId(context), // Android ID duy nhất của máy
        newScreen = "MatchDetail"         // Tên màn hình hiện tại
    )
}
```

#### Ý nghĩa tham số:
* `deviceId`: ID định danh duy nhất thiết bị (ví dụ: `Settings.Secure.ANDROID_ID`).
* `newScreen`: Tên màn hình vừa mở. Khuyên dùng định dạng camelCase (ví dụ: `Splash`, `Onboarding`, `Home`, `MatchDetail`, `PredictionsTab`).
* `previous_screen` (Tự động): Tên màn hình trước đó.
* `duration_seconds` (Tự động): Số giây người dùng lưu lại ở màn hình trước đó trước khi chuyển qua màn mới.

---

### 📌 3.2. Vòng Đời Quảng Cáo & Suy Giảm eCPM (Log Ad Event)
* **Mục tiêu:** Tính toán tỷ lệ Load/Show, cảnh báo ad unit nạp nhưng không hiển thị (Show Rate < 60%) và theo dõi eCPM Decay (suy giảm giá thầu).
* **API:** `POST /api/v1/users/log-ad-event`

#### Chu trình log khi tích hợp AdMob SDK:

**Bước A: Log khi gửi yêu cầu tải Quảng cáo (Request)**
Gọi trước khi gọi hàm `.load(...)` của AdMob SDK.
```kotlin
// event_type: "request"
logAdEvent(
    eventType = "request",
    adFormat = "interstitial",
    adUnit = "ca-app-pub-3940256099942544/1033173712",
    screen = "MatchDetail"
)
```

**Bước B: Log khi quảng cáo tải thành công (Load Success)**
Gọi trong callback `onAdLoaded()` của listener quảng cáo.
```kotlin
// event_type: "load_success"
logAdEvent(
    eventType = "load_success",
    adFormat = "interstitial",
    adUnit = adUnitId,
    screen = "MatchDetail"
)
```

**Bước C: Log khi quảng cáo tải thất bại (Load Failed)**
Gọi trong callback `onAdFailedToLoad(loadAdError)` để phát hiện lỗi nạp quảng cáo trên CMS.
```kotlin
// event_type: "load_failed"
logAdEvent(
    eventType = "load_failed",
    adFormat = "interstitial",
    adUnit = adUnitId,
    screen = "MatchDetail",
    errorCode = loadAdError.code // Mã lỗi từ SDK
)
```

**Bước D: Log khi hiển thị quảng cáo thành công (Show) và Lắng nghe eCPM**
Gọi trong callback lắng nghe doanh thu trả phí của SDK (`OnPaidEventListener`) để lấy chính xác giá trị eCPM.
```kotlin
interstitialAd.onPaidEventListener = OnPaidEventListener { adValue: AdValue ->
    // Quy đổi từ micros sang eCPM USD: valueMicros / 1000.0
    val ecpm = adValue.valueMicros / 1000.0 
    
    // Tự tăng số thứ tự quảng cáo được hiển thị trong phiên (impression_index)
    val currentImpressionIndex = AdSessionTracker.incrementAndGet("interstitial")
    
    // event_type: "show"
    logAdEvent(
        eventType = "show",
        adFormat = "interstitial",
        adUnit = adUnitId,
        screen = "MatchDetail",
        impressionIndex = currentImpressionIndex,
        ecpm = ecpm
    )
}
```

#### Mô tả Chi Tiết Tham Số Request Log Ad Event:
| Tên trường | Kiểu dữ liệu | Trạng thái | Ý nghĩa / Giá trị |
| :--- | :--- | :--- | :--- |
| `device_id` | String | Bắt buộc | Định danh ID thiết bị. |
| `session_id` | String | Bắt buộc | ID phiên hiện tại lấy từ `ScreenTracker.getSessionId()`. |
| `event_type` | String | Bắt buộc | `request` (yêu cầu nạp), `load_success` (tải xong), `load_failed` (lỗi tải), `show` (hiển thị thành công). |
| `ad_type` | String | Bắt buộc | Định dạng: `banner`, `interstitial`, `native`, `rewarded`. |
| `ad_unit_id` | String | Bắt buộc | ID Ad Unit từ AdMob. |
| `screen_name` | String | Bắt buộc | Màn hình nơi quảng cáo được hiển thị/tải. |
| `impression_index`| Int | Tùy chọn | Lần hiển thị thứ n của định dạng này trong session (chỉ gửi khi `event_type = "show"`). |
| `ecpm` | Double | Tùy chọn | Giá trị eCPM USD thực tế (chỉ gửi khi `event_type = "show"`). |
| `error_code` | Int | Tùy chọn | Mã lỗi từ AdMob SDK (chỉ gửi khi `event_type = "load_failed"`). |
| `network` | String | Mặc định | Tên mạng quảng cáo, mặc định: `"admob"`. |

---

### 📌 3.3. Theo Dõi Lỗi Hệ Thống (Stability & Client Crashes)
* **Mục tiêu:** Ghi lại crash logs của ứng dụng để tính tỷ lệ vận hành ổn định (`Stability Index = 100% - Error Rate`) trên CMS.
* **API:** `POST /api/v1/users/log-action` (với param query `action_type=app_error`)

#### Cách tích hợp:
Khởi tạo `GlobalCrashHandler` tại lớp `Application` của ứng dụng Android:

```kotlin
class LiveScoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val deviceId = getAndroidId(this)
        
        // Đăng ký bộ bắt crash logs toàn cục gửi API ngầm trước khi app tắt
        GlobalCrashHandler(
            context = this,
            apiService = liveScoreApiService,
            deviceId = deviceId
        )
    }
}
```

#### Ý nghĩa tham số của payload Crash:
* `error_type`: Tên class Exception (ví dụ: `NullPointerException`, `SQLiteException`).
* `message`: Thông điệp lỗi chi tiết (ví dụ: `Attempt to invoke virtual method on a null object reference`).
* `stack_trace`: Toàn bộ nhật ký vết ngăn xếp (Stacktrace) để debug trên server (giới hạn lấy 2000 ký tự đầu tiên).

---

## 🧪 4. Gỡ Lỗi & Kiểm Tra (Debug Mode)

Trong giai đoạn phát triển, để bỏ qua cơ chế mã hóa AES-256 trên Backend giúp việc debug JSON thô dễ dàng, hãy thêm hai tham số sau vào URL request hoặc Interceptor:
* Thêm tham số: `param1=1` và `param2=1`

Ví dụ URL API gọi từ client sẽ là:
`http://vps-cua-ban.com/livescore/api/v1/users/log-ad-event?param1=1&param2=1`
