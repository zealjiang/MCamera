package com.example.camera.mcamera;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraActivity extends AppCompatActivity implements PhotoAdapter.OnRecyclerViewListener {

    private final int FLAG_CHOOCE_PICTURE = 1001;

    private final String TAG = CameraActivity.class.getName();
    public static final int MEDIA_TYPE_IMAGE = 1;
    private Camera mCamera;
    private CameraPreview mPreview;
    private CameraActivity activity;
    //屏幕宽高
    private int screenWidth;
    private int screenHeight;
    private int statusBarHeight;
    private ImageView ivBack;
    private Button captureButton;
    private ImageView cameraButton;
    private Button btnAlbum;
    private ImageView flashButton;
    private LinearLayout llControl;
    private FrameLayout fLpreview;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private TextView tvPhotoName;
    private LinearLayout llFlash;
    private LinearLayout llCamera;
    private View focusIndex;//手动对焦框
    private int preViewHeight;
    private int preViewWidth;
    //相机预览区和拍照后生成的宽高比
    private final float WIDTHHEIGHTSCALE = 16/9.0f;
    //默认前置或者后置相机 这里暂时设置为后置
    private int mCameraId = 0;
    //记录当前正在拍的是第几张图片
    private int curPhoto = 0;
    //是否在拍完一张照片后自动选中下一张要拍摄的图片
    private boolean isSelectedNextPhoto = true;
    //需要拍摄照片的名字数组
    private String[] photoShowNames;
    //需要拍摄照片的路径数组
    private String[] photoPaths;
    //拍摄照片的数组
    private ArrayList<PhotoBean> photoBeenList;
    public static final String PHOTO_SHOW_NAMES = "photoShowNames";
    public static final String PHOTO_PATHS = "photoPaths";
    //是否拍到最后一张了
    private boolean isTakeLast = false;
    //6.0拍照权限申请码
    private final static int CAMERA_REQESTCODE = 100;
    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private Handler handler = new Handler();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate() "+Util.time());
        Fresco.initialize(this);
        //去title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏ationBar
        //getSupportActionBar().hide();
        //横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera);
        activity = this;

        statusBarHeight = getStatusBarHeight();

        //接收拍照数据
        photoShowNames = getIntent().getStringArrayExtra(PHOTO_SHOW_NAMES);
        photoPaths = getIntent().getStringArrayExtra(PHOTO_PATHS);

        //如果需要拍照的图片名称数组为空或个数为0，就返回
        if(photoShowNames==null || photoPaths==null || photoShowNames.length!=photoPaths.length || photoShowNames.length==0){
            Toast.makeText(this,"接收参数错误", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        //检查设备是否有摄像头
        boolean hasCamera = checkCameraHardware(this);
        if (!hasCamera) {
            Toast.makeText(this, "此设备不支持拍照", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        //返回
        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back();
            }
        });

        focusIndex = (View) findViewById(R.id.focus_index);
        llControl = (LinearLayout) findViewById(R.id.ll_control);
        btnAlbum = (Button) findViewById(R.id.btnAlbum);
        btnAlbum.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (isTakeLast) {
                            Toast.makeText(activity, "已经拍完最后一张照片", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //从相册选择
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, FLAG_CHOOCE_PICTURE);
                    }
                }
        );

        llCamera = (LinearLayout) findViewById(R.id.ll_camera);
        cameraButton = (ImageView) findViewById(R.id.button_camera);
        llCamera.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        switchCamera();
                    }
                }
        );

        llFlash = (LinearLayout) findViewById(R.id.ll_flash);
        flashButton = (ImageView) findViewById(R.id.button_flash);
        llFlash.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Camera.Parameters p = mCamera.getParameters();
                        toggleFlash(p,p.getFlashMode());
                    }
                }
        );

        tvPhotoName = (TextView) findViewById(R.id.tv_photo_name);
        recyclerView = (RecyclerView) findViewById(R.id.rv);

        //初始化照片列表
        initRecyclerView();
        tvPhotoName.setText(photoShowNames[0]);

        // Add a listener to the Capture button
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        takePictureFormCamera();
                    }
                }
        );

        //初始化布局
        initData();

        //设置闪光灯模式
        flashButton.setImageResource(R.mipmap.btn_camera_flash_auto);

    }


    /**
     * 开关闪光灯
     * <p/>
     * 闪一下FLASH_MODE_ON
     * 关闭模式FLASH_MODE_OFF
     * 自动感应是否要用闪光灯FLASH_MODE_AUTO
     */
    public void toggleFlash(Camera.Parameters p, String cameraFlashMode) {
        if (mCamera == null) {
            return;
        }
        if (Camera.Parameters.FLASH_MODE_OFF.equals(cameraFlashMode)) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            flashButton.setImageResource(R.mipmap.btn_camera_flash_on);
            saveFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(cameraFlashMode)) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            flashButton.setImageResource(R.mipmap.btn_camera_flash_auto);
            saveFlashMode(Camera.Parameters.FLASH_MODE_ON);
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(cameraFlashMode)) {
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            flashButton.setImageResource(R.mipmap.btn_camera_flash_off);
            saveFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        } else {
            Toast.makeText(this, "Flash mode setting is not supported.", Toast.LENGTH_SHORT).show();
        }
        mCamera.setParameters(p);

    }


    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,permission))//表明用户已经彻底禁止弹出权限请求
                return false;
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(CameraActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    //处理6.0动态权限问题
    private void requestPermissionCamera() {

        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionsNeeded = new ArrayList<String>();
            final List<String> permissionsList = new ArrayList<String>();
            if (!addPermission(permissionsList, Manifest.permission.CAMERA))
                permissionsNeeded.add("CAMERA");
            if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                permissionsNeeded.add("WRITE_EXTERNAL_STORAGE");

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    // Need Rationale
                    String message = "You need to grant access to " + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++) {
                        message = message + ", " + permissionsNeeded.get(i);
                    }
                    showMessageOKCancel(message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(CameraActivity.this, permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            });
                    return;
                }
                ActivityCompat.requestPermissions(CameraActivity.this, permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                return;
            }
        }

        getCamera();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS){
            Map<String, Integer> perms = new HashMap<String, Integer>();
            // Fill with results
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
            }
            // Check for ACCESS_FINE_LOCATION
            if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // All Permissions Granted
                getCamera();
            } else {
                // Permission Denied
                Toast.makeText(CameraActivity.this, "您拒绝了一些权限，需要允许拍照权限来拍照和写入权限来存储图片", Toast.LENGTH_SHORT)
                        .show();
                this.finish();
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FLAG_CHOOCE_PICTURE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String imgPath = getPath(CameraActivity.this, uri);
            showPhoto(imgPath);
        }
    }

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        //     DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            //     ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                //     TODO     handle     non-primary     volumes
            }
            //     DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            //     MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        //     MediaStore     (and     general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            //     Return     the     remote     address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        //     File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get     the     value     of     the     data     column     for     this     Uri.     This     is     useful     for
     * MediaStore     Uris,     and     other     file-based     ContentProviders.
     *
     * @param context       The     context.
     * @param uri           The     Uri     to     query.
     * @param selection     (Optional)     Filter     used     in     the     query.
     * @param selectionArgs (Optional)     Selection     arguments     used     in     the     query.
     * @return The     value     of     the     _data     column,     which     is     typically     a     file     path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The     Uri     to     check.
     * @return Whether     the     Uri     authority     is     ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The     Uri     to     check.
     * @return Whether     the     Uri     authority     is     DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The     Uri     to     check.
     * @return Whether     the     Uri     authority     is     MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The     Uri     to     check.
     * @return Whether     the     Uri     authority     is     Google     Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


    private void takePictureFormCamera() {
        if (isTakeLast) {
            Toast.makeText(activity, "已经拍完最后一张照片", Toast.LENGTH_SHORT).show();
            return;
        }
        // get an image from the camera
        mCamera.takePicture(null, null, mPicture);
    }


    /**
     * 初始化图片列表
     * @author zealjiang
     * @time 2016/9/21 10:21
     */
    private void initRecyclerView() {
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        photoBeenList = new ArrayList<>();
        for (int i = 0; i < photoShowNames.length; i++) {
            PhotoBean photoBean = new PhotoBean();
            photoBean.setName(photoShowNames[i]);
            photoBean.setImgPath(photoPaths[i]);
            if (i == 0) {
                photoBean.setSelected(true);
            } else {
                photoBean.setSelected(false);
            }
            photoBeenList.add(photoBean);
        }

        photoAdapter = new PhotoAdapter(photoBeenList);
        photoAdapter.setOnRecyclerViewListener(this);
        recyclerView.setAdapter(photoAdapter);
    }

    /**
     * 初始化布局
     * @author zealjiang
     * @time 2016/9/21 17:50
     */
    private void initData() {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        //因为是横屏拍照，宽>高
        int width,height;
        if(screenWidth>screenHeight){
            width = screenWidth;
            height = screenHeight;
        }else{
            width = screenHeight;
            height = screenWidth;
        }

        //当前是横屏显示
        //预览区的宽高比4:3
        //高度 screenWidth - statusBarHeight 求宽度 高/宽 = 3/4  --->  宽=高*（4/3）

        int top  = (int) (50 * dm.density + 0.5f);
        preViewHeight = height - top - statusBarHeight;

        preViewWidth = (int)(preViewHeight * WIDTHHEIGHTSCALE);
        int rightWidth = width - preViewWidth;
        Log.e(TAG,"preViewHeight * 4/3: "+preViewHeight * 4/3);
        Log.e(TAG,"preViewHeight * (4/3): "+preViewHeight * 4/3);

        //这里相机取景框我这是为宽高比3:4 所以限制右部控件的高度是剩余部分
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llControl.getLayoutParams();
        params.width = rightWidth;
        llControl.setLayoutParams(params);
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance(int id) {
        Camera c = null;
        try {
            c = Camera.open(id); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            //拍完照后重新开始预览
            camera.startPreview(); // 拍完照后，重新开始预览

            //显示拍完的照片
            showPhoto(pictureFile.getAbsolutePath());

            //是否选中下一张要拍摄的图片
            if(!isSelectedNextPhoto){
                PhotoBean photoBean = photoBeenList.get(curPhoto);
                photoBean.setSelected(true);
                photoAdapter.notifyItemChanged(curPhoto);
                return;
            }

            //指向下一张图片的位置
            if (curPhoto < photoShowNames.length - 1) {
                curPhoto++;
            } else {
                isTakeLast = true;
            }

            //显示下一张拍摄图片的名称
            tvPhotoName.setText(photoShowNames[curPhoto]);
            PhotoBean photoBean = photoBeenList.get(curPhoto);
            photoBean.setSelected(true);
            photoAdapter.notifyItemChanged(curPhoto);
        }
    };

    private void showPhoto(String path) {
        if (curPhoto < photoShowNames.length - 1) {
            recyclerView.smoothScrollToPosition(curPhoto + 1);
        } else {
            recyclerView.smoothScrollToPosition(curPhoto);
        }

        PhotoBean photoBean = photoBeenList.get(curPhoto);
        photoBean.setImgPath(path);
        photoBean.setSelected(false);
        photoAdapter.notifyItemChanged(curPhoto);

    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * Create a file Uri for saving an image
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;   // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth  需要被进行对比的原宽
     * @param surfaceHeight 需要被进行对比的原高
     * @param preSizeList   需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    protected Camera.Size getCloselyPreSize(int surfaceWidth, int surfaceHeight,
                                            List<Camera.Size> preSizeList) {
        boolean mIsPortrait = false;
        int ReqTmpWidth;
        int ReqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        if (mIsPortrait) {
            ReqTmpWidth = surfaceHeight;
            ReqTmpHeight = surfaceWidth;
        } else {
            ReqTmpWidth = surfaceWidth;
            ReqTmpHeight = surfaceHeight;
        }
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : preSizeList) {
            if ((size.width == ReqTmpWidth) && (size.height == ReqTmpHeight)) {
                return size;
            }
        }

        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio, deltaWidth;
        float deltaRatioMin = Float.MAX_VALUE;
        float deltaWidthMin = 500;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            deltaWidth = Math.abs(size.width - ReqTmpWidth);

            if (deltaRatio < deltaRatioMin && deltaWidth < deltaWidthMin) {
                deltaRatioMin = deltaRatio;
                deltaWidthMin = deltaWidth;
                retSize = size;
            }
        }

        return retSize;
    }

    public void switchCamera() {
        releaseCamera();
        mCameraId = (mCameraId + 1) % mCamera.getNumberOfCameras();
        mCamera = getCameraInstance(mCameraId);

        if (mCamera == null) return;
        setupCamera(mCamera);
        mPreview.setCamera(mCamera);

    }


    /**
     * 设置相机
     * */
    private void setupCamera(Camera camera) {
        if (camera == null) return;
        Camera.Parameters params = camera.getParameters();
        setCameraDisplayOrientation(this, mCameraId, camera);
        List<Camera.Size> listPreSize = params.getSupportedPreviewSizes();

        Camera.Size size = getCloselyPreSize(preViewWidth, preViewHeight, listPreSize);
        params.setPreviewSize(size.width, size.height);

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            // Autofocus mode is supported
            // set the focus mode
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        //修改闪光灯选项
        toggleFlash(params,getFlashMode());


        camera.setParameters(params);
    }

    @Override
    public void onItemClick(int position) {
        //注意：点击选择已拍摄完成的缩略图，可直接拍摄替换刚才拍好的照片，此类操作，拍完后不自动跳到下一张
        isSelectedNextPhoto = false;
        //设置当前为选中
        if (curPhoto == position) {
            return;
        } else {
            PhotoBean photoBean = photoBeenList.get(curPhoto);
            photoBean.setSelected(false);
            photoAdapter.notifyItemChanged(curPhoto);

            photoBean = photoBeenList.get(position);
            photoBean.setSelected(true);
            photoAdapter.notifyItemChanged(position);

            curPhoto = position;
        }

    }

    /**
     * 获取相机
     * @author zealjiang
     * @time 2016/9/21 15:38
     */
    private void getCamera(){
        if (mCamera == null) {
            //异步线程获取相机
            //new OpenCameraTask().execute();
            //主线程获取相机
            onMainThreadGetCamera();
        }
    }

    /**
     * 启动预览
     * @author zealjiang
     * @time 2016/9/21 15:39
     */
    private void startPreview(){
        setupCamera(mCamera);

        //这段代码放到这是因为
        if (mPreview == null) {
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(this, this);
            fLpreview = (FrameLayout) findViewById(R.id.camera_preview);
            fLpreview.addView(mPreview);
            mPreview.setPreviewSize(preViewWidth,preViewHeight);
            mPreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            mPreview.focusOnTouch(event);
                            RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
                            layout.setMargins((int) event.getX() - 60, (int) event.getY() - 60, 0, 0);
                            focusIndex.setLayoutParams(layout);
                            focusIndex.setVisibility(View.VISIBLE);
                            ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
                            sa.setDuration(800);
                            focusIndex.startAnimation(sa);
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    focusIndex.setVisibility(View.INVISIBLE);
                                }
                            },800);
                            break;
                    }
                    return false;
                }
            });

        }
        // Create our Preview view and set it as the content of our activity.
        mPreview.setCamera(mCamera);
        Log.e(TAG,"mPreview.setCamera mCamera: "+mCamera);
    }


    /**
     * 保存闪光灯选项
     * @param flashMode
     */
    public void saveFlashMode(String flashMode){
        SharedPreferences preferences=getSharedPreferences("Camera_FLASH_MODE", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=preferences.edit();
        editor.putString("FLASH_MODE", flashMode);
        editor.commit();
    }

    /**
     * 闪光灯选项
     * @return
     */
    public String getFlashMode(){
        SharedPreferences preferences=getSharedPreferences("Camera_FLASH_MODE", Context.MODE_PRIVATE);
        return  preferences.getString("FLASH_MODE", Camera.Parameters.FLASH_MODE_AUTO);
    }

    /**
     * Asynchronous task for preparing the Camera open, since it's a long blocking
     * operation.
     */
    class OpenCameraTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Create an instance of Camera
            mCamera = getCameraInstance(mCameraId);
            Log.e(TAG,"onResume mCamera: "+mCamera);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mCamera == null) {
                finish();
                return;
            }else{
                startPreview();
            }
        }
    }

    /**
     * 在主线程获取相机，可以造成主线程阻塞
     * @author zealjiang
     * @time 2016/9/22 9:23
     */
    private void onMainThreadGetCamera(){
        mCamera = getCameraInstance(mCameraId);
        if (mCamera == null) {
            finish();
            return;
        }else{
            startPreview();
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 关闭此拍照Activity
     * @author zealjiang
     * @time 2016/9/21 11:06
     */
    private void back(){
        String[] photoPaths = new String[photoBeenList.size()];
        Intent intent = new Intent();
        for (int i = 0; i < photoBeenList.size(); i++) {
            photoPaths[i] = photoBeenList.get(i).getImgPath();
        }
        intent.putExtra(CameraActivity.PHOTO_PATHS, photoPaths);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        back();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume()");
        requestPermissionCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause()");
        releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy()");
    }


}
