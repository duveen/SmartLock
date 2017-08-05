package kr.o3selab.smartlock.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import butterknife.BindView;
import butterknife.ButterKnife;
import kr.o3selab.smartlock.R;

public class RegisterActivity extends AppCompatActivity implements OnCompleteListener<AuthResult> {

    public static final String TAG = "RegisterActivity";

    @BindView(R.id.activity_register_text_switcher)
    TextSwitcher mTextSwitcher;

    @BindView(R.id.activity_register_sign_in)
    View mLoginView;

    @BindView(R.id.activity_register_sign_in_btn)
    View mLoginButton;

    private GoogleApiClient mGoogleApiClient;

    private static final int GOOGLE_LOGIN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ButterKnife.bind(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_oauth_key))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mTextSwitcher.setInAnimation(this, R.anim.slide_in_left);
        mTextSwitcher.setOutAnimation(this, R.anim.slide_out_right);

        TextView inView = new TextView(this);
        inView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        inView.setTextColor(Color.WHITE);
        inView.setGravity(Gravity.CENTER);

        TextView outView = new TextView(this);
        outView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        outView.setTextColor(Color.WHITE);
        outView.setGravity(Gravity.CENTER);

        mTextSwitcher.addView(inView);
        mTextSwitcher.addView(outView);

        new TextThread().start();
    }

    private class TextThread extends Thread {
        @Override
        public void run() {
            String[] array = {"SHAKEY에 오신것을 환영합니다!", "여러분을 편리함의 세계로 모시겠습니다!", "신기한 경험 지금 시작하세요!"};

            for (final String anArray : array) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTextSwitcher.setText(anArray);
                    }
                });

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {

                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLoginView.setVisibility(View.VISIBLE);
                    mLoginButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                            startActivityForResult(intent, GOOGLE_LOGIN);
                        }
                    });
                    mTextSwitcher.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != GOOGLE_LOGIN) super.onActivityResult(requestCode, resultCode, data);

        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result.isSuccess()) {
            // 로그인 성공시
            GoogleSignInAccount account = result.getSignInAccount();

            if (account == null) {
                alertLoginError();
                return;
            }

            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(this);
        } else {
            alertLoginError();
        }
    }

    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
            startActivity(new Intent(this, MainActivity.class));
            this.finish();
        } else {
            alertLoginError();
        }
    }

    private void alertLoginError() {
        Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
    }
}
