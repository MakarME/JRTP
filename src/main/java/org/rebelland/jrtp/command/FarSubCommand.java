package org.rebelland.jrtp.command;

import config.MessageManager;
import org.bukkit.entity.Player;
import org.rebelland.jrtp.config.RtpConfig;
import org.rebelland.jrtp.manager.RtpManager;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;

import java.util.HashMap;
import java.util.Map;

public final class FarSubCommand extends RtpSubCommand {
    public FarSubCommand() {
        super("far");
    }

    @Override
    protected void onCommand() {
        checkConsole();
        Player player = (Player) sender;

        if (!RtpConfig.getInstance().getAllowedWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(MessageManager.getInstance().getString(player, "messages.wrong_world"));
            return;
        }

        // Кулдаун (rtpf)
        if (!player.hasPermission("rtp.noCooldown")) {
            long timeLeft = CooldownService.getInstance().getTimeLeft(RtpType.FAR, player.getUniqueId());
            if (timeLeft > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeLeft));
                player.sendMessage(MessageManager.getInstance().getJoinedString(player, "messages.cooldown", placeholders));
                return;
            }
        }

        RtpManager.getInstance().addRequest(
                player,
                RtpType.FAR,
                RtpConfig.getInstance().getModeSettings(RtpType.FAR),
                null,
                "rtpf"
        );
    }
}
