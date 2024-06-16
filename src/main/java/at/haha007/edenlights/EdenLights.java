package at.haha007.edenlights;

import net.coreprotect.CoreProtect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class EdenLights extends JavaPlugin implements Listener {
    public static final String USE_PERMISSION = "lights.use";
    public static final String ADJUST_PERMISSION = "lights.adjust";
    private int tick = -16;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::displayLights, 1, 1);
    }

    private void displayLights() {
        //every 80 ticks at 20tps
        if (Bukkit.getTPS()[2] < 15)
            return;

        tick += 1;
        if (tick > 16)
            tick = -16;

        for (Player player : Bukkit.getOnlinePlayers()) {
            displayLights(player);
        }
    }

    private void displayLights(Player player) {
        Material item = player.getInventory().getItemInMainHand().getType();
        if (item != Material.LIGHT)
            return;
        if (!player.hasPermission(USE_PERMISSION))
            return;

        Block center = player.getLocation().getBlock();
        for (int x = -16; x <= 16; x++) {
            for (int y = -16; y <= 16; y++) {
                for (int z = -16; z <= 16; z++) {
                    displayIfLight(player, x, y, z, center);
                }
            }
        }
    }

    private void displayIfLight(Player player, int x, int y, int z, Block center) {
        if ((x ^ y ^ z) != tick)
            return;
        Block block = center.getRelative(x, y, z);
        if (block.getType() != Material.LIGHT)
            return;
        BlockData bd = block.getBlockData();
        Location loc = block.getLocation().toCenterLocation();
        Particle.BLOCK_MARKER
                .builder()
                .data(bd)
                .location(loc)
                .receivers(player)
                .spawn();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void breakLight(PlayerInteractEvent event) {
        if (!event.getPlayer().hasPermission(ADJUST_PERMISSION))
            return;
        if (event.getPlayer().getGameMode() != GameMode.SURVIVAL)
            return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;
        if (item.getType() != Material.LIGHT)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (block.getType() != Material.LIGHT)
            return;

        BlockData bd = block.getBlockData();
        if (!(bd instanceof Light light))
            return;
        block.setType(light.isWaterlogged() ? Material.WATER : Material.AIR);
        ((Light) bd).setWaterlogged(false);
        Location loc = block.getLocation().toCenterLocation();
        ItemStack stack = new ItemStack(Material.LIGHT);
        stack.editMeta(meta -> ((BlockDataMeta) meta).setBlockData(block.getBlockData()));

        loc.getWorld().dropItem(loc, stack);
        CoreProtect.getInstance().getAPI().logRemoval(event.getPlayer().getName(), block.getLocation(), bd.getMaterial(), bd);
    }

    @EventHandler
    void updateLightLevel(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking())
            return;
        if (!player.hasPermission(USE_PERMISSION))
            return;

        PlayerInventory inventory = player.getInventory();
        int from = event.getPreviousSlot();
        int to = event.getNewSlot();
        int diff = (to - from + 13) % 9 - 4;
        ItemStack item = inventory.getItem(from);

        if (item == null)
            return;

        if (item.getType() != Material.LIGHT)
            return;

        if (!(item.getItemMeta() instanceof BlockDataMeta meta))
            return;

        BlockData bd = meta.hasBlockData() ? meta.getBlockData(Material.LIGHT) : Bukkit.createBlockData(Material.LIGHT);

        if (!(bd instanceof Light light))
            return;

        event.setCancelled(true);

        int level = light.getLevel();
        level = level + diff;
        level = Math.max(light.getMinimumLevel(), level);
        level = Math.min(light.getMaximumLevel(), level);
        light.setLevel(level);
        meta.setBlockData(light);
        item.setItemMeta(meta);
    }
}
