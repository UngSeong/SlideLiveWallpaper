package com.longseong.slidelivewallpaper.log;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.longseong.slidelivewallpaper.R;

public class LogActivity extends com.longseong.logcenter.log.LogActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
    }
}
