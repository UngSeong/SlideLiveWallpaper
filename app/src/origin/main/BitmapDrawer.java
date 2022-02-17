package com.example.doralivewallpaper;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.io.File;


public class BitmapDrawer {
    
    public static final int LOAD_BITMAP = 3;
    
    private final DoraWallpaperService context;
    
    public boolean fileNotFound;
    private boolean permissionGranted;
    
    long duration, slideDuration;
    int timeIndex = 0;
    int count = 0;
    
    Bitmap exporter;
    Canvas bitmapCanvas;
    Paint bitmapPaint;
    
    File[] imageFiles;
    String[] imageFilesName;
    
    Bitmap defaultBitmap;
    Bitmap[] loadedBitmap = new Bitmap[LOAD_BITMAP];
    int[] defaultBitmapBound = new int[2];
    int[][] loadedBitmapBound = new int[loadedBitmap.length][2];
    int[] imageNotFound = new int[loadedBitmap.length];
    
    public BitmapDrawer(DoraWallpaperService context, File[] imageFiles, long duration, long slideDuration, int screenWidth, int screenHeight) {
        this.context = context;
        this.imageFiles = imageFiles;
        this.duration = duration;
        this.slideDuration = slideDuration;
        exporter = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(exporter);
        
        if(checkPermission()) {
            if(imageFiles == null || imageFiles.length < 1) {
                fileNotFound = true;
                imageFilesName = null;
            } else {
                imageFilesName = (String[])DoraWallpaperService.getImageOnly(imageFiles[0].getParentFile(), false);
            }
        }
        
        initPaint();
        setDefaultBitmap();
        initBitmap();
    }
    
    public void initPaint() {
        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setTextSize(50);
        bitmapPaint.setColor(Color.BLACK);
        bitmapPaint.setTextAlign(Paint.Align.CENTER);
    }
    
