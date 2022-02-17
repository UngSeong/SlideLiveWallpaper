package com.longseong.slidelivewallpaper.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.longseong.slidelivewallpaper.App;
import com.longseong.slidelivewallpaper.R;

import java.io.Serializable;
import java.util.LinkedList;

public class PrefHolder extends RecyclerView.ViewHolder {

    static final int PREF_TYPE_DIALOG = 0;

    private final int prefType;

    TextView title;
    TextView description;
    TextView value_H, value_V, value;

    PrefDialog prefDialog;
    private OnResultListener onResultListener;

    PrefHolder(Context context, View itemView, int type) {
        super(itemView);

        prefType = type;

        initView();
        setPrefButton(context);
    }

    private void initView() {
        title = itemView.findViewById(R.id.tv_preference_title);
        description = itemView.findViewById(R.id.tv_preference_description);
        value_H = itemView.findViewById(R.id.tv_preference_value_horizontal);
        value_V = itemView.findViewById(R.id.tv_preference_value_vertical);
    }

    private void setPrefButton(Context context) {

        switch (PrefHolder.this.prefType) {
            default: {
                break;
            }
            case PREF_TYPE_DIALOG: {
                prefDialog = new PrefDialog(context);
                itemView.setOnClickListener((v) -> {
                    prefDialog.show();
                });
                break;
            }
        }
    }

    void setOnResultListener(OnResultListener listener) {
        if (listener != null) {
            onResultListener = listener;
        } else {
            onResultListener = intent -> {

            };
        }
    }

    private void sendResult(@Nullable Intent intent) {
        onResultListener.onResult(intent);
    }

    public interface OnResultListener extends Serializable {
        void onResult(Intent intent);
    }

    public class PrefDialog {

        private final Context mContext;
        private final AlertDialog.Builder mBuilder;

        private AlertDialog mDialog;
        private View mView;
        private TextView mTitle;
        private TextView mContent;
        private TextInputLayout mInput;
        private Button mButton;

        private String mResultData;

        PrefDialog(Context context) {
            this(context, ResourcesCompat.ID_NULL);
        }

        PrefDialog(Context context, @StyleRes int theme) {
            this.mContext = context;
            this.mBuilder = new AlertDialog.Builder(mContext, theme);
            initView();
        }

        private void initView() {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mView = inflater.inflate(R.layout.item_pref_edit, null, false);
            mTitle = mView.findViewById(R.id.title);
            mContent = mView.findViewById(R.id.content);
            mInput = mView.findViewById(R.id.input);
            mButton = mView.findViewById(R.id.button);
        }

        private void sendResult() {
            mInput.getEditText().clearFocus();
            Intent result = new Intent();
            result.putExtra("result", mResultData);
            PrefHolder.this.sendResult(result);
        }

        private void show() {
            createDialog();
            mDialog.show();
        }

        private void createDialog() {
            if (mView.getParent() != null) {
                ((ViewGroup) mView.getParent()).removeView(mView);
            }
            mBuilder.setView(mView);
            mDialog = mBuilder.create();
        }

        public void buildDialog(LinkedList<String> textList, PreferenceIdBundle.Info info) {
            mTitle.setText(textList.get(PreferenceText.TEXT_TYPE_TITLE));
            mContent.setText(textList.get(PreferenceText.TEXT_TYPE_DESCRIPTION));
            mInput.getEditText().setText(App.mPreferenceData.getData(info.getKey()));

            int flag = info.getFlag();

            if ((flag & PreferenceIdBundle.FLAG_USE_BUTTON) > 0) {
                mButton.setVisibility(View.VISIBLE);
                mButton.setText(textList.get(PreferenceText.TEXT_TYPE_ADDITIONAL));
            }
            if ((flag & PreferenceIdBundle.FLAG_USE_POSITIVE) > 0) {
                mBuilder.setPositiveButton(textList.get(PreferenceText.TEXT_TYPE_POSITIVE), (dialog, which) -> {
                    sendResult();
                });
            }
            if ((flag & PreferenceIdBundle.FLAG_USE_NEGATIVE) > 0) {
                mBuilder.setNegativeButton(textList.get(PreferenceText.TEXT_TYPE_NEGATIVE), (dialog, which) -> {
                });
            }
            if ((flag & PreferenceIdBundle.FLAG_USE_NEUTRAL) > 0) {
                mBuilder.setNeutralButton(textList.get(PreferenceText.TEXT_TYPE_NEUTRAL), (dialog, which) -> {
                });
            }
        }

        public View getView() {
            return mView;
        }

        public void setResultData(String result) {
            mResultData = result;
        }

        public AlertDialog getDialog() {
            return mDialog;
        }
    }

}