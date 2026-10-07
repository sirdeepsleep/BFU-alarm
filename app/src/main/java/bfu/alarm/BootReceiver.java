package bfu.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
                
            if (!Prefs.get(context).contains(Prefs.FLAG_1)) return;                    
            AlarmScheduler.set(context);
        
    }
}
