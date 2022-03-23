package com.longseong.slidelivewallpaper;

import android.app.Application;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.longseong.preference.Preference;
import com.longseong.preference.PreferenceListWrapper;
import com.longseong.preference.PreferenceManager;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class App extends Application {

    public static DisplayMetrics mDisplayMetrics;

    private static Toast mToast;

    public static void makeToast(Context context, String message) {
        if (mToast != null) {
            mToast.cancel();
            mToast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            mToast.show();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //new preference
        initPreference();

        initDisplayMetrics();

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

    }

    private void initDisplayMetrics() {
        mDisplayMetrics = new DisplayMetrics();

        Display display = getSystemService(WindowManager.class).getDefaultDisplay();
        display.getRealMetrics(mDisplayMetrics);
    }

    public void initPreference() {
        PreferenceManager.getInstance(this, R.xml.preference, initSaveOptimizer());
    }

    private PreferenceManager.SaveOptimizer initSaveOptimizer() {
        return (manager, preference, saveFlag) -> {
            int id = preference.getId();
            boolean reloadFile = false;

            if (id == R.id.preferenceData_imageDuration && saveFlag == PreferenceManager.SaveOptimizer.FLAG_SAVE_CONTENT_VALUE) {
                String minStr = manager.getPreferenceById(R.id.preferenceData_imageFadeDuration).getContentValue();
                float min = minStr.equals("") ? 10 : Float.parseFloat(minStr);
                float max = 60;
                float value;
                try {
                    value = Float.parseFloat(preference.getContentValue());
                } catch (NumberFormatException e) {
                    value = 30;
                }

                value = Math.min(value, max);
                value = Math.max(value, min);
                preference.setContentValue(value + "");
            } else if (id == R.id.preferenceData_imageFadeDuration && saveFlag == PreferenceManager.SaveOptimizer.FLAG_SAVE_CONTENT_VALUE) {
                float min = 1;
                float max = 10;
                float value;
                try {
                    value = Float.parseFloat(preference.getContentValue());
                } catch (NumberFormatException e) {
                    value = 30;
                }

                value = Math.min(value, max);
                value = Math.max(value, min);
                preference.setContentValue(value + "");

                //imageDuration 값 재가공 및 업데이트
                Preference imageDuration = manager.getPreferenceById(R.id.preferenceData_imageDuration);
                manager.savePreferenceContentValue(imageDuration);
                if (manager.getPreferenceListWrapper() != null) {
                    ((PreferenceListWrapper.PreferenceViewHolder) manager.getPreferenceListWrapper().getHolderAt(0, 1)).update(imageDuration);
                }
            } else if (id == R.id.preferenceData_includeSubDirectory && saveFlag == PreferenceManager.SaveOptimizer.FLAG_SAVE_SWITCH_VALUE) {
                reloadFile = true;
            } else if (id == R.id.preferenceData_debug && saveFlag == PreferenceManager.SaveOptimizer.FLAG_SAVE_SWITCH_VALUE) {
                String message = preference.getSwitch().isChecked() ? "배경화면 정보를 표시합니다." : "배경화면 정보를 표시하지 않습니다.";
                App.makeToast(this, message);
            }

            LiveWallpaperService.notifyPreferenceChanged(reloadFile);
            return preference;
        };
    }

}
