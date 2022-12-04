package cn.scewin.gr.android;

import android.view.View;

public interface RecyclerItemLongClickListener<T> {
    void onLongClick(View view, T t, int postion);
}
