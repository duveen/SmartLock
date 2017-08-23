package kr.o3selab.smartlock.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import kr.o3selab.smartlock.common.utils.Debug;

public class FCMIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Debug.d("Refreshed Token: " + refreshedToken);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) checkFCMToken(refreshedToken);
    }

    public static void checkFCMToken(final String fcmToken) {
        String token = FirebaseInstanceId.getInstance().getToken();
        Debug.d(token);

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("Token/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = dataSnapshot.getValue(String.class);
                if(token == null || !token.equals(fcmToken)) {
                    FirebaseDatabase.getInstance().getReference("Token/" + user.getUid()).setValue(fcmToken);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