    public void setDefaultBitmap() {
        defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dorabell);
        float signedScaler = getSignedScaler(defaultBitmap.getWidth(), defaultBitmap.getHeight(), true);
        defaultBitmap = Bitmap.createScaledBitmap(defaultBitmap, (int)(defaultBitmap.getWidth() / Math.abs(signedScaler)), (int)(defaultBitmap.getHeight() / Math.abs(signedScaler)), true);
        defaultBitmapBound[0] = -(defaultBitmap.getWidth() - exporter.getWidth()) / 2;
        defaultBitmapBound[1] = -(defaultBitmap.getHeight() - exporter.getHeight()) / 2;
        
    }
    
    public void initBitmap() {
 
        checkTimeIndex(getTimeInfo());
        checkPermission();
        for(int i = 0; i < loadedBitmap.length; i++) {
            int loaderIndex = (timeIndex + i) % loadedBitmap.length;
            if(!permissionGranted || fileNotFound) {
                loadedBitmap[loaderIndex] = null;
            } else {
                loadedBitmap[loaderIndex] = BitmapFactory.decodeFile(imageFiles[(timeIndex + i + LOAD_BITMAP - 1) % imageFiles.length].getAbsolutePath());
            }
            if(loadedBitmap[loaderIndex] == null) {
                if(!permissionGranted || fileNotFound) {
                    imageNotFound[loaderIndex] = -1;
                } else {
                    imageNotFound[loaderIndex] = ((timeIndex + LOAD_BITMAP - 1) % imageFiles.length) ^ -1;
                }
                continue;
            } else {
                imageNotFound[loaderIndex] = (timeIndex + LOAD_BITMAP - 1) % imageFiles.length;
            }
            
            float signedScaler = getSignedScaler(loadedBitmap[loaderIndex].getWidth(), loadedBitmap[loaderIndex].getHeight(), false);
            loadedBitmap[(timeIndex + i) % loadedBitmap.length] = Bitmap.createScaledBitmap(loadedBitmap[loaderIndex], (int)(loadedBitmap[loaderIndex].getWidth() / Math.abs(signedScaler)), (int)(loadedBitmap[loaderIndex].getHeight() / Math.abs(signedScaler)), true);
            if(signedScaler > 0) {
                //가로로 긴 영상
                loadedBitmapBound[(timeIndex + i) % loadedBitmap.length][0] = -(loadedBitmap[(timeIndex + i) % loadedBitmap.length].getWidth() - exporter.getWidth()) / 2;
            } else {
                //세로로 긴 영상
                loadedBitmapBound[(timeIndex + i) % loadedBitmap.length][1] = -(loadedBitmap[(timeIndex + i) % loadedBitmap.length].getHeight() - exporter.getHeight()) / 2;
            }
        }
        //Toast.makeText(context, "bitmap initialized", Toast.LENGTH_SHORT).show();
    }
    
    public void loadBitmap(){
        new Thread() {
            @Override
            public void run() {
                checkPermission();
                int[] timeInfo = getTimeInfo();
                if(timeIndex != checkTimeIndex(timeInfo)) {
                    int loaderIndex = (timeIndex + LOAD_BITMAP - 1) % loadedBitmap.length;
                    if(!permissionGranted || fileNotFound) {
                        loadedBitmap[loaderIndex] = null;
                    } else {
                        loadedBitmap[loaderIndex] = BitmapFactory.decodeFile(imageFiles[(timeInfo[0] + LOAD_BITMAP - 1) % imageFiles.length].getAbsolutePath());
                    }
                    if(loadedBitmap[loaderIndex] == null) {
                        if(!permissionGranted || fileNotFound) {
                            imageNotFound[loaderIndex] = -1;
                        } else {
                            imageNotFound[loaderIndex] = ((timeIndex + LOAD_BITMAP - 1) % imageFiles.length) ^ -1;
                        }
                        return;
                    } else {
                        imageNotFound[loaderIndex] = (timeIndex + LOAD_BITMAP - 1) % imageFiles.length;
                    }
                    float signedScaler = getSignedScaler(loadedBitmap[loaderIndex].getWidth(), loadedBitmap[loaderIndex].getHeight(), false);
                    loadedBitmap[loaderIndex] = Bitmap.createScaledBitmap(loadedBitmap[loaderIndex], (int)(loadedBitmap[loaderIndex].getWidth() / Math.abs(signedScaler)), (int)(loadedBitmap[loaderIndex].getHeight() / Math.abs(signedScaler)), true);
                    if(signedScaler > 0) {
                        //가로로 긴 영상
                        loadedBitmapBound[loaderIndex][0] = -(loadedBitmap[loaderIndex].getWidth() - exporter.getWidth()) / 2;
                    } else {
                        //세로로 긴 영상
                        loadedBitmapBound[loaderIndex][1] = -(loadedBitmap[loaderIndex].getHeight() - exporter.getHeight()) / 2;
                    }
                }
            }
        }.start();
    }
    
    public void changeImageSource(File[] imageFiles, int slide, int stop) {
        this.duration = stop;
        this.slideDuration = slide;
        this.imageFiles = imageFiles;
        setImageSource(this.imageFiles);
    }
    
    private void setImageSource(File[] imageFiles) {
        if(checkPermission()) {
            if(imageFiles == null || imageFiles.length < 1) {
                fileNotFound = true;
                imageFilesName = null;
            } else {
                fileNotFound = false;
                imageFilesName = (String[])DoraWallpaperService.getImageOnly(imageFiles[0].getParentFile(), false);
            }
        }
    }
    
    public int checkTimeIndex(int[] timeInfo) {
        return (timeIndex = timeInfo[0]);
    }
    
    public Bitmap exportBitmap() {
        int[] timeInfo = getTimeInfo();
        int i = 0;
        this.count++;
        do {
            int loaderIndex = (timeInfo[0] + i) % loadedBitmap.length;
            
            if(i == 1) {
                bitmapPaint.setAlpha((int)(((float)timeInfo[1] + slideDuration - duration) * 0xFF / slideDuration));
            }
            
            if(imageNotFound[loaderIndex] < 0) {
                bitmapCanvas.drawColor(0x33C5FF + (bitmapPaint.getAlpha() << 24));
                bitmapCanvas.drawBitmap(defaultBitmap, defaultBitmapBound[0], defaultBitmapBound[1], bitmapPaint);
                if(!permissionGranted) {
                    bitmapCanvas.drawText("외부 저장소 읽기 권한이 없습니다", exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8), bitmapPaint);
                    bitmapCanvas.drawText("권한을 허용해주시기 바랍니다", exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8) + bitmapPaint.getTextSize(), bitmapPaint);
                } else if(fileNotFound) {
                    bitmapCanvas.drawText("이미지 파일이 없습니다", exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8), bitmapPaint);
                    bitmapCanvas.drawText((String)MainActivity.getValue(MainActivity.FILE_PATH, context), exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8) + bitmapPaint.getTextSize(), bitmapPaint);
                } else {
                    bitmapCanvas.drawText("지원하지 않는 형식의 파일입니다", exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8) + bitmapPaint.getTextSize() * 2 * ((timeInfo[0] + i) % 2), bitmapPaint);
                    bitmapCanvas.drawText(imageFiles[imageNotFound[loaderIndex] ^ -1].getName(), exporter.getWidth() / 2, (float)(exporter.getHeight() * 0.8) + bitmapPaint.getTextSize(), bitmapPaint);
                }
            } else {
                bitmapCanvas.drawBitmap(loadedBitmap[loaderIndex], loadedBitmapBound[loaderIndex][0], loadedBitmapBound[loaderIndex][1], bitmapPaint);
            }
            
            if(i == 1) {
                bitmapPaint.setAlpha(0xFF);
                /*Paint paint = new Paint();
                paint.setTextSize(100);
                bitmapCanvas.drawText(count + "", 500, 800, paint);*/
            }
        } while(timeInfo[1] + slideDuration > duration && i++ == 0);
        loadBitmap();
        return exporter;
    }
    
    public float getSignedScaler(float x, float y, boolean matchInScreen) {
        if(x == 0 || y == 0) return 1;
        float scaleX = x / exporter.getWidth();
        float scaleY = y / exporter.getHeight();
        if(matchInScreen) {
            return (float)Math.max(scaleX, scaleY) * (scaleX > scaleY ? 1 : -1);
        } else {
            return (float)Math.min(scaleX, scaleY) * (scaleX > scaleY ? 1 : -1);
        }
    }
    
    public int[] getTimeInfo() {
        long time = System.currentTimeMillis();
        return new int[] {
            (int)(time / duration),
            (int)(time % duration),
        };
    }
    
    public boolean isFileNotFound() {
        return fileNotFound;
    }
    
    public boolean checkPermission() {
        return permissionGranted = MainActivity.checkPermission(context);
    }
    
    public boolean isPermissionGranted() {
        return permissionGranted;
    }
    
}
