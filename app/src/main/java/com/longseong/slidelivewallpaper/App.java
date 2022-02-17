package com.longseong.slidelivewallpaper;

import android.app.Application;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.longseong.slidelivewallpaper.preference.PreferenceData;

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
