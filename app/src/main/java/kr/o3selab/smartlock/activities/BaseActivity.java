package kr.o3selab.smartlock.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.services.BLEService;
import kr.o3selab.smartlock.services.ShakeyServiceConnectionCallback;

public class BaseActivity extends AppCompatActivity {

    protected static ServiceConnection mServiceConnection;
    protected static BroadcastReceiver mBroadcastReceiver;

    protected static BLEService mBleService;

    private ShakeyServiceConnectionCallback callback;

    public final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Debug.d(TAG + " onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Debug.d(TAG + " onResume");
    }

    public void setShakeyServiceConnectionCallback(ShakeyServiceConnectionCallback callback) {
        this.callback = callback;
    }

    public ServiceConnection getServiceConnection() {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mBleService = ((BLEService.LocalBinder) service).getService();
                    mBleService.initialize();

                    if (callback != null) callback.onServiceConnected(mBleService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mBleService = null;
                    if (callback != null) callback.onServiceDisconnected();
                }
            };
        }

        return mServiceConnection;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Debug.d(TAG + " onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Debug.d(TAG + " onStop");
    }
}

