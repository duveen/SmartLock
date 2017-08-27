package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.Extras;

public class FindUserActivity extends AppCompatActivity {

    @BindView(R.id.find_user_text)
    EditText mEditText;

    @BindView(R.id.find_user_list)
    LinearLayout mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_user);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.find_user_search)
    void search() {
        if (mEditText.getText().toString().length() < 2) {
            Toast.makeText(this, "검색어를 2글자 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String searchWord = mEditText.getText().toString();
        FirebaseDatabase.getInstance().getReference("Users").orderByChild("id").startAt(searchWord).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<ArrayList<HashMap<String, Object>>> indicator = new GenericTypeIndicator<ArrayList<HashMap<String, Object>>>() {
                };
                ArrayList<HashMap<String, Object>> searchList = dataSnapshot.getValue(indicator);

                if (searchList == null) return;

                for (final HashMap<String, Object> item : searchList) {
                    View view = getLayoutInflater().inflate(R.layout.item_user, null);
                    TextView nameView = (TextView) view.findViewById(R.id.item_user_email);
                    nameView.setText((String) item.get("id"));

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String[] contents = new String[3];

                            contents[0] = (String) item.get("id");
                            contents[1] = (String) item.get("profile");
                            contents[2] = (String) item.get("uid");

                            Intent intent = new Intent();
                            intent.putExtra(Extras.FIND_USER_CONTENT, contents);

                            FindUserActivity.this.setResult(Extras.FIND_USER_OK, intent);
                            FindUserActivity.this.finish();
                        }
                    });

                    mListView.addView(view);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
