package org.rebelland.jrtp.config;

import lombok.Getter;
import org.bukkit.Material;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.settings.YamlConfig;
import org.rebelland.jrtp.manager.RtpManager;
import org.rebelland.jrtp.model.RtpType;

import java.util.*;

@AutoRegister
public final class RtpConfig extends YamlConfig {

    private static final RtpConfig instance = new RtpConfig();

    // КЭШ: Тип RTP -> Настройки
    private final Map<RtpType, ModeSettings> settingsCache = new EnumMap<>(RtpType.class);

    private final Set<Material> unsafeBlocks = new HashSet<>();
    private List<String> allowedWorlds;

    public static RtpConfig getInstance() {
        return instance;
    }

    private RtpConfig() {
        this.loadConfiguration("config.yml");
    }

    @Override
    protected void onLoad() {
        this.allowedWorlds = getStringList("allowed_worlds");

        this.unsafeBlocks.clear();
        List<String> blockList = getStringList("unsafe_blocks");
        if (blockList.isEmpty()) {
            unsafeBlocks.add(Material.WATER);
            unsafeBlocks.add(Material.LAVA);
        } else {
            for (String s : blockList) {
                try {
                    unsafeBlocks.add(Material.valueOf(s));
                } catch (Exception ignored) {
                }
            }
        }

        settingsCache.clear();

        for (RtpType type : RtpType.values()) {
            // DEFAULT -> default, PRIVATE -> private
            String path = "rtp_settings." + type.name().toLowerCase();

            int minRadius = getInteger(path + ".min_radius", 100);
            int maxRadius = getInteger(path + ".max_radius", 2000);
            int cooldown = getInteger(path + ".cooldown", 300);
            int tpTime = getInteger(path + ".tp_time", 5);

            settingsCache.put(type, new ModeSettings(minRadius, maxRadius, cooldown, tpTime));
        }
    }

    // --- Геттеры ---

    public ModeSettings getModeSettings(RtpType type) {
        return settingsCache.get(type);
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public Set<Material> getUnsafeBlocks() {
        return unsafeBlocks;
    }

    @Override
    protected boolean saveComments() {
        return false;
    }

    // Статический класс-хранилище данных
    public static class ModeSettings {
        private final int minRadius;
        private final int maxRadius;
        private final int cooldown;
        private final int tpTime;

        public ModeSettings(int minRadius, int maxRadius, int cooldown, int tpTime) {
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.cooldown = cooldown;
            this.tpTime = tpTime;
        }

        public int getMinRadius() { return minRadius; }
        public int getMaxRadius() { return maxRadius; }
        public int getCooldown() { return cooldown; }
        public int getTpTime() { return tpTime; }
    }
}