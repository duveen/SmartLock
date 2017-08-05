package kr.o3selab.smartlock.common.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Debug {

    private static final String TAG = "Shakey";
    private static boolean DEBUG = true;

    // 에러 리포팅
    private static final StringBuilder logs = new StringBuilder();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA);

    // Error
    public static void e(String message) {
        if (DEBUG) Log.e(TAG, buildMsg(message));
    }

    // Warning
    public static void w(String message) {
        if (DEBUG) Log.w(TAG, buildMsg(message));
    }

    // Information
    public static void i(String message) {
        if (DEBUG) Log.i(TAG, buildMsg(message));
    }

    // Debug
    public static void d(String message) {
        if (DEBUG) Log.d(TAG, buildMsg(message));
    }

    public static void d(int value) {
        if (DEBUG) Log.d(TAG, buildMsg(String.valueOf(value)));
    }

    // Verbose
    public static void v(String message) {
        if (DEBUG) Log.v(TAG, buildMsg(message));
    }

    private static String buildMsg(String message) {

        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];

        StringBuilder log = new StringBuilder();
        log.append("[");
        log.append(TAG);
        log.append("$");
        log.append(stackTraceElement.getFileName().replace(".java", ""));
        log.append("$");
        log.append(stackTraceElement.getMethodName());
        log.append("$");
        log.append(sdf.format(new Date(System.currentTimeMillis())));
        log.append("] ");
        log.append(message);

        logs.append(log.toString()).append("\n");

        return log.toString();
    }
}