package com.example.filemanagerex;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.util.List;

public class HeaderItem extends AbstractItem<HeaderItem, HeaderItem.ViewHolder> {
    String fileOrFolder;

    public HeaderItem(String fileOrFolder) {
        this.fileOrFolder = fileOrFolder;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public int getType() {
        return R.id.tvfileOrFolder;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.header_row;
    }

    public class ViewHolder extends FastAdapter.ViewHolder<HeaderItem> {
        TextView tvfileOrFolder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvfileOrFolder = itemView.findViewById(R.id.tvfileOrFolder);
        }

        @Override
        public void bindView(HeaderItem item, List<Object> payloads) {
            tvfileOrFolder.setText(item.fileOrFolder);
        }

        @Override
        public void unbindView(HeaderItem item) {

        }
    }
}
