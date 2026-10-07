package bfu.alarm;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private TimePicker timePicker;
    private NumberPicker dayPicker;
    private Button set;
    private TextView setHint;
    private android.app.AlertDialog deleteDialog;

    private boolean isRussian() {
        return Locale.getDefault().getLanguage().equals("ru");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AlarmActivity.isAlarmServiceRunning(this)) {
            startActivity(new Intent(this, AlarmActivity.class));
        } else if (android.os.Build.VERSION.SDK_INT >= 31 && !((android.app.AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {
            startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
        }
    }

    @Override
    protected void onDestroy() {
    if (deleteDialog != null && deleteDialog.isShowing()) {
        deleteDialog.dismiss();        
    }
    deleteDialog = null;    

    super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);        		

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        
        float density = getResources().getDisplayMetrics().density;
        int safeMarginPx = (int) (44 * density);
        int cornerRadius = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.view.RoundedCorner topCorner = getWindowManager().getDefaultDisplay().getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_RIGHT);
            if (topCorner != null) {
                cornerRadius = topCorner.getRadius();
            } }
        
        int topPadding = Math.max(cornerRadius, safeMarginPx);
        int bottomPadding = (int) (44 * density);
        root.setPadding((int)(12 * density), topPadding, (int)(12 * density), bottomPadding);

        TextView timeLabel = new TextView(this);

        timeLabel.setText(isRussian() ? "Часы:минуты в 24-часовом формате" : "Hours:minutes in 24-hour format");
        timeLabel.setTextSize(18);

        root.addView(timeLabel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        timePicker = new TimePicker(this);

        timePicker.setIs24HourView(true);

        root.addView(timePicker, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView dayLabel = new TextView(this);

        dayLabel.setText(isRussian() ? "День от текущего" : "Day from current");
        dayLabel.setTextSize(18);

        root.addView(dayLabel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        dayPicker = new NumberPicker(this);

        dayPicker.setMinValue(0);
        dayPicker.setMaxValue(365);
        dayPicker.setValue(0);

        root.addView(dayPicker, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Switch repeat = new Switch(this);

        repeat.setText(isRussian() ? "Повторять во все дни" : "Repeat on all days");
        repeat.setTextSize(18);

        root.addView(repeat, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        set = new Button(this);

        set.setText(isRussian() ? "Установить будильник" : "Set alarm");

        root.addView(set, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setHint = new TextView(this);
        setHint.setText(isRussian() ? "Установка будильника доступна только если выбранное время больше текущего" : "Setting the alarm is available only if the selected time is later than the current time");
        setHint.setTextColor(Color.RED);
        setHint.setTextSize(14);
        setHint.setVisibility(View.GONE);
        root.addView(setHint, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button deleteAll = new Button(this);
        deleteAll.setText(isRussian() ? "Удалить все будильники" : "Delete all alarms");
        root.addView(deleteAll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        deleteAll.setOnClickListener(v -> {
            deleteDialog = new android.app.AlertDialog.Builder(this)
            .setTitle(isRussian() ? "Удалить все будильники?" : "Delete all alarms?")
            .setMessage(isRussian() ? "Все установленные будильники будут удалены" : "All set alarms will be deleted")
            .setNegativeButton(isRussian() ? "Не удалять" : "Do not delete", null)
            .setPositiveButton(isRussian() ? "Удалить" : "Delete", (dialog, which) -> {
                AlarmScheduler.cancel(this);
                Prefs.get(this).edit()
                        .remove(Prefs.FLAG_1)
                        .remove(Prefs.FLAG_2)
                        .remove(Prefs.FLAG_3)
                        .commit();

                Toast.makeText(this, isRussian() ? "Все будильники удалены" : "All alarms have been deleted", Toast.LENGTH_LONG).show();
            })
            .create();            
            deleteDialog.show();
        });
        
        updateSetButton();

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> updateSetButton());

        dayPicker.setOnValueChangedListener((picker, oldVal, newVal) -> updateSetButton());

        set.setOnClickListener(v -> {
            
            int hour = timePicker.getHour();

            int minute = timePicker.getMinute();

            int dayPlus = dayPicker.getValue();

            boolean repeatValue = repeat.isChecked();

            String hhmm = String.format(
                            Locale.US,
                            "%02d:%02d",
                            hour,
                            minute);

            Prefs.get(this).edit()
                .putString(Prefs.FLAG_1, hhmm)
                .putInt(Prefs.FLAG_2, dayPlus)
                .putBoolean(Prefs.FLAG_3, repeatValue)
                .commit();

            AlarmScheduler.set(this);

            Calendar now = Calendar.getInstance();

            Calendar alarm = Calendar.getInstance();

            alarm.set(Calendar.HOUR_OF_DAY, hour);

            alarm.set(Calendar.MINUTE, minute);

            alarm.set(Calendar.SECOND, 0);

            alarm.set(Calendar.MILLISECOND, 0);

            alarm.add(Calendar.DAY_OF_YEAR, dayPlus);

            long delay = alarm.getTimeInMillis() - now.getTimeInMillis();

            showDelayToast(delay);
       
        });

        setContentView(root);
    }

    private void updateSetButton() {

        int dayPlus = dayPicker.getValue();

        if (dayPlus > 0) {
            set.setEnabled(true);
            return;
        }

        int hour = timePicker.getHour();

        int minute = timePicker.getMinute();

        Calendar now = Calendar.getInstance();

        Calendar selected = Calendar.getInstance();

        selected.set(Calendar.HOUR_OF_DAY, hour);

        selected.set(Calendar.MINUTE, minute);

        selected.set(Calendar.SECOND, 0);

        selected.set(Calendar.MILLISECOND, 0);

        boolean enabled = selected.getTimeInMillis() > now.getTimeInMillis();

        set.setEnabled(enabled);
        setHint.setVisibility(enabled ? View.GONE : View.VISIBLE);
        
    }

    private void showDelayToast(long delayMillis) {

    if (delayMillis <= 0) {
        return;
    }

    long totalMinutes = (delayMillis + 59_999L) / (60 * 1000);

    long days = totalMinutes / (24 * 60);

    long hours = (totalMinutes % (24 * 60)) / 60;

    long minutes = totalMinutes % 60;

    StringBuilder text = new StringBuilder();

    if (days > 0) {
        text.append(days).append(" ").append(isRussian() ? russianDays(days) : englishDays(days));
    }

    if (hours > 0) {
        if (text.length() > 0) {
            text.append(" ");
        }
        text.append(hours).append(" ").append(isRussian() ? russianHours(hours) : englishHours(hours));
    }

    if (minutes > 0) {
        if (text.length() > 0) {
            text.append(" ");
        }
        text.append(minutes).append(" ").append(isRussian() ? russianMinutes(minutes) : englishMinutes(minutes));
    }

    Toast.makeText(this, isRussian() ? "Будильник через " + text : "Alarm in " + text, Toast.LENGTH_LONG).show(); 
        
    }

    
    private String russianDays(long value) {

        long n = value % 100;

        if (n >= 11 && n <= 19) {
            return "дней";
        }

        switch ((int) (value % 10)) {
            case 1:
                return "день";

            case 2:
            case 3:
            case 4:
                return "дня";

            default:
                return "дней";
        }
    }

    private String russianHours(long value) {

        long n = value % 100;

        if (n >= 11 && n <= 19) {
            return "часов";
        }

        switch ((int) (value % 10)) {
            case 1:
                return "час";

            case 2:
            case 3:
            case 4:
                return "часа";

            default:
                return "часов";
        }
    }

    private String russianMinutes(long value) {

        long n = value % 100;

        if (n >= 11 && n <= 19) {
            return "минут";
        }

        switch ((int) (value % 10)) {
            case 1:
                return "минуту";

            case 2:
            case 3:
            case 4:
                return "минуты";

            default:
                return "минут";
        }
    }

    private String englishDays(long value) {
        return value == 1 ? "day" : "days";
    }

    private String englishHours(long value) {
        return value == 1 ? "hour" : "hours";
    }

    private String englishMinutes(long value) {
        return value == 1 ? "minute" : "minutes";
    }
}
