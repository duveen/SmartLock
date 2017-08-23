package kr.o3selab.smartlock.services;

public interface ShakeyServiceConnectionCallback {

    void onServiceConnected(BLEService service);

    void onServiceDisconnected();

}
