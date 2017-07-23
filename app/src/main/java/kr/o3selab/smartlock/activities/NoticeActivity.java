package kr.o3selab.smartlock.activities;

import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import kr.o3selab.smartlock.R;

/**
 * Created by LGY on 2016-10-31.
 */

public class NoticeActivity extends BaseActivity {
    private WebView mWebView;    // 웹뷰 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        mWebView = (WebView)findViewById(R.id.webnotice);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl("http://www.naver.com");
        mWebView.setWebViewClient(new WishWebViewClient());

    }
    @Override

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()){

            mWebView.goBack();

            return true;

        }

        return super.onKeyDown(keyCode, event);

    }



    private class WishWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            view.loadUrl(url);

            return true;

        }

    }
}