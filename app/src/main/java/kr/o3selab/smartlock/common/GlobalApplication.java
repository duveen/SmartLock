package kr.o3selab.smartlock.common;

import android.app.Application;

import kr.o3selab.smartlock.bluetooth.BLEHelper;

public class GlobalApplication extends Application {

    private static volatile GlobalApplication instance = null;

    public static GlobalApplication getGlobalApplicationContext() {
        if (instance == null)
            throw new IllegalStateException("this application does not inherit com.kakao.GlobalApplication");
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        BLEHelper.getInstance().init(this);
    }
}
