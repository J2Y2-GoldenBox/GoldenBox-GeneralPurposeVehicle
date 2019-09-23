package com.leehoyoon.computer.goldenbox;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class CaseEventListener implements ChildEventListener {
    private Location emergencyLocation = null;
    private Location emergencyLocation2 = null;
    public int alarmDistanceFromEmergencyCar = 1000;
    //ArrayList<Map<String, Double>> route;
    private Context context;

    public CaseEventListener(Context context){
        this.context = context;
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        if(dataSnapshot.getKey().equals("emergencyCarLocation")) {
            final Location myCurrentLocation = ((ReceiveActivity)context).findMyCurrentPosition();
            emergencyLocation2 = emergencyLocation;
            emergencyLocation = new Location("emergency");

            Log.d("Emergency", dataSnapshot.getValue().toString());

            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

            double distance = ((ReceiveActivity)context).distanceByDegree(myCurrentLocation, emergencyLocation);

            Log.d("distance", distance + "m");

            String alarm = "NoAlarm";

            if(distance < alarmDistanceFromEmergencyCar){
                if(emergencyLocation2 != null) {
                    if(((ReceiveActivity)context).checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                        alarm = "Car";
                    }
                }
            }
            //((ReceiveActivity)context).makeAlarm(alarm, (int)distance, dataSnapshot.child("caseNumber").getValue().toString(), emergencyLocation);
            ((ReceiveActivity)context).updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
        }
    }

    @Override
    public void onChildChanged(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
        if(dataSnapshot.getKey().equals("emergencyLocation")) {
            final Location myCurrentLocation = ((ReceiveActivity)context).findMyCurrentPosition();
            emergencyLocation2 = emergencyLocation;
            emergencyLocation = new Location("emergency");

            Log.d("Emergency", dataSnapshot.getValue().toString());

            emergencyLocation.setLatitude(Double.parseDouble(dataSnapshot.child("latitude").getValue().toString()));
            emergencyLocation.setLongitude(Double.parseDouble(dataSnapshot.child("longitude").getValue().toString()));

            double distance = ((ReceiveActivity)context).distanceByDegree(myCurrentLocation, emergencyLocation);

            Log.d("distance", distance + "m");

            String alarm = "NoAlarm";

            if(distance < alarmDistanceFromEmergencyCar){
                //if(myLocation2 != null && checkRoute(myCurrentLocation, route)) {
                if(((ReceiveActivity)context).checkLocation(myCurrentLocation, emergencyLocation, emergencyLocation2)) {
                    alarm = "Car";
                }
                //}
            }
            //((ReceiveActivity)context).makeAlarm(alarm, (int)distance, dataSnapshot.child("caseNumber").getValue().toString(), emergencyLocation);
            ((ReceiveActivity)context).updateInfo(dataSnapshot.child("caseNumber").getValue().toString(), myCurrentLocation, alarm);
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        ((ReceiveActivity)context).alarmNotification.cancel(dataSnapshot.child("caseNumber").getValue().toString());
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }


}
