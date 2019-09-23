package com.leehoyoon.computer.goldenbox;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static java.sql.DriverManager.println;

public class ForegroundService extends Service {
    public FirebaseDatabase firebaseDatabase;
    public ChildEventListener childEventListener;
    public NotificationService notificationService;
    public String token;
    public int alarmDistanceFromDestination = 500;
    public int alarmDistanceFromStart = 500;
    public Location myLocation = null;
    public Location myLocation2 = null;

    public ForegroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startForegroundService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void startForegroundService(){
        firebaseDatabase = FirebaseDatabase.getInstance();

        childEventListener = new DestinationEventListener(getApplicationContext());
        firebaseDatabase.getReference("destination").addChildEventListener(childEventListener);

        notificationService = new NotificationService();

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

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "foreground";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Foreground",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        Intent notificationIntent = new Intent(getApplicationContext(), ReceiveActivity.class).setAction(Intent.ACTION_MAIN) .addCategory(Intent.CATEGORY_LAUNCHER) .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent);

        Notification notification = new NotificationCompat.BigTextStyle(builder).setBigContentTitle("GoldenBox").bigText("어플리케이션이 실행 중 입니다.").build();

        startForeground(1, notification);
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
                firebaseDatabase.getReference(emergencyLocation.getProvider()).addChildEventListener(new CaseEventListener(getApplicationContext()));
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
            notificationService.setNotification(msg, caseNumber);
            Intent intent = new Intent(getApplicationContext(), NotificationService.class);
            getApplicationContext().startService(intent);
        }
        else {
            Intent intent = new Intent(getApplicationContext(), notificationService.getClass());
            getApplicationContext().stopService(intent);
        }
    }

    public boolean checkLocation(Location myCurrentLocation, Location emergencyLocation, Location emergencyLocation2){
        if(distanceByDegree(myCurrentLocation, emergencyLocation) < distanceByDegree(myCurrentLocation, emergencyLocation2)){
            return true;
        }
        return false;
    }

    private void processIntent(Intent intent){
        String from = intent.getStringExtra("from");

        if(from == null){
            Log.d("Intent", "NoFrom");
            return;
        }

        Log.d("Intent", intent.getStringExtra("title") + " : " + intent.getStringExtra("body"));
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

    /*class DestinationEventListener implements ChildEventListener {
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
    };*/

    /*private class CaseEventListener implements ChildEventListener {
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
                //GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
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
                }
            }*/
            /*else if(dataSnapshot.getKey().equals("finish")){
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
            /*else if(dataSnapshot.getKey().equals("route")){
                GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                route = dataSnapshot.getValue(t);

            }*/
            /*else if(dataSnapshot.getKey().equals("finish")){
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
    };*/
}
