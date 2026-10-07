package bfu.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class AlarmScheduler {

    private static final int ALARM_REQUEST = 100;
    private static final int SHOW_REQUEST = 101;
    
    public static void set(Context context) {

        String hhmm = Prefs.get(context).getString(Prefs.FLAG_1, "08:00");

        int dayPlus = Prefs.get(context).getInt(Prefs.FLAG_2, 0);

        boolean repeat = Prefs.get(context).getBoolean(Prefs.FLAG_3, false);

        if (dayPlus == 0 || repeat) {
            setThis(context, hhmm);
        } else {
            setDayPlus(context, hhmm, dayPlus);
        }
    }


    public static void setThis(Context context, String hhmm) {

        String[] parts = hhmm.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar now = Calendar.getInstance();

        Calendar alarm = Calendar.getInstance();

        alarm.set(Calendar.HOUR_OF_DAY, hour);

        alarm.set(Calendar.MINUTE, minute);

        alarm.set(Calendar.SECOND, 0);
        alarm.set(Calendar.MILLISECOND, 0);


        if (alarm.getTimeInMillis() <= now.getTimeInMillis()) {
            alarm.add(Calendar.DAY_OF_YEAR, 1);
        }

        setAlarmClock(context, alarm.getTimeInMillis());
    }

    public static void setDayPlus(Context context, String hhmm, int dayPlus) {

        String[] parts = hhmm.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar alarm = Calendar.getInstance();

        alarm.add(Calendar.DAY_OF_YEAR, dayPlus);

        alarm.set(Calendar.HOUR_OF_DAY, hour);

        alarm.set(Calendar.MINUTE, minute);

        alarm.set(Calendar.SECOND, 0);
        alarm.set(Calendar.MILLISECOND, 0);

        setAlarmClock(context, alarm.getTimeInMillis());
        
    }

    public static void cancel(Context context) {

    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    Intent alarmIntent = new Intent(context, AlarmReceiver.class);

    PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    alarmManager.cancel(alarmPendingIntent);
    alarmPendingIntent.cancel();

    Intent showIntent = new Intent(context, AlarmActivity.class);

    PendingIntent showPendingIntent = PendingIntent.getActivity(
            context,
            SHOW_REQUEST,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    showPendingIntent.cancel();
    }

    private static void setAlarmClock(Context context, long triggerAtMillis) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
       
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                        context,
                        ALARM_REQUEST,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent showIntent = new Intent(context, AlarmActivity.class);

        PendingIntent showPendingIntent = PendingIntent.getActivity(
                        context,
                        SHOW_REQUEST,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(
                        triggerAtMillis,
                        showPendingIntent);
        
        alarmManager.setAlarmClock(info, alarmPendingIntent);
        
    }
}
