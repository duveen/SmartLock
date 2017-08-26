package kr.o3selab.smartlock.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.bluetooth.BLEHelper;
import kr.o3selab.smartlock.bluetooth.ShakeyReceiver;
import kr.o3selab.smartlock.common.Extras;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.LoadingProgressDialog;
import kr.o3selab.smartlock.layouts.OptionsDialog;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.models.ValueEventAdapter;
import kr.o3selab.smartlock.services.BLEService;
import kr.o3selab.smartlock.services.ShakeyServiceConnectionCallback;

public class ShakeyConnectActivity extends BaseActivity {

    private BLEHelper bleHelper;

    @BindView(R.id.shakey_connect_list)
    LinearLayout mListLayout;
    @BindView(R.id.shakey_connect_no_list)
    LinearLayout mNoFoundItemView;
    @BindView(R.id.shakey_connect_search_button)
    Button mSearchButton;
    @BindView(R.id.shakey_connect_progress)
    AVLoadingIndicatorView mProgress;

    private HashMap<String, BluetoothDevice> mDevices;
    private ConnectTimer mTimer;

    private BLEHelper.BLEFindListener callback;

    private boolean isDialogShowing;

    @Override
    protected void onStart() {
        super.onStart();
        mDevices = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_connect);

        ButterKnife.bind(this);

        bleHelper = BLEHelper.getInstance();
        mProgress.hide();

        loading = new LoadingProgressDialog(ShakeyConnectActivity.this);

