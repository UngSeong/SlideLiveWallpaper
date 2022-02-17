package com.longseong.slidelivewallpaper;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.longseong.slidelivewallpaper.preference.PrefHolder;
import com.longseong.slidelivewallpaper.preference.PreferenceIdBundle;
import com.longseong.slidelivewallpaper.preference.PreferenceListAdapter;
import com.longseong.slidelivewallpaper.preference.PreferenceService;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class MainActivity extends AppCompatActivity {

    private static final int ID_MENU_DEBUG = R.id.menu_debug;
    private static final int ID_MENU_INCLUDE_SUB_DIRECTORY = R.id.menu_include_sub_directory;
    private static final int ID_MENU_REFRESH_FILES = R.id.menu_refresh_files;

    private ActivityResultLauncher<Uri> mDocumentLauncher;

    private RecyclerView preferenceList;
    private Button setWallpaperButton;

    private PreferenceListAdapter preferenceListAdapter;

    private ResultCallback mResultCallback;

    public interface ResultCallback {
        void onResult(Uri result);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initLauncher();
        initView();
        initService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        App.mPreferenceData.saveData(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem item;

        for (int i = 0; i < menu.size(); i++) {
            item = menu.getItem(i);

            switch (item.getItemId()) {
                case ID_MENU_DEBUG: {
                    item.setChecked(App.mPreferenceData.isDebug());
                    break;
                }
                case ID_MENU_INCLUDE_SUB_DIRECTORY: {
                    item.setChecked(App.mPreferenceData.isIncludeSubDirectory());
                    break;
                }
                case ID_MENU_REFRESH_FILES: {
                    if (LiveWallpaperService.getWallpaperEngineList().size() == 0) {
                        menu.removeItem(ID_MENU_REFRESH_FILES);
                    }
                    break;
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case ID_MENU_DEBUG: {
                boolean debug = !item.isChecked();
                item.setChecked(debug);
                App.mPreferenceData.setDebug(debug);
                break;
            }
            case ID_MENU_INCLUDE_SUB_DIRECTORY: {
                boolean includeSubDirectory = !item.isChecked();
                item.setChecked(includeSubDirectory);
                App.mPreferenceData.setIncludeSubDirectory(includeSubDirectory);
                break;
            }
            case ID_MENU_REFRESH_FILES: {
                Toast.makeText(this, "이미지 파일을 다시 탐색 합니다.", Toast.LENGTH_SHORT).show();
                LiveWallpaperService.notifyPreferenceChanged(true);
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void initLauncher() {
        mDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                result -> {
                    if (mResultCallback != null) {
                        mResultCallback.onResult(result);
                    }
                }
        );
    }

    private void initView() {
        initWallpaperButton();
        initPreferenceList();
    }

    private void initService() {
        Intent preferenceServiceIntent = new Intent(this, PreferenceService.class);
        startService(preferenceServiceIntent);
    }

    private void initWallpaperButton() {
        setWallpaperButton = findViewById(R.id.set_background_button);
        setWallpaperButton.setOnClickListener(v -> {
            Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            startActivity(intent);
        });
    }

    private void initPreferenceList() {
        preferenceList = findViewById(R.id.preference_list_view);

        preferenceListAdapter = new PreferenceListAdapter(this);
        preferenceList.setLayoutManager(new LinearLayoutManager(this));
        preferenceList.setAdapter(preferenceListAdapter);
    }

    public ActivityResultLauncher<Uri> getDocumentLauncher() {
        return mDocumentLauncher;
    }

    public void setResultCallback(ResultCallback resultCallback) {
        this.mResultCallback = resultCallback;
    }
}
