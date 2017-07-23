package kr.o3selab.smartlock.activities;

import android.app.Activity;
import android.os.Bundle;

import kr.o3selab.smartlock.common.Common;

public class BaseActivity extends Activity {

    public Common common;

    public static final String TAG = "BaseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        common = GlobalApplication.getCommon();
        Common.setCommon(common);
    }

    @Override
    protected void onResume() {
        super.onResume();
        common.setActivity(this);
        common.setContext(this);
    }

    @Override
    protected void onPause() {
        clearReferences();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences() {
        Activity currActivity = GlobalApplication.getCurrentActivity();
        if (currActivity != null && currActivity.equals(this))
            GlobalApplication.setCurrentActivity(null);
    }
}
