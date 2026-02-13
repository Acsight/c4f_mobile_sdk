Here is the comprehensive **MobileSDK Installation & Usage Guide** for developers who will use your SDK. This document includes the specific package manager instructions (JitPack, NPM, Pub) you requested.

---

# 📚 MobileSDK - Developer Guide / Geliştirici Kılavuzu

---

## 🇬🇧 ENGLISH DOCUMENTATION

### 📦 Part 1: Installation

#### 1. Android Native (Gradle / JitPack)

Add the JitPack repository to your build file.

**`settings.gradle`** (or project level `build.gradle`):

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <--- Add this
    }
}

```

**`build.gradle` (Module: app):**

```gradle
dependencies {
    // Replace 'User' and 'Repo' with your GitHub username and repository name
    implementation 'com.github.User:Repo:1.0.0'
}

```

#### 2. React Native (NPM)

Install the package via npm or yarn.

```bash
npm install mobilesdk-react-native
# or
yarn add mobilesdk-react-native

```

*Note: Since the module uses native code, you might need to rebuild your android project:*

```bash
cd android && ./gradlew clean

```

#### 3. Flutter (Pub)

Add the dependency to your `pubspec.yaml`.

```yaml
dependencies:
  flutter:
    sdk: flutter
  # Add the SDK here
  mobilesdk_flutter:
    git:
      url: https://github.com/User/Repo.git
      path: mobilesdk_flutter
    # OR if published to pub.dev:
    # mobilesdk_flutter: ^1.0.0

```

Run `flutter pub get` to install.

---

### 🚀 Part 2: Quick Start

#### 🤖 Android Native (Kotlin)

**`MainActivity.kt`**:

```kotlin
import com.c4f.mobileSDK.MobileSDK

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Initialize
    MobileSDK.initialize(this, "YOUR_API_KEY")
    
    // 2. Enable Auto-Detection
    MobileSDK.getInstance().autoSetup(this)
}

```

#### ⚛️ React Native (JavaScript)

**`App.js`**:

```javascript
import { NativeModules } from 'react-native';
const { MobileSDK } = NativeModules;

// 1. Initialize
React.useEffect(() => {
  MobileSDK.initialize("YOUR_API_KEY");
  MobileSDK.autoSetup();
}, []);

```

#### 💙 Flutter (Dart)

**`main.Here is the continuation and completion of the document, including the Turkish translation.

```dart
  // 1. Initialize
  await MobileSdkFlutter.initialize('YOUR_API_KEY');
  await MobileSdkFlutter.autoSetup();
  
  runApp(const MyApp());
}

```

---

### 🎮 Part 3: Usage & Triggers

How to mark your UI elements so the SDK can detect them.

#### 🤖 Android Native

**Button Trigger:** Add a `tag` to your XML view.

```xml
<Button
    android:id="@+id/btn_buy"
    android:text="Buy Now"
    android:tag="checkout_button" /> ```

#### ⚛️ React Native
**Button Trigger:** Use the `nativeID` prop.
```javascript
<TouchableOpacity nativeID="checkout_button">
  <Text>Buy Now</Text>
</TouchableOpacity>

```

**Navigation:** Add listener to `NavigationContainer`.

```javascript
<NavigationContainer onStateChange={(s) => MobileSDK.triggerNavigationSurvey(s.routes[s.index].name)}>

```

#### 💙 Flutter

**Button Trigger:** Wrap buttons with `SurveyTrigger`.

```dart
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(onPressed: () {}, child: Text("Buy")),
)

```

**Scroll Trigger:** Use `SurveyScrollView`.

```dart
SurveyScrollView(
  threshold: 500,
  child: Column(...),
)

```

**Navigation:** Add observer to `MaterialApp`.

```dart
MaterialApp(navigatorObservers: [SurveyNavigationObserver()], home: Home())

```

---

---

## 🇹🇷 TÜRKÇE DOKÜMANTASYON

### 📦 Bölüm 1: Kurulum (Installation)

#### 1. Android Native (Gradle / JitPack)

JitPack deposunu projenize ekleyin.

**`settings.gradle`**:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <--- Bunu ekleyin
    }
}

```

**`build.gradle` (Module: app):**

```gradle
dependencies {
    // 'User' ve 'Repo' kısımlarını GitHub kullanıcı adınızla değiştirin
    implementation 'com.github.User:Repo:1.0.0'
}

```

#### 2. React Native (NPM)

Paketi npm veya yarn ile kurun.

```bash
npm install mobilesdk-react-native
# veya
yarn add mobilesdk-react-native

```

#### 3. Flutter (Pub)

`pubspec.yaml` dosyanıza ekleyin.

```yaml
dependencies:
  flutter:
    sdk: flutter
  # SDK'yı buraya ekleyin
  mobilesdk_flutter:
    git:
      url: https://github.com/User/Repo.git
      path: mobilesdk_flutter

```

Kurmak için `flutter pub get` çalıştırın.

---

### 🚀 Bölüm 2: Hızlı Başlangıç

#### 🤖 Android Native (Kotlin)

**`MainActivity.kt`**:

```kotlin
import com.c4f.mobileSDK.MobileSDK

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Başlatma
    MobileSDK.initialize(this, "API_ANAHTARINIZ")
    
    // 2. Otomatik Taramayı Aç
    MobileSDK.getInstance().autoSetup(this)
}

```

#### ⚛️ React Native (JavaScript)

**`App.js`**:

```javascript
import { NativeModules } from 'react-native';
const { MobileSDK } = NativeModules;

// 1. Başlatma
React.useEffect(() => {
  MobileSDK.initialize("API_ANAHTARINIZ");
  MobileSDK.autoSetup();
}, []);

```

#### 💙 Flutter (Dart)

**`main.dart`**:

```dart
import 'package:mobilesdk_flutter/mobilesdk_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // 1. Başlatma
  await MobileSdkFlutter.initialize('API_ANAHTARINIZ');
  await MobileSdkFlutter.autoSetup();
  
  runApp(const MyApp());
}

```

---

### 🎮 Bölüm 3: Kullanım ve Tetikleyiciler

Arayüz elemanlarını (butonlar, sayfalar) SDK'nın algılayabilmesi için nasıl işaretlemelisiniz?

#### 🤖 Android Native

**Buton Tetikleyici:** XML görünümüne `tag` ekleyin.

```xml
<Button
    android:id="@+id/btn_buy"
    android:text="Satın Al"
    android:tag="checkout_button" /> ```

#### ⚛️ React Native
**Buton Tetikleyici:** `nativeID` özelliğini kullanın.
```javascript
<TouchableOpacity nativeID="checkout_button">
  <Text>Satın Al</Text>
</TouchableOpacity>

```

**Navigasyon:** `NavigationContainer` içine dinleyici ekleyin.

```javascript
<NavigationContainer onStateChange={(s) => MobileSDK.triggerNavigationSurvey(s.routes[s.index].name)}>

```

#### 💙 Flutter

**Buton Tetikleyici:** Butonları `SurveyTrigger` ile sarmalayın.

```dart
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(onPressed: () {}, child: Text("Satın Al")),
)

```

**Scroll Tetikleyici:** `SurveyScrollView` kullanın.

```dart
SurveyScrollView(
  threshold: 500, // 500 piksel kaydırınca tetikler
  child: Column(...),
)

```

**Navigasyon:** `MaterialApp`'e gözlemci (observer) ekleyin.

```dart
MaterialApp(navigatorObservers: [SurveyNavigationObserver()], home: Home())

```