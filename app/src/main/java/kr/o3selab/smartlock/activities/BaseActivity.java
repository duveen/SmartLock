package kr.o3selab.smartlock.activities;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import kr.o3selab.smartlock.bluetooth.ShakeyReceiver;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.LoadingProgressDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ShakeyAlert;
import kr.o3selab.smartlock.models.ShakeyLog;
import kr.o3selab.smartlock.services.BLEService;
import kr.o3selab.smartlock.services.ShakeyServiceConnectionCallback;

public class BaseActivity extends AppCompatActivity {

    protected static ServiceConnection mServiceConnection;
    protected static ShakeyReceiver mShakeyReceiver;

    protected static BLEService mBleService;

    public LoadingProgressDialog loading;
    private ShakeyServiceConnectionCallback serviceCallback;

    public final String TAG = this.getClass().getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Debug.d(TAG + " onCreate");

        mShakeyReceiver = new ShakeyReceiver(new ShakeyReceiver.Callback() {
            @Override
            public void onConnect() {

            }

            @Override
            public void onDisconnect() {
                if (mBleService == null) return;
                mBleService.setShakey(null);
                mBleService.close();
            }

            @Override
            public void onDataAvailable(String data) {
                if (data == null) return;

                if (data.substring(0, 2).equals("S0")) {
                    openShakey();
                }
            }
        });
    }

    public void openShakey() {
        Shakey shakey = mBleService.getShakey();
        FirebaseUser user = mBleService.getUser();

        if (shakey == null || user == null) return;

        final long openTime = System.currentTimeMillis();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Logs/" + shakey.getSecret()).push();
        ShakeyLog log = new ShakeyLog();

        log.setWho(user.getUid());
        log.setEmail(user.getEmail());
        log.setRegdate(openTime);

        reference.setValue(log);

        shakey.setLastOpen(openTime);
        FirebaseDatabase.getInstance().getReference("Shakeys/" + shakey.getSecret()).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Shakey shakey = mutableData.getValue(Shakey.class);
                if (shakey == null) {
                    return Transaction.success(mutableData);
                }

                shakey.setLastOpen(openTime);

                mutableData.setValue(shakey);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });

        if (!user.getUid().equals(shakey.getOwner())) {
            ShakeyAlert.getInstance().alert(this, shakey);
        }

        mBleService.send(shakey.unlockCommand());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Debug.d(TAG + " onResume");
        if (mShakeyReceiver != null && !TAG.equals("DeviceAddActivity"))
            registerReceiver(mShakeyReceiver, BLEService.getIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Debug.d(TAG + " onPause");
        if (mShakeyReceiver != null && !TAG.equals("DeviceAddActivity"))
            unregisterReceiver(mShakeyReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Debug.d(TAG + " onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Debug.d(TAG + " onDestroy");
    }

    public void setShakeyServiceConnectionCallback(ShakeyServiceConnectionCallback callback) {
        this.serviceCallback = callback;
    }

    public ServiceConnection getServiceConnection() {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Debug.d("onServiceConnected");
                    mBleService = ((BLEService.LocalBinder) service).getService();
                    mBleService.initialize();

                    if (mBleService.isMonitoring()) {
                        mBleService.stopMonitorSystem();
                        mBleService.close();
                    }

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) mBleService.setUser(user);

                    if (serviceCallback != null) serviceCallback.onServiceConnected(mBleService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Debug.d("onServiceDisconnected");
                    mBleService = null;
                    if (serviceCallback != null) serviceCallback.onServiceDisconnected();
                }
            };
        }

        return mServiceConnection;
    }
}

