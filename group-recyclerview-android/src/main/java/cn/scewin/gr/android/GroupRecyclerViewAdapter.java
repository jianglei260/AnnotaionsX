package cn.scewin.gr.android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.scewin.annotionsx.common.utils.ObjectUtil;


public class GroupRecyclerViewAdapter<K, T> extends RecyclerView.Adapter<AutoViewHolder> {
    public static final int VIEW_TYPE_GROUP = 0;
    public static final int VIEW_TYPE_DATA = 1;
    private Map<K, List<T>> dataMap = new HashMap<>();
    private List<T> datas = new ArrayList<>();
    private List<K> keys = new ArrayList<>();
    private List allObjects = new ArrayList();
    private boolean[] checkItems = new boolean[]{};
    private boolean checkMode = false;
    @LayoutRes
    private int groupLayoutRes;
    @LayoutRes
    private int dataLayoutRes;

    private ViewHolderBinder groupViewHolderBinder;
    private ViewHolderBinder dataViewHolderBinder;

    private RecyclerItemClickListener<K> groupRecyclerItemClickListener;
    private RecyclerItemClickListener<T> dataRecyclerItemClickListener;

    private Class<K> groupClass;
    private Class<T> dataClass;

    private Comparator comparator;

    private Class<? extends AutoViewHolder> groupViewHolderClass, dataViewHolderClass;

    private LayoutInflater inflater;
    private Field field;

    public GroupRecyclerViewAdapter(Class<K> groupClass, Class<T> dataClass, Comparator comparator, Class<? extends AutoViewHolder> groupViewHolderClass, Class<? extends AutoViewHolder> dataViewHolderClass) {
        this.groupClass = groupClass;
        this.dataClass = dataClass;
        this.comparator = comparator;
        this.groupViewHolderClass = groupViewHolderClass;
        this.dataViewHolderClass = dataViewHolderClass;
    }

    public List<T> getDatas() {
        return datas;
    }

    public void setDatas(List<T> datas, String keyField) {
        if (datas == null) {
            datas = new ArrayList<>();
        }
        this.datas = datas;
        this.checkItems = new boolean[datas.size()];
        boolean hasGroup = ObjectUtil.isNotEmpty(keyField);
        try {
            field = dataClass.getDeclaredField(keyField);
            field.setAccessible(true);
            hasGroup = true;
        } catch (Exception e) {
            e.printStackTrace();
            hasGroup = false;
        }
        if (datas != null) {
            if (hasGroup) {
                try {
                    dataMap = DataObjectManager.getInstance().mapObjects(datas, keyField, groupClass, dataClass);
                    keys = new ArrayList<>(dataMap.size());
                    allObjects = new ArrayList();
                    for (K k : dataMap.keySet()) {
                        keys.add(k);
                    }
                    if (comparator != null) {
                        Collections.sort(keys, comparator);
                    }
                    for (K key : keys) {
                        allObjects.add(key);
                        allObjects.addAll(dataMap.get(key));
                    }
                    notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                allObjects.addAll(datas);
            }

        }
    }

    @NonNull
    @Override
    public AutoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AutoViewHolder viewHolder = null;
        inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_GROUP:
                viewHolder = AutoViewHolder.of(groupViewHolderClass, parent, inflater);
                break;
            case VIEW_TYPE_DATA:
                viewHolder = AutoViewHolder.of(dataViewHolderClass, parent, inflater);
                break;
        }
        return viewHolder;
    }

    public List<T> getGroupList(K group) {
        return dataMap.get(group);
    }

    public K getGroup(T dataItem) {
        try {
            return (K) field.get(dataItem);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull AutoViewHolder holder, int position) {
        Object o = getObject(position);
        if (o.getClass().equals(groupClass)) {
            if (groupViewHolderBinder != null) {
                groupViewHolderBinder.onBindViewHolder(holder, o, holder.getAdapterPosition());
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (groupRecyclerItemClickListener != null) {
                        groupRecyclerItemClickListener.onClick(v, (K) o, holder.getAdapterPosition());
                    }
                }
            });
        } else {
            if (dataViewHolderBinder != null) {
                dataViewHolderBinder.onBindViewHolder(holder, o, position);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dataRecyclerItemClickListener != null) {
                        dataRecyclerItemClickListener.onClick(v, (T) o, holder.getAdapterPosition());
                    }
                }
            });

        }
    }

    public boolean isCheckMode() {
        return checkMode;
    }

    public void setCheckMode(boolean checkMode) {
        this.checkMode = checkMode;
        checkItems = new boolean[datas.size()];
        notifyDataSetChanged();
    }

    public List<T> getCheckedItems() {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < checkItems.length; i++) {
            if (checkItems[i]) {
                result.add(datas.get(i));
            }
        }
        return result;
    }

    public int getGroupLayoutRes() {
        return groupLayoutRes;
    }

    public void setGroupLayoutRes(int groupLayoutRes) {
        this.groupLayoutRes = groupLayoutRes;
    }

    public int getDataLayoutRes() {
        return dataLayoutRes;
    }

    public void setDataLayoutRes(int dataLayoutRes) {
        this.dataLayoutRes = dataLayoutRes;
    }

    public ViewHolderBinder getGroupViewHolderBinder() {
        return groupViewHolderBinder;
    }

    public void setGroupViewHolderBinder(ViewHolderBinder groupViewHolderBinder) {
        this.groupViewHolderBinder = groupViewHolderBinder;
    }

    public ViewHolderBinder getDataViewHolderBinder() {
        return dataViewHolderBinder;
    }

    public void setDataViewHolderBinder(ViewHolderBinder dataViewHolderBinder) {
        this.dataViewHolderBinder = dataViewHolderBinder;
    }

    public RecyclerItemClickListener<K> getGroupRecyclerItemClickListener() {
        return groupRecyclerItemClickListener;
    }

    public void setGroupRecyclerItemClickListener(RecyclerItemClickListener<K> groupRecyclerItemClickListener) {
        this.groupRecyclerItemClickListener = groupRecyclerItemClickListener;
    }

    public RecyclerItemClickListener<T> getDataRecyclerItemClickListener() {
        return dataRecyclerItemClickListener;
    }

    public void setDataRecyclerItemClickListener(RecyclerItemClickListener<T> dataRecyclerItemClickListener) {
        this.dataRecyclerItemClickListener = dataRecyclerItemClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = getObject(position);
        if (o != null && o.getClass().equals(groupClass)) {
            return VIEW_TYPE_GROUP;
        } else {
            return VIEW_TYPE_DATA;
        }
    }

    public Object getObject(int position) {
        return allObjects.get(position);
    }

    @Override
    public int getItemCount() {
        return allObjects.size();
    }

}
