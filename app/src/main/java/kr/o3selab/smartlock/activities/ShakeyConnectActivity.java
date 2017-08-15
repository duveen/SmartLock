package kr.o3selab.smartlock.activities;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.bluetooth.BLEHelper;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Constants;
import kr.o3selab.smartlock.common.Logs;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.layouts.OptionsDialog;
import kr.o3selab.smartlock.service.BTCTemplateService;

public class ShakeyConnectActivity extends BaseActivity {

    private BTCTemplateService myService;
    public static final String TAG = "ShakeyConnectActivity";

    @BindView(R.id.shakey_connect_list)
    LinearLayout mListLayout;
    @BindView(R.id.shakey_connect_no_list)
    LinearLayout mNoFoundItemView;
    @BindView(R.id.shakey_connect_progress)
    View mProgress;

    private HashMap<String, BluetoothDevice> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shakey_connect);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.shakey_register_button)
    void register() {
        if (!BLEHelper.getInstance().isBluetoothEnabled()) {
            return;
        }

        mDevices = new HashMap<>();
        mListLayout.removeAllViewsInLayout();

        BLEHelper.getInstance().startLEScan(this, callback);
    }

    @OnClick(R.id.shakey_next_button)
    void close() {
        onBackPressed();
    }

    BLEHelper.BLEFindListener callback = new BLEHelper.BLEFindListener() {
        @Override
        public void onStart() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onEnd() {
            mProgress.setVisibility(View.GONE);
        }

        @Override
        public void onFind(BluetoothDevice device) {
            Debug.d(String.valueOf(mDevices.size()));
            if (!mDevices.containsKey(device.getAddress())) {
                mDevices.put(device.getAddress(), device);
                mListLayout.addView(getBluetoothItemView(device));
            }

        }
    };

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
                new OptionsDialog.Builder(ShakeyConnectActivity.this)
                        .setTitle("연결시도")
                        .setMessage("Shakey 장치와 연결을 시도하시겠습니까?")
                        .setOptions(OptionsDialog.Options.YES_NO)
                        .setBleDevice(device)
                        .setOnClickListener(new OptionsDialog.OptionsDialogClickListener() {
                            @Override
                            public void onClick(OptionsDialog dialog, OptionsDialog.ANSWER options) {
                                dialog.dismiss();
                                if (options.equals(OptionsDialog.ANSWER.NO)) return;
                            }
                        })
                        .show();
            }
        });

        return view;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logs.d(TAG, "onActivityResult " + resultCode);
        AppSettings.GATT_SUCCEESS = 0;

        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE:
                // 디바이스 리스트에서 요청된 디바이스로 연결
                if (resultCode == Activity.RESULT_OK) {
                    // 디바이스 어드레스로 인텐트에서 값 추출
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Logs.d(TAG, "연결할 mac 주소 :  " + address);
                    // 장치 연결 시도
                    if (myService == null) {
                        Log.d(TAG, "myService NULL임 ");
                    }
                    if (address != null && myService != null) {
                        Log.d(TAG, "Service 디바이스 연결 호출");
                        AppSettings.GATT_SUCCEESS = 0;
                        myService.connectDevice(address);
                    }
                }
                break;

            case Constants.REQUEST_ENABLE_BT:
                // 블루투스 반환을 요청하는 경우
                if (resultCode == Activity.RESULT_OK) {
                    // 블루투스는 지금, 사용하도록 설정된 BT 세션을 설정
                    myService.setupBLE();
                } else {

                    Logs.e(TAG, "블루투스가 꺼져있습니다");
                    Toast.makeText(this, "블루투스가 꺼져있습니다 설정에서 블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
