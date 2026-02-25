package org.rebelland.jrtp.menu;

import config.MessageManager;
import model.GlobalTimedMenu;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.annotation.Position;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.rebelland.jrtp.command.FarSubCommand;
import org.rebelland.jrtp.command.PlayerSubCommand;
import org.rebelland.jrtp.command.PrivateSubCommand;
import org.rebelland.jrtp.model.RtpType;
import org.rebelland.jrtp.service.CooldownService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RtpMenu extends GlobalTimedMenu {

    private final UUID uuid;
    private final MessageManager manager = MessageManager.getInstance();

    private final Map<RtpType, String> cooldowns = new ConcurrentHashMap<>();

    private final FarSubCommand farSubCommand = new FarSubCommand();
    private final PrivateSubCommand privateSubCommand = new PrivateSubCommand();
    private final PlayerSubCommand playerSubCommand = new PlayerSubCommand();

    public RtpMenu(UUID uuid) {
        this.uuid = uuid;
        setSize(9 * 3);
        setTitle(manager.getString(null, "menu.rtp.title"));

        for (RtpType type : RtpType.values()) {
            long left = CooldownService.getInstance().getTimeLeft(type, uuid);
            cooldowns.put(type, formatTime(left));
        }

        registerRtpButton(10, RtpType.DEFAULT, "rtpItem", "rtp", CompMaterial.GRASS_BLOCK, "");
        registerRtpButton(12, RtpType.FAR, "rtpfItem", "rtp far", CompMaterial.MAP, "shadow");
        registerRtpButton(14, RtpType.PRIVATE, "rtpbItem", "rtp private", CompMaterial.ANCIENT_DEBRIS, "phoenix");
        registerRtpButton(16, RtpType.PLAYER, "rtppItem", "rtp player", CompMaterial.PLAYER_HEAD, "warden");
    }

    private void registerRtpButton(int slot, RtpType type, String keySuffix, String command, CompMaterial item, String groupName) {
        Button button = new Button(slot) {
            @Override
            public void onClickedInMenu(Player player, Menu menu, ClickType clickType) {
                player.closeInventory();
                player.performCommand(command);
            }

            @Override
            public ItemStack getItem() {
                String ready = cooldowns.getOrDefault(type, "");
                if(Objects.equals(ready, "")){
                    ready = manager.getString(null, "placeholders.ready");
                } else {
                    ready = manager.getString(null, "placeholders.time",
                            "time", cooldowns.getOrDefault(type, ""));
                }
                String perm = "";
                switch (type){
                    case FAR:
                        perm = farSubCommand.getPermission() == null ? "" : farSubCommand.getPermission();
                    break;
                    case PRIVATE:
                        perm = privateSubCommand.getPermission() == null ? "" : privateSubCommand.getPermission();
                    break;
                    case PLAYER:
                        perm = playerSubCommand.getPermission() == null ? "" : playerSubCommand.getPermission();
                    break;
                }
                if(!Objects.equals(perm, ""))
                    if(!Remain.getPlayerByUUID(uuid).hasPermission(perm))
                        ready = manager.getString(null, "placeholders.noPerm");
                return ItemCreator.of(item)
                        .name(manager.getString(null, "menu.rtp." + keySuffix + ".name"))
                        .lore(manager.getJoinedString(null, "menu.rtp." + keySuffix + ".lore",
                                "ready", ready,
                                "group", getGroupPrefix(groupName)))
                        .make();
            }
        };
        registerButton(button);
    }

    public String getGroupPrefix(String groupName) {
        // 1. Получаем провайдер API LuckPerms
        LuckPerms luckPerms = LuckPermsProvider.get();

        // 2. Ищем нужную группу по её названию (например, "admin" или "default")
        Group group = luckPerms.getGroupManager().getGroup(groupName);

        if (group == null) {
            return ""; // Группы не существует
        }

        // 3. Достаем префикс из кэшированной метадаты группы.
        // getStaticQueryOptions() учитывает глобальные параметры (например, если сервер в Bungee-сети)
        String prefix = group.getCachedData()
                .getMetaData(luckPerms.getContextManager().getStaticQueryOptions())
                .getPrefix();

        if (prefix == null) {
            return ""; // У группы не установлен префикс
        }

        // 4. Окрашиваем строку для отображения в игре
        return prefix;
    }

    private final List<Integer> orangePanels = Arrays.asList(
            0, 2, 4, 6, 8, 18, 20, 22, 24, 26
    );

    private final List<Integer> yellowPanels = Arrays.asList(
            1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25
    );

    @Override
    public ItemStack getItemAt(int slot) {
        ItemStack pagedItem = super.getItemAt(slot);
        if (pagedItem != null) return pagedItem;

        if (orangePanels.contains(slot)) {
            return ItemCreator.of(CompMaterial.ORANGE_STAINED_GLASS_PANE)
                    .name(" ")
                    .make();
        }

        if (yellowPanels.contains(slot)) {
            return ItemCreator.of(CompMaterial.YELLOW_STAINED_GLASS_PANE)
                    .name(" ")
                    .make();
        }

        return NO_ITEM;
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
