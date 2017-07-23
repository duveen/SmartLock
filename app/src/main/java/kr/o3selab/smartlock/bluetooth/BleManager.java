package kr.o3selab.smartlock.bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import kr.o3selab.smartlock.activities.GlobalApplication;
import kr.o3selab.smartlock.common.API;
import kr.o3selab.smartlock.common.AppSettings;
import kr.o3selab.smartlock.common.Common;
import kr.o3selab.smartlock.common.JSONHandler;
import kr.o3selab.smartlock.common.Logs;
import kr.o3selab.smartlock.activities.MainActivity;
import kr.o3selab.smartlock.R;

public class BleManager {

    // Debugging
    private static final String TAG = "BleManager";

    // Constants that indicate the current connection state
    public static final int STATE_ERROR = -1;
    public static final int STATE_NONE = 0;      // Initialized
    public static final int STATE_IDLE = 1;      // Not connected
    public static final int STATE_SCANNING = 2;   // Scanning
    public static final int STATE_CONNECTING = 13;   // Connecting
    public static final int STATE_CONNECTED = 16;   // Connected

    // Message types sent from the BluetoothManager to Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_NO_CONNECT = 6;

    public static final long SCAN_PERIOD = 5 * 1000;   // Stops scanning after a pre-defined scan period.
    public static final long SCAN_INTERVAL = 5 * 60 * 1000;

    // System, Management
    private static Context mContext = null;
    private static BleManager mBleManager = null;      // Singleton pattern
    private final Handler mHandler;
    private NotificationManager Notifi_M;
    private Notification Notifi;

    // Bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();//페어링 디바이스 목록
    private BluetoothDevice mDefaultDevice = null;

    private BluetoothGatt mBluetoothGatt = null;

    private ArrayList<BluetoothGattService> mGattServices
            = new ArrayList<BluetoothGattService>();
    private BluetoothGattService mDefaultService = null;
    private ArrayList<BluetoothGattCharacteristic> mGattCharacteristics
            = new ArrayList<BluetoothGattCharacteristic>();
    private ArrayList<BluetoothGattCharacteristic> mWritableCharacteristics
            = new ArrayList<BluetoothGattCharacteristic>();
    private BluetoothGattCharacteristic mDefaultChar = null;


    // Parameters
    private int mState = -1;


    /**
     * 생성자
     */
    private BleManager(Context context, Handler h) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = h;
        mContext = context;
        Notifi_M = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mContext == null)
            return;
    }

    public synchronized static BleManager getInstance(Context c, Handler h) {
        if (mBleManager == null)
            mBleManager = new BleManager(c, h);

        return mBleManager;
    }

    public synchronized void finalize() {
        Log.d(TAG, "finalize: BleManager");
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mState = STATE_IDLE;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            disconnect();
        }

        mDefaultDevice = null;
        mBluetoothGatt = null;
        mDefaultService = null;
        mGattServices.clear();
        mGattCharacteristics.clear();
        mWritableCharacteristics.clear();

        if (mContext == null)
            return;

        // Don't forget this!!
        // Unregister broadcast listeners
