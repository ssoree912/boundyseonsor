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

    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private int tapCount = 0;
    private Handler handler = new Handler();
    private boolean isYouTubeLaunched = false; // Flag to prevent multiple launches
    private final Runnable resetTapCount = () -> tapCount = 0;
    private final double TARGET_LATITUDE = 37.6097174;
    private final double TARGET_LONGITUDE = 126.9980122;
    private final float LOCATION_RADIUS = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check GPS permission and start location updates if granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }

        // Initialize sensor manager and proximity sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                displayLocation(location);
                if (!isYouTubeLaunched && isWithinTargetLocation(location)) { // Check flag before launching
                    openYouTubeApp();
                    isYouTubeLaunched = true; // Set flag after launching YouTube
                }
            }
        }
    };

    private void displayLocation(Location location) {
        String locationText = "Current location: Latitude = " + location.getLatitude() +
                ", Longitude = " + location.getLongitude();
        tvLocation.setText(locationText);
        Log.d(TAG, locationText);
    }

    private boolean isWithinTargetLocation(Location location) {
        float[] distance = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                TARGET_LATITUDE, TARGET_LONGITUDE, distance);
        return distance[0] <= LOCATION_RADIUS;
    }

    private void openYouTubeApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"));
            startActivity(intent);
            Toast.makeText(this, "YouTube app launched.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com"));
            startActivity(webIntent);
            Toast.makeText(this, "YouTube app not installed, opened in browser.", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle sensor events for proximity sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] < proximitySensor.getMaximumRange()) {
                tapCount++;
                handler.removeCallbacks(resetTapCount); // Reset timer on each tap
                handler.postDelayed(resetTapCount, 2000); // 2-second timeout for triple tap

                if (tapCount == 3) {
                    bringAppToForeground();
                    tapCount = 0; // Reset tap count after bringing app to foreground
                }
            }
        }
    }

    private void bringAppToForeground() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        Toast.makeText(this, "App brought to foreground.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Location permission required.", Toast.LENGTH_SHORT).show();
        }
    }
}
