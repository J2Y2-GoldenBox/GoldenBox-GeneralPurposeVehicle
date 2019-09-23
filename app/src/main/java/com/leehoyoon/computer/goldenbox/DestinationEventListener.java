package com.leehoyoon.computer.goldenbox;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class DestinationEventListener implements ChildEventListener {
    public Context context;

    public DestinationEventListener(Context context){
        this.context = context;
    }
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

        if(context.getClass().getName().equals("ReceiveActivity")) {
            ((ReceiveActivity) context).perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }
        else if(context.getClass().getName().equals("ForegroundService")){
            ((ForegroundService) context).perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }
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

        if(context.getClass().getName().equals("ReceiveActivity")) {
            ((ReceiveActivity) context).perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }
        else if(context.getClass().getName().equals("ForegroundService")){
            ((ForegroundService) context).perceiveEmergencyVehicle(dataSnapshot, emergencyLocation, startLocation);
        }
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        ((ReceiveActivity)context).firebaseDatabase.getReference(dataSnapshot.getKey()).addChildEventListener(null);

    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }
}
