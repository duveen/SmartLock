package kr.o3selab.smartlock.common;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import java.util.Vector;

import kr.o3selab.smartlock.common.utils.Utils;
import kr.o3selab.smartlock.models.Shakey;

public class AppConfig {

    private static AppConfig instance;

    public static AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    private Vector<Shakey> shakeys;

    private AppConfig() {
        shakeys = new Vector<>();
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

    public Shakey getAutoConnectedDevice() {
        SharedPreferences sharedPreferences = Utils.getSharedPreferences(getContext());

        Shakey shakey;
        try {
            shakey = new Shakey(sharedPreferences.getString(Extras.AUTO_START_DEVICE, "설정된 장치 없음"));
        } catch (JSONException e) {
            shakey = null;
        }

        return shakey;
    }

    public void setAutoConnectedDevice(Shakey shakey) {
        SharedPreferences.Editor editor = Utils.getEditor(getContext());
        if (shakey == null) editor.remove(Extras.AUTO_START_DEVICE);
        else editor.putString(Extras.AUTO_START_DEVICE, shakey.toJSON().toString());
        editor.commit();
    }

    public Vector<Shakey> getShakeys() {
        return shakeys;
    }

    private Context getContext() {
        return GlobalApplication.getGlobalApplicationContext();
    }
}
