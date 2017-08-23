package kr.o3selab.smartlock.services;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import java.util.UUID;

import kr.o3selab.smartlock.bluetooth.BLEHelper;
import kr.o3selab.smartlock.common.utils.Debug;

public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;

    private BroadcastReceiver mShakeyReceiver;

    public static final String BLE_CONNECTED = "kr.o3selab.BLE_CONNECTED";
    public static final String BLE_DISCONNECTED = "kr.o3selab.BLE_DISCONNECTED";
    public static final String BLE_DATA_AVAILABLE = "kr.o3selab.BLE_DATA_AVAILABLE";
    public static final String BLE_EXTRA_DATA = "kr.o3selab.BLE_EXTRA_DATA";

    public static final UUID UUID_SERVICE = BLEHelper.sixteenBitUuid(0x2220);
    public static final UUID UUID_RECEIVE = BLEHelper.sixteenBitUuid(0x2221);
    public static final UUID UUID_SEND = BLEHelper.sixteenBitUuid(0x2222);
    public static final UUID UUID_DISCONNECT = BLEHelper.sixteenBitUuid(0x2223);
    public static final UUID UUID_CLIENT_CONFIGURATION = BLEHelper.sixteenBitUuid(0x2902);

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastUpdate(BLE_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Debug.e("BLE GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic = mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);
                        receiveConfigDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Debug.e("BLE receive config descriptor not found!");
                    }

                } else {
                    Debug.e("BLE receive characteristic not found!");
                }

                BLEHelper.getInstance().setGattInfo(mBluetoothGatt, mBluetoothGattService);
                broadcastUpdate(BLE_CONNECTED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(BLE_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(BLE_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent, Manifest.permission.BLUETOOTH);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        if (UUID_RECEIVE.equals(characteristic.getUuid())) {
            final Intent intent = new Intent(action);
            intent.putExtra(BLE_EXTRA_DATA, characteristic.getValue());
            sendBroadcast(intent, Manifest.permission.BLUETOOTH);
        }
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Debug.d("onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Debug.d("onDestroy");
    }

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Debug.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Debug.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Debug.w("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Debug.d("Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Debug.d("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;

        registerReceiver();
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Debug.w("BluetoothAdapter not initialized");
            return;
        }

        if (isActivationReceiver()) unregisterReceiver();
        mBluetoothGatt.disconnect();
    }

    public boolean isActivationReceiver() {
        return mShakeyReceiver != null;
    }

    public boolean isConnecting() {
        return mBluetoothGatt != null;
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void read() {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Debug.w("BluetoothGatt not initialized");
            return;
        }

        BluetoothGattCharacteristic characteristic = mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean send(byte[] data) {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Debug.w("BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic characteristic = mBluetoothGattService.getCharacteristic(UUID_SEND);

        if (characteristic == null) {
            Debug.w("Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void registerReceiver() {
        mShakeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }
        };

        registerReceiver(mShakeyReceiver, getIntentFilter());
    }

    public void unregisterReceiver() {
        unregisterReceiver(mShakeyReceiver);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BLE_CONNECTED);
        filter.addAction(BLE_DISCONNECTED);
        filter.addAction(BLE_DATA_AVAILABLE);
        return filter;
    }
}
