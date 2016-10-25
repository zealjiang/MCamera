package com.example.camera.mcamera;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 要求：拍照比例宽高比为4:3
 * A basic Camera preview class
 * Created by zealjiang on 2016/9/12 15:47.
 * Email: zealjiang@126.com
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback ,Camera.AutoFocusCallback{
    private final String TAG = "mcamera.CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private CameraActivity activity;
    private int preViewWidth;
    private int preViewHeight;


    public CameraPreview(Context context, CameraActivity activity) {
        super(context);
        Log.e(TAG, "CameraPreview()");

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        this.activity = activity;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "surfaceCreated()" + mCamera);
        if(mCamera==null){
            Toast.makeText(activity,"初始化相机预览失败", Toast.LENGTH_SHORT).show();
            return;
        }
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed()" + mCamera);
        // empty. Take care of releasing the Camera preview in your activity.
        activity.releaseCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.e(TAG, "surfaceChanged()");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }


    public void setCamera(Camera camera) {
        mCamera = camera;
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }


    public Camera.Size getResolution() {
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size s = params.getPreviewSize();
        return s;
    }

    public void setPreviewSize(int width,int height){
        preViewWidth = width;
        preViewHeight = height;
    }


    public void focusOnTouch(MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        float x = event.getX();
        float y = event.getY();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                focusAreas.add(new Camera.Area(focusRect, 1000));
                parameters.setFocusAreas(focusAreas);
            }

            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                meteringAreas.add(new Camera.Area(meteringRect, 1000));
                parameters.setMeteringAreas(meteringAreas);
            }
        }

        mCamera.setParameters(parameters);
        mCamera.autoFocus(this);
    }

    /**
     * 前提是x,y对于Preview来说左上角坐标是(0,0)
     * Convert  touch  position  x:y  to  {@link  Camera.Area}  position  -1000:-1000  to  1000:1000.
     */
    protected Rect calculateTapArea(float x, float y, float coefficient) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        int rw = getResolution().width;
        int rh = getResolution().height;
        if(preViewWidth==0||preViewHeight==0){
            preViewWidth = getResolution().width;
            preViewHeight = getResolution().height;
        }
        int centerX = (int) (x / preViewWidth * 2000 - 1000);
        int centerY = (int) (y / preViewHeight * 2000 - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int right = clamp(left + areaSize, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        int bottom = clamp(top + areaSize, -1000, 1000);

        if(left>=right||top>=bottom){
            left = -100;
            right = 100;
            top = -100;
            bottom = 100;
        }

        return new Rect(left, top, right, bottom);
    }

    protected int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }
}
