package kr.o3selab.smartlock.models;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.common.utils.VolleyUtils;
import kr.o3selab.smartlock.services.FCMService;

public class ShakeyAlert {

    private static ShakeyAlert instance;

    public static ShakeyAlert getInstance() {
        if (instance == null) instance = new ShakeyAlert();
        return instance;
    }

    private String mAlertUrl;


    private ShakeyAlert() {
        mAlertUrl = "https://fcm.googleapis.com/fcm/send";
    }

    public void alert(final Context context, final Shakey shakey) {

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("Token/" + shakey.getOwner()).addListenerForSingleValueEvent(new ValueEventAdapter(context) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token = dataSnapshot.getValue(String.class);

                try {
                    JSONObject notification = new JSONObject();
                    notification.put("title", shakey.getName() + " 열림 알림");
                    notification.put("body", user.getDisplayName() + "님이 " + shakey.getName() + "을(를) 열었습니다.");
                    notification.put("sound", "sound");
                    notification.put("icon", "icon");

                    JSONObject data = new JSONObject();
                    data.put("type", FCMService.OPEN_SHAKEY);

                    JSONObject body = new JSONObject();
                    body.put("notification", notification);
                    body.put("data", data);
                    body.put("to", token);

                    Debug.d(body.toString());

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, mAlertUrl, body, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Debug.d(new String(error.networkResponse.data));
                        }
                    }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            HashMap<String, String> headers = new HashMap<>();

                            headers.put("Authorization", "key=" + context.getString(R.string.fcm_key));
                            headers.put("Content-Type", "application/json");

                            return headers;
                        }
                    };

                    VolleyUtils.getInstance().getRequestQueue().add(request);
                } catch (Exception ignored) {

                }
            }
        });
    }
}
