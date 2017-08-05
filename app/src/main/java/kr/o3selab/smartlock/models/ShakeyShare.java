package kr.o3selab.smartlock.models;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class ShakeyShare {

    private String sid;
    private String from;
    private String to;
    private Integer type;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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
