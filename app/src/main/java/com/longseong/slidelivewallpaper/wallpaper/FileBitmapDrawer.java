package com.longseong.slidelivewallpaper.wallpaper;

import static com.longseong.slidelivewallpaper.App.mDisplayMetrics;
import static com.longseong.slidelivewallpaper.App.mPreferenceData;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_DIRECTORY_URI;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_FPS_LIMIT;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_IMAGE_DURATION;
import static com.longseong.slidelivewallpaper.preference.PreferenceIdBundle.PREF_ID_IMAGE_FADE_DURATION;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;

import com.longseong.slidelivewallpaper.R;
import com.longseong.slidelivewallpaper.preference.PreferenceData;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class FileBitmapDrawer {

    public static final String BITMAP_ERROR_1 = "이미지가 로드되지 않음";
    public static final String BITMAP_ERROR_2 = "비트맵의 인덱스가 NULL임";
    public static final String BITMAP_ERROR_3 = "도큐먼트 파일이 NULL임";
    public static final String BITMAP_ERROR_4 = "도큐먼트를 찾을 수 없음";

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
    private Paint mPaint;
    private Paint mMainBitmapPaint;
    private Paint mSubBitmapPaint;
    private Paint mCoverBitmapPaint;
    private Paint mDebugTextPaint;
    private Paint mDebugBackgroundPaint;

    //document (파일 접근)
    private DocumentFile mRootDocument;
    private LinkedList<DocumentFile> mImageFiles;

    //handler
    private Handler bitmapLoaderHandler;
    private Runnable bitmapLoaderRunnable;

    //scene (그려진 화면)
    private Bitmap mDefaultBitmap;
    private Bitmap mResizedMainBitmap;
    private Bitmap mResizedSubBitmap;
    private final LinkedList<Bitmap> mLoadedBitmapQueue = new LinkedList<>();
    private final LinkedList<Bitmap> mResizedBitmapQueue = new LinkedList<>();
    private final LinkedList<Integer> mLoadedBitmapIndexQueue = new LinkedList<>();

    //image index (이미지 순서)
    private int mFullIndex;
    private int mMainIndex;

    //image progress (이미지 진행 상태)
    private float mDurationPercentage;
    private float mFadeDurationPercentage;
    private long mPreviousTime;
    private int mTakenTime;
    private int mFps;
    private int mFpsAvg;
    private final LinkedList<Integer> mFpsList = new LinkedList<>();
    private boolean mImageFilesLoading;
    private boolean mImageFilesSorting;
    private int mSortingImage;

    //status
    private boolean mInitialized;
    private boolean mFileInitialized;
    private boolean mWallpaperReady;

    //wallpaper cover (배경화면 커버)
    private float mCoverAlpha;
    private Canvas mCoverCanvas;
    private Bitmap mCoverBitmap;
    private final LinkedHashMap<Integer, String> mCoverReasonMap = new LinkedHashMap<>();


    public FileBitmapDrawer(LiveWallpaperService liveWallpaperService) {
        mContext = liveWallpaperService;
        mEngine = liveWallpaperService.getWallpaperEngine();
        initFilesData();
    }

    public void init() {
        if (!mInitialized) {
            initDrawer();
        }
    }

    private void initFilesData() {
        initPreferenceData();
        initFiles();
    }

    private void initDrawer() {
        initBitmap();
        initCanvas();
        initPaint();
        initBitmapLoader();
//        initPreferenceData();
        initDefaultBitmap();
//        initFiles();
        mInitialized = true;

        if (mFileInitialized) {
            resizeLoadedBitmap(false);
            if (mImageFiles.size() > 0) {
                mWallpaperReady = true;
            }
        }
    }

    public void configChanged() {
        initBitmap();
        initCanvas();

        if (mFileInitialized) {
            resizeLoadedBitmap(true);
        }
    }

    private void initBitmapLoader() {
        bitmapLoaderHandler = new Handler();
        bitmapLoaderRunnable = () -> loadBitmapIndex(mMainIndex);
    }

    private void initPreferenceData() {
        pref_imageDuration = Integer.parseInt(mPreferenceData.getData(PREF_ID_IMAGE_DURATION));
        pref_imageFadeDuration = Integer.parseInt(mPreferenceData.getData(PREF_ID_IMAGE_FADE_DURATION));
        pref_directoryUri = Uri.parse(mPreferenceData.getData(PREF_ID_DIRECTORY_URI));
        pref_fpsLimit = Integer.parseInt(mPreferenceData.getData(PREF_ID_FPS_LIMIT));
        pref_debug = mPreferenceData.isDebug();
        pref_includeSubDirectory = mPreferenceData.isIncludeSubDirectory();
    }

    private void initFiles() {
        mWallpaperReady = mFileInitialized = false;
        if (pref_directoryUri != null && !pref_directoryUri.toString().equals(PreferenceData.default_directoryUri)) {
            mRootDocument = DocumentFile.fromTreeUri(mContext, pref_directoryUri);
        }
        mImageFiles = new LinkedList<>();

        mImageFilesLoading = true;
        new Thread(() -> {
            mSortingImage = INDEX_NULL;
            getChild(mImageFiles, mRootDocument);

            mImageFilesLoading = false;
            mImageFilesSorting = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mImageFiles.sort((o1, o2) -> {
                    mSortingImage = Math.max(mSortingImage, mImageFiles.indexOf(o1));
                    return o1.getName().compareToIgnoreCase(o2.getName());
                });
                mSortingImage = INDEX_NULL;
            }
            mImageFilesSorting = false;
        }, "이미지 탐색").start();


        new Thread(() -> {
            while (!mInitialized || mImageFilesLoading && mImageFiles.size() == 0) {
                //드로어가 초기화 될때까지 대기
                //이미지가 한개 이상 로드 되거나 로드가 끝날 때까지 대기
            }

            mLoadedBitmapQueue.clear();
            mResizedBitmapQueue.clear();
            mLoadedBitmapIndexQueue.clear();

            for (int i = 0; mLoadedBitmapQueue.size() < MAX_LOADED_BITMAP; i++) {
                if (mImageFiles.size() > 0) {
                    loadBitmapIndex(i % mImageFiles.size());
                } else {
                    loadBitmapIndex(INDEX_NULL);
                }

            }
            mFileInitialized = true;

            if (mInitialized) {
                resizeLoadedBitmap(false);
                if (mImageFiles.size() > 0) {
                    mWallpaperReady = true;
                }
            }
        }, "이미지 로딩").start();
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
//        localScreenWidth = mDisplayMetrics.widthPixels;
//        localScreenHeight = mDisplayMetrics.heightPixels;
        localScreenWidth = mEngine.mWidth;
        localScreenHeight = mEngine.mHeight;

        mBitmap = Bitmap.createBitmap((int) localScreenWidth, (int) localScreenHeight, Bitmap.Config.ARGB_8888);
        mCoverBitmap = Bitmap.createBitmap((int) localScreenWidth, (int) localScreenHeight, Bitmap.Config.ARGB_8888);
    }

    private void initCanvas() {
        mCanvas = new Canvas(mBitmap);
        mCoverCanvas = new Canvas(mCoverBitmap);
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSubBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCoverBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDebugTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDebugBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint.setStrokeWidth(2f);

        float scaledPixels = TEXT_SIZE * mDisplayMetrics.scaledDensity;
        mPaint.setTextSize(scaledPixels * 3);
        mDebugTextPaint.setTextSize(scaledPixels);
        mDebugTextPaint.setColor(0xFFFFFFFF);

        mDebugBackgroundPaint.setColor(0x7F000000);
    }

    //임시 기본 비트맵
    private void initDefaultBitmap() {
        mDefaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.dorabell);
        Bitmap bitmap = Bitmap.createBitmap(mDefaultBitmap.getWidth(), mDefaultBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mDefaultBitmap, 0, 0, mMainBitmapPaint);
        mDefaultBitmap = bitmap;
        mResizedMainBitmap = mResizedSubBitmap = mDefaultBitmap;
    }

    private void loadBitmapIndex(int index) {
        ContentResolver contentResolver = mContext.getContentResolver();
        Bitmap bitmap;

        try {
            if (index == INDEX_NULL) {
                if (!mImageFilesLoading) {
                    throw new IOException(BITMAP_ERROR_1);
                } else {
                    throw new IOException(BITMAP_ERROR_2);
                }
            }
            if (mImageFiles.get(index) == null) {
                throw new IOException(BITMAP_ERROR_3);
            }
            if (!mImageFiles.get(index).exists()) {
                notifyPreferenceChanged(true);
                throw new IOException(BITMAP_ERROR_4);
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
            String errorExplanation;
            switch (e.getMessage()) {
                case BITMAP_ERROR_1:
                    errorExplanation = "이미지를 찾을 수 없습니다.\n폴더를 확인해주세요" + (!pref_includeSubDirectory ? "\n하위폴더를 포함하는 경우에는\n하위폴더 옵션을 활성화 해주세요" : "");
                    break;
                case BITMAP_ERROR_2:
                    errorExplanation = "이미지를 찾는중입니다.\n잠시만 기다려주세요";
                    break;
                case BITMAP_ERROR_3:
                    errorExplanation = "이미지 폴더 주소가 설정되지 않았습니다.\n이미지 폴더를 설정해주세요";
                    break;
                case BITMAP_ERROR_4:
                    errorExplanation = "도큐먼트를 찾을 수 없습니다.\n파일을 확인해주세요\n본적 없는 오류이니 웅성이에게 알려주세요\n" + pref_directoryUri.getPath() + mImageFiles.get(index).getUri().getPath().replace(mRootDocument.getUri().getPath(), "").replace("/", "/\n");
                    break;
                default:
                    errorExplanation = "이미지를 해석할 수 없습니다.\n지원되지 않는 형식일 수 있습니다.\n" + pref_directoryUri.getPath() + mImageFiles.get(index).getUri().getPath().replace(mRootDocument.getUri().getPath(), "").replace("/", "/\n");
                    break;
            }
            mCoverReasonMap.put(index, errorExplanation);
            bitmap = mDefaultBitmap;
        }

        //메모리 사용 최소화를 위해 비트맵은 최대 개수 이상으로 로드할 수 없게 함
        if (mLoadedBitmapIndexQueue.size() == MAX_LOADED_BITMAP) {
            mLoadedBitmapQueue.set(mLoadedBitmapQueue.size() - 1, bitmap);
            mLoadedBitmapIndexQueue.set(mLoadedBitmapQueue.size() - 1, index);
        } else {
            mLoadedBitmapQueue.add(bitmap);
            mLoadedBitmapIndexQueue.add(index);
        }

        if (mFileInitialized) {
            resizeLoadedBitmap(false);
        }
    }

    private void resizeLoadedBitmap(boolean configChanged) {

        Bitmap bitmap_1 = null;
        Bitmap bitmap_2 = null;
        Bitmap bitmap_3;

        int bitmapWidth;
        int bitmapHeight;
        float horizontalScale;
        float verticalScale;
        float compoundScale;

        if (mResizedBitmapQueue.size() == 0 || mResizedBitmapQueue.size() >= MAX_LOADED_BITMAP || configChanged) {
            bitmap_1 = mLoadedBitmapQueue.get(0);

            bitmapWidth = bitmap_1.getWidth();
            bitmapHeight = bitmap_1.getHeight();

            horizontalScale = bitmapWidth / localScreenWidth;
            verticalScale = bitmapHeight / localScreenHeight;
            compoundScale = Math.min(horizontalScale, verticalScale);

            bitmap_1 = Bitmap.createScaledBitmap(bitmap_1, (int) (bitmapWidth / compoundScale), (int) (bitmapHeight / compoundScale), true);


            bitmap_2 = mLoadedBitmapQueue.get(1);

            bitmapWidth = bitmap_2.getWidth();
            bitmapHeight = bitmap_2.getHeight();

            horizontalScale = bitmapWidth / localScreenWidth;
            verticalScale = bitmapHeight / localScreenHeight;
            compoundScale = Math.min(horizontalScale, verticalScale);

            bitmap_2 = Bitmap.createScaledBitmap(bitmap_2, (int) (bitmapWidth / compoundScale), (int) (bitmapHeight / compoundScale), true);
        }

        bitmap_3 = mLoadedBitmapQueue.get(2);

        bitmapWidth = bitmap_3.getWidth();
        bitmapHeight = bitmap_3.getHeight();

        horizontalScale = bitmapWidth / localScreenWidth;
        verticalScale = bitmapHeight / localScreenHeight;
        compoundScale = Math.min(horizontalScale, verticalScale);

        bitmap_3 = Bitmap.createScaledBitmap(bitmap_3, (int) (bitmapWidth / compoundScale), (int) (bitmapHeight / compoundScale), true);


        synchronized (mResizedBitmapQueue) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                resizeLoadedBitmap_VersionQ_orOlder(bitmap_1, bitmap_2, bitmap_3);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resizeLoadedBitmap_VersionR_orNewer(bitmap_1, bitmap_2, bitmap_3);
            }
        }

    }

    private void resizeLoadedBitmap_VersionQ_orOlder(Bitmap bitmap_1, Bitmap bitmap_2, Bitmap bitmap_3) {
        Matrix matrix = new Matrix();
        if (LiveWallpaperService.screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            matrix.postRotate(90);
        }

        if (bitmap_1 != null) {
            bitmap_1 = BitmapUtil.cropCenterBitmap(bitmap_1, (int) localScreenWidth, (int) localScreenHeight);
            bitmap_2 = BitmapUtil.cropCenterBitmap(bitmap_2, (int) localScreenWidth, (int) localScreenHeight);
            mResizedBitmapQueue.clear();
            mResizedBitmapQueue.add(mResizedMainBitmap = Bitmap.createBitmap(bitmap_1, 0, 0, (int) localScreenWidth, (int) localScreenHeight, matrix, true));
            mResizedBitmapQueue.add(mResizedSubBitmap = Bitmap.createBitmap(bitmap_2, 0, 0, (int) localScreenWidth, (int) localScreenHeight, matrix, true));
        }
        bitmap_3 = BitmapUtil.cropCenterBitmap(bitmap_3, (int) localScreenWidth, (int) localScreenHeight);
        mResizedBitmapQueue.add(Bitmap.createBitmap(bitmap_3, 0, 0, (int) localScreenWidth, (int) localScreenHeight, matrix, true));
    }

    private void resizeLoadedBitmap_VersionR_orNewer(Bitmap bitmap_1, Bitmap bitmap_2, Bitmap bitmap_3) {
        if (bitmap_1 != null) {
            mResizedBitmapQueue.clear();
            mResizedBitmapQueue.add(mResizedMainBitmap = BitmapUtil.cropCenterBitmap(bitmap_1, (int) localScreenWidth, (int) localScreenHeight));
            mResizedBitmapQueue.add(mResizedSubBitmap = BitmapUtil.cropCenterBitmap(bitmap_2, (int) localScreenWidth, (int) localScreenHeight));
        }
        mResizedBitmapQueue.add(BitmapUtil.cropCenterBitmap(bitmap_3, (int) localScreenWidth, (int) localScreenHeight));
    }

    private void loadNextBitmap() {
        //bitmapLoaderHandler.post(bitmapLoaderRunnable);
        new Thread(bitmapLoaderRunnable).start();
    }

    private void drawBitmap(Canvas canvas) {
        if (mCoverBitmapPaint.getAlpha() < 255) {
            drawImage(canvas);
        }

        if (!mWallpaperReady || mCoverBitmapPaint.getAlpha() > 0) {
            drawCover(canvas);
        }

        if (pref_debug) {
            drawDebugInfo(canvas);
        }
    }

    private void drawImage(Canvas canvas) {
        //clear
        canvas.drawColor(Color.BLACK);

        //draw main image
        canvas.drawBitmap(mResizedMainBitmap, 0, 0, mMainBitmapPaint);


        if (mFadeDurationPercentage > 0) {
            mSubBitmapPaint.setAlpha((int) (0xFF * mFadeDurationPercentage));

            //draw sub image
            canvas.drawBitmap(mResizedSubBitmap, 0, 0, mSubBitmapPaint);
        }
    }

    private void drawCover(Canvas canvas) {

        //커버의 알파값 설정
        int alpha = mCoverBitmapPaint.getAlpha();
        if (mWallpaperReady) {
            alpha -= Math.round(1f * 0xFF * mTakenTime / pref_imageFadeDuration);
            if (alpha < 0) alpha = 0;
        } else {
            alpha += Math.round(1f * 0xFF * mTakenTime / pref_imageFadeDuration);
            if (alpha > 255) alpha = 255;
        }

        //커버의 로딩 아이콘 설정
        //아래의 세 변수는 모두 각도임 (degree)
        boolean direction = mFullIndex % 2 == 1;
        float checkPoint = 1f * pref_imageFadeDuration / pref_imageDuration;
        float sPos = 270;
        float fadeProgress;
        float progress;

        if (direction) {
            fadeProgress = mFadeDurationPercentage * checkPoint * 360;
            progress = mDurationPercentage * 360 - fadeProgress;
        } else {
            fadeProgress = (mFadeDurationPercentage - 1) * checkPoint * 360;
            progress = (mDurationPercentage - 1) * 360;
        }

        mCoverCanvas.drawColor(ResourcesCompat.getColor(mContext.getResources(), R.color.colorPrimary, null));

        if (checkPoint != 1) {
            Path path = new Path();
            path.arcTo(localScreenWidth / 2 - 150, localScreenHeight / 2 - 150, localScreenWidth / 2 + 150, localScreenHeight / 2 + 150,
                    sPos, progress, false);
            path.arcTo(localScreenWidth / 2 - 120, localScreenHeight / 2 - 120, localScreenWidth / 2 + 120, localScreenHeight / 2 + 120,
                    sPos + progress, -progress, false);
            path.close();

            mPaint.setColor(Color.BLACK);
            mCoverCanvas.drawPath(path, mPaint);
        }

        if (fadeProgress != 0) {
            Path fadePath = new Path();
            fadePath.arcTo(localScreenWidth / 2 - 150, localScreenHeight / 2 - 150, localScreenWidth / 2 + 150, localScreenHeight / 2 + 150,
                    (direction ? sPos + progress : sPos), fadeProgress, false);
            fadePath.arcTo(localScreenWidth / 2 - 120, localScreenHeight / 2 - 120, localScreenWidth / 2 + 120, localScreenHeight / 2 + 120,
                    (direction ? sPos + progress : sPos) + fadeProgress, -fadeProgress, false);
            fadePath.close();

            mPaint.setColor(Color.WHITE);
            mCoverCanvas.drawPath(fadePath, mPaint);
        }

        String emoticon;
        if (mWallpaperReady)  {
            emoticon = ": )";
        } else if (mImageFilesLoading)  {
            emoticon = "; )";
        } else {
            emoticon = ": (";
        }
        mPaint.setColor(Color.WHITE);
        mCoverCanvas.drawText(emoticon, (localScreenWidth - mPaint.measureText(emoticon)) / 2, localScreenHeight / 4, mPaint);

        if (mLoadedBitmapIndexQueue.size() > 0) {
            float ascent = mDebugTextPaint.ascent();
            float descent = mDebugTextPaint.descent();
            float width;
            int index = mLoadedBitmapIndexQueue.get(0);
            String text = mCoverReasonMap.get(index);
            String[] textList;

            if (mWallpaperReady) {
                text = "준비 됐어요!";
            }

            if (text != null) {
                if (text.contains("\n")) {
                    textList = (index + ": " + text).split("\\n");
                } else {
                    textList = new String[]{index + ": " + text};
                }
                for (int i = 0; i < textList.length; i++) {
                    width = mDebugTextPaint.measureText(textList[i]);
                    mCoverCanvas.drawText(textList[i], (localScreenWidth - width) / 2, localScreenHeight / 2 + 200 + i * (descent - ascent) - ascent, mDebugTextPaint);
                }
            }
        }

        mCoverBitmapPaint.setAlpha(alpha);
        //draw cover image
        canvas.drawBitmap(mCoverBitmap, 0, 0, mCoverBitmapPaint);
    }

    private void drawDebugInfo(Canvas canvas) {
        String[] debugDetailsList = {
                "사용중인 엔진 인덱스: ",
                "루트 디렉터리 경로: ",
                "하위 디렉터리 포함: ",
                "이미지 지속시간: ",
                "이미지 페이드 지속시간: ",
                "전체 인덱스: ",
                "메인 인덱스: ",
                "이미지 파일 로드중: ",
                "이미지 파일 정렬중: ",
                "정렬중인 이미지 인덱스: ",
                "로드 된 이미지 파일 개수: ",
                "로드 된 비트맵 개수: ",
                "리사이즈 된 비트맵 개수: ",
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
                mFullIndex + "",
                mMainIndex + "",
                mImageFilesLoading + "",
                mImageFilesSorting + "",
                mSortingImage == INDEX_NULL ? "정렬 완료" : mSortingImage + "",
                mImageFiles.size() + "",
                mLoadedBitmapQueue.size() + "",
                mResizedBitmapQueue.size() + "",
                +mFps + "/" + pref_fpsLimit + "(" + mFpsAvg + ")",
                ((mLoadedBitmapIndexQueue.size() == 0 || mLoadedBitmapIndexQueue.get(0) == INDEX_NULL) ? "INDEX_NULL" : String.valueOf(mLoadedBitmapIndexQueue.get(0))),
                ((mLoadedBitmapIndexQueue.size() <= 1 || mLoadedBitmapIndexQueue.get(1) == INDEX_NULL) ? "INDEX_NULL" : String.valueOf(mLoadedBitmapIndexQueue.get(1))),
                String.format("%.2f", mFadeDurationPercentage)
        };
        float ascent = mDebugTextPaint.ascent();
        float descent = mDebugTextPaint.descent();
        float width1, width2;
        for (int i = 0; i < debugDetailsList.length; i++) {
            width1 = mDebugTextPaint.measureText(debugDetailsList[i]);
            width2 = mDebugTextPaint.measureText(debugDetailsValueList[i]);
            canvas.drawRect(0, 100 + i * (descent - ascent), width1 + width2, 100 + (i + 1) * (descent - ascent), mDebugBackgroundPaint);
            canvas.drawText(debugDetailsList[i], 0, 100 + i * (descent - ascent) - ascent, mDebugTextPaint);
            canvas.drawText(debugDetailsValueList[i], width1, 100 + i * (descent - ascent) - ascent, mDebugTextPaint);
        }
    }

    private int getCurrentIndex(long currentTime) {
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
        mDurationPercentage = 1f * (currentTime % pref_imageDuration) / pref_imageDuration;

        // (진행된 시간 + fadeDuration - duration) / fadeDuration
        float fadeValue = 1f * ((currentTime % pref_imageDuration) + pref_imageFadeDuration - pref_imageDuration) / pref_imageFadeDuration;
        if (fadeValue > 0f) {
            mFadeDurationPercentage = fadeValue;
        } else {
            mFadeDurationPercentage = 0;
        }
    }

    private void updateTick() {
        long currentTime;
        if (false/*mImageFilesLoading && mImageFiles.size() < mMainIndex + MAX_LOADED_BITMAP*/) {
            currentTime = SystemClock.uptimeMillis();
        } else {
            currentTime = mEngine.getStartedTime() + SystemClock.uptimeMillis();
        }

        int currentIndex = getCurrentIndex(currentTime);/*
        if (mImageFiles.size() == 0) {
            currentIndex = INDEX_NULL;
        }*/

        computeFps(currentTime);

        computeDurationPercent(currentTime);

        if (mFullIndex != currentIndex/* || mMainIndex == INDEX_NULL*/) {
            mFullIndex = currentIndex;

            if (mImageFiles.size() == 0) {
                mMainIndex = INDEX_NULL;
            } else {
                mMainIndex = mFullIndex % mImageFiles.size();
            }

            if (mResizedBitmapQueue.size() >= MAX_LOADED_BITMAP/* && mWallpaperReady*/) {
                mLoadedBitmapQueue.pop();
                mResizedBitmapQueue.pop();
                mLoadedBitmapIndexQueue.pop();

                try {
                    mResizedMainBitmap = mResizedBitmapQueue.get(0);
                    mResizedSubBitmap = mResizedBitmapQueue.get(1);

                    if (mLoadedBitmapQueue.get(0) == mDefaultBitmap || (mLoadedBitmapQueue.get(1) == mDefaultBitmap && mFadeDurationPercentage > 0)) {
                        mWallpaperReady = false;
                    } else {
                        mWallpaperReady = true;
                    }
                } catch (IndexOutOfBoundsException e) {
                    //do nothing
                }
            }
            loadNextBitmap();
        }
    }

    @Deprecated
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
