package com.whj.multiscreenproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Stack;

public class AppManager {
    private static Stack<Activity> activityStack;
    private static AppManager instance;
    private static Intent serviceIntent;

    private WeakReference<Activity> sCurrentActivityWeakRef;

    public Activity getCurrentActivity() {
        Activity currentActivity = null;
        if (sCurrentActivityWeakRef != null) {
            currentActivity = sCurrentActivityWeakRef.get();
        }
        return currentActivity;
    }

    public void setCurrentActivity(Activity activity) {
        sCurrentActivityWeakRef = new WeakReference<Activity>(activity);
    }


    public static Intent getServiceIntent() {
        return serviceIntent;
    }

    public static void setServiceIntent(Intent serviceIntent) {
        AppManager.serviceIntent = serviceIntent;
    }

    private AppManager() {
    }

    /**
     * 单一实例
     */
    public static AppManager getAppManager() {
        if (instance == null) {
            instance = new AppManager();
        }
        return instance;
    }

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<>();
        }
        activityStack.add(activity);
    }


    /**
     * 包括包名的类
     *
     * @param className
     * @return
     */
    public boolean existClass(String className) {
        if (activityStack == null || activityStack.size() == 0 || TextUtils.isEmpty(className))
            return false;

        for (int i = 0; i < activityStack.size(); i++) {
            String name = activityStack.get(i).getClass().getName();
            if (className.equals(name))
                return true;
        }
        return false;
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    public Activity currentActivity() {
        Activity activity = activityStack.lastElement();
        return activity;
    }

    public int size() {
        return (activityStack == null || activityStack.size() == 0) ? 0 : activityStack.size();
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    public void finishActivity() {
        Activity activity = activityStack.lastElement();
        finishActivity(activity);
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            activityStack.remove(activity);
            activity.finish();
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(Class<?> cls) {

        Iterator<Activity> iterator = activityStack.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.getClass().equals(cls)) {
                iterator.remove();
                activity.finish();
            }
        }

    }

    /**
     * 结束所有Activity
     */
    public void finishAllActivity() {
        if (activityStack == null || activityStack.size() == 0)
            return;
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (null != activityStack.get(i)) {
                activityStack.get(i).finish();
            }
        }
        activityStack.clear();
    }

    /**
     * 退出课堂
     */
    public void exitKeTang(Class<?> cls) {
        if (activityStack == null || activityStack.size() == 0)
            return;
        for (int i = activityStack.size()-1; i >=0; i--) {
            if (null != activityStack.get(i)) {
                activityStack.get(i).finish();
                if (activityStack.get(i).getClass().equals(cls)){
                    break;
                }
            }
        }
    }

    /**
     * 退出应用程序
     */
    public void exit(Context context) {
        try {
            finishAllActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}