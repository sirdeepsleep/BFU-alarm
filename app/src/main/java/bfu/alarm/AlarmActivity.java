package bfu.alarm;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.Locale;

public class AlarmActivity extends Activity {

    private boolean isRussian() {
        return Locale.getDefault().getLanguage().equals("ru");
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);        		

        if (!isAlarmServiceRunning(this)) {
            finish();
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding((int)(32 * density), (int)(32 * density), (int)(32 * density), (int)(32 * density));

        Button cancel = new Button(this);
        cancel.setText(isRussian() ? "ОТМЕНА БУДИЛЬНИКА" : "CANCEL ALARM");
        cancel.setTextSize(16);
        cancel.setTextColor(Color.WHITE);

        Button listen = new Button(this);
        listen.setText(isRussian() ? "СЛУШАТЬ ДАЛЬШЕ" : "LISTEN FURTHER");
        listen.setTextSize(16);
        listen.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                (int)(280 * density),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        buttonParams.setMargins(0, (int)(8 * density), 0, (int)(8 * density));

        root.addView(cancel, buttonParams);
        root.addView(listen, buttonParams);

        cancel.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmService.class);
            stopService(intent);
            finishAndRemoveTask(); 
        });

        listen.setOnClickListener(v -> {
            finish();
        });

        setContentView(root);
    }

    static boolean isAlarmServiceRunning(Context context) {

        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AlarmService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
