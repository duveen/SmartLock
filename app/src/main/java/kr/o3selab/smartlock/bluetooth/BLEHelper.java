package kr.o3selab.smartlock.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import java.util.List;
import java.util.UUID;

import kr.o3selab.smartlock.common.utils.Debug;

public class BLEHelper {

    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    public static final int BLE_STATUS_SUCCESS = 0;

    public static final int BLE_NOT_SUPPORT = 1;
    public static final int BLE_NOT_ENABLED = 2;

    private static volatile BLEHelper instance;

    public static BLEHelper getInstance() {
        if (instance == null) instance = new BLEHelper();
        return instance;
    }

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;

    private ScanCallback scanCallback;
    private BluetoothAdapter.LeScanCallback leScanCallback;

    private boolean isScanning = false;

    public BLEHelper() {

    }

    public void init(Context context) {
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    public void startLEScan(final Context context, final BLEFindListener listener) {

        if (isSuccess(context) != BLE_STATUS_SUCCESS) {
            return;
        }

        isScanning = true;

        listener.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLEScan(listener);
                }
            }, 10000);

            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(getScanCallback(listener));
        } else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLEScan(listener);
                }
            }, 10000);

            mBluetoothAdapter.startLeScan(getLeScanCallback(listener));
        }
    }

    public void stopLEScan(BLEFindListener listener) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.stopScan(getScanCallback(listener));
        } else {
            mBluetoothAdapter.stopLeScan(getLeScanCallback(listener));
        }

        isScanning = false;
        listener.onEnd();
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback(final BLEFindListener listener) {

        if (leScanCallback != null) return leScanCallback;

        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                listener.onFind(device);
            }
        };

        return leScanCallback;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback getScanCallback(final BLEFindListener listener) {

        if (scanCallback != null) return scanCallback;

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                listener.onFind(result.getDevice());
                Debug.d("LEScanner : " + result.getDevice().getAddress() + ", " + result.getDevice().getName());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {

            }

            @Override
            public void onScanFailed(int errorCode) {
                listener.onEnd();
            }
        };

        return scanCallback;
    }

    public int isSuccess(Context context) {
        if (!isLESupport(context)) {
            return BLE_NOT_SUPPORT;
        }

        if (!isBluetoothEnabled()) {
            return BLE_NOT_ENABLED;
        }

        return BLE_STATUS_SUCCESS;
    }

    public boolean isLESupport(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public boolean isScanning() {
        return isScanning;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public void setGattInfo(BluetoothGatt gatt, BluetoothGattService service) {
        mBluetoothGatt = gatt;
        mBluetoothGattService = service;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public BluetoothGattService getBluetoothGattService() {
        return mBluetoothGattService;
    }

    public interface BLEFindListener {
        void onStart();

        void onEnd();

        void onFind(BluetoothDevice device);
    }


}
