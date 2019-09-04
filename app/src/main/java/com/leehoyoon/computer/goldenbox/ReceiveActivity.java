package com.leehoyoon.computer.goldenbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Map;

import static java.sql.DriverManager.println;

public class ReceiveActivity extends AppCompatActivity {
    public FirebaseDatabase firebaseDatabase;
    public TextView textView;
    public Location myLocation = null;
    public Location myLocation2 = null;
    public String token = null;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        textView = findViewById(R.id.textView);

        FirebaseMessaging.getInstance().subscribeToTopic("Alarm");

        firebaseDatabase = FirebaseDatabase.getInstance();

        firebaseDatabase.getReference("목적지").addChildEventListener(childEventListener);

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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

    public void perceiveEmergencyVehicle(DataSnapshot dataSnapshot, Location emergencyLocation, Location emergencyCarLocation){
        Location myCurrentLocation = findMyCurrentPosition();

        if(myCurrentLocation == null){
            return;
        }

        Location center = new Location("center");
        center.setLatitude((emergencyLocation.getLatitude() + emergencyCarLocation.getLatitude()) / 2);
        center.setLongitude((emergencyLocation.getLongitude() + emergencyCarLocation.getLongitude()) / 2);

        double distanceFromCenter = distanceByDegree(myCurrentLocation, emergencyCarLocation);
        double distance = distanceByDegree(emergencyLocation, emergencyCarLocation);

        //double distance = myCurrentLocation.distanceTo(emergencyLocation);
        //Log.d("distance1", distance + "m");

        double distanceFromEmergency = distanceByDegree(myCurrentLocation, emergencyLocation);

        Log.d("distanceWithDestination", distance + "m");

        if(distanceFromEmergency < 500){
            Log.d("dastanceToDestination", distanceFromEmergency + "m");
            makeAlarm("Destination", (int)distanceFromEmergency);
            updateInfo(dataSnapshot.getKey(), myCurrentLocation, "Destination");
        }
        else if(distanceFromCenter < distance){
            Log.d("farFromDestination", distanceFromCenter + "m");
            firebaseDatabase.getReference("일반차량").child(emergencyLocation.getProvider()).child(token).child("Alarm").setValue("NoAlarm");
            firebaseDatabase.getReference(emergencyLocation.getProvider()).addChildEventListener(new childEventListener2());
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

    protected Location findMyCurrentPosition() {
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

                        firebaseDatabase.getReference("일반차량").child(caseNumber).child(token).child("위도").setValue(myCurrentLocation.getLatitude());
                        firebaseDatabase.getReference("일반차량").child(caseNumber).child(token).child("경도").setValue(myCurrentLocation.getLongitude());
                        firebaseDatabase.getReference("일반차량").child(caseNumber).child(token).child("Alarm").setValue(alarm);

                        Log.d("확인", "confirm");
                    }
                });
    }

    public void makeAlarm(String alarm, int distance){
        if(alarm != "NoAlarm") {
            NotificationManager notificationManager = (NotificationManager)ReceiveActivity.this.getSystemService(NOTIFICATION_SERVICE);
            Intent intent = new Intent(ReceiveActivity.this.getApplicationContext(), ReceiveActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendnoti = PendingIntent.getActivity(ReceiveActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String msg = null;

            switch (alarm) {
                case "Car":
                    msg = distance + "m 근방에 긴급차량이 있습니다.";
                    break;
                case "Destination":
                    msg = "500m 근방에 사고가 일어났습니다.";
                    break;
            }

            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setSmallIcon(R.drawable.ic_launcher_background).setTicker("Ticker").setWhen(System.currentTimeMillis())
                    .setNumber(1).setContentTitle("Content Title").setContentText("Content Text")
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setContentIntent(pendnoti).setAutoCancel(true).setOngoing(true);



            //NotificationManager를 이용하여 푸시 알림 보내기
            notificationManager.notify(1, builder.build());Log.d("확인", msg);
        }
    }

    public boolean checkRoute(Location myCurrentLocation, ArrayList<Map<String, Double>> route){
        if(route.size() > 0) {
            int minIndex = 0;
            int minIndex2 = 0;

            for (int i = 0; i < route.size(); i++) {
                Location tmp = new Location("tmp");

                tmp.setLatitude(route.get(minIndex).get("위도"));
                tmp.setLongitude(route.get(minIndex).get("경도"));
                double minDis = distanceByDegree(myCurrentLocation, tmp);

                tmp.setLatitude(route.get(i).get("위도"));
                tmp.setLongitude(route.get(i).get("경도"));
                double newDis = distanceByDegree(myCurrentLocation, tmp);

                if (newDis < minDis) {
                    minIndex2 = minIndex;
                    minIndex = i;
                }
            }
            if (minIndex - 1 != minIndex2) {
                minIndex2 = minIndex;
                minIndex++;
            }

            short routeBearing = bearingP1toP2(route.get(minIndex2).get("위도"), route.get(minIndex2).get("경도"), route.get(minIndex).get("위도"), route.get(minIndex).get("경도"));
            short myBearing = bearingP1toP2(myLocation2.getLatitude(), myLocation2.getLongitude(), myLocation.getLatitude(), myLocation.getLongitude());

            Log.d("이동방향", myBearing + ", " + routeBearing);

            Location minLocation = new Location("minLocation");
            minLocation.setLatitude(route.get(minIndex).get("위도"));
            minLocation.setLongitude(route.get(minIndex).get("경도"));

            Location minLocation2 = new Location("minLocation2");
            minLocation2.setLatitude(route.get(minIndex2).get("위도"));
            minLocation2.setLongitude(route.get(minIndex2).get("경도"));

            double rouBear = minLocation2.bearingTo(minLocation);
            double myBear = myLocation2.bearingTo(myLocation);

            Log.d("이동방향(공식)", myBear + ", " + rouBear);

            return false;
        }
        return false;
    }

    public short bearingP1toP2(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude)
    {
        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Cur_Lat_radian = P1_latitude * (3.141592 / 180);
        double Cur_Lon_radian = P1_longitude * (3.141592 / 180);


        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Dest_Lat_radian = P2_latitude * (3.141592 / 180);
        double Dest_Lon_radian = P2_longitude * (3.141592 / 180);

        // radian distance
        double radian_distance = 0;
        radian_distance = Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian) + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안값이다.
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian) * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));        // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing = 0;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0)
        {
            true_bearing = radian_bearing * (180 / 3.141592);
            true_bearing = 360 - true_bearing;
        }
        else
        {
            true_bearing = radian_bearing * (180 / 3.141592);
        }

        return (short)true_bearing;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        println("onNewIntent 호출됨");

        if(intent != null){
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

    private LocationListener gpsLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            String provider = location.getProvider();
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            double altitude = location.getAltitude();

            textView.setText("위치정보 : " + provider + "\n" +
                    "위도 : " + longitude + "\n" +
                    "경도 : " + latitude + "\n" +
                    "고도  : " + altitude);

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

            Location emergencyLocation = new Location(dataSnapshot.getKey());
            Location emergencyCarLocation = new Location("emergencyLocation");
            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("목적지위도").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("목적지경도").getValue().toString()));
            emergencyCarLocation.setLatitude(Double.parseDouble(dataSnapshot.child("소방차출발위도").getValue().toString()));
            emergencyCarLocation.setLongitude(Double.parseDouble(dataSnapshot.child("소방차출발경도").getValue().toString()));

            perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, emergencyCarLocation);
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            Log.d("GPSUpdated", dataSnapshot.getKey() + " : " +dataSnapshot.getValue().toString());

            Location emergencyLocation = new Location(dataSnapshot.getKey());
            Location emergencyCarLocation = new Location("emergencyLocation");
            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("목적지위도").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("목적지경도").getValue().toString()));
            emergencyCarLocation.setLatitude(Double.parseDouble(dataSnapshot.child("소방차출발위도").getValue().toString()));
            emergencyCarLocation.setLongitude(Double.parseDouble(dataSnapshot.child("소방차출발경도").getValue().toString()));

            perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, emergencyCarLocation);
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
        ArrayList<Map<String, Double>> route;
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey().equals("경로리스트")){
                GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                route = dataSnapshot.getValue(t);
            }
        }

        @Override
        public void onChildChanged(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
            if(dataSnapshot.getKey().equals("소방차위치")) {
                final Location myCurrentLocation = findMyCurrentPosition();
                Location emergencyLocation = new Location("emergency");

                Log.d("Emergency", dataSnapshot.getValue().toString());

                emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("위도").getValue().toString()));
                emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("경도").getValue().toString()));

                double distance = distanceByDegree(myCurrentLocation, emergencyLocation);

                Log.d("distance", distance + "m");

                String alarm = "NoAlarm";



                if(distance < 1000){
                    if(myLocation2 != null && checkRoute(myCurrentLocation, route)) {
                        alarm = "Car";
                    }
                }
                makeAlarm(alarm, (int)distance);
                updateInfo(dataSnapshot.child("사건번호").getValue().toString(), myCurrentLocation, alarm);
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
}
