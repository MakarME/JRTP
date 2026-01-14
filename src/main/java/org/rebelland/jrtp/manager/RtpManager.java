package org.rebelland.jrtp.manager;

import config.MessageManager;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.rebelland.jrtp.JRTP;
import org.rebelland.jrtp.config.RtpConfig;
import org.rebelland.jrtp.logic.LocationGenerator;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RtpManager {
    private static final RtpManager instance = new RtpManager();
    public static RtpManager getInstance() { return instance; }

    // Очередь (FIFO)
    private final Queue<RtpRequest> searchQueue = new LinkedList<>();

    // Флаг: занят ли поиск
    private boolean isSearching = false;

    // Сет для предотвращения повторного ввода команды
    private final Set<UUID> runningTeleports = ConcurrentHashMap.newKeySet();

    public static class RtpRequest {
        final Player player;
        final RtpType type;
        final RtpConfig.ModeSettings settings;
        final Player target; // Для режима player (сюда передается рандомный игрок из команды)

        public RtpRequest(Player player, RtpType type, RtpConfig.ModeSettings settings, Player target) {
            this.player = player;
            this.type = type;
            this.settings = settings;
            this.target = target;
        }
    }

    // Входная точка
    public void addRequest(Player player, RtpType type, RtpConfig.ModeSettings settings, Player target, String cooldownKey) {
        if (runningTeleports.contains(player.getUniqueId())) {
            String msg = MessageManager.getInstance().getString(player, "messages.in_process");
            player.sendMessage(msg);
            return;
        }

        runningTeleports.add(player.getUniqueId());

        RtpRequest req = new RtpRequest(player, type, settings, target);
        searchQueue.add(req);

        // Пишем позицию в очереди
        Map<String, String> plc = new HashMap<>();
        plc.put("queue", String.valueOf(searchQueue.size()));
        String msg = MessageManager.getInstance().getJoinedString(player, "messages.searching", plc);
        player.sendMessage(msg);

        processQueue();
    }

    private void processQueue() {
        if (isSearching || searchQueue.isEmpty()) return;

        RtpRequest req = searchQueue.poll();
        if (req.player == null || !req.player.isOnline()) {
            runningTeleports.remove(req.player.getUniqueId());
            processQueue();
            return;
        }

        isSearching = true; // БЛОКИРУЕМ ПОИСК

        final Location[] resultLocation = new Location[1];

        // 1. Асинхронный поиск
        Common.runAsync(() -> {
            try {
                switch (req.type) {
                    case DEFAULT:
                    case FAR:
                        resultLocation[0] = LocationGenerator.generateRandom(req.player.getWorld(), req.settings.getMinRadius(), req.settings.getMaxRadius());
                        break;
                    case PRIVATE:
                        resultLocation[0] = LocationGenerator.generatePrivate(req.player, req.settings.getMinRadius(), req.settings.getMaxRadius());
                        break;
                    case PLAYER:
                        // Для random player target должен быть передан из команды
                        resultLocation[0] = LocationGenerator.generateNearPlayer(req.target, req.settings.getMinRadius(), req.settings.getMaxRadius());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Common.runLater(() -> {
                isSearching = false;
                processQueue(); // Освобождаем очередь для следующего
                startTeleportLogic(req, resultLocation);
            });
        });
    }

    private void startTeleportLogic(RtpRequest req, Location[] resultContainer) {
        int seconds = req.settings.getTpTime();
        if (req.player.hasPermission("jrtp.instantTp")) {
            seconds = 0;
        }

        Map<String, String> ph = new HashMap<>();
        ph.put("world", req.player.getWorld().getName());

        // --- 1. ЗАПОМИНАЕМ СТАРТОВУЮ ЛОКАЦИЮ ---
        final Location startLocation = req.player.getLocation();
        int finalSeconds = seconds;

        new BukkitRunnable() {
            int timeLeft = finalSeconds;

            @Override
            public void run() {
                if (!req.player.isOnline()) {
                    runningTeleports.remove(req.player.getUniqueId());
                    cancel();
                    return;
                }

                // --- 2. ПРОВЕРКА НА ДВИЖЕНИЕ ---
                // Проверяем, не сменил ли мир и не отошел ли больше чем на 1 блок (distanceSquared > 1)
                // Используем distanceSquared, так как это быстрее (не извлекает корень)
                if (req.player.getWorld() != startLocation.getWorld() || startLocation.distanceSquared(req.player.getLocation()) > 1.0) {
                    // Отправляем сообщение об отмене
                    String msg = MessageManager.getInstance().getString(req.player, "messages.moved");
                    req.player.sendMessage(msg);

                    // Удаляем из списка и отменяем задачу
                    runningTeleports.remove(req.player.getUniqueId());
                    cancel();
                    return;
                }
                // --------------------------------

                ph.put("time", String.valueOf(timeLeft));

                if (timeLeft > 0) {
                    String msg = MessageManager.getInstance().getJoinedString(req.player, "messages.wait", ph);

                    req.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                    timeLeft--;
                } else {
                    if (resultContainer[0] != null) {
                        teleportPlayer(req, resultContainer[0]);
                        cancel();
                    } else {
                        startRetryTask(req, resultContainer);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(SimplePlugin.getInstance(), 0L, 20L);
    }

    // Ретрайер (если поиск не успел за время таймера)
    private void startRetryTask(RtpRequest req, Location[] resultContainer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (resultContainer[0] != null) {
                    teleportPlayer(req, resultContainer[0]);
                    cancel();
                }
            }
        }.runTaskTimer(SimplePlugin.getInstance(), 0L, 5L);

        // Fail таск
        new BukkitRunnable() {
            int retries = 20;
            @Override
            public void run() {
                if (resultContainer[0] != null) {
                    cancel();
                } else if (--retries <= 0) {
                    String msg = MessageManager.getInstance().getString(req.player, "messages.fail");
                    req.player.sendMessage(msg);
                    runningTeleports.remove(req.player.getUniqueId());
                    cancel();
                }
            }
        }.runTaskTimer(SimplePlugin.getInstance(), 0L, 5L);
    }

    private void teleportPlayer(RtpRequest req, Location loc) {
        loc.setYaw(req.player.getLocation().getYaw());
        loc.setPitch(req.player.getLocation().getPitch());
        req.player.teleport(loc);

        Map<String, String> ph = new HashMap<>();
        ph.put("x", String.valueOf(loc.getBlockX()));
        ph.put("y", String.valueOf(loc.getBlockY()));
        ph.put("z", String.valueOf(loc.getBlockZ()));

        String msg = MessageManager.getInstance().getJoinedString(req.player, "messages.success", ph);
        req.player.sendMessage(msg);

        runningTeleports.remove(req.player.getUniqueId());

        // --- КУЛДАУН ЛОГИКА ---
        if (!req.player.hasPermission("jrtp.noCooldown")) {
            // Берем секунды из настроек
            int cdSeconds = req.settings.getCooldown();

            // Вычисляем время истечения: Текущее время + (секунды * 1000)
            long expireTimeUnix = System.currentTimeMillis() + (cdSeconds * 1000L);

            // Устанавливаем в сервис (предполагаем наличие метода setCooldown(key, uuid, expireTime))
            CooldownService.getInstance().setCooldown(
                    req.type,
                    req.player.getUniqueId(),
                    expireTimeUnix
            );
        }
    }
}