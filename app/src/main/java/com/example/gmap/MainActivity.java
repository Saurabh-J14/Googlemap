package com.example.gmap;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.example.gmap.databinding.ActivityMainBinding;
import com.example.gmap.helper.LocationTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final String TAG = "MainActivity";
    private GoogleMap googleMap;
    private LocationTracker locationTracker;
    private ActivityMainBinding binding;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline polyline;
    private LatLng targetLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        locationTracker = new LocationTracker(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        targetLocation = new LatLng(28.61815670046732, 77.27900317616552);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        locationTracker.setLocationUpdateListener(new LocationTracker.LocationUpdateListener() {
            @Override
            public void onLocationChanged(Location newLocation,String address) {
                updateMap(newLocation, address);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, error);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        requestLocationPermissions();

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
//        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (targetLocation != null) {
            googleMap.addMarker(new MarkerOptions().position(targetLocation).title("Target Location"));
        }

    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            startTracking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTracking();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void startTracking() {
        if (googleMap != null) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
        locationTracker.getLastKnownLocation();
        locationTracker.startLocationUpdates();
    }

    @SuppressLint("SetTextI18n")
    private void updateMap(Location newLocation, String address) {
        if (googleMap == null) return;
        binding.addressText.setText("Current Address: " + address);

        LatLng currentLatLng = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
        pathPoints.add(currentLatLng);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f));

        if (polyline != null) {
            polyline.remove();
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(pathPoints)
                .color(Color.BLUE)
                .width(10f);
        polyline = googleMap.addPolyline(polylineOptions);

        if (targetLocation != null) {
            Location targetLoc = new Location("target");
            targetLoc.setLatitude(targetLocation.latitude);
            targetLoc.setLongitude(targetLocation.longitude);
            float distance = newLocation.distanceTo(targetLoc);
            binding.distanceText.setText("Target: " + String.format("%.2f", distance / 1000) + " km");
        } else {
            binding.distanceText.setText("Target: N/A");
        }

        Log.d(TAG, "Path Points: " + pathPoints.toString());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationTracker.stopLocationUpdates();
    }
}