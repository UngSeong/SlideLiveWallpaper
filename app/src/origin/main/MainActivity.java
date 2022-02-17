package com.example.doralivewallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.doralivewallpaper.R;
import java.io.File;

public class MainActivity extends Activity {
    
    public static final int CHOOSE_DIRECTORY_CODE = 1;
    
    public static final String SLIDE_PREF = "SLIDE_DURATION";
    public static final String STOP_PREF = "STOP_DURATION";
    public static final String FILE_PATH = "FILE_PATH";
    public static final String PATH_DEFAULT = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String ACTION_CHANGE_SETTING = "com.example.doralivewallpaper.MainActivity.ACTION_CHANGE_SETTING";
    
    public static final int SLIDE_MIN = 2;
    public static final int SLIDE_MAX = 10;
    public static final int SLIDE_DEFAULT = 3;
    public static final int STOP_MAX = 60;
    public static final int STOP_DEFAULT = 30;
    
    View slidePref, stopPref, pathPref;
    TextView slidePrefValue, stopPrefValue, pathPrefValue;
    SharedPreferences sPref;
    
    public static boolean checkPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) >= 0;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        
        slidePref = findViewById(R.id.slide_pref);
        slidePrefValue = findViewById(R.id.slide_pref_value);
        stopPref = findViewById(R.id.stop_pref);
        stopPrefValue = findViewById(R.id.stop_pref_value);
        pathPref = findViewById(R.id.path_pref);
        pathPrefValue = findViewById(R.id.path_pref_value);
        
        setValue(SLIDE_PREF, getValue(SLIDE_PREF));
        setValue(STOP_PREF, getValue(STOP_PREF)); 
        setValue(FILE_PATH, getValue(FILE_PATH)); 
        
        slidePref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PrefDialog(MainActivity.this) {
                        @Override
                        public void onStart() {
                            dialog.input.setKeyListener(DigitsKeyListener.getInstance());
                            super.onStart();
                        }
                    }
                    .setTitle("화면 전환 지속시간")
                    .setInputText(getValue(SLIDE_PREF) + "")
                    .setPosiBtn("확인", new PrefDialog.OnClickListener() {
                        @Override
                        public void onClick(PrefDialog dialog, View view) {
                            int value = (int)checkValue(SLIDE_PREF, dialog.input.getText().toString());
                            setValue(SLIDE_PREF, value);
                            if((int)getValue(STOP_PREF) < value) {
                                setValue(STOP_PREF, value);
                            }
                            sendBroadcast(ACTION_CHANGE_SETTING);
                        }
                    })
                    .setNegaBtn("취소", new PrefDialog.OnClickListener() {
                        @Override
                        public void onClick(PrefDialog dialog, View view) {
                            
                        }
                    }).show();
            }
        });
        
        stopPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new PrefDialog(MainActivity.this) {
                        @Override
                        public void onStart() {
                            dialog.input.setKeyListener(DigitsKeyListener.getInstance());
                            super.onStart();
                        }
                    }
                    .setTitle("이미지 지속시간")
                    .setInputText(getValue(STOP_PREF) + "")
                    .setPosiBtn("확인", new PrefDialog.OnClickListener() {
                        @Override
                        public void onClick(PrefDialog dialog, View view) {
                            int value = (int)checkValue(STOP_PREF, dialog.input.getText().toString());
                            setValue(STOP_PREF, value);
                            sendBroadcast(ACTION_CHANGE_SETTING);
                        }
                    })
                    .setNegaBtn("취소", new PrefDialog.OnClickListener() {
                        @Override
                        public void onClick(PrefDialog dialog, View view) {
                            
                        }
                    }).show();
            }
        });
        
        pathPref.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDocumentTree();
            }
        });
        
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if(requestCode != CHOOSE_DIRECTORY_CODE) return;
        if(data == null) return;
        
        String path = data.getData().getPath();
        
        path = path.replace("/tree", "/storage/emulated");
        path = path.replace("primary", "0");
        path = path.replace(":", "/");
        Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
        
        setValue(FILE_PATH, checkValue(FILE_PATH, path));
        sendBroadcast(ACTION_CHANGE_SETTING);
    }
    
    public static Object getValue(String key, Context context) {
        return getValue(key, PreferenceManager.getDefaultSharedPreferences(context));
    }
    
    private static Object getValue(String key, SharedPreferences sPref) {
        switch(key) {
            default: break;
            case SLIDE_PREF:
                return sPref.getInt(key, SLIDE_DEFAULT);
            case STOP_PREF:
                return sPref.getInt(key, STOP_DEFAULT);
            case FILE_PATH:
                return sPref.getString(key, PATH_DEFAULT);
        }
        return null;
    }
    
    private Object getValue(String key) {
        return getValue(key, sPref);
    }
    
    private void setValue(String key, Object value) {
        int intValue ;
        String stringValue = value.toString();
        switch(key) {
            default: break;
            case SLIDE_PREF:
                intValue = Integer.parseInt(stringValue);
                slidePrefValue.setText(intValue + "초");
                sPref.edit().putInt(SLIDE_PREF, intValue).apply();
                break;
            case STOP_PREF:
                intValue = Integer.parseInt(stringValue);
                stopPrefValue.setText(intValue + "초");
                sPref.edit().putInt(STOP_PREF, intValue).apply();
                break;
            case FILE_PATH:
                pathPrefValue.setText(stringValue);
                sPref.edit().putString(FILE_PATH, stringValue).apply();
                break;
        }
    }
    
    private Object checkValue(String key, String value) {
        switch(key) {
            default:
            case SLIDE_PREF:
                try {
                    int intValue = Integer.parseInt(value);
                    if(intValue < SLIDE_MIN) return SLIDE_MIN;
                    if(intValue > SLIDE_MAX) return SLIDE_MAX;
                    return intValue;
                } catch(NumberFormatException e) {
                    return SLIDE_MIN;
                }
            case STOP_PREF:
                int minValue = sPref.getInt(SLIDE_PREF, SLIDE_MIN);
                try {
                    int intValue = Integer.parseInt(value);
                    if(intValue < minValue) return minValue;
                    if(intValue > STOP_MAX) return STOP_MAX;
                    return intValue;
                } catch(NumberFormatException e) {
                    return checkValue(key, minValue + "");
                }
            case FILE_PATH:
                File file = new File(value);
                if(file.exists() && file.isDirectory()) {
                    return value;
                }
                return PATH_DEFAULT;
        }                
    }
     
    public void setIntent(View view) {
        
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) >= 0) {
            startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
        } else {
            /*new AlertDialog.Builder(this)
                .setTitle("권한 요청")
                .setMessage(getResources().getString(R.string.app_name) + "는 저장소에 있는 이미지 파일을 사용하기 때문에 외부 저장소 읽기 권한이 있어야 합니다!")
                .create().show();*/
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        
    }
    public void startDocumentTree() {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), CHOOSE_DIRECTORY_CODE);
    }
    
    public void sendBroadcast(String action) {
        sendBroadcast(new Intent(action));
    }
    
    public boolean checkPermission() {
        return checkPermission(this);
    }
    
}
