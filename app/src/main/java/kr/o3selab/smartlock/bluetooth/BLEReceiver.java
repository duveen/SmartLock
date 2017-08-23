package kr.o3selab.smartlock.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import kr.o3selab.smartlock.common.utils.HexAsciiHelper;
import kr.o3selab.smartlock.services.BLEService;

public class BLEReceiver extends BroadcastReceiver {

    private Callback callback;

    public BLEReceiver(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        if (callback == null) return;

        switch (action) {
            case BLEService.BLE_CONNECTED:
                callback.onConnect();
                break;
            case BLEService.BLE_DISCONNECTED:
                callback.onDisconnect();
                break;
            case BLEService.BLE_DATA_AVAILABLE:
                String message = HexAsciiHelper.bytesToAsciiMaybe(intent.getByteArrayExtra(BLEService.BLE_EXTRA_DATA));
                callback.onDataAvailable(message);
                break;
        }

    }

    public interface Callback {
        void onConnect();

        void onDisconnect();

        void onDataAvailable(String data);
    }


}
