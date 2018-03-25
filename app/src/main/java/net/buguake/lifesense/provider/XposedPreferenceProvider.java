package net.buguake.lifesense.provider;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import net.buguake.lifesense.util.SharedConstant;

/**
 * Created by zts1993 on 2018/3/21.
 */

public class XposedPreferenceProvider extends RemotePreferenceProvider {
    public XposedPreferenceProvider() {
        super(SharedConstant.PREF_AUTHORITY, new String[]{SharedConstant.PREF});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        if (write) {
            return false;
        }

        return true;
    }
}