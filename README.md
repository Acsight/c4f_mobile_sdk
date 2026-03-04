Here is the fully updated `README.md`. I have revised it to reflect the **latest architectural changes** we made, including the dynamic parameter initialization, the updated React Native `autoSetupReact` flow, the switch to `accessibilityLabel`, and the new dynamic `SurveyScrollView` & `MobileSdkWrapper` for Flutter.

I updated both the English and Turkish sections to keep them perfectly synced. You can copy and paste this directly into your GitHub repository!

---

# 🇬🇧 MobileSDK - Technical Documentation (v2.2)

## 1. Architecture & File Structure

The project follows a **3-Tier Architecture** to separate business logic from platform-specific code.

### 📂 Directory Hierarchy

**1. Native Core Module (The Brain)**
Handles survey rules, API requests, queues, and displaying views (Dialogs/BottomSheets).

```text
/mobileSDK/src/main/java/com/example/mobileSDK/
├── MobileSDK.kt                  # 🧠 CORE SINGLETON. Manages all logic.
├── Config.kt                     # Data Models (SurveyConfig, Triggers).
├── core/SurveyPlatform.kt        # Interface contract.
└── android/AndroidMobileSDK.kt   # 🔌 WRAPPER. The interface React Native/Flutter talks to.

```

**2. React Native Bridge (The Scanner)**
Uses native interceptors to detect UI interactions without slowing down the JS thread.

```text
/mobilesdk-react-native/android/src/.../reactnative/
├── MobileSDKPackage.kt
└── MobileSDKModule.kt            # 🕵️ SCANNER. Runs 'GlobalTouchInterceptor' to find accessibility labels.

```

**3. Flutter Bridge (The Signal Receiver)**
Since Flutter draws its own pixels on a flat canvas, this module receives signals from Dart wrappers.

```text
/mobilesdk_flutter/android/src/.../mobilesdk_flutter/
├── MobileSdkFlutterPlugin.kt     # 📡 RECEIVER. Receives signals from Dart MethodChannel.

```

---

## 2. Execution Flow (How it works under the hood)

### Scenario A: Auto-Setup

What happens when `autoSetup()` is called?

1. **Platform Side (JS/Dart):** Calls `autoSetup` (or `autoSetupReact`).
2. **Native Side:**
* **Android:** Attaches `ActivityLifecycleCallbacks` to track App Start/Exit.
* **React Native:** Starts the `GlobalTouchInterceptor` to scan touched views for `accessibilityLabel` matching survey rules.
* **Flutter:** Sets up the communication channel, waiting for layout and scroll signals.



### Scenario B: Button Click (Trigger Flow)

When a user clicks a button marked for a survey:

1. **User Action:** User touches the button.
2. **Detection:**
* **Android:** Native listener intercepts the tag.
* **React Native:** Touch interceptor catches the `accessibilityLabel`.
* **Flutter:** The `SurveyTrigger` widget captures the `onPointerUp` event.


3. **Signal:** The ID (e.g., `"btn_checkout"`) is sent to `MobileSDK.kt`.
4. **Core Logic:**
* Checks Config: Is there a survey for `"btn_checkout"`?
* Checks Rules: Is user excluded? Is the cooldown period active?


5. **Result:** If valid, the `SurveyDialogFragment` or `BottomSheet` is launched safely on top of the Activity.

---

## 3. Integration Guide

### 🤖 Android Native (Kotlin)

Direct access. No bridge needed.

```kotlin
// MainActivity.kt
// 1. Initialize (Supports optional user params directly or via SharedPreferences)
MobileSDK.initializeWithBridgeParams(this, "API_KEY", "userID", Pair("rank", "chef"))

// 2. Setup Auto-Detection
MobileSDK.getInstance().autoSetup(this)

// 3. XML Layout Trigger
<Button android:id="@+id/checkout_button" ... />

```

### ⚛️ React Native

Uses the **Native Interceptor** to find component labels smoothly.

**App.js:**

```javascript
import { NativeModules } from 'react-native';
const { MobileSDK } = NativeModules;

// 1. Init
useEffect(() => {
   const init = async () => {
      await MobileSDK.initializeWithParams("API_KEY", ["userID", { rank: "chef" }]);
      // Wait for native config to download, then setup triggers
      setTimeout(() => MobileSDK.autoSetupReact(), 1000); 
   };
   init();
}, []);

// 2. Navigation
<NavigationContainer onStateChange={(state) => {
   const route = state.routes[state.index].name;
   MobileSDK.trackScreenView(route); // Required for context
}}>

// 3. UI
<TouchableOpacity accessibilityLabel="checkout_button">...</TouchableOpacity>

```

