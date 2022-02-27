package com.longseong.slidelivewallpaper.preference;

import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import androidx.documentfile.provider.DocumentFile;

import com.longseong.slidelivewallpaper.BuildConfig;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class PreferenceData {

    private static final String PREFERENCE_NAME = BuildConfig.APPLICATION_ID + "preference.data.id";

    public static final String default_imageDuration = String.valueOf(15000);
    public static final String default_imageFadeDuration = String.valueOf(5000);
    public static final String default_directoryUri = "지정 된 경로 없음";
    public static final String default_fpsLimit = String.valueOf(30);

    public static void saveData(Context context, PreferenceData data) {
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_ID_IMAGE_DURATION, data.imageDuration)
                .putString(PREF_ID_IMAGE_FADE_DURATION, data.imageFadeDuration)
                .putString(PREF_ID_DIRECTORY_URI, data.directoryUri)
                .putString(PREF_ID_FPS_LIMIT, data.fpsLimit)
                .putBoolean(PREF_ID_DEBUG, data.mDebug)
                .putBoolean(PREF_ID_INCLUDE_SUB_DIRECTORY, data.mIncludeSubDirectory)
                .apply();
    }

    public static PreferenceData loadData(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        PreferenceData data = new PreferenceData();

        data.imageDuration = preferences.getString(PREF_ID_IMAGE_DURATION, default_imageDuration);
        data.imageFadeDuration = preferences.getString(PREF_ID_IMAGE_FADE_DURATION, default_imageFadeDuration);
        data.directoryUri = preferences.getString(PREF_ID_DIRECTORY_URI, default_directoryUri);
        data.fpsLimit = preferences.getString(PREF_ID_FPS_LIMIT, default_fpsLimit);
        data.mDebug = preferences.getBoolean(PREF_ID_DEBUG, false);
        data.mIncludeSubDirectory = preferences.getBoolean(PREF_ID_INCLUDE_SUB_DIRECTORY, false);

        return data;
    }

    public static String getDefaultData(String key) {
        switch (key) {
            default: {
                return null;
            }
            case PREF_ID_IMAGE_DURATION: {
                return default_imageDuration;
            }
            case PREF_ID_IMAGE_FADE_DURATION: {
                return default_imageFadeDuration;
            }
            case PREF_ID_DIRECTORY_URI: {
                return default_directoryUri;
            }
            case PREF_ID_FPS_LIMIT: {
                return default_fpsLimit;
            }
        }
    }

    public static String contentUriToPathData(Context context, Uri uri) {
        String path = uri.getPath();
        String[] splitPath = path.split(":");
        String primary;
        String lastDirectory = "";

        try {
            if (path.equals("/tree/downloads") || splitPath[0].endsWith("msd")) {
                primary = "다운로드";
                if (splitPath.length > 1) {
                    lastDirectory = " - " + DocumentFile.fromTreeUri(context, uri).getName();
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
        } catch (Exception e) {
            return uri.toString();
        }


        return primary + lastDirectory;
    }

    private String imageDuration;
    private String imageFadeDuration;
    private String directoryUri;
    private String fpsLimit;

    private boolean mDebug;
    private boolean mIncludeSubDirectory;

    private PreferenceData() {

    }

    public void saveData(Context context) {
        saveData(context, this);
    }

    public String getData(String key) {
        switch (key) {
            default: {
                return null;
            }
            case PREF_ID_IMAGE_DURATION: {
                return imageDuration;
            }
            case PREF_ID_IMAGE_FADE_DURATION: {
                return imageFadeDuration;
            }
            case PREF_ID_DIRECTORY_URI: {
                return directoryUri;
            }
            case PREF_ID_FPS_LIMIT: {
                return fpsLimit;
            }
        }
    }

    public void setData(String key, String value) {
        boolean filesChanged = false;
        switch (key) {
            default: {
                return;
            }
            case PREF_ID_IMAGE_DURATION: {
                imageDuration = value;
                break;
            }
            case PREF_ID_IMAGE_FADE_DURATION: {
                imageFadeDuration = value;
                if (Integer.parseInt(imageDuration) < Integer.parseInt(imageFadeDuration)) {
                    imageDuration = imageFadeDuration;
                }
                break;
            }
            case PREF_ID_DIRECTORY_URI: {
                filesChanged = true;
                directoryUri = value;
                break;
            }
            case PREF_ID_FPS_LIMIT: {
                fpsLimit = value;
                break;
            }
        }
        LiveWallpaperService.notifyPreferenceChanged(filesChanged);
    }

    public boolean isDebug() {
        return mDebug;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
        LiveWallpaperService.notifyPreferenceChanged(false);
    }

    public boolean isIncludeSubDirectory() {
        return mIncludeSubDirectory;
    }

    public void setIncludeSubDirectory(boolean mIncludeSubDirectory) {
        this.mIncludeSubDirectory = mIncludeSubDirectory;
        LiveWallpaperService.notifyPreferenceChanged(true);
    }
}
