package kr.o3selab.smartlock.common;

import android.content.Context;
import android.content.SharedPreferences;

public class Utils {

    // Shared Preference
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static SharedPreferences.Editor getEditor(Context context) {
        return getSharedPreferences(context).edit();
    }

}
