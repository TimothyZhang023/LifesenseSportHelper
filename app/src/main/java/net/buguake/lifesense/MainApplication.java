package net.buguake.lifesense;

import android.app.Application;

/**
 * Created by zts1993 on 2018/3/20.
 */

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.instance = this;
    }

    private static MainApplication instance = null;

    public synchronized static MainApplication getInstance() {
        return instance;
    }

    public boolean isXposedWork() {
        return false;
    }

}
