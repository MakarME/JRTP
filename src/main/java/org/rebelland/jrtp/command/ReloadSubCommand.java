package org.rebelland.jrtp.command;

import config.MessageManager;
import org.mineacademy.fo.Common;
import org.rebelland.jrtp.config.RtpConfig;

public final class ReloadSubCommand extends RtpSubCommand {
    public ReloadSubCommand() {
        super("reload");
    }

    @Override
    protected void onCommand() {
        RtpConfig.getInstance().reload();

        MessageManager.getInstance().reload();

        tell("&aКонфигурация и сообщения перезагружены!");
    }
}
