package bfu.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import java.util.Locale;

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 100;

    private MediaPlayer player;

    private boolean isRussian() {
        return Locale.getDefault().getLanguage().equals("ru");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {            
        player = new MediaPlayer();
            
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build());
            
            player.setDataSource(this, Settings.System.DEFAULT_ALARM_ALERT_URI);

            player.setLooping(true);
            player.setVolume(1.0f, 1.0f);
            player.prepare();
            player.start();             
        } catch (Throwable t) {}
                
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {    
    createNotificationChannel();
    return START_STICKY;
    }

    private void createNotificationChannel() {

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, isRussian() ? "Будильник" : "Alarm",NotificationManager.IMPORTANCE_HIGH);

        channel.setDescription(isRussian() ? "Звонящий будильник" : "Ringing alarm");

        channel.setSound(null,null);

        NotificationManager manager = getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(isRussian() ? "Будильник" : "Alarm")            
            .setContentText(isRussian() ? "Будильник звонит" : "Alarm is ringing")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, 1024);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }        
    }

    @Override
    public void onDestroy() {

        if (player != null) {

            if (player.isPlaying()) {
                player.stop();
            }

            player.release();
            player = null;
        }

        stopForeground(true);
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
