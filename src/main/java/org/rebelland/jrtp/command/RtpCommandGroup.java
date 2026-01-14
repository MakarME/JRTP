package org.rebelland.jrtp.command;

import config.MessageManager;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.model.SimpleComponent;
import org.rebelland.jrtp.config.RtpConfig;
import org.rebelland.jrtp.manager.RtpManager;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoRegister
public final class  RtpCommandGroup extends SimpleCommandGroup {

    public RtpCommandGroup() {
        super("rtp");
    }

    @Override
    protected void registerSubcommands() {
        registerSubcommand(RtpSubCommand.class);
    }

    @Override
    protected List<SimpleComponent> getNoParamsHeader() {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        // 2. Мир
        if (!RtpConfig.getInstance().getAllowedWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(MessageManager.getInstance().getString(player, "messages.wrong_world"));
            return Collections.emptyList();
        }

        // 3. Кулдаун
        if (!player.hasPermission("rtp.noCooldown")) {
            long timeLeft = CooldownService.getInstance().getTimeLeft(RtpType.DEFAULT, player.getUniqueId());
            if (timeLeft > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeLeft));
                player.sendMessage(MessageManager.getInstance().getJoinedString(player, "messages.cooldown", placeholders));
                return Collections.emptyList();
            }
        }

        // 4. Добавляем запрос (Тип DEFAULT, ключ rtp)
        RtpManager.getInstance().addRequest(
                player,
                RtpType.DEFAULT,
                RtpConfig.getInstance().getModeSettings(RtpType.DEFAULT),
                null,
                "rtp"
        );

        return Collections.emptyList();
    }

    @Override
    protected boolean sendHelpIfNoArgs() {
        return false;
    }
}
