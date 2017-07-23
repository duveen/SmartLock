package kr.o3selab.smartlock.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;

import kr.o3selab.smartlock.bluetooth.BleManager;
import kr.o3selab.smartlock.common.Common;
import kr.o3selab.smartlock.common.Logs;
import kr.o3selab.smartlock.R;

public class DeviceListActivity extends BaseActivity {

    private static final String TAG = "DeviceListActivity";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private String selectAddress;
    //상수
    private static final long SCAN_PERIOD = 10 * 1000; //스캔 시간 10초
    private ActivityHandler mActivityHandler;

    // Refresh timer
    private Timer mRefreshTimer = null;

    //블루투스 관련
    private BleManager mBleManager;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter; //페어링된 디바이스 목록
    private ArrayAdapter<String> mNewDevicesArrayAdapter; //새로 스캔된 디바이스 목록

    private ArrayList<BluetoothDevice> mDevices = new ArrayList<BluetoothDevice>();//디바이스 저장 목록

    //UI
    private Button btnScan = null;
    private static final int  MY_PERMISSIONS_REQUEST_READ_CONTACTS =101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        //핸들러 생성
        mActivityHandler = new ActivityHandler();

        //버튼 찾기
        btnScan = (Button) findViewById(R.id.button_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewDevicesArrayAdapter.clear();
                ScaningStart();
                v.setVisibility(View.GONE);
            }
        });


        //이미 페어링된 장치 목록 과 새로운 기기 목록에대한 어댑터 생성
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.adapter_device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.adapter_device_name);


        //리스트뷰를 찾아 페어링된 장치 어댑터 셋팅
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);


        //리스트뷰를 찾아 새로스캔된 장치 어댑터 셋팅
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);


        //로컬 블루투스 어댑터 가져오기
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        // Get BLE Manager 스캔 콜백 등록
        mBleManager = BleManager.getInstance(getApplicationContext(), null);
        mBleManager.setScanCallback(mLeScanCallback);


        // 현재 페어링된 기기의 목록을 가져온다.
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();


        // 페어링 된 장치가있는 경우에 ArrayAdapter에 각각 추가
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("Shakey"))
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "페어링된 기기들이 없습니다.";
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }


    //이미 스캔된 장치인지 비교 메서드
    private boolean checkDuplicated(BluetoothDevice device) {
        for (BluetoothDevice dvc : mDevices) {
            if (device.getAddress().equalsIgnoreCase(dvc.getAddress())) {
                return true;
            }
        }
        return false;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Logs.d("# Scan device rssi is " + rssi);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {//새로 스캔된 장치 추가
                            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {//새로 검색된 장치인경우
                                if (!checkDuplicated(device)) {//같은 디바이스 검색 중복확인 처리
                                    Log.d("Scan divece : ",device.getName() + device.getAddress());
                                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                                    mDevices.add(device);
                                }
                            }
                        }
                    });
                }
            };


    //목록에 추가된 장치 아이템들의 대한 온클릭 리스너
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            //검색 취소
            bluetoothAdapter.cancelDiscovery();

            // item의 마지막 17 문자는 mac주소 의미. 장치 MAC 주소를 가져온다
            String info = ((TextView) v).getText().toString();
            if (info != null && info.length() > 16) {
                String address = info.substring(info.length() - 17);
                Log.d(TAG, "User selected device : " + address);

                for (BluetoothDevice de : mDevices) {
                    if(address.equals(de.getAddress())) {
                        startBluetoothPairing(de); break;
                    }
                }

                // 결과 Intent에 Mac 주소를 포함시킨다.
                selectAddress= address;
                Intent intent1 = new Intent();
                intent1.putExtra(EXTRA_DEVICE_ADDRESS, selectAddress);
                // 화면을 닫으면서 Intent를 보냄
                setResult(Activity.RESULT_OK, intent1);
                Common.addShakeyStatus = -1;
                finish();
            }
        }
    };

    /**
     * 장치 검색 시작 부분
     */
    private void startBluetoothPairing(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 장치 검색 시작 부분
     */

    private void ScaningStart() {
        Log.d(TAG, "doScan");

        setProgressBarIndeterminateVisibility(true);
        setTitle("스캔중");

        //새 장치 검색시나올 listview 활성화
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        //캐시 지우기
        mDevices.clear();

        //이미 스캔중이면 멈춤
        if (mBleManager.getState() == BleManager.STATE_SCANNING) {
            mBleManager.scanLeDevice(false);//멈춤
        }

        //장치 스캔 요청
        mBleManager.scanLeDevice(true);//시작

        mActivityHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        },SCAN_PERIOD);//지정 시간후 스캔 멈춤

    }


    private void stopScan() {
        setProgressBarIndeterminateVisibility(false);
        setTitle("연결할 디바이스 선택");
        btnScan.setVisibility(View.VISIBLE); //스캔 버튼 다시 표시
        mBleManager.scanLeDevice(false);
    }

    @Override
    public void onStop() {
        // Stop the timer
        if (mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(bluetoothAdapter != null)
        {
            bluetoothAdapter.cancelDiscovery();//검색 멈춤
        }
    }


    public class ActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            default:break;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state        = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState    = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                //if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {

                if (state == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(context,"페어링됨",Toast.LENGTH_LONG).show();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(context,"페어링실패",Toast.LENGTH_LONG).show();
                }

            }
        }
    };


}
