package com.leehoyoon.computer.goldenbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.MarkerIcons;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShowCaseActivity extends AppCompatActivity implements OnMapReadyCallback {
    private TextView textViewStart, textViewDesination;
    private MapFragment mapFragment;
    private CaseInfo caseInfo;
    private NaverMap map = null;
    private Location myLocation = null;
    private Location newLocation = null;
    private Marker markerMyLocation;
    private LocationThread locationThread = null;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_case);

        Intent intent = new Intent(this.getIntent());
        caseInfo = intent.getParcelableExtra("Information");
        myLocation = intent.getParcelableExtra("MyLocation");

        textViewStart = findViewById(R.id.textViewStart);
        textViewDesination = findViewById(R.id.textViewDestination);

        Geocoder geocoder = new Geocoder(this);
        List<Address> startAddresses = null;
        List<Address> destinationAddresses = null;
        String startAddress = "";
        String destinationAddress = "";
        try {
            startAddresses = geocoder.getFromLocation(caseInfo.getStart().getLatitude(), caseInfo.getStart().getLongitude(), 10);
            destinationAddresses = geocoder.getFromLocation(caseInfo.getDestination().getLatitude(), caseInfo.getDestination().getLongitude(), 10);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(startAddresses != null) {
            if (startAddresses.size() == 0) {
                startAddress += "주소 정보가 없습니다.";
            } else {
                startAddress += startAddresses.get(0).getAddressLine(0);
            }
            textViewStart.setText(startAddress);
        }

        if(destinationAddresses != null) {
            if (destinationAddresses.size() == 0) {
                destinationAddress += "주소 정보가 없습니다.";
            } else {
                destinationAddress += destinationAddresses.get(0).getAddressLine(0);
            }
            textViewDesination.setText(destinationAddress);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        mapFragment = (MapFragment)fragmentManager.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fragmentManager.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!locationThread.isInterrupted()){
            locationThread.interrupt();
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.map = naverMap;
        LatLng start = new LatLng(caseInfo.getStart());
        LatLng destination = new LatLng(caseInfo.getDestination());
        LatLng centerLocation = new LatLng((caseInfo.getStart().getLatitude() + caseInfo.getDestination().getLatitude()) / 2, (caseInfo.getStart().getLongitude() + caseInfo.getDestination().getLongitude()) / 2);

        map.setCameraPosition(new CameraPosition(centerLocation, 13));

        Marker markerStart = new Marker(start);
        markerStart.setIcon(MarkerIcons.BLUE);
        markerStart.setCaptionText("Start");
        markerStart.setCaptionAligns(Align.Center);

        Marker markerDestination = new Marker(destination);
        markerDestination.setIcon(MarkerIcons.RED);
        markerDestination.setCaptionText("Destination");
        markerDestination.setCaptionAligns(Align.Center);

        markerMyLocation = new Marker(new LatLng(myLocation));
        markerMyLocation.setIcon(MarkerIcons.YELLOW);
        markerMyLocation.setCaptionText("MyLocation");
        markerMyLocation.setCaptionAligns(Align.Center);

        markerStart.setMap(map);
        markerDestination.setMap(map);
        markerMyLocation.setMap(map);

        PathOverlay path = new PathOverlay();
        if(caseInfo.getRoute() != null) {
            ArrayList<HashMap<String, Double>> route = caseInfo.getRoute();
            ArrayList<LatLng> latLngs = new ArrayList<>();
            for (int i = 0; i < route.size(); i++) {
                latLngs.add(new LatLng(route.get(i).get("latitude"), route.get(i).get("longitude")));
            }
            path.setCoords(latLngs);
            path.setColor(Color.argb(255, 100, 220, 200));
            path.setMap(map);

            locationThread = new LocationThread();
            locationThread.start();
        }
    }

    public Location findMyCurrentPosition() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            newLocation = null;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationListener);
            if (newLocation == null) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsLocationListener);
                if (newLocation == null) {
                    newLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (newLocation == null) {
                        newLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
            if(newLocation != null){
                myLocation = newLocation;
            }
            return myLocation;
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this,"위치 권한 승인이 허가되어 있습니다.",Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(this,"위치 권한을 아직 승인받지 않았습니다.",Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            newLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    class LocationThread extends Thread{
        @Override
        public void run() {
            super.run();

            while(!isInterrupted()){
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findMyCurrentPosition();
                        }
                    });
                    if(map != null){
                        markerMyLocation = new Marker();
                        markerMyLocation.setPosition(new LatLng(myLocation));
                    }
                    synchronized (this) {
                        wait(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
