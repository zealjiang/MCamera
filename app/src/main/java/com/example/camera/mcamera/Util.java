package com.example.camera.mcamera;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zealjiang on 2016/10/28 10:29.
 * Email: zealjiang@126.com
 */

public class Util {

    /**
     * 获取当前时间 时间格式是 年-月-日 时:分:秒 毫秒
     * @author zealjiang
     * @time 2016/10/28 11:06
     */
    public static String time(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SS");
        String sTime = simpleDateFormat.format(new Date());
        return sTime;
    }
}
