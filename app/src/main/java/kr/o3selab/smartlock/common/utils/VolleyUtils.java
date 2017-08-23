package kr.o3selab.smartlock.common.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import kr.o3selab.smartlock.common.GlobalApplication;

public class VolleyUtils {
    private static VolleyUtils me;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    public VolleyUtils(Context context) {
        mRequestQueue = Volley.newRequestQueue(context);
        mImageLoader = new ImageLoader(this.mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<>(4);

            @Override
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }
        });
    }

    public static VolleyUtils getInstance() {
        if (me == null) {
            me = new VolleyUtils(GlobalApplication.getGlobalApplicationContext());
        }
        return me;
    }

    public RequestQueue getRequestQueue() {
        return this.mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        return this.mImageLoader;
    }
}