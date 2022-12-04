package cn.scewin.gr.android;

import androidx.annotation.NonNull;

public interface ViewHolderBinder<H,T> {
    void onBindViewHolder(@NonNull H holder, T item, int position);
}