### 💙 Flutter

Uses **Smart Widgets** to signal the Native SDK. The threshold logic is dynamically handled by the backend!

**main.dart:**

```dart
import 'package:cloud4feed_mobilesdk_flutter/mobilesdk_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 1. Init with optional parameters
  await MobileSdkFlutter.initialize('API_KEY', params: ['userID', {'rank': 'chef'}]);
  await MobileSdkFlutter.autoSetup();

  runApp(
    // 2. Global Scroll Tracker (Optional: Automatically tracks all lists)
    MobileSdkWrapper(
      child: MaterialApp(
        // 3. Navigation Tracker
        navigatorObservers: [SurveyNavigationObserver()],
        home: const MyApp(),
      ),
    )
  );
}

// 4. Button Trigger
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(child: Text("Buy"), onPressed: (){}),
)

// 5. Targeted Scroll Trigger (If not using MobileSdkWrapper)
SurveyScrollView(
  child: Column(...), // Threshold is checked automatically by Native backend!
)

```

---

## 4. Debugging & Testing

Filter Logcat by the tag: **`MobileSDK`** or **`MobileSDKFlutter`**

* `👀 Continuous Scanning Started`: React Native scanner is active.
* `👆 Auto-Detected Click`: A click was caught and sent to Core.
* `✅ Found specific survey match`: Logic successful, survey opening.
* `❌ Cannot show survey`: Rules prevented display (Cooldown, Frequency Cap, etc.).

---

---

# 🇹🇷 MobileSDK - Teknik Dokümantasyon (v2.2)

## 1. Mimari ve Dosya Yapısı

Proje, iş mantığını platform kodlarından ayırmak için **3 Katmanlı Mimari** kullanır.

### 📂 Dizin Hiyerarşisi

**1. Native Core Modülü (Beyin)**
Anket kuralları, API istekleri, kuyruk yönetimi ve görünüm (Dialog/BottomSheet) buradadır.

```text
/mobileSDK/src/main/java/com/example/mobileSDK/
├── MobileSDK.kt                  # 🧠 CORE SINGLETON. Tüm mantık merkezi.
├── Config.kt                     # Veri Modelleri (SurveyConfig).
├── core/SurveyPlatform.kt        # Arayüz sözleşmesi.
└── android/AndroidMobileSDK.kt   # 🔌 WRAPPER. RN ve Flutter'ın konuştuğu kapı.

```

**2. React Native Bridge (Tarayıcı)**
JS thread'ini yavaşlatmadan arayüz etkileşimlerini algılamak için yerel (native) dinleyiciler kullanır.

```text
/mobilesdk-react-native/android/src/.../reactnative/
├── MobileSDKPackage.kt
└── MobileSDKModule.kt            # 🕵️ TARAYICI. 'GlobalTouchInterceptor' ile accessibility etiketlerini yakalar.

```

**3. Flutter Bridge (Sinyal Alıcı)**
Flutter kendi piksellerini düz bir tuval üzerine çizdiği için, bu modül Dart widget'larından gelen sinyalleri dinler.

```text
/mobilesdk_flutter/android/src/.../mobilesdk_flutter/
├── MobileSdkFlutterPlugin.kt     # 📡 ALICI. Dart MethodChannel'dan gelen emirleri uygular.

```

---

## 2. Çalışma Mantığı ve Akış (Execution Flow)

### Senaryo A: Otomatik Kurulum (Auto-Setup)

`autoSetup()` çağrıldığında arka planda ne olur?

1. **Platform Tarafı (JS/Dart):** `autoSetup` (veya `autoSetupReact`) komutunu gönderir.
2. **Native Tarafı:**
* **Android:** Uygulama Açılış/Kapanışlarını takip etmek için `ActivityLifecycleCallbacks` başlatır.
* **React Native:** Dokunulan bileşenlerdeki `accessibilityLabel` etiketlerini anket kurallarıyla eşleştirmek için `GlobalTouchInterceptor` başlatır.
* **Flutter:** İletişim kanalını açar ve arayüz/scroll sinyallerini beklemeye başlar.



