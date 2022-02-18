package com.longseong.slidelivewallpaper;

import android.app.Application;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.longseong.slidelivewallpaper.preference.PreferenceData;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class App extends Application {

    public static PreferenceData mPreferenceData;
    public static DisplayMetrics mDisplayMetrics;

    @Override
    public void onCreate() {
        super.onCreate();

        initPreferenceData();
        initDisplayMetrics();
    }

    private void initPreferenceData() {
        mPreferenceData = PreferenceData.loadData(this);
    }

    private void initDisplayMetrics() {
        mDisplayMetrics = new DisplayMetrics();

        Display display = getSystemService(WindowManager.class).getDefaultDisplay();
        display.getRealMetrics(mDisplayMetrics);
    }

}
