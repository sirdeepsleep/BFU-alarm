package bfu.alarm;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    public static final String NAME = "PREFS";

    public static final String FLAG_1 = "FLAG_1"; // HH:MM
    public static final String FLAG_2 = "FLAG_2"; // DAY+
    public static final String FLAG_3 = "FLAG_3"; // REPEATE?
    
    public static SharedPreferences get(Context context) {

        Context dpContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        return dpContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        
    }
}
