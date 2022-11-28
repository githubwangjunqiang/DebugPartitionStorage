package org.xbet.debugpartitionstorage;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @autor ${王俊强} on 2018/9/27.
 */
public class App extends Application {
    private static final String TAG = "App";
    private static Context mContext;
    private static Handler mHandler;


    public App() {
        super();
        Log.d(TAG, "App: ");
    }

    public static Context getContext() {
        return mContext;
    }



    public static void toast(String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg + "", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mHandler = new Handler(Looper.getMainLooper());
        mContext = this;

    }

}
