package kr.o3selab.smartlock.activities;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import kr.o3selab.smartlock.common.Common;

/**
 * Created by duvee on 2016-10-23.
 */

public class GlobalApplication extends Application {
    private static volatile GlobalApplication self = null;
    private static volatile Common common;
    private static volatile Activity currentActivity = null;

    private static Context mContext;

    public static Activity getCurrentActivity() { return currentActivity; }
    public static void setCurrentActivity(Activity currentActivity) {
        GlobalApplication.currentActivity = currentActivity;
    }
    public static GlobalApplication getGlobalApplicationContext() {
        if(self == null) throw new IllegalStateException("this application");
        return self;
    }

    public static Common getCommon() {
        return common;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        mContext = getApplicationContext();
        common = new Common();
        common.setContext(getGlobalApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        self = null;
        common = null;
    }
}
