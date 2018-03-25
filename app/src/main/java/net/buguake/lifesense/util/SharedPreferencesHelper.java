package net.buguake.lifesense.util;

import android.content.SharedPreferences;

import com.alibaba.fastjson.JSON;

import net.buguake.lifesense.MainApplication;
import net.buguake.lifesense.model.AppInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * from AppEnv
 * thks to Sollyu
 */

public class SharedPreferencesHelper {
    private static final String TAG = "AppEnv";

    private static final SharedPreferencesHelper instance = new SharedPreferencesHelper();

    private SharedPreferencesHelper() {
    }

    public static SharedPreferencesHelper getInstance() {
        return instance;
    }

    public SharedPreferences getSharedPreferences() {
        return MainApplication.getInstance().getSharedPreferences(SharedConstant.PREF, 0);
    }

    public AppInfo get(String packageName) {
        return JSON.toJavaObject(JSON.parseObject(getSharedPreferences().getString(packageName, null)), AppInfo.class);
    }

    public Map<String, AppInfo> getAll() {
        Map<String, AppInfo> r = new HashMap<>();
        Map<String, ?> all = getSharedPreferences().getAll();
        for (Map.Entry<String, ?> stringEntry : all.entrySet()) {
            try {
                r.put(stringEntry.getKey(), JSON.toJavaObject(JSON.parseObject((String) stringEntry.getValue()), AppInfo.class));
            } catch (Exception ignored) {
            }
        }
        return r;
    }

    public void set(String packageName, AppInfo appInfo) {
        getSharedPreferences().edit().putString(packageName, JSON.toJSONString(appInfo, false)).apply();
    }

    public void remove(String packageName) {
        getSharedPreferences().edit().remove(packageName).apply();
    }

}
