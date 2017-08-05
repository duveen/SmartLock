package kr.o3selab.smartlock.models;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import kr.o3selab.smartlock.common.utils.Debug;

public class ValueEventAdapter implements ValueEventListener {

    Context mContext;

    public ValueEventAdapter(Context context) {
        mContext = context;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        String message = databaseError.getCode() + ":" + databaseError.getMessage() + "(" + databaseError.getDetails() + ")";
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        Debug.e(message);
    }
}
