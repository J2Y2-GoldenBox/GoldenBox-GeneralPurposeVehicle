package com.leehoyoon.computer.goldenbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;

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
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    public LoadingAnimationView loadingAnimationView;
    public View viewForAnimation;
    public LinearLayout linearLayout;
    public View viewForList;
    public ListView listView;
    public SimpleAdapter simpleAdapter;
    public boolean layoutFlag = false;
    public ArrayList<HashMap<String, String>> caseList;
    public HashMap<String, CaseInfo> caseInfos;
    public ChildEventListener childEventListener;

    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        Intent intent = new Intent(getApplicationContext(), ForegroundService.class);
        getApplicationContext().stopService(intent);

        firebaseDatabase = FirebaseDatabase.getInstance();

        childEventListener = new DestinationEventListener();
        firebaseDatabase.getReference("destination").addChildEventListener(childEventListener);

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

        linearLayout = findViewById(R.id.linearLayout);
        linearLayout.setBackground(new ShapeDrawable(new OvalShape()));
        linearLayout.setClipToOutline(true);
        loadingAnimationView = findViewById(R.id.loadingAnimationView);

        LayoutInflater layoutInflater = (LayoutInflater)ReceiveActivity.this.getSystemService(this.LAYOUT_INFLATER_SERVICE);
        viewForAnimation = layoutInflater.inflate(R.layout.activity_receive, null, false);
        viewForList = layoutInflater.inflate(R.layout.list_view_layout, null, false);

        caseInfos = new HashMap<>();
        caseList = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(this, caseList, android.R.layout.simple_list_item_2, new String[]{"title", "message"}, new int[]{android.R.id.text1, android.R.id.text2});
        listView = viewForList.findViewById(R.id.listView);
        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(new OnListViewListener());

        firebaseDatabase.getReference("finish").addChildEventListener(finishEventListener);
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
                firebaseDatabase.getReference(destinationLocation.getProvider()).addChildEventListener(new CaseEventListener(destinationLocation, startLocation, distance, alarm));
            } else if (distanceFromCenter < distanceBetween) {
                if(distanceFromStart < alarmDistanceFromStart){
                    distance = distanceFromStart;
                    alarm = "Start";
                }
                else {
                    alarm = "NoAlarm";
                }
                firebaseDatabase.getReference(destinationLocation.getProvider()).addChildEventListener(new CaseEventListener(destinationLocation, startLocation, distance, alarm));
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

    public void makeAlarm(String alarm, int distance, String caseNumber, Location alarmLocation, CaseInfo caseInfo){
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
            alarmNotification = new AlarmNotification(ReceiveActivity.this);
            alarmNotification.alarm(msg, caseNumber);
            TextToSpeech textToSpeech;
            TextToSpeechListener textToSpeechListener = new TextToSpeechListener(caseNumber + "번 사건 " + msg);
            textToSpeech = new TextToSpeech(getApplicationContext(), textToSpeechListener);
            textToSpeechListener.setTextToSpeech(textToSpeech);

            addListItem(caseNumber, alarmLocation, caseInfo, alarm);
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

    public void addListItem(String caseNumber, Location alarmLocation, CaseInfo caseInfo, String alarm){
        caseInfos.put(caseNumber, caseInfo);

        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = null;
        String info = alarm + " : ";

        try {
            addresses = geocoder.getFromLocation(alarmLocation.getLatitude(), alarmLocation.getLongitude(), 10);
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

            for(int i = 0; i < caseList.size(); i++){
                if(caseList.get(i).get("title").equals(caseNumber)){
                    caseList.set(i, hashMap);
                    simpleAdapter.notifyDataSetChanged();
                    simpleAdapter.notifyDataSetInvalidated();
                    return;
                }
            }

            caseList.add(hashMap);
            simpleAdapter.notifyDataSetChanged();

            if(!layoutFlag){
                loadingAnimationView = viewForAnimation.findViewById(R.id.loadingAnimationView);
                loadingAnimationView.stopAnimation();
                setContentView(viewForList);
                layoutFlag = true;
            }
        }
    }

    public void deleteListItem(String caseNumber){
        for(int i = 0; i < caseList.size(); i++){
            if(caseList.get(i).get("title").equals(caseNumber)){
                caseList.remove(i);
                listView.clearChoices();
                simpleAdapter.notifyDataSetChanged();
                simpleAdapter.notifyDataSetInvalidated();
                break;
            }
        }
        if(layoutFlag) {
            if (caseList.size() <= 0) {
                setContentView(viewForAnimation);
                linearLayout = findViewById(R.id.linearLayout);
                linearLayout.setBackground(new ShapeDrawable(new OvalShape()));
                linearLayout.setClipToOutline(true);
                loadingAnimationView = viewForAnimation.findViewById(R.id.loadingAnimationView);
                loadingAnimationView.startAnimation();
                layoutFlag = false;
            }
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

    class DestinationEventListener implements ChildEventListener {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(!dataSnapshot.child("finish").exists()) {
                Log.d("GPSUpdated", dataSnapshot.getKey() + " : " + dataSnapshot.getValue().toString());

                Log.d("Reference", dataSnapshot.toString());

                Location destinationLocation = new Location(dataSnapshot.getKey());
                Location startLocation = new Location("emergencyLocation");
                destinationLocation.setLatitude(Double.parseDouble(dataSnapshot.child("destinationLatitude").getValue().toString()));
                destinationLocation.setLongitude(Double.parseDouble(dataSnapshot.child("destinationLongitude").getValue().toString()));
                startLocation.setLatitude(Double.parseDouble(dataSnapshot.child("startLatitude").getValue().toString()));
                startLocation.setLongitude(Double.parseDouble(dataSnapshot.child("startLongitude").getValue().toString()));

                perceiveEmergencyVehicle(dataSnapshot, destinationLocation, startLocation);
            }
            else{
                deleteListItem(dataSnapshot.getKey());
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if(!dataSnapshot.child("finish").exists()) {
                Log.d("GPSUpdated", dataSnapshot.getKey() + " : " + dataSnapshot.getValue().toString());

                Location destinationLocation = new Location(dataSnapshot.getKey());
                Location startLocation = new Location("emergencyLocation");
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
    };

    private class CaseEventListener implements ChildEventListener {
        private Location emergencyLocation = null;
        private Location emergencyLocation2 = null;
        private Location destination;
        private Location start;
        private double distanceFirst;
        private String alarmFirst;
        private ArrayList<HashMap<String, Double>> route;
        private String caseNumber = "";
        private CaseInfo caseInfo = null;

        public CaseEventListener(Location destination, Location start, double distance, String alarm){
            this.destination = destination;
            this.start = start;
            this.distanceFirst = distance;
            this.alarmFirst = alarm;
            this.caseNumber = destination.getProvider();
        }

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
                if(caseInfo != null) {
                    makeAlarm(alarm, (int) distance, caseNumber, emergencyLocation, caseInfo);
                    updateInfo(caseNumber, myCurrentLocation, alarm);
                }
            }
            else if(dataSnapshot.getKey().equals("route")){
                //GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                //route = dataSnapshot.getValue(t);

                String stringRoute = (String)dataSnapshot.getValue();
                //Log.d("route", stringRoute);

                route = new ArrayList<>();
                JSONArray jsonRoute = null;
                try {
                    jsonRoute = new JSONArray(stringRoute);

                    for(int i=0;i<jsonRoute.length();i++) {
                        JSONArray pathLATLNG = jsonRoute.getJSONArray(i);
                        Double lng = pathLATLNG.getDouble(0);
                        Double lat = pathLATLNG.getDouble(1);

                        HashMap<String, Double> location = new HashMap<>();
                        location.put("latitude", lat);
                        location.put("longitude", lng);
                        route.add(location);
                    }

                    caseInfo = new CaseInfo(start, destination, route);
                    caseInfos.put(caseNumber, caseInfo);
                    final Location myCurrentLocation = findMyCurrentPosition();
                    if(!alarmFirst.equals("NoAlarm")) {
                        Location location = null;
                        switch (alarmFirst){
                            case "Destination":
                                location = destination;
                                break;
                            case "Start":
                                location = start;
                        }

                        makeAlarm(alarmFirst, (int) distanceFirst, caseNumber, location, caseInfo);
                    }
                    updateInfo(caseNumber, myCurrentLocation, alarmFirst);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            /*else if(dataSnapshot.getKey().equals("finish")){
                if(!caseNumber.equals("") && dataSnapshot.getValue().equals("true")){
                    firebaseDatabase.getReference(caseNumber).removeEventListener(this);
                    deleteListItem(caseNumber);
                    Log.d("finish", caseNumber);
                }
            }*/
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
                if(caseInfo != null) {
                    makeAlarm(alarm, (int) distance, caseNumber, emergencyLocation, caseInfo);
                    updateInfo(caseNumber, myCurrentLocation, alarm);
                }
            }
            else if(dataSnapshot.getKey().equals("route")) {
                //GenericTypeIndicator<ArrayList<Map<String, Double>>> t = new GenericTypeIndicator<ArrayList<Map<String, Double>>>() {};
                //route = dataSnapshot.getValue(t);

                String stringRoute = (String) dataSnapshot.getValue();
                //Log.d("route", stringRoute);

                route = new ArrayList<>();
                JSONArray jsonRoute = null;
                try {
                    jsonRoute = new JSONArray(stringRoute);

                    for (int i = 0; i < jsonRoute.length(); i++) {
                        JSONArray pathLATLNG = jsonRoute.getJSONArray(i);
                        Double lng = pathLATLNG.getDouble(0);
                        Double lat = pathLATLNG.getDouble(1);

                        HashMap<String, Double> location = new HashMap<>();
                        location.put("latitude", lat);
                        location.put("longitude", lng);
                        route.add(location);
                    }

                    caseInfo = new CaseInfo(start, destination, route);
                    caseInfos.put(caseNumber, caseInfo);
                    final Location myCurrentLocation = findMyCurrentPosition();
                    if (!alarmFirst.equals("NoAlarm")) {
                        makeAlarm(alarmFirst, (int) distanceFirst, caseNumber, emergencyLocation, caseInfo);
                    }
                    updateInfo(caseNumber, myCurrentLocation, alarmFirst);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            /*else if(dataSnapshot.getKey().equals("finish")){
                if(!caseNumber.equals("") && dataSnapshot.getValue().equals("true")){
                    firebaseDatabase.getReference(caseNumber).removeEventListener(this);
                    deleteListItem(caseNumber);
                    Log.d("finish", caseNumber);
                }
            }*/
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            //alarmNotification.cancel(dataSnapshot.child("caseNumber").getValue().toString());
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    ChildEventListener finishEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            deleteListItem(dataSnapshot.getKey());
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            deleteListItem(dataSnapshot.getKey());
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

    class OnListViewListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String, String> item = (HashMap)parent.getAdapter().getItem(position);

            CaseInfo caseInfo = caseInfos.get(item.get("title"));
            Location location = findMyCurrentPosition();
            Intent intent = new Intent(getApplicationContext(), ShowCaseActivity.class);
            intent.putExtra("Information", caseInfo);
            intent.putExtra("MyLocation", location);
            //intent.setExtrasClassLoader(CaseInfo.class.getClassLoader());
            startActivity(intent);
        }
    }
}
