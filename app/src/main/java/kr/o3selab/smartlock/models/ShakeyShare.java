package kr.o3selab.smartlock.models;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ShakeyShare {

    public static final int TYPE_ONE = 1;
    public static final int TYPE_THREE = 2;
    public static final int TYPE_FIVE = 3;
    public static final int TYPE_TEN = 10;
    public static final int TYPE_UNLIMITED = Integer.MAX_VALUE;

    private String sid;
    private String from;
    private String to;
    private Integer remain;

    @Exclude
    private Shakey shakey;
    @Exclude
    private ShakeyCallBack callback;

    public ShakeyShare() {

    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Integer getRemain() {
        return remain;
    }

    public void setRemain(Integer remain) {
        this.remain = remain;
    }

    public static int getRemainFromIndex(int index) {
        switch (index) {
            case 0:
                return TYPE_ONE;
            case 1:
                return TYPE_THREE;
            case 2:
                return TYPE_FIVE;
            case 3:
                return TYPE_TEN;
            case 4:
                return TYPE_UNLIMITED;
            default:
                return TYPE_UNLIMITED;
        }
    }

    public void setCallback(ShakeyCallBack callback) {
        this.callback = callback;
    }

    @Exclude
    public void asReceiveData(Context context) {
        asReceiveData(context, null);
    }

    @Exclude
    public void asReceiveData(Context context, ShakeyCallBack callback) {
        this.callback = callback;
        FirebaseDatabase.getInstance().getReference("Shakeys/" + sid).addListenerForSingleValueEvent(new ValueEventAdapter(context) {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    return;
                }

                Shakey shakey = dataSnapshot.getValue(Shakey.class);

                ShakeyShare.this.shakey = shakey;
                if (ShakeyShare.this.callback != null) ShakeyShare.this.callback.onSuccess(shakey);
            }
        });
    }

    public interface ShakeyCallBack {
        void onSuccess(Shakey shakey);
    }

}
