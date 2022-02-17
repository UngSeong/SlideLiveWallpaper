package com.longseong.slidelivewallpaper.wallpaper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.LinkedList;

public class LiveWallpaperService extends WallpaperService {

    private static LinkedList<WallpaperEngine> mWallpaperEngineList = new LinkedList<>();

    private WallpaperEngine mWallpaperEngine;

    public static void notifyPreferenceChanged(boolean filesChanged) {
        if (mWallpaperEngineList.size() > 0) {
            for (WallpaperEngine wallpaperEngine : mWallpaperEngineList) {
                wallpaperEngine.mFileBitmapDrawer.notifyPreferenceChanged(filesChanged);
            }
        }
    }

    public static LinkedList<WallpaperEngine> getWallpaperEngineList() {



        return mWallpaperEngineList;
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

        private long mServiceStartedTime;
        private Paint mPaint;

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

        private void initPaint() {
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
