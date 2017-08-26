package kr.o3selab.smartlock.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import kr.o3selab.smartlock.common.utils.Utils;
import kr.o3selab.smartlock.models.Shakey;

public class AppConfig {

    public static AppConfig instance;

    public static AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    private AppConfig() {

    }

    public boolean isAutoStart() {
        SharedPreferences sharedPreferences = Utils.getSharedPreferences(getContext());
        return sharedPreferences.getBoolean(Extras.AUTO_START, false);
    }

    public void setAutoStart(boolean value) {
        SharedPreferences.Editor editor = Utils.getEditor(getContext());
        editor.putBoolean(Extras.AUTO_START, value);
        editor.commit();
    }

    public Shakey getAutoConnectedDevice() throws JSONException {
        SharedPreferences sharedPreferences = Utils.getSharedPreferences(getContext());
        return new Shakey(sharedPreferences.getString(Extras.AUTO_START_DEVICE, "설정된 장치 없음"));
    }

    public void setAutoConnectedDevice(Shakey shakey) {
        SharedPreferences.Editor editor = Utils.getEditor(getContext());
        editor.putString(Extras.AUTO_START_DEVICE, shakey.toJSON().toString());
    }

    private Context getContext() {
        return GlobalApplication.getGlobalApplicationContext();
    }
}
