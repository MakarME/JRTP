package org.rebelland.jrtp.service;

import config.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.rebelland.jrtp.model.RtpType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownService {
    // Map<Ключ_Кулдауна, Map<UUID_Игрока, Время_Истечения_UnixMillis>>
    private final Map<RtpType, Map<UUID, Long>> cooldowns = new HashMap<>();

    private static CooldownService instance;

    public static CooldownService getInstance() {
        if (instance == null)
            instance = new CooldownService();

        return instance;
    }

    /**
     * Установить кулдаун.
     * @param key Ключ (например "rtp", "rtpf")
     * @param uuid UUID игрока
     * @param expireTimeUnix Время истечения в Unix Millis (System.currentTimeMillis() + seconds * 1000)
     */
    public void setCooldown(RtpType key, UUID uuid, long expireTimeUnix) {
        cooldowns.computeIfAbsent(key, k -> new HashMap<>()).put(uuid, expireTimeUnix);
    }

    /**
     * Проверить, есть ли активный кулдаун.
     */
    public boolean isOnCooldown(RtpType key, UUID uuid) {
        if (!cooldowns.containsKey(key)) return false;

        Long expireTime = cooldowns.get(key).get(uuid);
        if (expireTime == null) return false;

        if (System.currentTimeMillis() >= expireTime) {
            // Кулдаун истек — удаляем из памяти
            cooldowns.get(key).remove(uuid);
            return false;
        }
        return true;
    }

    /**
     * Получить оставшееся время в секундах.
     */
    public long getTimeLeft(RtpType key, UUID uuid) {
        if (!isOnCooldown(key, uuid)) return 0;

        long expireTime = cooldowns.get(key).get(uuid);
        long diff = expireTime - System.currentTimeMillis();
        return diff / 1000L;
    }

    /**
     * Получить оставшееся время в секундах.
     */
    public long getTimeUnix(RtpType key, UUID uuid) {
        if (!isOnCooldown(key, uuid)) return 0;

        return cooldowns.get(key).get(uuid);
    }
}
