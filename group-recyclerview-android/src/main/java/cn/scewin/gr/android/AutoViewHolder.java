package cn.scewin.gr.android;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;

import cn.scewin.gr.AutoHolder;
import cn.scewin.gr.R;


public class AutoViewHolder extends RecyclerView.ViewHolder {
    public CheckBox checkBox;
    public static <T extends AutoViewHolder> T of(Class<T> holderClass, ViewGroup parent, LayoutInflater inflater) {
//        ViewBinding binding = Res.getInstance().getBinding(layoutRes, inflater);
        int layoutRes = holderClass.getAnnotation(AutoHolder.class).value();
        RelativeLayout root = (RelativeLayout) inflater.inflate(R.layout.layout_auto_view_holder, parent, false);
        LinearLayout container = root.findViewById(R.id.container);
        View itemView = inflater.inflate(layoutRes, container, false);
        T holder = null;
        try {
            container.addView(itemView);
            holder = holderClass.getConstructor(View.class).newInstance(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return holder;
    }

    public AutoViewHolder(View itemView) {
        super(itemView);
        try {
            checkBox = itemView.findViewById(R.id.check_box);
            Class helperClass = Class.forName("cn.scewin.gr.AutoHolderHelper");
            Method method = helperClass.getMethod("init", AutoViewHolder.class);
            method.invoke(null, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
