package com.example.camera.mcamera;

/**
 * Created by zealjiang on 2016/9/18 11:06.
 * Email: zealjiang@126.com
 */
public class PhotoBean {
    private String name;
    private String imgPath;
    private boolean isSelected;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
