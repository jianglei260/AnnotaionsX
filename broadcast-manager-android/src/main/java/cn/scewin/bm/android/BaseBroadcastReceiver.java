package cn.scewin.bm.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;

import java.lang.ref.WeakReference;

public abstract class BaseBroadcastReceiver<T> extends BroadcastReceiver {
    protected WeakReference<T> reference;
    public static Gson gson=new Gson();

    public void setReference(T referent) {
        this.reference = new WeakReference<>(referent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (reference.get() != null) {
            invoke(intent, reference.get());
        }
    }

    public  abstract String getFilter();

    public abstract void invoke(Intent intent, T t);
}
