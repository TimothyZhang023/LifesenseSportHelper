package net.buguake.lifesense.xposed;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.crossbowffs.remotepreferences.RemotePreferences;

import net.buguake.lifesense.BuildConfig;
import net.buguake.lifesense.model.AppInfo;
import net.buguake.lifesense.model.FakeInfo;
import net.buguake.lifesense.util.CommonUtil;
import net.buguake.lifesense.util.SharedConstant;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static net.buguake.lifesense.model.FakeInfo.FakeTypeName;

/**
 * Created by zts1993 on 2018/3/20.
 */

public class LifeSenseBLEPushHook implements IXposedHookLoadPackage {

    private static final String TAG = "LifeSenseBLEPushHook";
    private static String targetPkgName = "gz.lifesense.weidong";

    private Set<String> whiteList = new CopyOnWriteArraySet<>();

//    {
//        whiteList.add("com.xiaomi.xmsf"); //xiaomi push
//    }

    private boolean inWhiteList(String pkgName) {
        return whiteList.contains(pkgName.toLowerCase());
    }

    private synchronized void initSettings(Context context) {
        SharedPreferences prefs = new RemotePreferences(context, SharedConstant.PREF_AUTHORITY, SharedConstant.PREF);
        FakeInfo.FakeTypeName = prefs.getString("fake_type", "WECHAT");
        XposedBridge.log("init fake_type : " + FakeTypeName);

    }

    private synchronized void initWhiteList(Context context) {
        SharedPreferences prefs = new RemotePreferences(context, SharedConstant.PREF_AUTHORITY, SharedConstant.PREF);
        boolean show_system_app = prefs.getBoolean("show_system_app", false);
        XposedBridge.log("show_system_app :" + show_system_app);

        List<ApplicationInfo> installedApplications = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo applicationInfo : installedApplications) {
            if (show_system_app || CommonUtil.isUserApplication(applicationInfo)) {
                String prefVal = prefs.getString(applicationInfo.packageName, null);
                if (prefVal != null) {
                    XposedBridge.log(applicationInfo.packageName + ":" + prefVal);
                    AppInfo appInfo = JSON.toJavaObject(JSON.parseObject(prefVal), AppInfo.class);
                    if (appInfo != null && appInfo.pushEnable) {
                        whiteList.add(applicationInfo.packageName);
                    }
                }
            }
        }

        XposedBridge.log("init whiteList : " + JSON.toJSONString(whiteList));
    }

    private volatile Context applicationContext = null;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {
            if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
                XposedHelpers.findAndHookMethod("net.buguake.lifesense.MainApplication", lpparam.classLoader, "isXposedWork",
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                return true;
                            }
                        });
                return;
            }

            if (!lpparam.packageName.equals(targetPkgName)) {
                return;
            }

            Class<?> ContextClass = findClass("android.content.ContextWrapper", lpparam.classLoader);
            findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    if (applicationContext != null) {
                        return;
                    }

                    if (param.getResult() != null) {
                        applicationContext = (Context) param.getResult();
                        XposedBridge.log("getApplicationContext now");
                        initWhiteList(applicationContext);
                        initSettings(applicationContext);
                    }
                }
            });


            findAndHookMethod("com.lifesense.ble.message.a.c", lpparam.classLoader, "a", Context.class, String.class, new XC_MethodHook() {
                Class<?> messageTypeClass = findClass("com.lifesense.ble.bean.constant.MessageType", lpparam.classLoader);

                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String pkgName = (String) param.args[1];
                    if ((pkgName == null) || (pkgName.length() == 0)) return;
                    if (inWhiteList(pkgName)) {
                        param.setResult(Enum.valueOf((Class<Enum>) messageTypeClass, FakeTypeName));
                    }
                }
            });

            findAndHookMethod("com.lifesense.ble.message.a.c", lpparam.classLoader, "a",
                    Context.class, String.class, Notification.class, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String pkgName = (String) param.args[1];

                            if ((pkgName == null) || (pkgName.length() == 0)) return;
                            if (!inWhiteList(pkgName)) return;

                            Notification notification = (Notification) param.args[2];
                            Bundle localBundle = notification.extras;
                            if (localBundle == null) return;

                            String title = localBundle.getString("android.title");
                            CharSequence content = localBundle.getCharSequence("android.text");

                            if (TextUtils.isEmpty(content)) return;

                            notification.tickerText = title;
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        }
                    });

        } catch (Throwable throwable) {
            Log.e(TAG, "lifesense hook meet exception : " + throwable.getLocalizedMessage(), throwable);
            XposedBridge.log(throwable);

        }
    }
}