        isDialogShowing = false;
    }

    @OnClick(R.id.shakey_connect_search_button)
    void search() {
        if (!bleHelper.isScanning()) {
            if (!bleHelper.isBluetoothEnabled()) {
                return;
            }

            callback = new BLEHelper.BLEFindListener() {

                @Override
                public void onStart() {
                    Debug.d("onStart()");
                    mDevices.clear();

                    mProgress.show();
                    mSearchButton.setText("검색중지");
                }

                @Override
                public void onEnd() {
                    mProgress.hide();
                    mSearchButton.setText("검색하기");
                }

                @Override
                public void onFind(BluetoothDevice device) {
                    Debug.d("onFind()");
                    if (!mDevices.containsKey(device.getAddress())) {
                        mDevices.put(device.getAddress(), device);
                        mListLayout.addView(getBluetoothItemView(device));
                    }
                }
            };

            mListLayout.removeAllViewsInLayout();
            bleHelper.startLEScan(this, callback);
        } else {
            bleHelper.stopLEScan(callback);
        }
    }

    @OnClick(R.id.shakey_connect_next_button)
    void close() {
        if (bleHelper.isScanning()) bleHelper.stopLEScan(callback);

        if (mBleService != null && !mBleService.isConnected()) {
            unbindService(getServiceConnection());
            mBleService = null;
        }

        callback = null;
        ShakeyConnectActivity.this.finish();
    }

    @Override
    public void onBackPressed() {
        if (isDialogShowing) {
            super.onBackPressed();
            return;
        }

        close();
    }

    private View getBluetoothItemView(final BluetoothDevice device) {
        View view = getLayoutInflater().inflate(R.layout.item_shakey_add_list, null);

        TextView nameView = (TextView) view.findViewById(R.id.item_shakey_name);
        if (device.getName() != null) nameView.setText(device.getName());
        else nameView.setText("이름 정보 없음");

        TextView macView = (TextView) view.findViewById(R.id.item_shakey_mac);
        macView.setText(device.getAddress());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDialogShowing = true;
                new OptionsDialog.Builder(ShakeyConnectActivity.this)
                        .setTitle("연결시도")
                        .setMessage("Shakey 장치와 연결을 시도하시겠습니까?")
                        .setOptions(OptionsDialog.Options.YES_NO)
                        .putExtras(Extras.BLE_DEVICE, device)
                        .setOnClickListener(bluetoothItemClickListener)
                        .show();
            }
        });

        return view;
    }

    OptionsDialog.OnClickListener bluetoothItemClickListener = new OptionsDialog.OnClickListener() {
        @Override
        public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
            isDialogShowing = false;
            dialog.dismiss();
            if (mProgress.isShown()) mProgress.smoothToHide();
            if (options.equals(OptionsDialog.ANSWER.NO)) return;

            if (bleHelper.isScanning()) bleHelper.stopLEScan(callback);

            isDialogShowing = true;
            loading.show();

            BluetoothDevice device = (BluetoothDevice) dialog.getExtras(Extras.BLE_DEVICE);
            if (device != null) connectBluetoothDevice(device);
            else loading.dismiss();
        }
    };

    private void connectBluetoothDevice(final BluetoothDevice device) {

        if (mBleService != null) {
            mBleService.connect(device.getAddress());
            mTimer = new ConnectTimer();
            mTimer.start();

            return;
        }

        setShakeyServiceConnectionCallback(new ShakeyServiceConnectionCallback() {
            @Override
            public void onServiceConnected(BLEService service) {
                mBleService.connect(device.getAddress());
                mTimer = new ConnectTimer();
                mTimer.start();
            }

            @Override
            public void onServiceDisconnected() {
                mBleService = null;
            }
        });

        Intent bleIntent = new Intent(ShakeyConnectActivity.this, BLEService.class);
        bindService(bleIntent, getServiceConnection(), BIND_AUTO_CREATE);

        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new ShakeyReceiver(new ShakeyReceiver.Callback() {
                @Override
                public void onConnect() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            mBleService.send(Shakey.requestSecretCommand());
                        }
                    }).start();
                }

                @Override
                public void onDisconnect() {
                    showFailedAlert();
                }

                @Override
                public void onDataAvailable(String data) {
                    receiveMessage(data);
                }
            });
            registerReceiver(mBroadcastReceiver, BLEService.getIntentFilter());
        }
    }

    private void receiveMessage(String message) {
        if (message.contains("secret+")) {

            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            BluetoothDevice device = BLEHelper.getInstance().getBluetoothGatt().getDevice();

            final String secretKey = message.substring(message.indexOf("+") + 1, message.length());

            Shakey shakey = new Shakey();
            shakey.setOwner(user.getUid());
            shakey.setOwnerEmail(user.getEmail());
            shakey.setName(device.getName());
            shakey.setMac(device.getAddress());
            shakey.setSecret(secretKey);
            shakey.setLastOpen(null);
            shakey.setRegdate(System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference("Shakeys/" + secretKey).setValue(shakey);
            FirebaseDatabase.getInstance().getReference("Owner/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventAdapter(ShakeyConnectActivity.this) {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<String> list = (ArrayList<String>) dataSnapshot.getValue();
                    list.add(secretKey);

                    FirebaseDatabase.getInstance().getReference("Owner/" + user.getUid()).setValue(list);
                }
            });

            mBleService.send(Shakey.responseReceiveSecretKeyCommand());
            mTimer.interrupt();
            loading.dismiss();

            isDialogShowing = true;
            new OptionsDialog.Builder(ShakeyConnectActivity.this)
                    .setOptions(OptionsDialog.Options.YES)
                    .setTitle("등록 완료")
                    .setMessage("Shakey가 등록되었습니다.")
                    .setCancelable(false)
                    .setOnClickListener(new OptionsDialog.OnClickListener() {
                        @Override
                        public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                            isDialogShowing = false;
                            dialog.dismiss();
                            ShakeyConnectActivity.this.finish();
                        }
                    })
                    .show();

        } else if (message.equals("get_key_nack")) {
            loading.dismiss();
            mTimer.interrupt();
            mBleService.disconnect();

            isDialogShowing = true;
            new OptionsDialog.Builder(ShakeyConnectActivity.this)
                    .setTitle("등록 실패")
                    .setOptions(OptionsDialog.Options.YES)
                    .setMessage("Shakey 등록을 실패했습니다. 이미 등록된 Shakey 입니다.")
                    .setCancelable(true)
                    .setOnClickListener(new OptionsDialog.OnClickListener() {
                        @Override
                        public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                            isDialogShowing = false;
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    private void showFailedAlert() {
        if (!loading.isShowing()) return;

        isDialogShowing = false;
        loading.dismiss();

        isDialogShowing = true;
        new OptionsDialog.Builder(ShakeyConnectActivity.this)
                .setOptions(OptionsDialog.Options.YES)
                .setTitle("실패")
                .setMessage("연결에 실패했습니다.")
                .setCancelable(true)
                .setOnClickListener(new OptionsDialog.OnClickListener() {
                    @Override
                    public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                        isDialogShowing = false;
                        dialog.dismiss();
                    }
                }).show();
    }


    @Override
    protected void onDestroy() {
        if (mTimer != null && mTimer.isAlive()) mTimer.interrupt();

        if (bleHelper.isScanning()) bleHelper.stopLEScan(callback);

        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }

        super.onDestroy();
    }

    private class ConnectTimer extends Thread {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 10; i++) {
                    if (isInterrupted()) return;
                    Thread.sleep(1000);
                }
                if (mBleService.isConnected()) mBleService.disconnect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showFailedAlert();
                    }
                });
            } catch (InterruptedException ignored) {

            }
        }
    }
}