### Senaryo B: Buton Tıklaması (Trigger Flow)

Kullanıcı anket tanımlı bir butona tıkladığında:

1. **Kullanıcı Eylemi:** Ekrana dokunur.
2. **Algılama:**
* **Android:** Native listener tag'i yakalar.
* **React Native:** Touch interceptor `accessibilityLabel`'ı yakalar.
* **Flutter:** `SurveyTrigger` widget'ı `onPointerUp` olayını yakalar.


3. **Sinyal:** Buton ID'si (örn: `"btn_checkout"`) `MobileSDK.kt`'ye iletilir.
4. **Core Mantık:**
* Config Kontrolü: Bu ID için bir anket var mı?
* Kural Kontrolü: Kullanıcı engelli mi? Soğuma süresi (cooldown) aktif mi?


5. **Sonuç:** Her şey uygunsa, Activity üzerinde güvenli bir şekilde `SurveyDialogFragment` veya `BottomSheet` açılır.

---

## 3. Entegrasyon Kılavuzu

### 🤖 Android Native (Kotlin)

Köprüye gerek yoktur. Doğrudan erişim sağlanır.

```kotlin
// MainActivity.kt
// 1. Başlatma (Parametreleri doğrudan veya SharedPreferences üzerinden okuyabilir)
MobileSDK.initializeWithBridgeParams(this, "API_KEY", "userID", Pair("rank", "chef"))

// 2. Otomatik Algılamayı Kur
MobileSDK.getInstance().autoSetup(this)

// 3. XML Layout Tetikleyici
<Button android:id="@+id/checkout_button" ... />

```

### ⚛️ React Native

Bileşen etiketlerini sorunsuz bulmak için **Native Interceptor** kullanır.

**App.js:**

```javascript
import { NativeModules } from 'react-native';
const { MobileSDK } = NativeModules;

// 1. Başlatma
useEffect(() => {
   const init = async () => {
      await MobileSDK.initializeWithParams("API_KEY", ["userID", { rank: "chef" }]);
      // Native konfigürasyonun inmesini bekle, sonra tetikleyicileri kur
      setTimeout(() => MobileSDK.autoSetupReact(), 1000); 
   };
   init();
}, []);

// 2. Navigasyon
<NavigationContainer onStateChange={(state) => {
   const route = state.routes[state.index].name;
   MobileSDK.trackScreenView(route); // Bağlam (context) tespiti için zorunludur
}}>

// 3. Arayüz
<TouchableOpacity accessibilityLabel="checkout_button">...</TouchableOpacity>

```

### 💙 Flutter

Native SDK'ya sinyal göndermek için **Akıllı Widget'lar** kullanır. Scroll derinliği (threshold) doğrudan backend üzerinden dinamik yönetilir!

**main.dart:**

```dart
import 'package:cloud4feed_mobilesdk_flutter/mobilesdk_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 1. Opsiyonel parametrelerle başlatma
  await MobileSdkFlutter.initialize('API_KEY', params: ['userID', {'rank': 'chef'}]);
  await MobileSdkFlutter.autoSetup();

  runApp(
    // 2. Global Scroll Takipçisi (Opsiyonel: Tüm listeleri otomatik takip eder)
    MobileSdkWrapper(
      child: MaterialApp(
        // 3. Navigasyon Takipçisi
        navigatorObservers: [SurveyNavigationObserver()],
        home: const MyApp(),
      ),
    )
  );
}

// 4. Buton Tetikleyici
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(child: Text("Satın Al"), onPressed: (){}),
)

// 5. Hedefli Scroll Tetikleyici (MobileSdkWrapper kullanılmıyorsa)
SurveyScrollView(
  child: Column(...), // Threshold değeri Native backend tarafından otomatik kontrol edilir!
)

```

---

## 4. Test ve Debugging

Logcat üzerinden **`MobileSDK`** veya **`MobileSDKFlutter`** etiketiyle filtreleyin.

* `👀 Continuous Scanning Started`: React Native tarayıcısı aktif.
* `👆 Auto-Detected Click`: Tıklama yakalandı ve Core'a iletildi.
* `✅ Found specific survey match`: Mantık başarılı, anket açılıyor.
* `❌ Cannot show survey`: Kurallar gösterimi engelledi (Soğuma süresi, gösterim sınırı vb.).