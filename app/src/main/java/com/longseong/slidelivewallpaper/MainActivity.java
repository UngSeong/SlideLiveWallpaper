package com.longseong.slidelivewallpaper;

import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.documentfile.provider.DocumentFile;

import com.longseong.logcenter.LogActivity;
import com.longseong.logcenter.LogCenter;
import com.longseong.preference.Preference;
import com.longseong.preference.PreferenceListWrapper;
import com.longseong.preference.PreferenceManager;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class MainActivity extends AppCompatActivity {

    private PreferenceManager mPreferenceManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        newPreference();
    }

    private void newPreference() {
        mPreferenceManager = PreferenceManager.getInstance(this);


        mPreferenceManager.setPreferenceCreatedListener((manager, preference, activity) -> {
            LogCenter.postLog(this, "pref");
            int id = preference.getId();

            if (id == R.id.preferenceData_changeWallpaper) {
                preference.getEvent().setRunnable(new Handler(), () -> {
                    Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                    activity.startActivity(intent);
                });
            } else if (id == R.id.preferenceData_directoryUri) {
                Preference.Intent intentData = preference.getIntent();
                intentData.setDefaultUri(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
                intentData.setLauncher(activity.registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
                    //도큐먼트를 선택하지 않으면 result == null
                    if (result != null) {
                        activity.getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        String normalizedPath;
                        String path = result.getPath();
                        String[] splitPath = path.split(":");
                        String primary;
                        String lastDirectory = "";

                        try {
                            if (path.equals("/tree/downloads") || splitPath[0].endsWith("msd")) {
                                primary = "다운로드";
                                if (splitPath.length > 1) {
                                    lastDirectory = " - " + DocumentFile.fromTreeUri(activity, result).getName();
                                } else {
                                    lastDirectory = "";
                                }
                            } else if (splitPath[0].endsWith("primary")) {
                                primary = Environment.getExternalStoragePublicDirectory("").getPath();
                                if (splitPath.length > 1) {
                                    lastDirectory = "/" + splitPath[1];
                                }
                            } else {
                                primary = splitPath[0].replace("tree", "storage");
                                if (splitPath.length > 1) {
                                    lastDirectory = "/" + splitPath[1];
                                }
                            }
                            normalizedPath = primary + lastDirectory;
                        } catch (Exception e) {
                            normalizedPath = result.toString();
                        }

                        String original = preference.getContentValueRaw();
                        if (!original.equals(result.toString())) {
                            preference.setContentValueRaw(result.toString());
                            manager.savePreferenceContentValueRaw(preference);

                            preference.setContentValue(normalizedPath);
                            manager.savePreferenceContentValue(preference);
                            ((PreferenceListWrapper.PreferenceViewHolder) manager.getPreferenceListWrapper().getHolderAt(0, 4)).update(preference);

                            LiveWallpaperService.notifyPreferenceChanged(true);
                        }
                    }
                }));
            } else if (id == R.id.preferenceData_reloadImage) {
                preference.getEvent().setRunnable(new Handler(), () -> {
                    LiveWallpaperService.notifyPreferenceChanged(true);
                    App.makeToast(activity, "이미지를 다시 불러옵니다.");
                });
            } else if (id == R.id.preferenceData_log) {
                preference.getEvent().setRunnable(new Handler(), () -> {
                    startActivity(new Intent(this, LogActivity.class));
                });
            }
        });

        NestedScrollView preferenceView = findViewById(R.id.preference_layout);

        mPreferenceManager.registerPreferenceLayout(this, preferenceView);
    }
}
