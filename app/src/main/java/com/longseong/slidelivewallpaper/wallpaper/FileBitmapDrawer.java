package com.longseong.slidelivewallpaper.wallpaper;

import static com.longseong.slidelivewallpaper.App.mDisplayMetrics;
import static com.longseong.slidelivewallpaper.App.mPreferenceData;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_DIRECTORY_URI;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_FPS_LIMIT;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_IMAGE_DURATION;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_IMAGE_FADE_DURATION;
import static com.longseong.slidelivewallpaper.wallpaper.LiveWallpaperService.screenOrientation;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.longseong.slidelivewallpaper.R;
import com.longseong.slidelivewallpaper.preference.PreferenceData;

import java.io.IOException;
import java.util.LinkedList;

public class FileBitmapDrawer {

    //상수
    private static final int MAX_LOADED_BITMAP = 3;
    private static final int INDEX_NULL = -1;
    private static final float TEXT_SIZE = 20f;

    //context
    private final Context mContext;
    private final LiveWallpaperService.WallpaperEngine mEngine;

    //preference
    protected long pref_imageDuration;
    protected long pref_imageFadeDuration;
    protected Uri pref_directoryUri;
    protected int pref_fpsLimit;
    protected boolean pref_debug;
    protected boolean pref_includeSubDirectory;

    //screen data (화면 정보)
    private float localScreenWidth;
    private float localScreenHeight;

    //canvas
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint;
    private Paint mTextPaint;
    private Paint mForegroundPaint;

    //document (파일 접근)
    private DocumentFile mRootDocument;
    private LinkedList<DocumentFile> mImageFiles;

    //handler
    private Handler bitmapLoaderHandler;
    private Runnable bitmapLoaderRunnable;

    //scene (그려진 화면)
    private Bitmap mDefaultBitmap;
    private LinkedList<Bitmap> mLoadedBitmapQueue;
    private LinkedList<Integer> mLoadedBitmapIndexQueue;

    //image index (이미지 순서)
    private int mFullIndex;
    private int mMainIndex;

    //image progress (이미지 진행 상태)
    private float mDurationPercentage;
    private long mPreviousTime;
    private int mTakenTime;
    private int mFps;
    private int mFpsAvg;
    private final LinkedList<Integer> mFpsList = new LinkedList<>();
    private boolean mImageFilesLoading;
    private boolean mImageFilesSorting;
    private int mSortingImage;

    public FileBitmapDrawer(LiveWallpaperService liveWallpaperService) {
        mContext = liveWallpaperService;
        mEngine = liveWallpaperService.getWallpaperEngine();
        initBitmap();
        initCanvas();
        initPaint();
        initBitmapLoader();
        initPreferenceData();
        initDefaultBitmap();
        initFiles();
    }

    public void configChanged() {
        initBitmap();
        initCanvas();
    }

    private void initBitmapLoader() {
        bitmapLoaderHandler = new Handler();
        bitmapLoaderRunnable = () -> loadBitmapIndex(mMainIndex);
    }

    private void initPreferenceData() {
        pref_imageDuration = 1000L * Integer.parseInt(mPreferenceData.getData(PREF_ID_IMAGE_DURATION));
        pref_imageFadeDuration = 1000L * Integer.parseInt(mPreferenceData.getData(PREF_ID_IMAGE_FADE_DURATION));
        pref_directoryUri = Uri.parse(mPreferenceData.getData(PREF_ID_DIRECTORY_URI));
        pref_fpsLimit = Integer.parseInt(mPreferenceData.getData(PREF_ID_FPS_LIMIT));
        pref_debug = mPreferenceData.isDebug();
        pref_includeSubDirectory = mPreferenceData.isIncludeSubDirectory();
    }

