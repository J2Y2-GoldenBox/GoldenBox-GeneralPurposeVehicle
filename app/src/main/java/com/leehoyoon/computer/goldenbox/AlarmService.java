package com.leehoyoon.computer.goldenbox;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AlarmService extends FirebaseMessagingService {
    private static final String TAG = "FCM";
    AlarmService(){

    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

        Log.e(TAG, "DeviceToken" + s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.e(TAG, "MessageReceived" + remoteMessage);

        String from = remoteMessage.getFrom();

        sendToActivity(getApplicationContext(), from, remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), remoteMessage.getData().toString());
    }

    private void sendToActivity(Context context, String from, String title, String body, String contents){
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra("from", from);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("contents", contents);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);
    }
}