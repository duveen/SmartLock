

package kr.o3selab.smartlock.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import kr.o3selab.smartlock.bluetooth.BleManager;
import kr.o3selab.smartlock.bluetooth.ConnectionInfo;
import kr.o3selab.smartlock.bluetooth.TransactionBuilder;
import kr.o3selab.smartlock.bluetooth.TransactionReceiver;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.CommandParser;
import kr.o3selab.smartlock.common.Constants;
import kr.o3selab.smartlock.common.Logs;


public class BTCTemplateService extends Service {
    private static final String TAG = "LLService";

    // Context, System
    private Context mContext = null;
    private static Handler mActivityHandler = null;
    private ServiceHandler mServiceHandler = new ServiceHandler();
    private final IBinder mBinder = new ServiceBinder();
    private NotificationManager Notifi_M;
    private Notification Notifi ;

    // Bluetooth
    private BluetoothAdapter mBluetoothAdapter = null;      // 로컬 블루투스 어댑터

    private BleManager mBleManager = null;
    private boolean mIsBleSupported = true;
    private ConnectionInfo mConnectionInfo = null;      // 연결이 이루어 질때 연결정보 기억
    private CommandParser mCommandParser = null;

    private TransactionBuilder mTransactionBuilder = null;
    private TransactionReceiver mTransactionReceiver = null;

