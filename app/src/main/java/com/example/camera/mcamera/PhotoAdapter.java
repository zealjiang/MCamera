package com.example.camera.mcamera;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.List;

/**
 * Created by zealjiang on 2016/9/18 10:45.
 * Email: zealjiang@126.com
 */
public class PhotoAdapter extends RecyclerView.Adapter{

    private OnRecyclerViewListener onRecyclerViewListener;
    private static final String TAG = PhotoAdapter.class.getSimpleName();
    private List<PhotoBean> list;

    public interface OnRecyclerViewListener {
        void onItemClick(int position);
    }

    public void setOnRecyclerViewListener(OnRecyclerViewListener onRecyclerViewListener) {
        this.onRecyclerViewListener = onRecyclerViewListener;
    }

    public PhotoAdapter(List<PhotoBean> list) {
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.e(TAG, "onCreateViewHolder, i: " + viewType);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_photo_item, null);

        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Log.e(TAG, "onBindViewHolder, i: " + position + ", viewHolder: " + holder);
        PhotoViewHolder viewHolder = (PhotoViewHolder) holder;
        viewHolder.position = position;
        PhotoBean photoBean = list.get(position);
        viewHolder.tvName.setText(photoBean.getName());
        viewHolder.ivPhoto.setImageURI(Uri.fromFile(new File(photoBean.getImgPath())));

        if(photoBean.isSelected()){
            viewHolder.rootView.setBackgroundResource(R.drawable.border_corner_selected);
        }else{
            viewHolder.rootView.setBackgroundResource(0);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        public View rootView;
        public TextView tvName;
        public SimpleDraweeView ivPhoto;
        public int position;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            ivPhoto = (SimpleDraweeView) itemView.findViewById(R.id.iv_photo);
            rootView = itemView.findViewById(R.id.rl_rootView);
            rootView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (null != onRecyclerViewListener) {
                onRecyclerViewListener.onItemClick(position);
            }
        }
    }

}
