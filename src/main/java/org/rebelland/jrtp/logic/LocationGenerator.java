package org.rebelland.jrtp.logic;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.rebelland.jrtp.config.RtpConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class LocationGenerator {

    private static final Random random = ThreadLocalRandom.current();

    // Обычный RTP и FAR
    public static Location generateRandom(World world, int minR, int maxR) {
        Location safe = null;
        int attempts = 50;

        // Получаем центр мира из настроек WorldBorder
        Location center = world.getWorldBorder().getCenter();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        while (safe == null && attempts-- > 0) {
            // Генерируем смещение и прибавляем к центру мира
            int x = centerX + getRandomCoordinate(minR, maxR);
            int z = centerZ + getRandomCoordinate(minR, maxR);

            safe = getSafeY(world, x, z);
        }
        return safe;
    }

    // RTP PLAYER
    public static Location generateNearPlayer(Player target, int minR, int maxR) {
        if (target == null) return null;
        World world = target.getWorld();
        Location base = target.getLocation();

        Location safe = null;
        int attempts = 50;
        while (safe == null && attempts-- > 0) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = minR + (maxR - minR) * random.nextDouble();
            int x = base.getBlockX() + (int)(Math.cos(angle) * dist);
            int z = base.getBlockZ() + (int)(Math.sin(angle) * dist);
            safe = getSafeY(world, x, z);
        }
        return safe;
    }

    // RTP PRIVATE (WorldGuard)
    public static Location generatePrivate(Player player, int minR, int maxR) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
            if (regions == null) return null;

            List<ProtectedRegion> valid = new ArrayList<>();
            LocalPlayer localPlayer = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player);

            for (ProtectedRegion r : regions.getRegions().values()) {
                if (!r.isOwner(localPlayer)) valid.add(r);
            }
            if (valid.isEmpty()) return null;

            ProtectedRegion region = valid.get(random.nextInt(valid.size()));
            // Центр региона
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            int cx = (min.getBlockX() + max.getBlockX()) / 2;
            int cz = (min.getBlockZ() + max.getBlockZ()) / 2;

            // Ищем точку рядом
            int attempts = 50;
            while (attempts-- > 0) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double dist = minR + (maxR - minR) * random.nextDouble();
                int x = cx + (int)(Math.cos(angle) * dist);
                int z = cz + (int)(Math.sin(angle) * dist);
                Location loc = getSafeY(player.getWorld(), x, z);
                if (loc != null) return loc;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private static int getRandomCoordinate(int min, int max) {
        int val = min + random.nextInt(max - min);
        return random.nextBoolean() ? val : -val;
    }

    private static Location getSafeY(World world, int x, int z) {
        Block block = world.getHighestBlockAt(x, z);
        int y = block.getY();

        if (RtpConfig.getInstance().getUnsafeBlocks().contains(block.getType())) return null;

        Block up1 = world.getBlockAt(x, y + 1, z);
        Block up2 = world.getBlockAt(x, y + 2, z);

        if (up1.getType() == Material.AIR && up2.getType() == Material.AIR) {
            if (!world.getWorldBorder().isInside(new Location(world, x, y, z))) return null;
            return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }
}
