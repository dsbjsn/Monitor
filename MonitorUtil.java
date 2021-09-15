package com.game.hlwzdt.utils;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.game.hlwzdt.MyApplication;
import com.game.hlwzdt.interfaces.MonitorListener;
import com.hjq.permissions.XXPermissions;

import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * 试玩功能
 * Created on 2021/9/15 10
 *
 * @author xjl
 */
public class MonitorUtil {
    public static final String TAG = "MonitorUtil";
    public static final String Permission = "android.permission.PACKAGE_USAGE_STATS";
    private static MonitorUtil sMonitorUtil = new MonitorUtil();

    public static MonitorUtil getInstance() {
        return sMonitorUtil;
    }

    private MonitorUtil() {
    }

    /**
     * 检查权限
     *
     * @param pContext
     * @return
     */
    private boolean checkPermission(Context pContext) {
        try {
            AppOpsManager appOps = (AppOpsManager) pContext.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), pContext.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            //用此方法返回的结果一直为true
            return XXPermissions.isGranted(pContext, Permission);
        }
    }

    /**
     * 获取权限
     *
     * @param pActivity
     */
    private void getPermission(Activity pActivity) {
        Intent lIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        lIntent.setData(Uri.parse("package:" + MyApplication.getContext().getPackageName()));
        pActivity.startActivity(lIntent);
    }

    private Activity mActivity = null;
    private String appPackageName = null;
    private MonitorListener mListener = null;

    private Long openTime = 0L;

    /**
     * 获取权限后尝试重新打开应用
     */
    public void retryOpenApp() {
        if (mActivity == null || appPackageName == null || mListener == null) {
            if (mListener != null) {
                mListener.error("跳转失败");
            }
            return;
        }

        if (checkPermission(mActivity)) {
            if (AppUtils.checkAppExit(mActivity, appPackageName)) {
                AppUtils.openApp(mActivity, appPackageName);
                openTime = System.currentTimeMillis();
                mListener.opened();
                startMonitor(mActivity);
            } else {
                mListener.error("未找到此应用");
            }
        } else {
            mListener.error("未获得权限");
        }
    }

    /**
     * 打开应用
     *
     * @param pActivity
     * @param packageName      应用包名
     * @param pMonitorListener 监听器
     */
    public void openApp(Activity pActivity, String packageName, MonitorListener pMonitorListener) {
        mActivity = pActivity;
        appPackageName = packageName;
        mListener = pMonitorListener;

        if (checkPermission(pActivity)) {
            if (AppUtils.checkAppExit(pActivity, packageName)) {
                AppUtils.openApp(pActivity, packageName);
                if (pMonitorListener != null) {
                    pMonitorListener.opened();
                }
                startMonitor(pActivity);
            } else {
                pMonitorListener.error("未找到此应用");
            }
        } else {
            getPermission(pActivity);
        }
    }

    private List<UsageStats> mUsageStatsList;
    private String lastApp = "";
    private boolean startTimer = false;

    /**
     * 开始计时
     *
     * @param pContext
     */
    private void startMonitor(Context pContext) {
        lastApp = pContext.getPackageName();
        openTime = 0L;
        startTimer = false;

        UsageStatsManager lUsageStatsManager = (UsageStatsManager) pContext.getSystemService(Context.USAGE_STATS_SERVICE);
        Timer lTimer = new Timer();
        TimerTask lTimerTask = new TimerTask() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                mUsageStatsList = lUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 1000, time);
                if (mUsageStatsList == null || mUsageStatsList.isEmpty()) {
                    //Log.i(TAG, "运行应用列表为空");
                    if (startTimer) {
                        Log.i(TAG, "计时：" + (System.currentTimeMillis() - openTime));
                    }
                } else {
                    SortedMap<Long, UsageStats> lSortedMap = new TreeMap<>();
                    for (UsageStats lUsageStats : mUsageStatsList) {
                        lSortedMap.put(lUsageStats.getLastTimeUsed(), lUsageStats);
                    }

                    lastApp = lSortedMap.get(lSortedMap.lastKey()).getPackageName();
                    if (lastApp.equals(appPackageName)) {
                        if (openTime == 0) {
                            openTime = System.currentTimeMillis();
                            startTimer = true;
                            Log.i(TAG, "开始计时");
                        } else {
                            Log.i(TAG, "计时：" + (System.currentTimeMillis() - openTime));
                        }
                    } else {
                        if (openTime != 0 && startTimer) {
                            startTimer = false;
                            lTimer.cancel();

                            long totalTime = System.currentTimeMillis() - openTime;
                            if (mListener != null) {
                                mListener.finish(totalTime);
                            }
                            Log.i(TAG, "最终时长:" + totalTime);
                        }
                    }
                }
                //Log.i(TAG, "最后运行应用:" + lastApp);
            }
        };
        lTimer.schedule(lTimerTask, 0, 1000);
    }
}
