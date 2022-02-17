package com.example.doralivewallpaper;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;


public class DoraWallpaperService extends WallpaperService {
    
    private DoraWallpaperService context;
    
    private int screenWidth;
    private int screenHeight;
    private int slideDuration;
    private int stopDuration;
    
    private DoraEngine doraEngine;
    
    private BroadcastReceiver SCReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MainActivity.ACTION_CHANGE_SETTING)) {
                if(doraEngine != null) {
                    doraEngine.reloadFile();
                    loadImageFolder();
                    //Toast.makeText(context, "배경화면 재설정 됨", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    
    public String imageFolderDirectory;
    
    public static Object[] getImageOnly(File directory, boolean asFile) {
        File[] files =  directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return new File(file, name).isFile() && (name.endsWith("png") || name.endsWith("jpg") || name.endsWith("jpeg"));
            }
        });
        if(files.length == 0) return null;
        Arrays.sort(files);
        if(asFile) return files;
        String[] filesName = new String[files.length];
        for(int i = 0; i < files.length; i ++) {
            filesName[i] = files[i].getName();
        }
        return filesName;
    }
        
    @Override
    public void onCreate() {
        super.onCreate();
        
        context = this;
        
        loadImageFolder();
        
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(metrics);
        } else {
            display.getMetrics(metrics);
        }
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        slideDuration = (int)MainActivity.getValue(MainActivity.SLIDE_PREF, this) * 1000;
        stopDuration = (int)MainActivity.getValue(MainActivity.STOP_PREF, this) * 1000;
        
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_CHANGE_SETTING);
        registerReceiver(SCReceiver, filter);
    }
    
    @Override
    public Engine onCreateEngine() {
        return (doraEngine = new DoraEngine());
    }
    
    public void loadImageFolder() {
        imageFolderDirectory = (String)MainActivity.getValue(MainActivity.FILE_PATH, this);
    }
    
    private class DoraEngine extends Engine {
        
        public static final int THREAD_FAST = 5;
        public static final int THREAD_SLOW = 50;
        
        SurfaceHolder mHolder;
        Handler mHandler;
        private boolean visible = false;
        
        File[] mImages;
        
        BitmapDrawer bitmapDrawer;
        
        public DoraEngine() {
            mHolder = getSurfaceHolder();
            mHandler = new Handler();
        }
        
        Runnable drawThread = new Runnable() {
            @Override
            public void run() {
                if(!checkAvailable()) {
                    reloadFile();
                }
                draw();
                mHandler.removeCallbacks(drawThread);
                if((stopDuration - bitmapDrawer.getTimeInfo()[1]) < slideDuration + THREAD_SLOW) {
                    mHandler.postDelayed(drawThread, THREAD_FAST);
                } else {
                    mHandler.postDelayed(drawThread, THREAD_SLOW);
                }
            }
        };
        
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            loadFile();
        }
        
        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if(visible) {
                mHandler.post(drawThread);
            } else {
                mHandler.removeCallbacks(drawThread);
            }
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(drawThread);
        }
        
/*        @Override
        public void onTouchEvent(MotionEvent event) {
            int pointerCount = event.getPointerCount();
            if(pointerCount > 2) {pointerCount = 2;}
            
            switch(event.getAction() & MotionEvent.ACTION_MASK) {
                default:
                    break;
                case MotionEvent.ACTION_DOWN:
                    touchId[0] = event.getPointerId(0);
                    touchX[0] = event.getX(0);
                    touchY[0] = event.getY(0);
                    mode = MODE_DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    for(int i = 0; i < pointerCount; i++) {
                        touchId[i] = event.getPointerId(i);
                        touchX[i] = event.getX(i);
                        touchY[i] = event.getY(i);
                    }
                    mode = MODE_ZOOM;
                    setDistance();
                    break;
                case MotionEvent.ACTION_MOVE:
                    for(int i = 0; i < pointerCount; i++) {
                        touchId[i] = event.getPointerId(i);
                        touchDX[i] = event.getX(i) - touchX[i];
                        touchDY[i] = event.getY(i) - touchY[i];
                        touchX[i] = event.getX(i);
                        touchY[i] = event.getY(i);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    touchId[0] = event.getPointerId(0);
                    touchDX[0] = event.getX(0) - touchX[0];
                    touchDY[0] = event.getY(0) - touchY[0];
                    touchX[0] = event.getX(0);
                    touchY[0] = event.getY(0);
                    mode = MODE_NONE;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    for(int i = 0; i < pointerCount; i++) {
                        touchId[i] = event.getPointerId(i);
                        touchDX[i] = event.getX(i) - touchX[i];
                        touchDY[i] = event.getY(i) - touchY[i];
                        touchX[i] = event.getX(i);
                        touchY[i] = event.getY(i);
                    }
                    mode = MODE_DRAG;
                    break;
            }
        }*/
        
        private void draw() {
            if(!visible) return;
            Canvas canvas = mHolder.lockCanvas();
            
            Bitmap bitmap = bitmapDrawer.exportBitmap();
            canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
            
            mHolder.unlockCanvasAndPost(canvas);
        }
        
        private void loadFile() {
            if(initializeFile().equals("성공")) {
                slideDuration = (int)MainActivity.getValue(MainActivity.SLIDE_PREF, context) * 1000;
                stopDuration = (int)MainActivity.getValue(MainActivity.STOP_PREF, context) * 1000;
                bitmapDrawer = new BitmapDrawer(context, mImages, stopDuration, slideDuration, screenWidth, screenHeight);
            }
        }
        
        private void reloadFile() {
            if(bitmapDrawer == null) {
                loadFile();
            } else {
                slideDuration = (int)MainActivity.getValue(MainActivity.SLIDE_PREF, context) * 1000;
                stopDuration = (int)MainActivity.getValue(MainActivity.STOP_PREF, context) * 1000;
                bitmapDrawer.changeImageSource(mImages, slideDuration, stopDuration);
            }
        }        
        
        private String initializeFile() {
            if(!MainActivity.checkPermission(context)) {
                mImages = null;
                return "성공";
            }
            try {
                File imageFolder = new File(imageFolderDirectory);
                if(!imageFolder.exists() || !imageFolder.isDirectory()) {return "주소가 디렉터리가 아님";}
                File[] childArray = (File[])getImageOnly(imageFolder, true);
                mImages = childArray;
            } catch(Exception e) {
                StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw)); new AlertDialog.Builder(DoraWallpaperService.this).setMessage(sw.toString()).show();
                return "excp 발생";
            }
            return "성공";
        }
               
        private boolean checkAvailable() {
            if(initializeFile().equals("성공")) {
                if(bitmapDrawer == null) {return false;}
                if(mImages == null && bitmapDrawer.imageFiles == null) {return true;}
                else {return compareStringArray(bitmapDrawer.imageFilesName, (String[])getImageOnly(new File(imageFolderDirectory), false));}
            } else {
                if(bitmapDrawer == null) {return true;}
                else {return false;}
            }
            
        }
        
        private boolean compareStringArray(String[] arr1, String[] arr2) {
            if(arr1 == null && arr2 == null) {return true;}
            if(arr1 == null || arr2 == null) {return false;}
            if(arr1.length == arr2.length) {
                for(int i = 0; i < arr1.length; i++) {
                    if(!arr1[i].equals(arr2[i])) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        
    }
    
}
