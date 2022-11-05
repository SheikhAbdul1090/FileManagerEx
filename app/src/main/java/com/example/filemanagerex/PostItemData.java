package com.example.filemanagerex;

import static com.example.filemanagerex.FileList.isShowHiddenFileEnable;
import static com.example.filemanagerex.FileList.isSortByDragAndDropEnable;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.commons.utils.FastAdapterUIUtils;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter_extensions.drag.IDraggable;
import com.mikepenz.materialize.util.UIUtils;

import java.io.File;
import java.util.List;

public class PostItemData extends AbstractItem<PostItemData, PostItemData.ViewHolder> implements IDraggable<PostItemData, IItem> {
    private final onFileSelectedListener listener;
    private final boolean isGridViewEnable;
    protected File file;
    private boolean mIsDraggable = true;

    public PostItemData(File file, onFileSelectedListener listener, boolean isGridViewEnable) {
        this.file = file;
        this.listener = listener;
        this.isGridViewEnable = isGridViewEnable;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public PostItemData withIsDraggable(boolean draggable) {
        this.mIsDraggable = draggable;
        return this;
    }

    @Override
    public int getType() {
        if (isGridViewEnable) {
            return R.id.file_grid_parent_layout;
        } else {
            return R.id.file_row_parent_layout;
        }
    }

    @Override
    public int getLayoutRes() {
        if (isGridViewEnable)
            return R.layout.file_row_gridview;
        else
            return R.layout.file_row_listview;
    }


    @Override
    public boolean isDraggable() {
        return mIsDraggable;
    }

    public class ViewHolder extends FastAdapter.ViewHolder<PostItemData> {
        protected View view;
        AppCompatImageView fileIcon, ivMenu;
        TextView tvFileName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.view = itemView;

            fileIcon = itemView.findViewById(R.id.fileIcon);
            ivMenu = itemView.findViewById(R.id.ivMenu);
            tvFileName = itemView.findViewById(R.id.tvFileName);

        }

        @Override
        public void bindView(PostItemData item, List<Object> payloads) {

            Context ctx = itemView.getContext();

            if (!isSortByDragAndDropEnable)
                //set the background for the item
                UIUtils.setBackground(view, FastAdapterUIUtils.getSelectableBackground(ctx, Color.LTGRAY, true));

            if (isShowHiddenFileEnable) {
                if (item.file.isHidden()) {
                    tvFileName.setTextColor(Color.LTGRAY);
                    fileIcon.setAlpha(0.2f);
                } else {
                    fileIcon.setAlpha(1.0f);
                    tvFileName.setTextColor(ctx.getResources().getColor(R.color.textColorBlackOrWhite));
                }
            }

            tvFileName.setText(item.file.getName());
            if (item.file.isDirectory()) {

                Glide.with(ctx).load(R.drawable.ic_baseline_folder_24).into(fileIcon);

            } else {

                if (item.file.getName().toLowerCase().endsWith(".jpeg") ||
                        item.file.getName().toLowerCase().endsWith(".jpg") ||
                        item.file.getName().toLowerCase().endsWith(".png") ||
                        item.file.getName().toLowerCase().endsWith(".apng") ||
                        item.file.getName().toLowerCase().endsWith(".avif") ||
                        item.file.getName().toLowerCase().endsWith(".jfif") ||
                        item.file.getName().toLowerCase().endsWith(".pjpeg") ||
                        item.file.getName().toLowerCase().endsWith(".pjp") ||
                        item.file.getName().toLowerCase().endsWith(".svg") ||
                        item.file.getName().toLowerCase().endsWith(".webp")) {

                    Glide.with(ctx).load(item.file.getAbsolutePath()).centerCrop().into(fileIcon);

                } else if (item.file.getName().toLowerCase().endsWith(".gifv") ||
                        item.file.getName().toLowerCase().endsWith(".mp4") ||
                        item.file.getName().toLowerCase().endsWith(".m4p") ||
                        item.file.getName().toLowerCase().endsWith(".m4v") ||
                        item.file.getName().toLowerCase().endsWith(".mpg") ||
                        item.file.getName().toLowerCase().endsWith(".mp2") ||
                        item.file.getName().toLowerCase().endsWith(".mpeg") ||
                        item.file.getName().toLowerCase().endsWith(".mpe") ||
                        item.file.getName().toLowerCase().endsWith(".mpv") ||
                        item.file.getName().toLowerCase().endsWith(".m2v") ||
                        item.file.getName().toLowerCase().endsWith(".m4v")) {

                    Glide.with(ctx).load(item.file.getAbsolutePath()).centerCrop().into(fileIcon);

                } else if (item.file.getName().toLowerCase().endsWith(".mp3") ||
                        item.file.getName().toLowerCase().endsWith(".ogg") ||
                        item.file.getName().toLowerCase().endsWith(".wav") ||
                        item.file.getName().toLowerCase().endsWith(".m4a")) {

                    Glide.with(ctx).load(R.drawable.music).into(fileIcon);

                } else if (item.file.getName().toLowerCase().endsWith(".pdf")) {

                    Glide.with(ctx).load(R.drawable.pdf).into(fileIcon);

                } else if (item.file.getName().toLowerCase().endsWith(".doc") ||
                        item.file.getName().toLowerCase().endsWith(".docx") ||
                        item.file.getName().toLowerCase().endsWith(".docs")) {

                    Glide.with(ctx).load(R.drawable.docs).into(fileIcon);

                } else if (item.file.getName().toLowerCase().endsWith(".apk")) {

                    Glide.with(ctx).load(R.drawable.android).into(fileIcon);

                } else {

                    Glide.with(ctx).load(R.drawable.file).into(fileIcon);

                }
            }
            ivMenu.setOnClickListener(v -> listener.onFileMenuClicked(item.file, getAdapterPosition()));
        }

        @Override
        public void unbindView(PostItemData item) {

        }
    }

}