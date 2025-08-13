import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Channel Communication Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const ChannelDemo(title: 'Channel Communication Demo'),
    );
  }
}

class ChannelDemo extends StatefulWidget {
  const ChannelDemo({super.key, required this.title});

  final String title;

  @override
  State<ChannelDemo> createState() => _ChannelDemoState();
}

class _ChannelDemoState extends State<ChannelDemo> {
  // method channel 정의 - Android의 특정 메서드를 호출하기 위한 채널
  static const MethodChannel _methodChannel =
      MethodChannel('com.hongdroid.method_channel');

  // event channel 정의 - Android에서 연속적인 데이터를 받기 위한 채널
  static const EventChannel _eventChannel =
      EventChannel('com.hongdroid.event_channel');

  // UI 상태 변수
  String _batteryLevel = 'Unknown';
  String _deviceInfo = 'Unknown';
  List<String> _sensorData = [];
  StreamSubscription? _sensorSubscription;
  bool _isListening = false;

  @override
  void initState() {
    super.initState();
    _getBatteryLevel();
    _getDeviceInfo();
  }

  // Method Channel을 통해 배터리 레벨 가져오기
  Future<void> _getBatteryLevel() async {
    String batteryLevel;
    try {
      // Android의 getBatteryLevel 메서드 호출
      final int result = await _methodChannel.invokeMethod('getBatteryLevel');
      batteryLevel = 'Battery level: $result%';
    } on PlatformException catch (e) {
      // 플랫폼 에러 처리
      batteryLevel = "Failed to get battery level: '${e.message}'.";
    }
    setState(() {
      _batteryLevel = batteryLevel;
    });
  }

  // Method Channel을 통해 디바이스 정보 가져오기 (파라미터 전달 활용)
  Future<void> _getDeviceInfo() async {
    try {
      // 파라미터를 포함하며 메서드 호출
      final Map<String, dynamic> arguments = {
        'includeModel': true,
        'includeVersion': true,
      };

      final Map<dynamic, dynamic> result =
          await _methodChannel.invokeMethod('getDeviceInfo', arguments);

      setState(() {
        _deviceInfo =
            'Device: ${result['model']}, Android ${result['version']}';
      });
    } on PlatformException catch (e) {
      setState(() {
        _deviceInfo = "Failed to get device info: '${e.message}'.";
      });
    }
  }

  // Event Channel을 통해 센서 데이터 스트림 시작 / 중지
  void _toggleSensorStream() {
    if (_isListening) {
      // 스트림 중지
      _sensorSubscription?.cancel();
      _sensorSubscription = null;
      setState(() {
        _isListening = false;
        _sensorData.insert(0, '--- 센서 스트림 중지됨 ---');
      });
    } else {
      // 스트림 시작
      setState(() {
        _isListening = true;
        _sensorData.clear();
        _sensorData.insert(0, '--- 센서 스트림 시작됨 ---');
      });

      _sensorSubscription = _eventChannel
          .receiveBroadcastStream() // 브로드캐스트 스트림 생성
          .listen((event) {
        // 데이터 수신 시 호출
        setState(() {
          _sensorData.insert(0, event.toString());
          // 최대 10개 항목만 유지
          if (_sensorData.length > 10) {
            _sensorData.removeLast();
          }
        });
      }, onError: (error) {
        // 에러 발생 시 호출
        setState(() {
          _sensorData.insert(0, 'Error: $error');
          _isListening = false;
        });
      }, onDone: () {
        // 스트림 완료 시 호출
        setState(() {
          _sensorData.insert(0, '--- 스트림 완료됨 ---');
          _isListening = false;
        });
      });
    }
  }

  // Android에 알림 표시 요청 (단방향 통신 예제)
  Future<void> _showNotification() async {
    try {
      final Map<String, dynamic> arguments = {
        'title': 'Flutter Notification',
        'message': 'This notification was sent from Flutter!',
      };

      await _methodChannel.invokeMethod('showNotification', arguments);
    } on PlatformException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to show notification: ${e.message}'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
        elevation: 2,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Method Channel 섹션
            Card(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Method Channel Examples',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    SizedBox(
                      height: 12,
                    ),
                    Text('배터리 레벨: $_batteryLevel'),
                    SizedBox(
                      height: 8,
                    ),
                    Text('디바이스 정보: $_deviceInfo'),
                    SizedBox(
                      height: 12,
                    ),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _getBatteryLevel,
                            child: Text('배터리 레벨 새로고침'),
                          ),
                        ),
                        SizedBox(
                          width: 8,
                        ),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _showNotification,
                            child: Text('알림 표시'),
                          ),
                        ),
                      ],
                    )
                  ],
                ),
              ),
            ),

            SizedBox(
              height: 16,
            ),

            // Event Channel 섹션
            Card(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          'Event Channel Example',
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                        SizedBox(width: 16,),
                        ElevatedButton(
                          style: ElevatedButton.styleFrom(
                              backgroundColor:
                                  _isListening ? Colors.red : Colors.green),
                          onPressed: _toggleSensorStream,
                          child: Text(_isListening ? '중지' : '시작', style: TextStyle(color: Colors.white),),
                        )
                      ],
                    ),
                    SizedBox(
                      height: 12,
                    ),
                    Text('센서 데이터 스트림 (최근 10개):'),
                    SizedBox(
                      height: 8,
                    ),
                  ],
                ),
              ),
            ),

            // 센서 데이터 리스트
            Expanded(
              child: Card(
                child: _sensorData.isEmpty
                    ? Center(
                        child: Text(
                          _isListening ? '데이터를 기다리는 중...' : '센서 스트림을 시작하세요',
                          style: TextStyle(color: Colors.grey),
                        ),
                      )
                    : ListView.builder(
                        padding: EdgeInsets.all(8),
                        itemCount: _sensorData.length,
                        itemBuilder: (context, index) {
                          return Card(
                            margin: EdgeInsets.symmetric(vertical: 2.0),
                            child: Padding(
                              padding: EdgeInsets.all(8),
                              child: Text(
                                _sensorData[index],
                                style: TextStyle(fontSize: 12),
                              ),
                            ),
                          );
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
