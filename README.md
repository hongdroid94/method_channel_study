# Flutter Method Channel 학습 프로젝트

Flutter와 Android Native 간의 Method Channel 및 Event Channel 통신을 학습하기 위한 예제 프로젝트입니다.

## 📱 프로젝트 개요

이 프로젝트는 Flutter 앱에서 Android Native 기능에 접근하는 방법을 다양한 예제를 통해 학습할 수 있도록 구성되었습니다.

### 주요 학습 내용
- **Method Channel**: Flutter에서 Android Native 메서드 호출
- **Event Channel**: Android에서 Flutter로 연속적인 데이터 스트림 전송
- **파라미터 전달**: 복잡한 데이터 타입의 양방향 전달
- **에러 핸들링**: 플랫폼 간 에러 처리 방법

## 🔧 Method Channel이란?

Method Channel은 Flutter와 플랫폼별 네이티브 코드(Android/iOS) 간의 통신을 위한 메커니즘입니다.

### 특징
- **비동기 통신**: Future를 사용한 비동기 메서드 호출
- **양방향 통신**: Flutter ↔ Native 양방향 데이터 전달
- **타입 안전**: 플랫폼별 데이터 타입 자동 변환
- **에러 처리**: PlatformException을 통한 에러 핸들링

### Event Channel vs Method Channel
- **Method Channel**: 일회성 요청-응답 패턴 (예: 배터리 레벨 조회)
- **Event Channel**: 연속적인 데이터 스트림 (예: 센서 데이터, 위치 정보)

## 🚀 구현된 기능

### 1. Method Channel 예제

#### 배터리 레벨 조회
```dart
// Flutter 코드
final int result = await _methodChannel.invokeMethod('getBatteryLevel');
```

```kotlin
// Android 코드
"getBatteryLevel" -> {
    val batteryLevel = getBatteryLevel()
    result.success(batteryLevel)
}
```

#### 디바이스 정보 조회 (파라미터 전달)
```dart
// Flutter 코드 - 파라미터와 함께 메서드 호출
final Map<String, dynamic> arguments = {
  'includeModel': true,
  'includeVersion': true,
};
final Map<dynamic, dynamic> result = 
    await _methodChannel.invokeMethod('getDeviceInfo', arguments);
```

```kotlin
// Android 코드 - 파라미터 처리
"getDeviceInfo" -> {
    val arguments = call.arguments as? Map<String, Any>
    val includeModel = arguments?.get("includeModel") as? Boolean ?: true
    // ... 디바이스 정보 수집 및 반환
}
```

#### 알림 표시 (단방향 통신)
```dart
// Flutter에서 Android로 알림 표시 요청
await _methodChannel.invokeMethod('showNotification', arguments);
```

### 2. Event Channel 예제

#### 센서 데이터 스트림
```dart
// Flutter 코드 - 스트림 구독
_sensorSubscription = _eventChannel
    .receiveBroadcastStream()
    .listen((event) {
        // 실시간 센서 데이터 처리
    });
```

```kotlin
// Android 코드 - 스트림 데이터 전송
override fun onSensorChanged(event: SensorEvent?) {
    event?.let {
        val sensorData = "Accelerometer - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}"
        handler.post {
            sensorEventSink?.success(sensorData)
        }
    }
}
```

## 📊 프로젝트 아키텍처

### 채널 구성
- **Method Channel**: `com.hongdroid.method_channel`
- **Event Channel**: `com.hongdroid.event_channel`

### 주요 컴포넌트

#### Flutter 측 (lib/main.dart)
- `ChannelDemo`: 메인 UI 위젯
- Method Channel을 통한 Native 메서드 호출
- Event Channel을 통한 실시간 데이터 수신
- 에러 처리 및 UI 상태 관리

#### Android 측 (MainActivity.kt)
- `setupMethodChannel()`: Method Channel 설정 및 메서드 핸들러 등록
- `setupEventChannel()`: Event Channel 설정 및 스트림 핸들러 등록
- `getBatteryLevel()`: 배터리 정보 조회
- `showNotification()`: 시스템 알림 표시
- `startSensorDataStream()`: 센서 데이터 스트림 시작

## 🛠️ 실행 방법

### 필수 요구사항
- Flutter SDK 3.6.2+
- Android Studio
- Android 디바이스 또는 에뮬레이터

### 설치 및 실행
```bash
# 의존성 설치
flutter pub get

# Android 디바이스에서 실행
flutter run
```

## 📱 앱 사용법

1. **배터리 레벨 확인**: "배터리 레벨 새로고침" 버튼을 눌러 현재 배터리 상태 확인
2. **알림 표시**: "알림 표시" 버튼을 눌러 Android 시스템 알림 생성
3. **센서 데이터 스트림**: "시작" 버튼을 눌러 실시간 가속도계 데이터 스트림 시작/중지

## 🔍 학습 포인트

### 1. 채널 이름 일치
Flutter와 Native 코드에서 동일한 채널 이름을 사용해야 합니다.

### 2. 데이터 타입 매핑
- Dart `int` ↔ Kotlin `Int`
- Dart `String` ↔ Kotlin `String`
- Dart `Map` ↔ Kotlin `Map`
- Dart `List` ↔ Kotlin `List`

### 3. 에러 처리
```dart
try {
  final result = await _methodChannel.invokeMethod('methodName');
} on PlatformException catch (e) {
  print("Error: ${e.message}");
}
```

### 4. 스트림 관리
Event Channel 사용 시 메모리 누수 방지를 위해 구독 해제가 필요합니다.

## 📚 참고 자료

- [Flutter Platform Channels 공식 문서](https://docs.flutter.dev/development/platform-integration/platform-channels)
- [Android Battery Manager](https://developer.android.com/reference/android/os/BatteryManager)
- [Android Sensor Framework](https://developer.android.com/guide/topics/sensors/sensors_overview)

## 🤝 기여

이 프로젝트는 학습 목적으로 작성되었습니다. 개선사항이나 추가 예제가 있다면 언제든 기여해주세요!
