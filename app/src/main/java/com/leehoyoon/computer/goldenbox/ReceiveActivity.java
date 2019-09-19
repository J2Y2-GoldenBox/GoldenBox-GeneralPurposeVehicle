package com.leehoyoon.computer.goldenbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.sql.DriverManager.println;

public class ReceiveActivity extends AppCompatActivity {
    public FirebaseDatabase firebaseDatabase;
    public Location myLocation = null;
    public Location myLocation2 = null;
    public String token = null;
    public int alarmDistanceFromDestination = 500;
    public int alarmDistanceFromStart = 500;
    public int alarmDistanceFromEmergencyCar = 1000;
    public AlarmNotification alarmNotification;
    public View view;
    public ListView listView;
    public boolean layoutFlag = false;
    public ArrayList<HashMap<String, String>> caseList;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        FirebaseMessaging.getInstance().subscribeToTopic("Alarm");

        firebaseDatabase = FirebaseDatabase.getInstance();

        firebaseDatabase.getReference("destination").addChildEventListener(childEventListener);

        alarmNotification = new AlarmNotification(ReceiveActivity.this, ReceiveActivity.this);

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("DeviceToken", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        if (token == null)
                            token = task.getResult().getToken();

                        // Log and toast

                        Log.d("DeviceToken", token);
                    }
                });

        LinearLayout linearLayout = findViewById(R.id.linearLayout);
        linearLayout.setBackground(new ShapeDrawable(new OvalShape()));
        linearLayout.setClipToOutline(true);

        LayoutInflater layoutInflater = (LayoutInflater)ReceiveActivity.this.getSystemService(ReceiveActivity.this.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.list_view_layout, null, false);

        caseList = new ArrayList<>();
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, caseList, android.R.layout.simple_list_item_2, new String[]{"title", "message"}, new int[]{android.R.id.text1, android.R.id.text2});
        listView = view.findViewById(R.id.listView);
        listView.setAdapter(simpleAdapter);
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

    public void perceiveEmergencyVehicle(DataSnapshot dataSnapshot, Location emergencyLocation, Location startLocation){
        if(emergencyLocation.getProvider() != null && token != null) {
            Location myCurrentLocation = findMyCurrentPosition();

            if (myCurrentLocation == null) {
                return;
            }

            Location center = new Location("center");
            center.setLatitude((emergencyLocation.getLatitude() + startLocation.getLatitude()) / 2);
            center.setLongitude((emergencyLocation.getLongitude() + startLocation.getLongitude()) / 2);

            double distanceFromCenter = distanceByDegree(myCurrentLocation, center);
            double distance = distanceByDegree(emergencyLocation, startLocation);
            double distanceFromStart = distanceByDegree(myCurrentLocation, startLocation);
            double distanceFromEmergency = distanceByDegree(myCurrentLocation, emergencyLocation);

            Log.d("distanceWithDestination", distance + "m");
            Log.d("distanceToDestination", distanceFromEmergency + "m");
            Log.d("distanceFromCenter", distanceFromCenter + "m");

            if (distanceFromEmergency < alarmDistanceFromDestination) {
                Log.d("distanceToDestination", distanceFromEmergency + "m");
                makeAlarm("Destination", (int) distanceFromEmergency, dataSnapshot.getKey(), emergencyLocation);
                updateInfo(dataSnapshot.getKey(), myCurrentLocation, "Destination");
            } else if (distanceFromCenter < distance) {
                if(distanceFromStart < alarmDistanceFromStart){
                    makeAlarm("Start", (int) distanceFromEmergency, dataSnapshot.getKey(), startLocation);
                }
                Log.d("distanceFromCenter", distanceFromCenter + "m");
                updateInfo(emergencyLocation.getProvider(), myCurrentLocation, "NoAlarm");
                firebaseDatabase.getReference(emergencyLocation.getProvider()).addChildEventListener(new childEventListener2());
            }
        }
    }

    public double distanceByDegree(Location locationStart, Location locationEnd){
        double _latitude1 = locationStart.getLatitude();
        double _longitude1 = locationStart.getLongitude();
        double _latitude2 = locationEnd.getLatitude();
        double _longitude2 = locationEnd.getLongitude();
        double theta, dist;

        theta = _longitude1 - _longitude2;
        dist = Math.sin(DegreeToRadian(_latitude1)) * Math.sin(DegreeToRadian(_latitude2)) + Math.cos(DegreeToRadian(_latitude1))
                * Math.cos(DegreeToRadian(_latitude2)) * Math.cos(DegreeToRadian(theta));
        dist = Math.acos(dist);
        dist = RadianToDegree(dist);

        dist = dist * 60 * 1.1515;
        dist = dist * 1.609344;    // 단위 mile 에서 km 변환.
        dist = dist * 1000.0;      // 단위  km 에서 m 로 변환

        return dist;
    }

    //degree->radian 변환
    public double DegreeToRadian(double degree){
        return degree * Math.PI / 180.0;
    }

    //randian -> degree 변환
    public double RadianToDegree(double radian){
        return radian * 180d / Math.PI;
    }

    public Location findMyCurrentPosition() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            myLocation2 = myLocation;
            myLocation = null;
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationListener);
            if (myLocation == null) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, gpsLocationListener);
                if (myLocation == null) {
                    myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (myLocation == null) {
                        myLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
            Log.d("MyCurrentLocation", myLocation.toString());
            return myLocation;
        }
        return null;
    }

    public void updateInfo(final String caseNumber, final Location myCurrentLocation, final String alarm) {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("DeviceToken", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        if(token == null)
                            token = task.getResult().getToken();

                        // Log and toast
                        Log.d("DeviceToken", token);

                        firebaseDatabase.getReference("generalUser").child(caseNumber).child(token).child("latitude").setValue(myCurrentLocation.getLatitude());
                        firebaseDatabase.getReference("generalUser").child(caseNumber).child(token).child("longitude").setValue(myCurrentLocation.getLongitude());
                        firebaseDatabase.getReference("generalUser").child(caseNumber).child(token).child("Alarm").setValue(alarm);

                        Log.d("확인", "confirm");
                    }
                });
    }

    public void makeAlarm(String alarm, int distance, String caseNumber, Location alarmLocation){
        Log.d("CaseNumber", String.valueOf(caseNumber));
        if(!alarm.equals("NoAlarm")) {
            String msg = null;
            switch (alarm) {
                case "Car":
                    msg = distance + "m 근방에 긴급차량이 있습니다.";
                    break;
                case "Destination":
                    msg = alarmDistanceFromDestination + "m 근방에 사고가 일어났습니다.";
                    break;
                case "Start":
                    msg = alarmDistanceFromStart + "m 근방에서 긴급차량이 출발합니다.";
                    break;
            }
            alarmNotification.alarm(msg, caseNumber);
            addListItem(caseNumber, alarmLocation, alarm);
        }
        else {
            alarmNotification.cancel(caseNumber);
        }
    }

    public boolean checkLocation(Location myCurrentLocation, Location emergencyLocation, Location emergencyLocation2){
        if(distanceByDegree(myCurrentLocation, emergencyLocation) < distanceByDegree(myCurrentLocation, emergencyLocation2)){
            return true;
        }
        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        println("onNewIntent 호출됨");

        if (intent != null) {
            processIntent(intent);
        }
    }

    private void processIntent(Intent intent){
        String from = intent.getStringExtra("from");

        if(from == null){
            Log.d("Intent", "NoFrom");
            return;
        }

        Log.d("Intent", intent.getStringExtra("title") + " : " + intent.getStringExtra("body"));
    }

    public void addListItem(String caseNumber, Location destination, String alarm){
        for(int i = 0; i < caseList.size(); i++){
            if(caseList.get(i).get("title").equals(caseNumber)){
                return;
            }
        }
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = null;
        String info = alarm + " : ";

        try {
            addresses = geocoder.getFromLocation(destination.getLatitude(), destination.getLongitude(), 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(addresses != null){
            if(addresses.size() == 0){
                info += "주소 정보가 없습니다.";
            }
            else{
                info += addresses.get(0).getAddressLine(0);
            }

            HashMap hashMap = new HashMap();
            hashMap.put("title", caseNumber);
            hashMap.put("message", info);
            caseList.add(hashMap);

            if(!layoutFlag){
                setContentView(view);
            }
        }
    }

    public void deleteListItem(String caseNumber){
        for(int i = 0; i < caseList.size(); i++){
            if(caseList.get(i).get("title").equals(caseNumber)){
                caseList.remove(i);
                break;
            }
        }
        if(caseList.size() == 0){
            setContentView(R.layout.activity_receive);
            LinearLayout linearLayout = findViewById(R.id.linearLayout);
            linearLayout.setBackground(new ShapeDrawable(new OvalShape()));
            linearLayout.setClipToOutline(true);
        }
    }

    private LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            /*String provider = location.getProvider();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double altitude = location.getAltitude();

            textView.setText("위치정보 : " + provider + "\n" +
                    "latitude : " + longitude + "\n" +
                    "longitude : " + latitude + "\n" +
                    "고도  : " + altitude);*/

            myLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.d("GPSUpdated", dataSnapshot.getKey() + " : " +dataSnapshot.getValue().toString());

            Log.d("Reference", dataSnapshot.toString());

            Location emergencyLocation = new Location(dataSnapshot.getKey());
            Location startLocation = new Location("emergencyLocation");
            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("destinationLatitude").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("destinationLongitude").getValue().toString()));
            startLocation.setLatitude(Double.parseDouble(dataSnapshot.child("startLatitude").getValue().toString()));
            startLocation.setLongitude(Double.parseDouble(dataSnapshot.child("startLongitude").getValue().toString()));

            perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.d("GPSUpdated", dataSnapshot.getKey() + " : " +dataSnapshot.getValue().toString());

            Location emergencyLocation = new Location(dataSnapshot.getKey());
            Location startLocation = new Location("emergencyLocation");
            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("destinationLatitude").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("destinationLongitude").getValue().toString()));
            startLocation.setLatitude(Double.parseDouble(dataSnapshot.child("startLatitude").getValue().toString()));
            startLocation.setLongitude(Double.parseDouble(dataSnapshot.child("startLongitude").getValue().toString()));

            perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            firebaseDatabase.getReference(dataSnapshot.getKey()).addChildEventListener(null);

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private class childEventListener2 implements ChildEventListener {
        private Location emergencyLocation = null;
        private Location emergencyLocation2 = null;
        //ArrayList<Map<String, Double>> route;
        private String caseNumber = "";

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey().equals("emergencyCarLocation")) {
                final Location myCurrentLocation = findMyCurrentPosition();
                emergencyLocation2 = emergencyLocation;
                emergencyLocation = new Location("emergency");

                caseNumber = dataSnapshot.child("caseNumber").getValue().toString();

                Log.d("Emergency", dataSnapshot.getValue().toString());

                emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
                emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

                double distance = distanceByDegree(myCurrentLocation, emergencyLocation);

                Log.d("distance", distance + "m");

                String alarm = "NoAlarm";

                if(distance < alarmDistanceFromEmergencyCar){
                    if(emergencyLocation2 != null) {
                    if(checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                        alarm = "Car";
                    }
                    }
                }
                makeAlarm(alarm, (int)distance, dataSnapshot.child("caseNumber").getValue().toString(), emergencyLocation);
                updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
            }
            else if(dataSnapshot.getKey().equals("route")){
                /*//GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                //route = dataSnapshot.getValue(t);

                String stringRoute = (String)dataSnapshot.getValue();
                //Log.d("route", stringRoute);

                JSONArray jsonRoute = null;
                try {
                    jsonRoute = new JSONArray(stringRoute);

                    for(int i=0;i<jsonRoute.length();i++) {
                        JSONArray pathLATLNG = jsonRoute.getJSONArray(i);
                        Double lng = pathLATLNG.getDouble(0);
                        Double lat = pathLATLNG.getDouble(1);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }*/
            }
            else if(dataSnapshot.getKey().equals("finish")){
                if(!caseNumber.equals("") && dataSnapshot.getValue().equals("true")){
                    firebaseDatabase.getReference(caseNumber).removeEventListener(this);
                    deleteListItem(caseNumber);
                    Log.d("finish", caseNumber);
                }
            }
        }

        @Override
        public void onChildChanged(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey().equals("emergencyLocation")) {
                final Location myCurrentLocation = findMyCurrentPosition();
                emergencyLocation2 = emergencyLocation;
                emergencyLocation = new Location("emergency");

                Log.d("Emergency", dataSnapshot.getValue().toString());

                emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
                emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

                double distance = distanceByDegree(myCurrentLocation, emergencyLocation);

                Log.d("distance", distance + "m");

                String alarm = "NoAlarm";

                if(distance < alarmDistanceFromEmergencyCar){
                    //if(myLocation2 != null && checkRoute(myCurrentLocation, route)) {
                    if(checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                        alarm = "Car";
                    }
                    //}
                }
                makeAlarm(alarm, (int)distance, dataSnapshot.child("caseNumber").getValue().toString(), emergencyLocation);
                updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
            }
            else if(dataSnapshot.getKey().equals("route")){
                /*GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                route = dataSnapshot.getValue(t);*/

            }
            else if(dataSnapshot.getKey().equals("finish")){
                if(!caseNumber.equals("") && dataSnapshot.getValue().equals("true")){
                    firebaseDatabase.getReference(caseNumber).addChildEventListener(null);
                    deleteListItem(caseNumber);
                    Log.d("finish", caseNumber);
                }
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            alarmNotification.cancel(dataSnapshot.child("caseNumber").getValue().toString());
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };
}
