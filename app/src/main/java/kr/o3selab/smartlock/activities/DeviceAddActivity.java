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

public class DeviceAddActivity extends BaseActivity {

    private BLEHelper bleHelper;

    @BindView(R.id.shakey_connect_list)
    LinearLayout mListLayout;
    @BindView(R.id.shakey_connect_no_list)
    LinearLayout mNoFoundItemView;
    @BindView(R.id.shakey_connect_search_button)
    Button mSearchButton;
    @BindView(R.id.shakey_connect_progress)
    AVLoadingIndicatorView mProgress;

    private BLEHelper.BLEFindListener callback;

    private boolean isDialogShowing;
    private boolean isBindingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_connect);

        ButterKnife.bind(this);

        bleHelper = BLEHelper.getInstance();
        mProgress.hide();

        loading = new LoadingProgressDialog(DeviceAddActivity.this);

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
                    mListLayout.addView(getBluetoothItemView(device));
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
        DeviceAddActivity.this.finish();
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
                new OptionsDialog.Builder(DeviceAddActivity.this)
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

    ShakeyReceiver mShakeyReceiver = new ShakeyReceiver(new ShakeyReceiver.Callback() {
        @Override
        public void onConnect() {
            Debug.d("onConnected");
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
            Debug.d("onDisconnected");
            mBleService.close();
            showFailedAlert();
        }

        @Override
        public void onDataAvailable(String data) {
            receiveMessage(data);
        }
    });

    private void connectBluetoothDevice(final BluetoothDevice device) {
        if (mBleService == null) {
            setShakeyServiceConnectionCallback(new ShakeyServiceConnectionCallback() {
                @Override
                public void onServiceConnected(BLEService service) {
                    mBleService.setShakeyReceiver(mShakeyReceiver);
                    mBleService.registerReceiver();

                    mBleService.connect(device.getAddress());
                }

                @Override
                public void onServiceDisconnected() {
                    mBleService = null;
                    isBindingService = false;
                }
            });

            Intent bleIntent = new Intent(DeviceAddActivity.this, BLEService.class);
            startService(bleIntent);
            bindService(bleIntent, getServiceConnection(), BIND_AUTO_CREATE);

            isBindingService = true;
        } else {
            mBleService.setShakeyReceiver(mShakeyReceiver);
            mBleService.registerReceiver();

            mBleService.connect(device.getAddress());
        }
    }

    private void receiveMessage(String message) {
        if (message.substring(0, 2).equals("R0")) {

            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            BluetoothDevice device = BLEHelper.getInstance().getBluetoothGatt().getDevice();

            final String secretKey = message.substring(2, message.length());

            final Shakey shakey = new Shakey();
            shakey.setOwner(user.getUid());
            shakey.setOwnerEmail(user.getEmail());
            shakey.setName(device.getName());
            shakey.setMac(device.getAddress());
            shakey.setSecret(secretKey);
            shakey.setLastOpen(null);
            shakey.setRegdate(System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference("Shakeys/" + secretKey).setValue(shakey);
            FirebaseDatabase.getInstance().getReference("Owner/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventAdapter(DeviceAddActivity.this) {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    ArrayList<String> list = (ArrayList<String>) dataSnapshot.getValue();
                    list.add(secretKey);

                    FirebaseDatabase.getInstance().getReference("Owner/" + user.getUid()).setValue(list);
                }
            });

            mBleService.send(Shakey.responseReceiveSecretKeyCommand());
            loading.dismiss();

            isDialogShowing = true;
            new OptionsDialog.Builder(DeviceAddActivity.this)
                    .setOptions(OptionsDialog.Options.YES)
                    .setTitle("등록 완료")
                    .setMessage("Shakey가 등록되었습니다.")
                    .setCancelable(false)
                    .setOnClickListener(new OptionsDialog.OnClickListener() {
                        @Override
                        public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                            isDialogShowing = false;

                            mBleService.disconnect();
                            mBleService.close();

                            Intent intent = new Intent();
                            intent.putExtra(Extras.SHAKEY, shakey);

                            setResult(Extras.ADD_SHAKEY_OK, intent);

                            dialog.dismiss();
                            DeviceAddActivity.this.finish();
                        }
                    })
                    .show();

        } else if (message.equals("R1")) {
            loading.dismiss();
            mBleService.disconnect();
            mBleService.close();

            isDialogShowing = true;
            new OptionsDialog.Builder(DeviceAddActivity.this)
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
        new OptionsDialog.Builder(DeviceAddActivity.this)
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
        if (bleHelper.isScanning()) bleHelper.stopLEScan(callback);

        if (isBindingService) {
            mBleService.unregisterReceiver();
            mBleService.setShakeyReceiver(null);
            unbindService(getServiceConnection());
        }

        super.onDestroy();
    }
}
