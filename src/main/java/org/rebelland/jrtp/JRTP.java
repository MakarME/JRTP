package org.rebelland.jrtp;

import config.MessageManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;

public final class JRTP extends SimplePlugin {

    @Override
    public void onPluginStart() {
        MessageManager.getInstance().reload();

        Remain.unregisterCommand("rtp:rtp");
    }

    @Override
    public void onPluginStop() {
        // Plugin shutdown logic
    }
}
