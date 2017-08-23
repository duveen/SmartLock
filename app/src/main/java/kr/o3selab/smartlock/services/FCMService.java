package kr.o3selab.smartlock.services;

import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import kr.o3selab.smartlock.R;

public class FCMService extends FirebaseMessagingService {

    public static final String OPEN_SHAKEY = "open_shakey";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData().size() == 0) return;

        String type = remoteMessage.getData().get("type");

        switch (type) {
            case OPEN_SHAKEY:
                openShakeyAlert(remoteMessage);
                break;
        }
    }

    private void openShakeyAlert(RemoteMessage remoteMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher);

        if (remoteMessage.getNotification() == null) {
            builder.setContentTitle("Shakey");
            builder.setContentText("Shakey가 홍길동 님에 의해서 열렸습니다.");
        } else {
            builder.setContentTitle(remoteMessage.getNotification().getTitle());
            builder.setContentText(remoteMessage.getNotification().getBody());
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(defaultSoundUri);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(new Random().nextInt(), builder.build());
    }

}
