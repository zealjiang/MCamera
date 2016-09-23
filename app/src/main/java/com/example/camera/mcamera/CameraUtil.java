package com.example.camera.mcamera;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by zealjiang on 2016/9/14 11:22.
 * Email: zealjiang@126.com
 */
public class CameraUtil {

    private static CameraUtil cameraUtil = null;

    private CameraUtil(){}

    public static CameraUtil getInstance() {
        if(null==cameraUtil){
            cameraUtil = new CameraUtil();
        }
        return cameraUtil;
    }

    /**
     * 打开闪关灯
     *
     * @param camera
     */
    public void turnLightOn(Camera camera) {
        if (camera == null) {
            return;
        }
        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            return;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }
        String flashMode = parameters.getFlashMode();
        if (!Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                camera.setParameters(parameters);
            }
        }

    }


    /**
     * 自动模式闪光灯
     *
     * @param mCamera
     */
    public void turnLightAuto(Camera mCamera) {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters == null) {
            return;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }
        String flashMode = parameters.getFlashMode();
        if (!Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)) {
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                mCamera.setParameters(parameters);
            } else {
            }
        }
    }

    /**
     * 关闭闪光灯
     *
     * @param mCamera
     */
    public void turnLightOff(Camera mCamera) {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters == null) {
            return;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        String flashMode = parameters.getFlashMode();
        // Check if camera flash exists
        if (flashModes == null) {
            return;
        }
        if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
            // Turn off the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            } else {
            }
        }
    }
}
