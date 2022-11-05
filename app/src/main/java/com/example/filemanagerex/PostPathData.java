package com.example.filemanagerex;


import static com.example.filemanagerex.FileList.storageDirectoryPath;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.io.File;
import java.util.List;

public class PostPathData extends AbstractItem<PostPathData, PostPathData.ViewHolder> {

    File file;

    public PostPathData(File file) {
        this.file = file;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public int getType() {
        return R.id.layoutPathParent;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.path_row;
    }

    public class ViewHolder extends FastAdapter.ViewHolder<PostPathData> {
        TextView tvPathHolder;

        public ViewHolder(View itemView) {
            super(itemView);

            tvPathHolder = itemView.findViewById(R.id.tvPathHolder);

        }

        @Override
        public void bindView(PostPathData item, List<Object> payloads) {

            if (item.file.getAbsolutePath().equals(storageDirectoryPath))
                tvPathHolder.setText("Storage");
            else
                tvPathHolder.setText(item.file.getName());

        }

        @Override
        public void unbindView(PostPathData item) {

        }
    }
}
