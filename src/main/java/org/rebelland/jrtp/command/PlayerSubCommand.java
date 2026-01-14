package org.rebelland.jrtp.command;

import config.MessageManager;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.rebelland.jrtp.config.RtpConfig;
import org.rebelland.jrtp.manager.RtpManager;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerSubCommand extends RtpSubCommand {
    public PlayerSubCommand() {
        super("player");
    }

    @Override
    protected void onCommand() {
        checkConsole();
        Player player = (Player) sender;

        // 1. Проверка мира
        if (!RtpConfig.getInstance().getAllowedWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(MessageManager.getInstance().getString(player, "messages.wrong_world"));
            return;
        }

        // 2. Поиск случайной цели в ЭТОМ же мире
        List<Player> potentialTargets = new ArrayList<>(player.getWorld().getPlayers());
        potentialTargets.remove(player); // Убираем себя из списка

        if (potentialTargets.isEmpty()) {
            // Добавь в messages.yml: no_players: "&cНе найдено подходящих игроков для телепортации."
            player.sendMessage(MessageManager.getInstance().getString(player, "messages.no_players"));
            return;
        }

        // Выбираем рандомного
        Player randomTarget = potentialTargets.get(ThreadLocalRandom.current().nextInt(potentialTargets.size()));

        // 3. Проверка кулдауна (ключ rtpp)
        if (!player.hasPermission("rtp.noCooldown")) {
            long timeLeft = CooldownService.getInstance().getTimeLeft(RtpType.PLAYER, player.getUniqueId());
            if (timeLeft > 0) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(timeLeft));
                player.sendMessage(MessageManager.getInstance().getJoinedString(player, "messages.cooldown", placeholders));
                return;
            }
        }

        // 4. Отправляем запрос с найденной целью
        RtpManager.getInstance().addRequest(
                player,
                RtpType.PLAYER,
                RtpConfig.getInstance().getModeSettings(RtpType.PLAYER),
                randomTarget, // <-- Передаем случайного игрока
                "rtpp"
        );
    }
}
