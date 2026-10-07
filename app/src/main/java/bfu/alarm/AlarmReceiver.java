package bfu.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.startForegroundService(serviceIntent);        

        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);

        boolean repeat = Prefs.get(context).getBoolean(Prefs.FLAG_3, false);

        if (repeat) {

            String hhmm = Prefs.get(context).getString(Prefs.FLAG_1, "08:00");

            AlarmScheduler.setThis(context, hhmm);
            
        }
    }
}
