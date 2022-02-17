package com.longseong.slidelivewallpaper.preference.intent;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.longseong.slidelivewallpaper.preference.PrefHolder;


public class IntentActivity extends AppCompatActivity {

    PrefHolder.OnResultListener listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == (short)listener.hashCode() && resultCode == RESULT_OK) {
            listener.onResult(data);
        } else {
            listener.onResult(null);
        }
        finish();
    }
};