package com.longseong.slidelivewallpaper.wallpaper;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.longseong.slidelivewallpaper.App;

import java.util.LinkedList;

public class LiveWallpaperService extends WallpaperService {

    private static LinkedList<WallpaperEngine> mWallpaperEngineList = new LinkedList<>();

    public static int screenOrientation;

    private WallpaperEngine mWallpaperEngine;

    public static void notifyPreferenceChanged(boolean reloadFile) {
        if (mWallpaperEngineList.size() > 0) {
            for (WallpaperEngine wallpaperEngine : mWallpaperEngineList) {
                wallpaperEngine.mFileBitmapDrawer.notifyPreferenceChanged(reloadFile);
            }
        }
    }

    public static LinkedList<WallpaperEngine> getWallpaperEngineList() {
        return mWallpaperEngineList;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        screenOrientation = newConfig.orientation;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        screenOrientation = getResources().getConfiguration().orientation;
    }

    @Override
    public Engine onCreateEngine() {
        mWallpaperEngineList.add(mWallpaperEngine = new WallpaperEngine());
        mWallpaperEngine.initStartTime();

        return mWallpaperEngineList.getLast();
    }

    public WallpaperEngine getWallpaperEngine() {
        return mWallpaperEngine;
    }

    public class WallpaperEngine extends WallpaperService.Engine {

        protected int mWidth;
        protected int mHeight;
        protected long mServiceStartedTime;

        private Handler mWallpaperHandler;
        private Runnable mWallpaperRunnable;

        private FileBitmapDrawer mFileBitmapDrawer;

        public long getStartedTime() {
            return mServiceStartedTime;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            //initPaint();
            initFileBitmapDrawer();
            initWallpaperHandler();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mWidth = width;
            mHeight = height;

            //init() 함수는 처음 한번만 작동함
            mFileBitmapDrawer.init();

            mWallpaperEngine.mWallpaperHandler.removeCallbacks(mWallpaperEngine.mWallpaperRunnable);
            mWallpaperEngine.mFileBitmapDrawer.configChanged();
            mWallpaperEngine.mWallpaperHandler.post(mWallpaperEngine.mWallpaperRunnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mWallpaperHandler.post(mWallpaperRunnable);
            } else {
                mWallpaperHandler.removeCallbacks(mWallpaperRunnable);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mWallpaperEngineList.remove(this);
        }

        private void initFileBitmapDrawer() {
            mFileBitmapDrawer = new FileBitmapDrawer(LiveWallpaperService.this);
        }

        private void initWallpaperHandler() {
            mWallpaperHandler = new Handler();
            mWallpaperRunnable = () -> {
                int frameOffset = 1000 / mFileBitmapDrawer.pref_fpsLimit;
                if (mFileBitmapDrawer.getTakenTime() > frameOffset) {
                    mWallpaperHandler.removeCallbacks(mWallpaperRunnable);
                }
                mWallpaperHandler.postDelayed(mWallpaperRunnable, frameOffset);

                drawWallpaper();
            };
        }

        private void drawWallpaper() {
            try {
                Canvas wallpaperCanvas = getSurfaceHolder().lockCanvas();

                //비트맵 드로어에서 비트맵을 받아와서 다시 그림 -> 같은 비트맵을 두번 그리게 됨
                //wallpaperCanvas.drawBitmap(mFileBitmapDrawer.exportBitmap(), 0, 0, mPaint);

                //직접 캔버스 객체를 넘겨주어 그려주기 -> 한번 그림
                mFileBitmapDrawer.directDraw(wallpaperCanvas);

                getSurfaceHolder().unlockCanvasAndPost(wallpaperCanvas);
            } catch (IllegalStateException e) {

            }
        }

        public FileBitmapDrawer getFileBitmapDrawer() {
            return mFileBitmapDrawer;
        }

        public void initStartTime() {
            mServiceStartedTime = System.currentTimeMillis();
        }
    }
}
