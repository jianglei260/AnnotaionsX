package cn.scewin.bm.android;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastManageService {
    private static BroadcastManageService instance;
    private Context context;
    private Map<Class, List<? extends BaseBroadcastReceiver>> broadcastReceiverMap;

    protected BroadcastManageService() {
        broadcastReceiverMap = new HashMap<>();
    }

    public static BroadcastManageService getInstance() {
        if (instance == null) {
            instance = new BroadcastManageService();
        }
        return instance;
    }

    public static void sendBroadcast(String action) {
        Intent intent = new Intent(action);
        instance.context.sendBroadcast(intent); // 发送广播
    }

    public void init(Context context) {
        this.context = context;
    }

    public <T> void registeReceiver(Class<? extends BaseBroadcastReceiver<T>> receiverClass) {
        try {
            ParameterizedType parameterizedType = (ParameterizedType) receiverClass.getGenericSuperclass();
            Class referentClass = (Class) parameterizedType.getActualTypeArguments()[0];
            List<BaseBroadcastReceiver> list = null;
            if (broadcastReceiverMap.containsKey(referentClass)) {
                list = (List<BaseBroadcastReceiver>) broadcastReceiverMap.get(referentClass);
            } else {
                list = new ArrayList<>();
                broadcastReceiverMap.put(referentClass, list);
            }
            BaseBroadcastReceiver<T> receiver = receiverClass.newInstance();
            list.add(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> void onCreated(T t) {
        if (broadcastReceiverMap.containsKey(t.getClass())) {
            List<? extends BaseBroadcastReceiver<T>> receivers = (List<? extends BaseBroadcastReceiver<T>>) broadcastReceiverMap.get(t.getClass());
            for (BaseBroadcastReceiver<T> receiver : receivers) {
                receiver.setReference(t);
                IntentFilter filter = new IntentFilter(receiver.getFilter());
                context.registerReceiver(receiver, filter);
            }
        }
    }

    public <T> void onDestroyed(T t) {
        if (broadcastReceiverMap.containsKey(t.getClass())) {
            List<? extends BaseBroadcastReceiver<T>> receivers = (List<? extends BaseBroadcastReceiver<T>>) broadcastReceiverMap.get(t.getClass());
            for (BaseBroadcastReceiver<T> receiver : receivers) {
                try {
                    receiver.setReference(null);
                    context.unregisterReceiver(receiver);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
