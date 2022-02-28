package com.longseong.slidelivewallpaper;

import android.app.WallpaperManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.longseong.slidelivewallpaper.R;
import com.longseong.slidelivewallpaper.log.LogActivity;
import com.longseong.slidelivewallpaper.preference.PreferenceListAdapter;
import com.longseong.slidelivewallpaper.preference.PreferenceService;
import com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService;

public class MainActivity extends AppCompatActivity {

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

            int itemId = item.getItemId();
            if (itemId == R.id.menu_debug) {
                item.setChecked(App.mPreferenceData.isDebug());
            } else if (itemId == R.id.menu_include_sub_directory) {
                item.setChecked(App.mPreferenceData.isIncludeSubDirectory());
            } else if (itemId == R.id.menu_refresh_files) {
                if (LiveWallpaperService.getWallpaperEngineList().size() == 0) {
                    menu.removeItem(R.id.menu_refresh_files);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int itemId = item.getItemId();
        if (itemId == R.id.menu_debug) {
            boolean debug = !item.isChecked();
            item.setChecked(debug);
            App.mPreferenceData.setDebug(debug);
        } else if (itemId == R.id.menu_include_sub_directory) {
            boolean includeSubDirectory = !item.isChecked();
            item.setChecked(includeSubDirectory);
            App.mPreferenceData.setIncludeSubDirectory(includeSubDirectory);
        } else if (itemId == R.id.menu_refresh_files) {
            Toast.makeText(this, "이미지 파일을 다시 탐색 합니다.", Toast.LENGTH_SHORT).show();
            LiveWallpaperService.notifyPreferenceChanged(true);
        } else if (itemId == R.id.menu_log) {
            startActivity(new Intent(this, LogActivity.class));
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