    private void initFiles() {
        if (pref_directoryUri != null && !pref_directoryUri.toString().equals(PreferenceData.default_directoryUri)) {
            mRootDocument = DocumentFile.fromTreeUri(mContext, pref_directoryUri);
        }
        mImageFiles = new LinkedList<>();

        mImageFilesLoading = true;
        new Thread(() -> {
            mSortingImage = INDEX_NULL;
            getChild(mImageFiles, mRootDocument);

            mImageFilesSorting = true;
            mImageFilesLoading = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mImageFiles.sort((o1, o2) -> {
                    mSortingImage = Math.max(mSortingImage, mImageFiles.indexOf(o1));
                    return o1.getName().compareToIgnoreCase(o2.getName());
                });
                mSortingImage = INDEX_NULL;
            }
            mImageFilesSorting = false;
        }).start();

        while (mImageFilesLoading && mImageFiles.size() == 0) {
            //이미지가 한개 이상 로드 되거나 로드가 끝날 때까지 대기
        }

        if (mImageFiles.size() == 0 || mLoadedBitmapQueue == null) {
            mLoadedBitmapQueue = new LinkedList<>();
            mLoadedBitmapIndexQueue = new LinkedList<>();
        }

        mMainIndex = getCurrentIndex(System.currentTimeMillis());
        for (int i = 0; mLoadedBitmapQueue.size() < MAX_LOADED_BITMAP; i++) {
            if (mImageFiles.size() > 0) {
                loadBitmapIndex(i % mImageFiles.size());
            } else {
                loadBitmapIndex(INDEX_NULL);
            }

        }
    }

    private void getChild(LinkedList<DocumentFile> list, DocumentFile parent) {
        if (parent == null || !parent.exists()) {
            if (mRootDocument == null || !mRootDocument.exists()) {
                list.add(null);
            }
            return;
        }
        DocumentFile[] children = parent.listFiles();

        for (DocumentFile child : children) {
            if (child.getType() != null) {
                if (child.getType().startsWith("image")) {
                    list.add(child);
                }
            } else if (pref_includeSubDirectory && child.isDirectory()) {
                getChild(list, child);
            }
        }
    }

    private void initBitmap() {
        if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
            localScreenWidth = mDisplayMetrics.widthPixels;
            localScreenHeight = mDisplayMetrics.heightPixels;
        } else {
            localScreenWidth = mDisplayMetrics.heightPixels;
            localScreenHeight = mDisplayMetrics.widthPixels;
        }
        mBitmap = Bitmap.createBitmap((int) localScreenWidth, (int) localScreenHeight, Bitmap.Config.ARGB_8888);
    }

    private void initCanvas() {
        mCanvas = new Canvas(mBitmap);
    }

    private void initPaint() {
        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mForegroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float scaledPixels = TEXT_SIZE * mDisplayMetrics.scaledDensity;
        mTextPaint.setTextSize(scaledPixels);
        mTextPaint.setColor(0xFFFFFFFF);

        mForegroundPaint.setColor(0x7F000000);
    }

    private void initDefaultBitmap() {
        mDefaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.dorabell);
        Bitmap bitmap = Bitmap.createBitmap(mDefaultBitmap.getWidth(), mDefaultBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mDefaultBitmap, 0, 0, mBitmapPaint);
        mDefaultBitmap = bitmap;
    }

    private void loadBitmapIndex(int index) {
        ContentResolver contentResolver = mContext.getContentResolver();
        Bitmap bitmap;

        Log.i("bitmap loaded", index + "번 비트맵 로드됨");

        try {
            if (index == INDEX_NULL) {
                throw new IOException("document index is null");
            }
            if (mImageFiles.get(index) == null) {
                throw new IOException("document file is null");
            }
            if (!mImageFiles.get(index).exists()) {
                notifyPreferenceChanged(true);
                throw new IOException("document file not found");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(contentResolver, mImageFiles.get(index).getUri()),
                        (decoder, info, source) -> {
                            decoder.setMutableRequired(true);
                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        });

            } else {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mImageFiles.get(index).getUri());
            }
        } catch (IOException e) {
            bitmap = mDefaultBitmap;
        }

        int bitmapWidth;
        int bitmapHeight;

        float horizontalScale;
        float verticalScale;
        float compoundScale;

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();

        horizontalScale = bitmapWidth / localScreenWidth;
        verticalScale = bitmapHeight / localScreenHeight;
        compoundScale = Math.min(horizontalScale, verticalScale);

        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmapWidth / compoundScale), (int) (bitmapHeight / compoundScale), true);

        //선입선출 add -> pop
        if (mLoadedBitmapIndexQueue.size() > 1 && mLoadedBitmapIndexQueue.getLast() == INDEX_NULL) {
            mLoadedBitmapQueue.set(mLoadedBitmapQueue.size() - 1, bitmap);
            mLoadedBitmapIndexQueue.set(mLoadedBitmapQueue.size() - 1, index);
        } else {
            mLoadedBitmapQueue.add(bitmap);
            mLoadedBitmapIndexQueue.add(index);
        }
    }

    private void loadNextBitmap() {
        bitmapLoaderHandler.post(bitmapLoaderRunnable);
    }

    private void drawBitmap(Canvas canvas) {
        drawImage(canvas);

        if (pref_debug) {
            drawDebugInfo(canvas);
        }
    }

    private void drawImage(Canvas canvas) {
        //clear
        canvas.drawColor(Color.BLACK);

        Bitmap main;
        Bitmap sub;

        int bitmapWidth;
        int bitmapHeight;

        float horizontalTranslation;
        float verticalTranslation;

        // load bitmap
        main = mLoadedBitmapQueue.get(0);
        sub = mLoadedBitmapQueue.get(1);

        //scale main image
        bitmapWidth = main.getWidth();
        bitmapHeight = main.getHeight();

        horizontalTranslation = -(bitmapWidth - localScreenWidth) / 2f;
        verticalTranslation = -(bitmapHeight - localScreenHeight) / 2f;

        //draw main image
        mBitmapPaint.setAlpha(0xFF);
        canvas.drawBitmap(main, horizontalTranslation, verticalTranslation, mBitmapPaint);

        mBitmapPaint.setAlpha((int) (0xFF * mDurationPercentage));
        if (mBitmapPaint.getAlpha() > 0) {
            //scale sub image
            bitmapWidth = sub.getWidth();
            bitmapHeight = sub.getHeight();

            horizontalTranslation = -(bitmapWidth - localScreenWidth) / 2f;
            verticalTranslation = -(bitmapHeight - localScreenHeight) / 2f;

            //draw sub image
            canvas.drawBitmap(sub, horizontalTranslation, verticalTranslation, mBitmapPaint);
        }
    }

    private void drawDebugInfo(Canvas canvas) {
        String[] debugDetailsList = {
                "사용중인 엔진 인덱스: ",
                "루트 디렉터리 경로: ",
                "하위 디렉터리 포함: ",
                "이미지 지속시간: ",
                "이미지 페이드 지속시간: ",
                "이미지 로드중: ",
                "이미지 정렬중: ",
                "정렬중인 이미지 인덱스: ",
                "로드 된 이미지 파일 개수: ",
                "로드 된 비트맵 개수: ",
                "FPS: ",
                "메인 비트맵 인덱스: ",
                "서브 비트맵 인덱스: ",
                "서브 비트맵 알파값: "
        };
        String[] debugDetailsValueList = {
                LiveWallpaperService.getWallpaperEngineList().indexOf(mEngine) + "/" + LiveWallpaperService.getWallpaperEngineList().size(),
                pref_directoryUri.getPath() + "",
                pref_includeSubDirectory + "",
                pref_imageDuration + "ms",
                pref_imageFadeDuration + "ms",
                mImageFilesLoading + "",
                mImageFilesSorting + "",
                mSortingImage == INDEX_NULL ? "INDEX_NULL" : mSortingImage + "",
                mImageFiles.size() + "",
                mLoadedBitmapQueue.size() + "",
                +mFps + "/" + pref_fpsLimit + "(" + mFpsAvg + ")",
                (mLoadedBitmapIndexQueue.get(0) == INDEX_NULL ? "INDEX_NULL" : String.valueOf(mLoadedBitmapIndexQueue.get(0))),
                (mLoadedBitmapIndexQueue.get(1) == INDEX_NULL ? "INDEX_NULL" : String.valueOf(mLoadedBitmapIndexQueue.get(1))),
                String.format("%.2f", mDurationPercentage)
        };
        float ascent = mTextPaint.ascent();
        float descent = mTextPaint.descent();
        float width1, width2;
        for (int i = 0; i < debugDetailsList.length; i++) {
            width1 = mTextPaint.measureText(debugDetailsList[i]);
            width2 = mTextPaint.measureText(debugDetailsValueList[i]);
            canvas.drawRect(0, 100 + i * (descent - ascent), width1 + width2, 100 + (i + 1) * (descent - ascent), mForegroundPaint);
            canvas.drawText(debugDetailsList[i], 0, 100 + i * (descent - ascent) - ascent, mTextPaint);
            canvas.drawText(debugDetailsValueList[i], width1, 100 + i * (descent - ascent) - ascent, mTextPaint);
        }
    }

    private int getCurrentIndex(long currentTime) {
        if (mImageFiles.size() == 0) {
            return INDEX_NULL;
        }

        /*if (mImageFilesLoading || mImageFilesSorting) {
            index = (int) ((currentTime / pref_imageDuration) % MAX_LOADED_BITMAP);
            Log.e("index", index + "");
        } else {
            index = (int) ((currentTime / pref_imageDuration) % mImageFiles.size());
        }*/

        return (int) (currentTime / pref_imageDuration);
    }

    private void computeFps(long currentTime) {
        mTakenTime = (int) (currentTime - mPreviousTime);
        if (mPreviousTime > 0) {
            mFps = Math.round(1000 / (mTakenTime));
        }
        mPreviousTime = currentTime;
        mFpsList.add(mFps);
        if (mFpsList.size() > 100) {
            mFpsList.pop();
        }
        for (Integer integer : mFpsList) {
            mFpsAvg += integer;
        }
        mFpsAvg /= mFpsList.size();
    }

    private void computeDurationPercent(long currentTime) {
        // (진행된 시간 + fadeDuration - duration) / fadeDuration
        float value = 1f * ((currentTime % pref_imageDuration) + pref_imageFadeDuration - pref_imageDuration) / pref_imageFadeDuration;
        if (value > 0f) {
            mDurationPercentage = value;
        } else {
            mDurationPercentage = 0;
        }
    }

    private void updateTick() {
        long currentTime;
        if (mImageFilesLoading && mImageFiles.size() < mMainIndex + MAX_LOADED_BITMAP) {
            currentTime = SystemClock.uptimeMillis();
        } else {
            currentTime = mEngine.getStartedTime() + SystemClock.uptimeMillis();
        }

        int currentIndex = getCurrentIndex(currentTime);

        computeFps(currentTime);

        computeDurationPercent(currentTime);

        if (mFullIndex != currentIndex) {

            mMainIndex = currentIndex % mImageFiles.size();
            mFullIndex = currentIndex;
            if (mLoadedBitmapQueue.size() >= MAX_LOADED_BITMAP) {
                mLoadedBitmapQueue.pop();
                mLoadedBitmapIndexQueue.pop();
            }
            loadNextBitmap();
        }
    }

    public Bitmap exportBitmap() {
        updateTick();
        drawBitmap(mCanvas);
        return mBitmap;
    }

    public void directDraw(Canvas canvas) {
        updateTick();
        drawBitmap(canvas);
    }

    public int getFps() {
        return mFps;
    }

    public int getTakenTime() {
        return mTakenTime;
    }

    public void notifyPreferenceChanged(boolean filesChanged) {
        initPreferenceData();
        if (filesChanged) {
            mEngine.initStartTime();
            initFiles();
        }
    }

}
