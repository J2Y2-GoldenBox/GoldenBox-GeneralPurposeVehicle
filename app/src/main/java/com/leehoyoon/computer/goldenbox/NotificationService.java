package com.leehoyoon.computer.goldenbox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class NotificationService extends Service {
    public AlarmNotification alarmNotification;
    private String msg;
    private String caseNumber;

    public NotificationService() {
        alarmNotification = new AlarmNotification(getBaseContext());
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public void setNotification(String msg, String caseNumber){
        this.msg = msg;
        this.caseNumber = caseNumber;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        alarmNotification.alarm(msg, caseNumber);
        Log.d("AlarmCheck", "Check");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        alarmNotification.cancel(caseNumber);
    }
}
