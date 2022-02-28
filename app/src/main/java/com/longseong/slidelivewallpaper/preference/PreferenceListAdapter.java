package com.longseong.slidelivewallpaper.preference;

import static com.longseong.slidelivewallpaper.App.*;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.*;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.longseong.slidelivewallpaper.App;
import com.longseong.slidelivewallpaper.MainActivity;
import com.longseong.slidelivewallpaper.R;

import java.util.LinkedList;

public class PreferenceListAdapter extends RecyclerView.Adapter<PrefHolder> {

    private final MainActivity mActivity;
    private final PreferenceIdBundle mPreferenceIdBundle;

    private LinkedList<String> mIdList;

    public PreferenceListAdapter(MainActivity activity) {
        mActivity = activity;
        mPreferenceIdBundle = PreferenceIdBundle.getInstance();
        setData();
    }

    @NonNull
    @Override
    public PrefHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_preference, parent, false);
        return new PrefHolder(mActivity, view, PrefHolder.PREF_TYPE_DIALOG);
    }

    @Override
    public void onBindViewHolder(@NonNull PrefHolder holder, int position) {

        LinkedList<String> prefTextList = PreferenceText.getText(holder.itemView.getContext(), position);

        String preferenceId = mIdList.get(position);
        PreferenceIdBundle.Info info = mPreferenceIdBundle.getInfo(preferenceId);

        holder.title.setText(prefTextList.get(PreferenceText.TEXT_TYPE_TITLE));
        holder.description.setText(prefTextList.get(PreferenceText.TEXT_TYPE_DESCRIPTION));
        if (info.getType() == TYPE_VERTICAL) {
            holder.value_V.setVisibility(View.VISIBLE);
            holder.value_H.setVisibility(View.GONE);
            holder.value = holder.value_V;
        } else if (info.getType() == TYPE_HORIZONTAL) {
            holder.value_H.setVisibility(View.VISIBLE);
            holder.value_V.setVisibility(View.GONE);
            holder.value = holder.value_H;
        }
        holder.value.setText(App.mPreferenceData.getData(info.getKey()));

        holder.setOnResultListener(intent -> {
            if (intent == null) {
                return;
            }
            mPreferenceData.setData(preferenceId, intent.getStringExtra("result"));
            notifyItemChanged(holder.getAdapterPosition());
        });

        ((TextInputLayout)holder.prefDialog.getView().findViewById(R.id.input)).getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                holder.prefDialog.getDialog().getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        holder.prefDialog.buildDialog(prefTextList, info);
        holder.prefDialog.setResultData(App.mPreferenceData.getData(info.getKey()));
        setRespectiveData(holder);
    }

    @Override
    public int getItemCount() {
        return mIdList.size();
    }

    private void setData() {
        mIdList = mPreferenceIdBundle.getPreferenceIdList();
    }

    private void setRespectiveData(PrefHolder holder) {
        String preferenceId = mIdList.get(holder.getAdapterPosition());
        Button button = holder.prefDialog.getView().findViewById(R.id.button);
        TextInputLayout input = holder.prefDialog.getView().findViewById(R.id.input);

        switch (preferenceId) {
            default: {
                break;
            }
            case PREF_ID_IMAGE_DURATION: {
                int min = Integer.parseInt(mPreferenceData.getData(PREF_ID_IMAGE_FADE_DURATION));
                input.setError(min + " ~ 60000 (ms)");
                input.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                input.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
                    EditText editText = ((EditText) v);
                    if (hasFocus) {
                        editText.setSelection(0, editText.getText().length());
                    } else {
                        String valueString = editText.getText().toString();
                        if (valueString.equals("")) {
                            valueString = PreferenceData.getDefaultData(preferenceId);
                        }
                        int max = 60000;
                        int value = Integer.parseInt(valueString);
                        if (value < min) {
                            valueString = String.valueOf(min);
                        } else if (value > max) {
                            valueString = String.valueOf(max);
                        }
                        holder.prefDialog.setResultData(valueString);
                    }
                });
                break;
            }
            case PREF_ID_IMAGE_FADE_DURATION: {
                input.setError("1000 ~ 10000 (ms)");
                input.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                input.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
                    EditText editText = ((EditText) v);
                    if (hasFocus) {
                        editText.setSelection(0, editText.getText().length());
                    } else {
                        String valueString = editText.getText().toString();
                        if (valueString.equals("")) {
                            valueString = PreferenceData.getDefaultData(preferenceId);
                        }
                        int min = 1000;
                        int max = 10000;
                        int value = Integer.parseInt(valueString);
                        if (value < min) {
                            valueString = String.valueOf(min);
                        } else if (value > max) {
                            valueString = String.valueOf(max);
                        }
                        holder.prefDialog.setResultData(valueString);
                    }
                });

                holder.setOnResultListener(intent -> {
                    if (intent == null) {
                        return;
                    }
                    mPreferenceData.setData(preferenceId, intent.getStringExtra("result"));
                    notifyItemChanged(mIdList.indexOf(PREF_ID_IMAGE_DURATION));
                    notifyItemChanged(holder.getAdapterPosition());
                });

                break;
            }
            case PREF_ID_DIRECTORY_URI: {
                String value = PreferenceData.contentUriToPathData(mActivity, Uri.parse(mPreferenceData.getData(PREF_ID_DIRECTORY_URI)));
                holder.value.setText(value);
                input.getEditText().setText(value);
                button.setOnClickListener(v -> {
                    mActivity.setResultCallback(result -> {
                        if (result != null) {
                            ((TextInputLayout) holder.prefDialog.getView().findViewById(R.id.input)).getEditText().setText(PreferenceData.contentUriToPathData(mActivity, result));
                            holder.prefDialog.setResultData(result.toString());

                            //지속적인 권한 설정(재부팅 후에도 권한 유지됨)
                            mActivity.getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    });
                    mActivity.getDocumentLauncher().launch(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
                });
                input.setEnabled(false);
                break;
            }
            case PREF_ID_FPS_LIMIT: {
                input.setError("10 ~ 60 (fps)");
                input.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                input.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
                    EditText editText = ((EditText) v);
                    if (hasFocus) {
                        editText.setSelection(0, editText.getText().length());
                    } else {
                        String valueString = editText.getText().toString();
                        if (valueString.equals("")) {
                            valueString = PreferenceData.getDefaultData(preferenceId);
                        }
                        int min = 10;
                        int max = 60;
                        int value = Integer.parseInt(valueString);
                        if (value < min) {
                            valueString = String.valueOf(min);
                        } else if (value > max) {
                            valueString = String.valueOf(max);
                        }
                        holder.prefDialog.setResultData(valueString);
                    }
                });
                break;
            }
        }
    }

}