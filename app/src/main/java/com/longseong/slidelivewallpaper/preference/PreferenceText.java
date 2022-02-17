package com.longseong.slidelivewallpaper.preference;

import android.content.Context;

import com.longseong.slidelivewallpaper.R;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class PreferenceText {

    private static final int RESOURCE_NULL = 0;

    public static final int TEXT_TYPE_TITLE = 0;
    public static final int TEXT_TYPE_DESCRIPTION = 1;
    public static final int TEXT_TYPE_POSITIVE = 2;
    public static final int TEXT_TYPE_NEGATIVE = 3;
    public static final int TEXT_TYPE_NEUTRAL = 4;
    public static final int TEXT_TYPE_ADDITIONAL = 5;

    public static final int[][] preferenceTextResArray = {
            {R.string.preference_1_title, R.string.preference_1_description, R.string.preference_shared_positive, R.string.preference_shared_negative, RESOURCE_NULL, RESOURCE_NULL},
            {R.string.preference_2_title, R.string.preference_2_description, R.string.preference_shared_positive, R.string.preference_shared_negative, RESOURCE_NULL, RESOURCE_NULL},
            {R.string.preference_3_title, R.string.preference_3_description, R.string.preference_shared_positive, R.string.preference_shared_negative, RESOURCE_NULL, R.string.preference_3_additional},
            {R.string.preference_4_title, R.string.preference_4_description, R.string.preference_shared_positive, R.string.preference_shared_negative, RESOURCE_NULL, RESOURCE_NULL}
    };

    public static LinkedList<String> getText(Context context, int preferenceIndex) {
        LinkedList<String> result = new LinkedList<>();

        int[] resArray = preferenceTextResArray[preferenceIndex];

        for (int res : resArray) {
            if (res == RESOURCE_NULL) {
                result.add("");
            } else {
                result.add(context.getString(res));
            }
        }

        return result;
    }

    private PreferenceText() {
    }

    private static class TextMap extends LinkedHashMap<Integer, String> {

        void setAllType(String... array) {
            clear();

            for (int i = 0; i < array.length; i++) {
                put(i, array[i]);
            }
        }

        private TextMap(String... array) {
            super();
            setAllType(array);
        }

    }
}
