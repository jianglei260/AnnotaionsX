package cn.scewin.gr.android;

import android.view.View;

public interface RecyclerItemClickListener<T> {
    void onClick(View view, T t, int postion);
}
