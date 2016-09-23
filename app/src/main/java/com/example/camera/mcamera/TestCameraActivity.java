package com.example.camera.mcamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by zealjiang on 2016/9/21 09:35.
 * Email: zealjiang@126.com
 */
public class TestCameraActivity extends Activity {

    private TextView tvPhotoPath;
    private Button btnSkip;
    //图片显示的名字，用来说明图片
    private String[] photoShowNames;
    //图片路径中用的名字，用来保存图片
    private String[] photoPaths;
    private final int PHOTO_REQUEST = 10;
    //6.0拍照权限申请码
    private final static int CAMERA_REQESTCODE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_camera);
        tvPhotoPath = (TextView)findViewById(R.id.tv_photo_path);
        btnSkip = (Button) findViewById(R.id.btn_skip);
        initData();

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userCamera();
            }
        });

    }

    private void initData(){
        photoShowNames = new String[]{"左前45°", "仪表盘", "主副驾座椅", "主驾安全带", "后排座椅", "中控台", "左侧底大边", "后备箱"};
        photoPaths = new String[]{"","","","","","","",""};
    }


    /**
     * 使用公共的Camera
     */
    private void userCamera(){
        Intent intent = new Intent(this,CameraActivity.class);
        intent.putExtra(CameraActivity.PHOTO_SHOW_NAMES,photoShowNames);
        intent.putExtra(CameraActivity.PHOTO_PATHS,photoPaths);
        startActivityForResult(intent,PHOTO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PHOTO_REQUEST && resultCode==RESULT_OK){
            if(null != data){
                photoPaths = data.getStringArrayExtra(CameraActivity.PHOTO_PATHS);
                if(photoPaths!=null){
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < photoPaths.length; i++) {
                        sb.append(photoPaths[i]+"\n");
                    }
                    tvPhotoPath.setText(sb.toString());
                }
            }
        }
    }
}
