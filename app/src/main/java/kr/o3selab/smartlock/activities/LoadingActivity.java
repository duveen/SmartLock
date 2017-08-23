package kr.o3selab.smartlock.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import kr.o3selab.smartlock.R;
import kr.o3selab.smartlock.common.utils.Debug;
import kr.o3selab.smartlock.common.utils.Utils;
import kr.o3selab.smartlock.models.Shakey;
import kr.o3selab.smartlock.services.FCMIDService;

public class LoadingActivity extends BaseActivity {

    public static final String TAG = "LoadingActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Utils.getAppKeyHash(this);

        new TedPermission(LoadingActivity.this)
                .setPermissionListener(permissionlistener)
                .setRationaleMessage("SHAKEY 서비스를 이용하기 위한 권한입니다.")
                .setDeniedMessage("권한 미제공시 서비스 제공이 어렵습니다. 프로그램을 종료합니다. 다시 실행해주세요!")
                .setPermissions(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE
                )
                .check();
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            getAuthStatus();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(LoadingActivity.this, "프로그램을 실행 할 수 없습니다. 다시 실행해주세요!", Toast.LENGTH_SHORT).show();
            LoadingActivity.this.finish();
        }
    };

    private void getAuthStatus() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(LoadingActivity.this);
        if (resultCode == ConnectionResult.SUCCESS) {
            mAuth = FirebaseAuth.getInstance();
            mAuth.addAuthStateListener(authStateListener);
        } else {
            Toast.makeText(LoadingActivity.this, "Google Play 서비스 버전 업데이트가 필요합니다.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.google.android.gms"));
            startActivity(intent);
            LoadingActivity.this.finish();
        }
    }

    FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            Debug.d("Firebase Login Check");

            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                Debug.d("Logout: Start Register Activity");
                startActivity(new Intent(LoadingActivity.this, RegisterActivity.class));
            } else {
                Debug.d("Login: Start Main Activity (UID:" + user.getUid() + ")");
                startActivity(new Intent(LoadingActivity.this, MainActivity.class));

                FCMIDService.checkFCMToken(FirebaseInstanceId.getInstance().getToken());
            }

            LoadingActivity.this.finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuth != null) mAuth.removeAuthStateListener(authStateListener);
    }
}
