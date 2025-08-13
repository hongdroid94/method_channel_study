package com.example.study.method_channel_study

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Runnable
import java.util.Date
import kotlin.random.Random

class MainActivity : FlutterActivity() {
    // 채널 이름 정의 (Flutter와 동일해야 함)
    private val METHOD_CHANNEL = "com.hongdroid.method_channel"
    private val EVENT_CHANNEL = "com.hongdroid.event_channel"

    // 알림 채널 ID
    private val NOTIFICATION_CHANNEL_ID = "flutter_channel"

    // 센서 관련 변수들
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorEventSink: EventChannel.EventSink? = null
    private var handler = Handler(Looper.getMainLooper())
    private var sensorListener: SensorEventListener? = null
    private var simulationRunnable: Runnable? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Method Channel 설정
        setupMethodChannel(flutterEngine)

        // Event Channel 설정
        setupEventChannel(flutterEngine)

        // 알림 채널 생성 (Android 8.0 이상)
        createNotificationChannel()

        // 센서 매니저 초기화
        setupSensorManager()
    }

    // Method Channel 설정 - Flutter에서 Android 메서드 호출
    private fun setupMethodChannel(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "getBatteryLevel" -> {
                        // 배터리 레벨 가져오기
                        val batteryLevel = getBatteryLevel()
                        if (batteryLevel != -1) {
                            result.success(batteryLevel)
                        } else {
                            result.error("UNAVAILABLE", "Battery level not available.", null)
                        }
                    }

                    "getDeviceInfo" -> {
                        // 디바이스 정보 가져오기 (파라미터 처리 예제)
                        try {
                            val arguments = call.arguments as? Map<String, Any>
                            val includeModel = arguments?.get("includeModel") as? Boolean ?: true
                            val includeVersion =
                                arguments?.get("includeVersion") as? Boolean ?: true

                            val deviceInfo = mutableMapOf<String, String>()

                            if (includeModel) {
                                deviceInfo["model"] = "${Build.MANUFACTURER} ${Build.MODEL}"
                            }

                            if (includeVersion) {
                                deviceInfo["version"] = Build.VERSION.RELEASE
                            }

                            result.success(deviceInfo)
                        } catch (e: Exception) {
                            result.error("ERROR", "Failed to get device info: ${e.message}", null)
                        }
                    }

                    "showNotification" -> {
                        // 알림 표시 (단방향 통신 예제)
                        try {
                            val arguments = call.arguments as? Map<String, Any>
                            val title = arguments?.get("title") as? String ?: "Default Title"
                            val message = arguments?.get("message") as? String ?: "Default Message"

                            showNotification(title, message)
                            result.success(null)
                        } catch (e: Exception) {
                            result.error("ERROR", "Failed to show notification: ${e.message}", null)
                        }
                    }

                    else -> {
                        // 처리되지 않은 메서드
                        result.notImplemented()
                    }
                }
            }
    }


    // Event Channel 설정 - Android 에서 Flutter로 연속 데이터 전송
    private fun setupEventChannel(flutterEngine: FlutterEngine) {
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(
                    arguments: Any?,
                    events: EventChannel.EventSink?
                ) {
                    // Flutter에서 스트림 구독 시작 시 호출
                    sensorEventSink = events
                    startSensorDataStream()
                }

                override fun onCancel(arguments: Any?) {
                    // Flutter에서 스트림 구독 취소 시 호출
                    stopSensorDataStream()
                    sensorEventSink = null
                }
            })
    }

    // 배터리 레벨 가져오기
    private fun getBatteryLevel(): Int {
        return try {
            // 방법 1: BatteryManager 사용 (API 21+)
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }

    // 알림 표시
    private fun showNotification(title: String, message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(1000), notification)
    }

    // 알림 채널 생성 (Android 8.0 이상 필요)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Flutter Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Flutter notifications"
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 센서 매니저 설정
    private fun setupSensorManager() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 센서 데이터 스트림 시작
    private fun startSensorDataStream() {
        // 실제 센서 데이터 리스너
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = String.format("%.2f", it.values[0])
                    val y = String.format("%.2f", it.values[1])
                    val z = String.format("%.2f", it.values[2])
                    val timestamp = Date().toString()

                    val sensorData = "Accelerometer - X: $x, Y: $y, Z: $z | $timestamp"

                    // UI Thread 에서 Flutter로 센서 정보 데이터 전송
                    handler.post {
                        sensorEventSink?.success(sensorData)
                    }
                }
            }

            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
                // 센서 정확도 변경 시 호출
            }
        }

        // 센서 리스너 등록 (100ms 간격)
        accelerometer?.let { sensor ->
            val registered = sensorManager?.registerListener(
                sensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            ) ?: false

            if (!registered) {
                // 센서 등록 실패 시 시뮬레이션 데이터로 대체
                startSimulatedDataStream()
            }
        } ?: run {
            // 센서가 없는 경우 시뮬레이션 데이터 전송
            startSimulatedDataStream()
        }
    }

    // 시뮬레이션 데이터 스트림 (센서가 없는 경우)
    private fun startSimulatedDataStream() {
        simulationRunnable = object : Runnable {
            override fun run() {
                if (sensorEventSink != null) {
                    val randomX = String.format("%.2f", Random.nextDouble(-10.0, 10.0))
                    val randomY = String.format("%.2f", Random.nextDouble(-10.0, 10.0))
                    val randomZ = String.format("%.2f", Random.nextDouble(-10.0, 10.0))
                    val timestamp = Date().toString()

                    val simulatedData =
                        "Simulated Sensor - X: $randomX, Y: $randomY, Z: $randomZ | $timestamp"
                    sensorEventSink?.success(simulatedData)

                    // 1초 후 다시 실행
                    handler.postDelayed(this, 1000)
                }
            }
        }
        simulationRunnable?.let { handler.post(it) }
    }

    // 센서 데이터 스트림 중지
    private fun stopSensorDataStream() {
        // 실제 센서 리스너 해제
        sensorListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
            sensorListener = null
        }

        // 시뮬레이션 데이터 중지
        simulationRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
            simulationRunnable = null
        }
    }

    override fun onDestroy() {
        // 액티비티 종료 시 센서 리스너 해제
        stopSensorDataStream()
        sensorEventSink = null
        super.onDestroy()
    }
}
