package com.longseong.slidelivewallpaper.preference;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PreferenceService extends Service {
    public PreferenceService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i("PreferenceService", "이 서비스는 유저가 앱을 강제 종료 했을 때 MainActivity 클래스의 OnDestroy 메서드를 호출하기 위해 실행 됩니다.");

        return START_STICKY;
    }

}