    /*****************************************************
     *   Overrided methods
     ******************************************************/
    @Override
    public void onCreate() {
        Logs.d(TAG, "# Service - onCreate() starts here");

        mContext = getApplicationContext();
        initialize();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logs.d(TAG, "# Service - onStartCommand() starts here");
        if(Notifi_M == null)//노티알림 매니저가 널일경우에
        {
            Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        //서비스가 종료되었을 때 서비스는 재 실행 또는 생성한다
        if(mContext!=null){
            setupBLE();

            /*if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {

                *//* Auto 커넥트 설정 구현 했을때 사용
                if(AppSettings.loadAutoConnectSetting()) {
                    if (mConnectionInfo.getDeviceName().contains("Shakey")) {
                        Log.d(TAG,"연결됨 : " + mConnectionInfo.getDeviceAddress().toString());
                        connectDevice(mConnectionInfo.getDeviceAddress());
                    }
                }*//*

                if (mConnectionInfo.getDeviceName().contains("Shakey")) {
                    Log.d(TAG,"연결됨 : " + mConnectionInfo.getDeviceAddress().toString());
                    connectDevice(mConnectionInfo.getDeviceAddress());
                }

            }
            else
            {
                Logs.d(TAG, "# Service - 이전 연결 정보 없음");
            }*/
        }
        return Service.START_STICKY;
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
    @Override
    public IBinder onBind(Intent intent) {
        Logs.d(TAG, "# Service - onBind()");
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Logs.d(TAG, "# Service - onUnbind()");
        return true;
    }
    @Override
    public void onDestroy() {
        Logs.d(TAG, "# Service - onDestroy()");
        finalizeService();
    }
    @Override
    public void onLowMemory (){
        Logs.d(TAG, "# Service - onLowMemory()");
        finalizeService();
    }
    /*****************************************************
     *   Private methods
     ******************************************************/
    private void initialize() {
        Logs.d(TAG, "# Service : initialize ---");

        AppSettings.initializeAppSettings(mContext);
        startServiceMonitoring();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            mIsBleSupported = false;
        }

        //연결 정보 인스턴스 생성
        mConnectionInfo = ConnectionInfo.getInstance(mContext);
        mCommandParser = new CommandParser();

        // 로컬 블루투스 어댑터 가져오기
        if(mBluetoothAdapter == null)
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 로컬 블루투스 어댑터가 널일경우 지원하지 않는 기기
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // 블루투스 수동 설정 할경우
        } else {
            if(mBleManager == null && mIsBleSupported) {
                setupBLE();
            }
        }
        //연결정보가 있을경우 자동연결
        if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
            if(AppSettings.loadAutoConnectSetting()) {
                if (mConnectionInfo.getDeviceName().contains("Shakey")) {
                    Log.d(TAG,"연결됨 : " + mConnectionInfo.getDeviceAddress().toString());
                    connectDevice(mConnectionInfo.getDeviceAddress());
                }
            }
        }
        else{
            Log.d(TAG,"이전 연결정보 없음 수동연결 필요");
        }
    }
    /**
     *  ble 장치에 메세지 보내기
     * @param message      message to send
     */
    private void sendMessageToDevice(String message) {

        AppSettings.GATT_SENDMESSAGE_RECEIVED = 0;

        if(message == null || message.length() < 1)
            return;

        TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
        transaction.begin();
        transaction.setMessage(message);
        transaction.settingFinished();
        transaction.sendTransaction();
    }
    /*****************************************************
     *   Public methods
     ******************************************************/
    public void finalizeService() {
        Logs.d(TAG, "# Service : finalize ---");

        // 블루투스 세션 종료
        mBluetoothAdapter = null;
        if (mBleManager != null) {
            mBleManager.finalize();
        }
        mBleManager = null;
    }
    /**
     * 블루투스 연결 설정
     * @param h
     */
    public void setupService(Handler h) {
        mActivityHandler = h;

        // BLEmanager 인스턴스 체크
        if(mBleManager == null)
            setupBLE();

        // 트랜젝션 빌더 & 리시버 초기화.
        if(mTransactionBuilder == null)
            mTransactionBuilder = new TransactionBuilder(mBleManager, mActivityHandler);
        if(mTransactionReceiver == null)
            mTransactionReceiver = new TransactionReceiver(mActivityHandler);

        // TODO: 만약에 이전에 연결 정보가 있다면
        // 이전 연결 정보가 있는경우 연결을 시도.

        /*if(mConnectionInfo.getDeviceAddress() != null && mConnectionInfo.getDeviceName() != null) {
            *//* Auto 커넥트 설정 구현 했을때 사용
                if(AppSettings.loadAutoConnectSetting()) {
                    if (mConnectionInfo.getDeviceName().contains("Shakey")) {
                        Log.d(TAG,"연결됨 : " + mConnectionInfo.getDeviceAddress().toString());
                        connectDevice(mConnectionInfo.getDeviceAddress());
                    }
                }*//*
            if (mConnectionInfo.getDeviceName().contains("Shakey")) {
                Log.d(TAG,"연결됨 : " + mConnectionInfo.getDeviceAddress().toString());
                connectDevice(mConnectionInfo.getDeviceAddress());
            }
        }
        else{
            Log.d(TAG,"이전 연결정보 없음 수동연결 필요");
        }*/
    }
    /**
     * Ble 세팅
     */
    public void setupBLE() {
        Log.d(TAG, "서비스 - setupBLE()");

        // Initialize the BluetoothManager to perform bluetooth le scanning
        if(mBleManager == null)
            mBleManager = BleManager.getInstance(mContext, mServiceHandler);
    }
    /**
     * 블루투스 활성화 상태인지 체크
     */
    public boolean isBluetoothEnabled() {
        if(mBluetoothAdapter==null) {
            Log.e(TAG, "# Service - 블루투스 어댑터를 찾을 수 없습니다.");
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }
    /**
     * 스캔 모드 상태 가져오기
     */
    public int getBluetoothScanMode() {
        int scanMode = -1;
        if(mBluetoothAdapter != null)
            scanMode = mBluetoothAdapter.getScanMode();

        return scanMode;
    }
    /**
     * 디바이스 연결.
     * @param address  어드레스
     */
    public void connectDevice(String address) {
        if(address != null && mBleManager != null) {
            mBleManager.disconnect();

            if(mBleManager.connectGatt(mContext, true, address)) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mConnectionInfo.setDeviceAddress(address);
                mConnectionInfo.setDeviceName(device.getName());
                Logs.d("#GATT 연결됨");

            }
        }
    }

    /**
     * 연결된 장치 이름 가져오기
     */
    public String getDeviceName() {
        return mConnectionInfo.getDeviceName();
    }


    /**
     * 메세지 보내기
     */
    public void sendMessageToRemote(String message) {
        sendMessageToDevice(message);
    }


    /**
     * 백그라운드 서비스 시작
     */
    public void startServiceMonitoring() {
        if(AppSettings.getBgService()) {
            ServiceMonitoring.startMonitoring(mContext);
        } else {
            ServiceMonitoring.stopMonitoring(mContext);
        }
    }


    /*****************************************************
     *   Handler, Listener, Timer, Sub classes
     ******************************************************/
    public class ServiceBinder extends Binder {
        public BTCTemplateService getService() {
            return BTCTemplateService.this;
        }
    }


    /**
     * Receives messages from bluetooth manager
     */
    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            if(mActivityHandler!=null){

                switch(msg.what) {
                    // Bluetooth state changed
                    case BleManager.MESSAGE_STATE_CHANGE:
                        // Bluetooth state Changed
                        Logs.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);

                        switch (msg.arg1) {
                            case BleManager.STATE_NONE:
                                mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
                                break;

                            case BleManager.STATE_CONNECTING:
                                mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTING).sendToTarget();
                                break;

                            case BleManager.STATE_CONNECTED:
                                mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_CONNECTED).sendToTarget();
                                break;

                            case BleManager.STATE_IDLE:
                                mActivityHandler.obtainMessage(Constants.MESSAGE_BT_STATE_INITIALIZED).sendToTarget();
                                break;


                        }
                        break;

                    // If you want to send data to remote
                    case BleManager.MESSAGE_WRITE:
                        Logs.d(TAG, "Service - MESSAGE_WRITE: ");
                        String message = (String) msg.obj;
                        if(message != null && message.length() > 0)
                            sendMessageToDevice(message);
                        break;

                    // Received packets from remote
                    case BleManager.MESSAGE_READ:
                        Logs.d(TAG, "Service - MESSAGE_READ: ");

                        if(msg.obj != null){
                            Log.d(TAG,"서비스수신 : "+ msg.obj.toString());
                            String aa = msg.obj.toString();
                            if(aa.equals("TryUnlock")) {
                                String secret = AppSettings.getSecretkey();
                                sendMessageToDevice(secret);
                                Log.d(TAG,"서비스->디바이스 시크릿 : "+secret);
                                mActivityHandler.obtainMessage(Constants.MESSAGE_READ_CHAT_DATA).sendToTarget();
                            }
                        }
                    case BleManager.MESSAGE_DEVICE_NAME:
                        Logs.d(TAG, "Service - MESSAGE_DEVICE_NAME: ");

                        // save connected device's name and notify using toast
                        String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
                        String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);

                        if(deviceName != null && deviceAddress != null) {
                            // Remember device's address and name
                            mConnectionInfo.setDeviceAddress(deviceAddress);
                            mConnectionInfo.setDeviceName(deviceName);

                            Toast.makeText(getApplicationContext(),
                                    "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                        }
                        break;

                    case BleManager.MESSAGE_TOAST:
                        Logs.d(TAG, "Service - MESSAGE_TOAST: ");

                        Toast.makeText(getApplicationContext(),
                                msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST),
                                Toast.LENGTH_SHORT).show();
                        break;

                }   // End of switch(msg.what)

                super.handleMessage(msg);
            }
        }
    }
}
