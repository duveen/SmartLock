package kr.o3selab.smartlock.common;

import android.app.Application;
import android.content.Intent;

import kr.o3selab.smartlock.bluetooth.BLEHelper;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.services.BLEService;

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

        Debug.d("onCreateApplication()");

        BLEHelper.getInstance().init(this);

        if (AppConfig.getInstance().isAutoStart()) {
            Intent intent = new Intent(this, BLEService.class);
            startService(intent);
        } else {
            stopService(new Intent(this, BLEService.class));
        }
    }
}
