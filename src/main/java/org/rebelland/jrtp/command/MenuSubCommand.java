package org.rebelland.jrtp.command;

import org.bukkit.entity.Player;
import org.rebelland.jrtp.menu.RtpMenu;

public final class MenuSubCommand extends RtpSubCommand {
    public MenuSubCommand() {
        super("menu");
    }

    @Override
    protected void onCommand() {
        checkConsole();
        Player player = (Player) sender;

        new RtpMenu(player.getUniqueId()).displayTo(player);
    }
}
