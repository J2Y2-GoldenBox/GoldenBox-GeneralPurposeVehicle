package com.leehoyoon.computer.goldenbox;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.concurrent.CountDownLatch;

public class BackgroundWorker extends Worker {
    final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    public FirebaseDatabase firebaseDatabase;
    public String token = "";
    public int alarmDistanceFromDestination = 500;
    public int alarmDistanceFromStart = 500;
    public int alarmDistanceFromEmergencyCar = 1000;
    public Location myLocation = null;
    public Location myLocation2 = null;

    public BackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public void onStopped() {
        super.onStopped();

        Log.d("CheckBackgroundWorker", "stopping");
    }

    @NonNull
    @Override
    public Result doWork() {
        //CountDownLatch countDownLatch = new CountDownLatch(1);

        Log.d("CheckBackgroundWorker", "working");

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
                        //FirebaseMessaging.getInstance().subscribeToTopic(token);
                        Log.d("DeviceToken", token);
                    }
                });


        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.getReference("destination").addChildEventListener(new BackgroundDestinationEventListener());

        /*try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        return Result.success();
    }

    public void perceiveEmergencyVehicle(DataSnapshot dataSnapshot, Location destinationLocation, Location startLocation){
        if(destinationLocation.getProvider() != null && token != null) {
            Location myCurrentLocation = findMyCurrentPosition();

            if (myCurrentLocation == null) {
                return;
            }

            Location center = new Location("center");
            center.setLatitude((destinationLocation.getLatitude() + startLocation.getLatitude()) / 2);
            center.setLongitude((destinationLocation.getLongitude() + startLocation.getLongitude()) / 2);

            double distanceFromCenter = distanceByDegree(myCurrentLocation, center);
            double distanceBetween = distanceByDegree(destinationLocation, startLocation);
            double distanceFromStart = distanceByDegree(myCurrentLocation, startLocation);
            double distanceFromEmergency = distanceByDegree(myCurrentLocation, destinationLocation);

            double distance = 0;
            String alarm = "";

            if (distanceFromEmergency < alarmDistanceFromDestination) {
                distance = distanceFromEmergency;
                alarm = "Destination";
                firebaseDatabase.getReference(destinationLocation.getProvider()).addChildEventListener(new BackgroundCaseEventListener(alarm));
            } else if (distanceFromCenter < distanceBetween) {
                if(distanceFromStart < alarmDistanceFromStart){
                    distance = distanceFromStart;
                    alarm = "Start";
                }
                else {
                    alarm = "NoAlarm";
                }
                firebaseDatabase.getReference(destinationLocation.getProvider()).addChildEventListener(new BackgroundCaseEventListener(alarm));
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
        final LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (((Activity)getApplicationContext()).shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ((Activity)getApplicationContext()).requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
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

    private LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            myLocation = location;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    class BackgroundDestinationEventListener implements ChildEventListener{

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(!dataSnapshot.child("finish").exists() || dataSnapshot.child("finish").getValue().equals("false")){
                Location destinationLocation = new Location(dataSnapshot.getKey());
                Location startLocation = new Location(dataSnapshot.getKey());

                destinationLocation.setLatitude(Double.parseDouble(dataSnapshot.child("destinationLatitude").getValue().toString()));
                destinationLocation.setLongitude(Double.parseDouble(dataSnapshot.child("destinationLongitude").getValue().toString()));

                startLocation.setLatitude(Double.parseDouble(dataSnapshot.child("startLatitude").getValue().toString()));
                startLocation.setLongitude(Double.parseDouble(dataSnapshot.child("startLongitude").getValue().toString()));

                perceiveEmergencyVehicle(dataSnapshot, destinationLocation, startLocation);
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(!dataSnapshot.child("finish").exists() || dataSnapshot.child("finish").getValue().equals("false")){
                Location destinationLocation = new Location(dataSnapshot.getKey());
                Location startLocation = new Location(dataSnapshot.getKey());

                destinationLocation.setLatitude(Double.parseDouble(dataSnapshot.child("destinationLatitude").getValue().toString()));
                destinationLocation.setLongitude(Double.parseDouble(dataSnapshot.child("destinationLongitude").getValue().toString()));

                startLocation.setLatitude(Double.parseDouble(dataSnapshot.child("startLatitude").getValue().toString()));
                startLocation.setLongitude(Double.parseDouble(dataSnapshot.child("startLongitude").getValue().toString()));

                perceiveEmergencyVehicle(dataSnapshot, destinationLocation, startLocation);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    }

    class BackgroundCaseEventListener implements ChildEventListener {
        private Location emergencyLocation = null;
        private Location emergencyLocation2 = null;
        private boolean firstAlarmFlag = false;
        private boolean secondAlarmFlag = false;
        private String alarm;

        public BackgroundCaseEventListener(String alarm) {
            this.alarm = alarm;
        }

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if (dataSnapshot.getKey().equals("emergencyCarLocation")) {
                final Location myCurrentLocation = findMyCurrentPosition();
                emergencyLocation2 = emergencyLocation;
                emergencyLocation = new Location("emergency");

                Log.d("Emergency", dataSnapshot.getValue().toString());

                emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
                emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

                double distance = distanceByDegree(myCurrentLocation, emergencyLocation);

                Log.d("distance", distance + "m");

                alarm = "NoAlarm";

                if (distance < alarmDistanceFromEmergencyCar) {
                    if (emergencyLocation2 != null) {
                        if (checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                            alarm = "Car";

                            if(distance < 300 && !firstAlarmFlag) {
                                makeAlarm(alarm, 300, dataSnapshot.child("caseNumber").getValue().toString());
                                firstAlarmFlag = true;
                            }
                            else if(distance < 600 && !secondAlarmFlag) {
                                makeAlarm(alarm, 600, dataSnapshot.child("caseNumber").getValue().toString());
                                secondAlarmFlag = true;
                            }
                        }
                    }
                }
                updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if (dataSnapshot.getKey().equals("emergencyCarLocation")) {
                final Location myCurrentLocation = findMyCurrentPosition();
                emergencyLocation2 = emergencyLocation;
                emergencyLocation = new Location("emergency");

                Log.d("Emergency", dataSnapshot.getValue().toString());

                emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
                emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

                double distance = distanceByDegree(myCurrentLocation, emergencyLocation);

                Log.d("distance", distance + "m");

                alarm = "NoAlarm";

                if (distance < alarmDistanceFromEmergencyCar) {
                    if (emergencyLocation2 != null) {
                        if (checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                            alarm = "Car";

                            if(distance < 600 && !firstAlarmFlag) {
                                makeAlarm(alarm, 600, dataSnapshot.child("caseNumber").getValue().toString());
                                firstAlarmFlag = true;
                            }
                            else if(distance < 300 && !secondAlarmFlag) {
                                makeAlarm(alarm, 300, dataSnapshot.child("caseNumber").getValue().toString());
                                secondAlarmFlag = true;
                            }
                        }
                    }
                }
                updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    public boolean checkLocation(Location myCurrentLocation, Location emergencyLocation, Location emergencyLocation2){
        if(distanceByDegree(myCurrentLocation, emergencyLocation) < distanceByDegree(myCurrentLocation, emergencyLocation2)){
            return true;
        }
        return false;
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

    public void makeAlarm(String alarm, int distance, String caseNumber){
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
            AlarmNotification alarmNotification = new AlarmNotification(getApplicationContext());
            alarmNotification.alarm(msg, caseNumber);
            TextToSpeech textToSpeech;
            TextToSpeechListener textToSpeechListener = new TextToSpeechListener(caseNumber + "번 사건 " + msg);
            textToSpeech = new TextToSpeech(getApplicationContext(), textToSpeechListener);
            textToSpeechListener.setTextToSpeech(textToSpeech);
        }
        else {
            //alarmNotification.cancel(caseNumber);
            //deleteListItem(caseNumber);
        }
    }

    class TextToSpeechListener implements TextToSpeech.OnInitListener{
        private TextToSpeech textToSpeech;
        private String msg;

        public TextToSpeechListener(String msg){
            this.msg = msg;
        }

        public void setTextToSpeech(TextToSpeech textToSpeech){
            this.textToSpeech = textToSpeech;
            textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }

        @Override
        public void onInit(int status) {
            if(textToSpeech != null) {
                textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }
}