//      mContext.unregisterReceiver(mReceiver);
    }


    /*****************************************************
     *   Private methods
     ******************************************************/

    /**
     * This method extracts UUIDs from advertised data
     * Because Android native code has bugs in parsing 128bit UUID
     * use this method instead.
     */
    private void stopScanning() {
        if (mState < STATE_CONNECTING) {
            mState = STATE_IDLE;
            mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_IDLE, 0).sendToTarget();
        }
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    /**
     * 서비스 확인과 쓰기 특성을 찾는다.
     */
    private int checkGattServices(List<BluetoothGattService> gattServices) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            if(mBluetoothAdapter == null){
                Logs.d("# 블루투스 어댑터 초기화 안됨");
            }
            if(mBluetoothAdapter == null){
                Logs.d("# 블루투스 GATT 초기화 안됨");
            }
            return -1;
        }

        for (BluetoothGattService gattService : gattServices) {
            // GATT 서비스
            Logs.d("# GATT Service : " + gattService.toString());

            // GATT 서비스 추가
            mGattServices.add(gattService);

            // 특성 추출
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                // 특성 추가
                mGattCharacteristics.add(gattCharacteristic);
                Logs.d("# GATT Char: " + gattCharacteristic.toString());

                boolean isWritable = isWritableCharacteristic(gattCharacteristic);
                if (isWritable) {
                    mWritableCharacteristics.add(gattCharacteristic);
                    Logs.d("# GATT 쓰기 Char : " + gattCharacteristic.toString());
                }

                boolean isReadable = isReadableCharacteristic(gattCharacteristic);
                if (isReadable) {
                    readCharacteristic(gattCharacteristic);
                    Logs.d("# GATT 읽기 Char : " + gattCharacteristic.toString());
                }

                if (isNotificationCharacteristic(gattCharacteristic)) {
                    setCharacteristicNotification(gattCharacteristic, true);
                    if (isWritable && isReadable) {
                        mDefaultChar = gattCharacteristic;
                    }
                }
            }
        }

        return mWritableCharacteristics.size();
    }

    private boolean isWritableCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) return false;

        final int charaProp = chr.getProperties();
        if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
            Logs.d("# Found writable characteristic");
            return true;
        } else {
            Logs.d("# Not writable characteristic");
            return false;
        }
    }

    private boolean isReadableCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) return false;

        final int charaProp = chr.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            Logs.d("# Found Readable characteristic");
            return true;
        } else {
            Logs.d("# Not Readable characteristic");
            return false;
        }
    }

    private boolean isNotificationCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) return false;

        final int charaProp = chr.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            Logs.d("# Found notification characteristic");
            return true;
        } else {
            Logs.d("# Not notification characteristic");
            return false;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logs.d("# BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logs.d("# BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /*****************************************************
     * Public methods
     ******************************************************/
    public void setScanCallback(BluetoothAdapter.LeScanCallback cb) {
        Log.d("setScanCallback", "성공");
        mLeScanCallback = cb;

    }

    public int getState() {
        return mState;
    }

    public boolean scanLeDevice(final boolean enable) {
        boolean isScanStarted = false;
        if (enable) {//스캔 시작 명령
            if (mState == STATE_SCANNING)
                return false;

            if (mBluetoothAdapter.startLeScan(mLeScanCallback)) {
                mState = STATE_SCANNING;
                mDeviceList.clear();//디바이스 리스트 초기화

                // 지정된 시간후 스캔 멈춤
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopScanning();
                    }
                }, SCAN_PERIOD);

                mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_SCANNING, 0).sendToTarget();
                isScanStarted = true;
            }
        } else { //스캔 중지 명령
            if (mState < STATE_CONNECTING) {
                mState = STATE_IDLE;
                mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_IDLE, 0).sendToTarget();
            }
            stopScanning();
        }

        return isScanStarted;
    }

    public boolean connectGatt(Context c, boolean bAutoReconnect, BluetoothDevice device) {
        if (c == null || device == null)
            return false;

        mGattServices.clear();
        mGattCharacteristics.clear();
        mWritableCharacteristics.clear();

        mBluetoothGatt = device.connectGatt(c, bAutoReconnect, mGattCallback);
        mDefaultDevice = device;

        mState = STATE_CONNECTING;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTING, 0).sendToTarget();
        Logs.d("#디바이스 정보로 연결중");
        return true;
    }

    public boolean connectGatt(Context c, boolean bAutoReconnect, String address) {
        if (c == null || address == null)
            return false;

        if (mBluetoothGatt != null && mDefaultDevice != null
                && address.equals(mDefaultDevice.getAddress())) {
            if (mBluetoothGatt.connect()) {
                Logs.d("#디바이스에 이미 연결중");
                BluetoothDevice device =
                        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);//전달된 Mac주소의 맞는 디바이스 정보 가져오기
                mBluetoothGatt = device.connectGatt(c, bAutoReconnect, mGattCallback);
                AppSettings.GATT_SUCCEESS = 2;
                Common.addShakeyStatus = 1;
                mState = STATE_CONNECTING;
                return true;
            }
        }

        BluetoothDevice device =
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);//전달된 Mac주소의 맞는 디바이스 정보 가져오기
        if (device == null) {//null일경우 오류 연결 실패
            Logs.d("# Device not found.  Unable to connect.");
            return false;
        }

        mGattServices.clear();
        mGattCharacteristics.clear();
        mWritableCharacteristics.clear();

        mBluetoothGatt = device.connectGatt(c, bAutoReconnect, mGattCallback);
        mDefaultDevice = device;

        mState = STATE_CONNECTING;
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTING, 0).sendToTarget();
        if (mBluetoothGatt != null) {
            Logs.d("# 연결됨");
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logs.d("# BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 메세지 전송부분
     *
     * @param chr  기본 null
     * @param data 전송될 데이터
     * @return 성공 여부
     */
    public boolean write(BluetoothGattCharacteristic chr, byte[] data) {
        if (mBluetoothGatt == null) {
            Logs.d(TAG, "# BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic writableChar = null;

        //정상 전송일경우
        if (chr == null) {
            if (mDefaultChar == null) {
                for (BluetoothGattCharacteristic bgc : mWritableCharacteristics) {
                    if (isWritableCharacteristic(bgc)) {
                        writableChar = bgc;
                    }
                }
                if (writableChar == null) {
                    Logs.d(TAG, "# 쓰기 실패");
                    return false;
                }
            } else {
                if (isWritableCharacteristic(mDefaultChar)) {
                    Logs.d("# Default GattCharacteristic is PROPERY_WRITE | PROPERTY_WRITE_NO_RESPONSE");
                    writableChar = mDefaultChar;
                } else {
                    Logs.d("# Default GattCharacteristic is not writable");
                    mDefaultChar = null;
                    return false;
                }
            }
        } else {
            if (isWritableCharacteristic(chr)) {
                Logs.d("# user GattCharacteristic is PROPERY_WRITE | PROPERTY_WRITE_NO_RESPONSE");
                writableChar = chr;
            } else {
                Logs.d("# user GattCharacteristic is not writable");
                return false;
            }
        }
        writableChar.setValue(data);
        writableChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothGatt.writeCharacteristic(writableChar); //전송 시작
        mDefaultChar = writableChar; //기본 전송 개체 참조 설정
        return true;
    }

    public void setWritableCharacteristic(BluetoothGattCharacteristic chr) {
        mDefaultChar = chr;
    }

    public ArrayList<BluetoothGattService> getServices() {
        return mGattServices;
    }

    public ArrayList<BluetoothGattCharacteristic> getCharacteristics() {
        return mGattCharacteristics;
    }

    public ArrayList<BluetoothGattCharacteristic> getWritableCharacteristics() {
        return mWritableCharacteristics;
    }

    /*****************************************************
     * Handler, Listener, Timer, Sub classes
     ******************************************************/
    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mState = STATE_CONNECTED;
                Logs.d(TAG, "# GATT 서버로 연결됨.");
                AppSettings.GATT_SUCCEESS = 0;
                mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_CONNECTED, 0).sendToTarget();

                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mState = STATE_IDLE;
                Logs.d(TAG, "# GATT 서버로 부터 연결 종료");
                mHandler.obtainMessage(MESSAGE_STATE_CHANGE, STATE_IDLE, 0).sendToTarget();
                mBluetoothGatt = null;
                mGattServices.clear();
                mDefaultService = null;
                mGattCharacteristics.clear();
                mWritableCharacteristics.clear();
                mDefaultChar = null;
                mDefaultDevice = null;
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logs.d(TAG, "# 새로운 GATT 서비스 발견");
                //ㅋ  mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                //ConnectionInfo con = ConnectionInfo.getCommon(mContext);
                //connectGatt(mContext, true, con.getDeviceAddress());
                checkGattServices(gatt.getServices());
            } else {
                Logs.d(TAG, "# onServicesDiscovered received: " + status);
            }
        }

        @Override
        //  특성 읽어오기 명령의 대한 결과
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logs.d(TAG, "# 특성 초기화 확인 성공(전송가능): " + characteristic.toString());
                AppSettings.GATT_SUCCEESS = 1;
                Log.d(TAG, "GATT_SUCCESS" + AppSettings.GATT_SUCCEESS);

            }
        }

        /**
         * ======================================================================================
         *  데이터 receive 부분 **************************************************************
         *  =====================================================================================
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //첨으로 레알 여기가  BLE 에서 데이터 읽어오는 부분
            Logs.d(TAG, "# onCharacteristicChanged (BLE에서 데이터 수신됨): " + characteristic.toString());

            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%d ", byteChar));

                String str = new String(data);
                Log.d(TAG, str);
                if (str.equals("TryUnlock")) {
                    Intent intent = new Intent(mContext, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                    //알림 설정에 따라 알림이 울림
                    if(AppSettings.getNotiSetting()){
                        Notifi = new Notification.Builder(mContext)
                                .setContentTitle("Shakey")
                                .setContentText("Secretkey Authentication request")
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setTicker("알림!!!")
                                .setContentIntent(pendingIntent)
                                .build();

                        //소리추가
                        Notifi.defaults = Notification.DEFAULT_SOUND;

                        //알림 소리를 한번만 내도록
                        Notifi.flags = Notification.FLAG_ONLY_ALERT_ONCE;

                        //확인하면 자동으로 알림이 제거 되도록
                        Notifi.flags = Notification.FLAG_AUTO_CANCEL;

                        Notifi_M.notify(1004, Notifi);
                    }


                    String aa = new String(data);
                    if (aa.equals("TryUnlock")) {
                        String secret = AppSettings.getSecretkey();
                        if (secret != null) {
                            write(null, secret.getBytes());
                        }
                        Log.d(TAG, "서비스->디바이스 : " + secret);
                    }
                    Common common = GlobalApplication.getCommon();
                    String userId = common.getSharedPreferences().getString(Common.NAVER_ID, "null");
                    try {
                        String result = new JSONHandler(API.SEND_LOG, "userId=" + userId + "&secret=" + AppSettings.getSecretkey()).execute().get();
                        Log.d(TAG, result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (str.contains("secret")) {
                    if (data.length > 0) {
                        StringTokenizer st = new StringTokenizer(str, "+");
                        if (st.countTokens() == 2) {
                            st.nextToken();
                            String sc = st.nextToken();

                            if (sc.contains("alreadyC")) {
                                Common.addShakeyStatus = 1;
                                Common.registerShakeyParam = "already";
                                Log.d(TAG, "시크릿키 저장 실패");
                            } else {
                                // Mac$$SecretKey
                                AppSettings.setSettingsValue(AppSettings.SETTINGS_SECRETKEY, false, 0, mDefaultDevice.getAddress() + "$$" + new String(sc));
                                Common.addShakeyStatus = 0;
                                Log.d(TAG, "시크릿키 저장함 : " + new String(sc));

                            }
                        }
                    }
                }

                //각 연결된 핸들로 메세지 보내기
                mHandler.obtainMessage(MESSAGE_READ, new String(data)).sendToTarget();
            }

            if (mDefaultChar == null && isWritableCharacteristic(characteristic)) {
                mDefaultChar = characteristic;
            }
        }
    };
}