package com.longseong.slidelivewallpaper.preference;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public final class PreferenceIdBundle {

    public static final String PREF_ID_IMAGE_DURATION = "preference.id.image_duration";
    public static final String PREF_ID_IMAGE_FADE_DURATION = "preference.id.image_fade_duration";
    public static final String PREF_ID_DIRECTORY_URI = "preference.id.directory_uri";
    public static final String PREF_ID_FPS_LIMIT = "preference.id.fps_limit";
    public static final String PREF_ID_DEBUG = "preference.id.debug";
    public static final String PREF_ID_INCLUDE_SUB_DIRECTORY = "preference.id.include_sub_directory";

    public static final int TYPE_NULL = 0;
    public static final int TYPE_HORIZONTAL = 1;
    public static final int TYPE_VERTICAL = 2;

    public static final int FLAG_NULL = 0;
    public static final int FLAG_USE_POSITIVE = 1;
    public static final int FLAG_USE_NEGATIVE = 1 << 1;
    public static final int FLAG_USE_NEUTRAL = 1 << 2;
    public static final int FLAG_USE_INPUT = 1 << 3;
    public static final int FLAG_USE_BUTTON = 1 << 4;

    private static PreferenceIdBundle preferenceIdBundleInstance;

    public static PreferenceIdBundle getInstance() {
        if (preferenceIdBundleInstance == null) {
            return preferenceIdBundleInstance = new PreferenceIdBundle();
        } else {
            return preferenceIdBundleInstance;
        }
    }

    private LinkedHashMap<String, Integer> typeMap;
    private LinkedHashMap<String, Integer> flagMap;

    private PreferenceIdBundle() {
        initMap();
    }

    private void initMap() {
        typeMap = new LinkedHashMap<>();
        flagMap = new LinkedHashMap<>();

        typeMap.put(PREF_ID_IMAGE_DURATION, TYPE_HORIZONTAL);
        typeMap.put(PREF_ID_IMAGE_FADE_DURATION, TYPE_HORIZONTAL);
        typeMap.put(PREF_ID_DIRECTORY_URI, TYPE_VERTICAL);
        typeMap.put(PREF_ID_FPS_LIMIT, TYPE_HORIZONTAL);

        flagMap.put(PREF_ID_IMAGE_DURATION, FLAG_USE_POSITIVE | FLAG_USE_NEGATIVE | FLAG_USE_INPUT);
        flagMap.put(PREF_ID_IMAGE_FADE_DURATION,  FLAG_USE_POSITIVE | FLAG_USE_NEGATIVE | FLAG_USE_INPUT);
        flagMap.put(PREF_ID_DIRECTORY_URI,  FLAG_USE_POSITIVE | FLAG_USE_NEGATIVE | FLAG_USE_INPUT | FLAG_USE_BUTTON);
        flagMap.put(PREF_ID_FPS_LIMIT,  FLAG_USE_POSITIVE | FLAG_USE_NEGATIVE | FLAG_USE_INPUT);
    }

    public LinkedList<String> getPreferenceIdList() {
        return new LinkedList<>(typeMap.keySet());
    }

    public int getPreferenceType(String preferenceId) {
        if (preferenceId == null) {
            return TYPE_NULL;
        }
        Integer type = typeMap.get(preferenceId);

        if (type == null) {
            return TYPE_NULL;
        } else {
            return type;
        }
    }

    public int getPreferenceFlag(String preferenceId) {
        if (preferenceId == null) {
            return FLAG_NULL;
        }
        Integer flag = flagMap.get(preferenceId);

        if (flag == null) {
            return FLAG_NULL;
        } else {
            return flag;
        }
    }

    public Info getInfo(String preferenceId) {
        if (preferenceId == null) {
            return null;
        }

        return new Info(preferenceId);
    }

    public class Info {

        private final String key;

        private int prefType;
        private int prefFlag;

        private Info(String preferenceKey) {
            key = preferenceKey;

            initInfo();
        }

        private void initInfo() {
            prefType = getPreferenceType(key);
            prefFlag = getPreferenceFlag(key);
        }

        public String getKey() {
            return key;
        }

        public int getType() {
            return prefType;
        }

        public int getFlag() {
            return prefFlag;
        }

    }

}
