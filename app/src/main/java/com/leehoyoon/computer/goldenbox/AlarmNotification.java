package com.leehoyoon.computer.goldenbox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class AlarmNotification {
    private Context context;
    private NotificationManager notificationManager;

    public AlarmNotification(Context context){
        this.context = context;
    }

    public void alarm(String msg, String caseNumber){
        Intent notificationIntent = new Intent(context, ReceiveActivity.class).setAction(Intent.ACTION_MAIN) .addCategory(Intent.CATEGORY_LAUNCHER) .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.alarm_service);
        remoteViews.setImageViewResource(R.id.imageView, R.drawable.siren);
        remoteViews.setTextViewText(R.id.textViewContent, msg);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "goldenBox";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Golden Box",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);

            notificationManager = context.getSystemService(NotificationManager.class); //((NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE));
            notificationManager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSmallIcon(R.drawable.siren)
                .setContent(remoteViews)
                .setContentTitle("Alarm")
                .setContentText(msg)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(uri)
                .setVibrate(new long[] {1000, 1000, 1000, 1000})
                .setLights(Color.RED, 500, 500);

        Notification notification = builder.build();

        notificationManager.notify(Integer.parseInt(caseNumber.substring(2)), notification);
    }

    public void cancel(String caseNumber){
        //notificationManager.cancel(Integer.parseInt(caseNumber.substring(2)));
    }
}
