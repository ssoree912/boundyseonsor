package com.example.boundy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // 로그 태그와 필요한 필드 선언
    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private int tapCount = 0;
    private Handler handler = new Handler();
    private boolean isYouTubeLaunched = false; // YouTube 앱 중복 실행 방지용 플래그
    private final Runnable resetTapCount = () -> tapCount = 0; // 탭 횟수 초기화 작업
    private final double TARGET_LATITUDE = 37.6097174; // 목표 위치 위도
    private final double TARGET_LONGITUDE = 126.9980122; // 목표 위치 경도
    private final float LOCATION_RADIUS = 100; // 목표 위치 반경(미터)

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 위치 및 센서 초기화
        tvLocation = findViewById(R.id.tvLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // GPS 권한 체크 및 위치 업데이트 시작
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        // 센서 관리자 및 근접 센서 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // 위치 업데이트 시작 메서드
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 위치 업데이트 주기 (10초)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // 높은 정확도의 위치 요청

        // 권한이 부여된 경우 위치 업데이트 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    // 위치 콜백 메서드
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                displayLocation(location); // 현재 위치를 화면에 표시
                if (!isYouTubeLaunched && isWithinTargetLocation(location)) { // 목표 위치 내 도달 시 YouTube 실행
                    openYouTubeApp();
                    isYouTubeLaunched = true; // YouTube 실행 후 플래그 설정
                }
            }
        }
    };

    // 위치 정보 화면에 표시
    private void displayLocation(Location location) {
        String locationText = "현재 위치: 위도 = " + location.getLatitude() +
                ", 경도 = " + location.getLongitude();
        tvLocation.setText(locationText);
        Log.d(TAG, locationText);
    }

    // 목표 위치 내인지 확인하는 메서드
    private boolean isWithinTargetLocation(Location location) {
        float[] distance = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                TARGET_LATITUDE, TARGET_LONGITUDE, distance);
        return distance[0] <= LOCATION_RADIUS;
    }

    // YouTube 앱 또는 웹브라우저 열기
    private void openYouTubeApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
            startActivity(intent);
            Toast.makeText(this, "YouTube 앱 실행됨.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com"));
            startActivity(webIntent);
            Toast.makeText(this, "YouTube 앱이 없어 웹브라우저로 열림.", Toast.LENGTH_SHORT).show();
        }
    }

    // 근접 센서의 이벤트 처리
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < proximitySensor.getMaximumRange()) {
                tapCount++;
                handler.removeCallbacks(resetTapCount); // 탭 횟수 초기화 타이머 제거
                handler.postDelayed(resetTapCount, 2000); // 2초 내 3번 탭 시 실행

                if (tapCount == 3) {
                    bringAppToForeground(); // 3번 탭 시 앱을 포그라운드로 이동
                    tapCount = 0; // 탭 횟수 초기화
                }
            }
        }
    }

    // 앱을 포그라운드로 이동하는 메서드
    private void bringAppToForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Toast.makeText(this, "앱이 포그라운드로 이동됨.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변화 시 처리 (현재는 필요 없음)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this); // 리스너 해제
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates(); // 권한 승인 시 위치 업데이트 시작
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
