package com.ch_linghu.fanfoudroid;

import java.util.HashSet;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.ch_linghu.fanfoudroid.data.db.StatusDatabase;
import com.ch_linghu.fanfoudroid.data.db.StatusTablesInfo.StatusTable;
import com.ch_linghu.fanfoudroid.helper.ImageManager;
import com.ch_linghu.fanfoudroid.helper.Preferences;
import com.ch_linghu.fanfoudroid.helper.ProfileImageCacheManager;
import com.ch_linghu.fanfoudroid.weibo.Weibo;

public class TwitterApplication extends Application {

    public static final String TAG = "TwitterApplication";
    
    public static ImageManager mImageManager;
    public static ProfileImageCacheManager mProfileImageCacheManager;
    public static StatusDatabase mDb;
    public static Weibo mApi; // new API
    public static Context mContext;
    
    public static int networkType = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this.getApplicationContext();
        mImageManager = new ImageManager(this);
        mProfileImageCacheManager = new ProfileImageCacheManager();
        mApi = new Weibo();
        mDb = StatusDatabase.getInstance(this);

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        String username = preferences.getString(Preferences.USERNAME_KEY, "");
        String password = preferences.getString(Preferences.PASSWORD_KEY, "");

        if (Weibo.isValidCredentials(username, password)) {
            mApi.setCredentials(username, password); // Setup API and HttpClient
        }

        // 为cmwap用户设置代理上网
        String type = getNetworkType();
        if (null != type && type.equalsIgnoreCase("cmwap")) {
            Toast.makeText(this, "您当前正在使用cmwap网络上网.", Toast.LENGTH_SHORT);
            mApi.getHttpClient().setProxy("10.0.0.172", 80, "http");
        }
    }

    public String getNetworkType() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        NetworkInfo mobNetInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return  mobNetInfo.getExtraInfo(); // 接入点名称: 此名称可被用户任意更改 如: cmwap, cmnet, internet ...
    }

    @Override
    public void onTerminate() {
        cleanupImages();
        mDb.close();
         Toast.makeText(this, "exit app", Toast.LENGTH_LONG);

        super.onTerminate();
    }

    private void cleanupImages() {
        HashSet<String> keepers = new HashSet<String>();

        Cursor cursor = mDb.fetchAllTweets(StatusTable.TYPE_HOME);

        if (cursor.moveToFirst()) {
            int imageIndex = cursor
                    .getColumnIndexOrThrow(StatusTable.FIELD_PROFILE_IMAGE_URL);
            do {
                keepers.add(cursor.getString(imageIndex));
            } while (cursor.moveToNext());
        }

        cursor.close();

        cursor = mDb.fetchAllDms(-1);

        if (cursor.moveToFirst()) {
            int imageIndex = cursor
                    .getColumnIndexOrThrow(StatusTable.FIELD_PROFILE_IMAGE_URL);
            do {
                keepers.add(cursor.getString(imageIndex));
            } while (cursor.moveToNext());
        }

        cursor.close();

        mImageManager.cleanup(keepers);
    }
}
