package org.rebelland.jrtp.menu;

import config.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;
import org.rebelland.ultralib.model.GlobalTimedMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RtpMenu extends GlobalTimedMenu {

    private final UUID uuid;
    private final MessageManager manager = MessageManager.getInstance();

    private final Map<RtpType, String> cooldowns = new ConcurrentHashMap<>();

    public RtpMenu(UUID uuid) {
        this.uuid = uuid;
        setSize(9 * 1);
        setTitle("RTP Menu");

        registerRtpButton(0, RtpType.DEFAULT, "rtpItem", "rtp");
        registerRtpButton(1, RtpType.FAR, "rtpfItem", "rtp far");
        registerRtpButton(2, RtpType.PLAYER, "rtppItem", "rtp player");
        registerRtpButton(3, RtpType.PRIVATE, "rtpbItem", "rtp private");
    }

    private void registerRtpButton(int slot, RtpType type, String keySuffix, String command) {
        Button button = new Button(slot) {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
                player.closeInventory();
                player.performCommand(command);
            }

            @Override
            public ItemStack getItem() {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", cooldowns.getOrDefault(type, ""));

                return ItemCreator.of(CompMaterial.STONE)
                        .name(manager.getString(null, "menu.rtp." + keySuffix + ".name"))
                        .lore(manager.getJoinedString(null, "menu.rtp." + keySuffix + ".lore", placeholders))
                        .make();
            }
        };
        registerButton(button);
    }

    public static String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) return "";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    protected void onAsyncTick() {
        for (RtpType type : RtpType.values()) {
            long left = CooldownService.getInstance().getTimeLeft(type, uuid);
            cooldowns.put(type, formatTime(left));
        }
    }

    @Override
    protected Object getTrackingKey() {
        return uuid;
    }
}
