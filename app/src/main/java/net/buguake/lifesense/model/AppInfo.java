package net.buguake.lifesense.model;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;

/**
 * 作者: Sollyu
 * 时间: 16/10/5
 * 联系: sollyu@qq.com
 * 说明:
 */
public class AppInfo {
    private static final String TAG = "AppEnv";

    public boolean pushEnable = false;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public boolean isEmpty() {
        for (Field field : this.getClass().getFields()) {
            try {
                if (field.get(this) != null && !field.get(this).toString().isEmpty())
                    return false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public com.alibaba.fastjson.JSONObject toJSON() {
        return (com.alibaba.fastjson.JSONObject) JSON.toJSON(this);
    }

    public void merge(AppInfo allAppInfo) {
        for (Field field : this.getClass().getFields()) {
            try {
                Object thisObject = field.get(this);
                Object mergeObject = field.get(allAppInfo);

                if (thisObject == null && mergeObject != null) {
                    field.setAccessible(true);
                    field.set(this, mergeObject);
                    continue;
                }

                if (thisObject instanceof String && thisObject.toString().isEmpty() && mergeObject != null) {
                    field.setAccessible(true);
                    field.set(this, mergeObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
