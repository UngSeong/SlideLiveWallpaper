package com.example.doralivewallpaper;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.doralivewallpaper.PrefDialog;


public class PrefDialog extends AlertDialog {
    
    PrefDialog dialog;
    Context context;
    TextView title;
    EditText input;
    LinearLayout buttonLayout;
    Button posiBtn, negaBtn;
    private boolean posiSet = false, negaSet = false;
    private OnClickListener posiListener, negaListener;
    
    String titleValue = "", inputValue, posiValue = "", negaValue = ""; 
    
    public PrefDialog(Context context) {
        super(context);
        this.context = context;
        dialog = this;
    }
    
    public interface OnClickListener {
        public void onClick(PrefDialog dialog, View view);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_layout);
        
        title = findViewById(R.id.title);
        input = findViewById(R.id.input);
        buttonLayout = findViewById(R.id.button_layout);
        posiBtn = (Button)getLayoutInflater().inflate(R.layout.button_layout, null);
        negaBtn = (Button)getLayoutInflater().inflate(R.layout.button_layout, null);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2, 1f);
        params.setMargins(5, 0, 5, 0);
        posiBtn.setLayoutParams(params);
        negaBtn.setLayoutParams(params);
        
        title.setText(titleValue);
        input.setText(inputValue);
        if(negaSet) {
            negaBtn.setText(negaValue);
            if(negaListener != null) {
                negaBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        negaListener.onClick(dialog, view);
                        dialog.dismiss();
                    }
                });
            }
            buttonLayout.addView(negaBtn);
        }
        if(posiSet) {
            posiBtn.setText(posiValue);
            if(posiListener != null) {
                posiBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        posiListener.onClick(dialog, view);
                        dialog.dismiss();
                    }
                });
            }
            buttonLayout.addView(posiBtn);
        }
    }
        
    @Override
    public void onStart() {
        super.onStart();
        ((InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
    
    public PrefDialog setTitle(String title) {
        titleValue = title;
        return this;
    }
    
    public PrefDialog setInputText(String inputText) {
        inputValue = inputText;
        return this;
    }
    
    public PrefDialog setPosiBtn(String text, final OnClickListener listener) {
        posiValue = text;
        posiListener = listener;
        posiSet = true;
        
        return this;
    }
    
    public PrefDialog setNegaBtn(String text, final OnClickListener listener) {
        negaValue = text;
        negaListener = listener;
        negaSet = true;
        return this;
    }
    
}
