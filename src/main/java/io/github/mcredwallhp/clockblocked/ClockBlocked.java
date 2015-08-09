package io.github.mcredwallhp.clockblocked;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClockBlocked extends JavaPlugin implements Listener {


    String currentPortal;
    List<String> worldNames;
    List<String> portalRegions;


    @Override
    public void onEnable() {

        this.saveDefaultConfig();
        this.loadConfigData();
        this.getServer().getPluginManager().registerEvents(this, this);

        this.unlightPortals();
        this.lightPortal(this.currentPortal);

    }


    @Override
    public void onDisable() {
        // Advance portal on disable
        // This way rev start isn't off by one, and crashes won't advance it
        this.advancePortal();
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("clockblocked")) {

            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            }

            if (args.length < 1) {
                sender.sendMessage(ChatColor.GOLD + "/clockblocked <subcommand>");
                sender.sendMessage(ChatColor.GOLD + "current - " + ChatColor.WHITE + "Check current active portal.");
                sender.sendMessage(ChatColor.GOLD + "cycle - " + ChatColor.WHITE + "Advance active portal by one.");
                sender.sendMessage(ChatColor.GOLD + "list - " + ChatColor.WHITE + "List registered portals.");
                sender.sendMessage(ChatColor.GOLD + "light <portal> - " + ChatColor.WHITE + "Make a specific portal active.");
                sender.sendMessage(ChatColor.GOLD + "lightall - " + ChatColor.WHITE + "Open all registerd portals.");
                sender.sendMessage(ChatColor.GOLD + "unlightall - " + ChatColor.WHITE + "Close all registerd portals.");
                sender.sendMessage(ChatColor.GOLD + "reload - " + ChatColor.WHITE + "Reload plugin configuration.");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                this.reloadConfig();
                this.loadConfigData();
                sender.sendMessage(ChatColor.GOLD + "ClockBlocked config reloaded");
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                String portals = "";
                for (String p : this.portalRegions) {
                    portals += p + ", ";
                }
                sender.sendMessage(ChatColor.GOLD + "ClockBlocked Portals: " + ChatColor.RESET + portals);
                return true;
            }

            if (args[0].equalsIgnoreCase("cycle")) {
                this.advancePortal();
                this.unlightPortals();
                this.lightPortal(this.currentPortal);
                sender.sendMessage(ChatColor.GOLD + "Lit portal " + this.currentPortal);
                return true;
            }

            if (args[0].equalsIgnoreCase("light")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.GOLD + "Usage: /clockblocked light <portal name>");
                } else if (!this.portalRegions.contains(args[1])) {
                    sender.sendMessage(ChatColor.RED + "That portal name was not found.");
                } else {
                    this.currentPortal = args[1];
                    this.getConfig().set("current_portal", args[1]);
                    this.saveConfig();
                    this.unlightPortals();
                    this.lightPortal(args[1]);
                    sender.sendMessage(ChatColor.GOLD + "Current portal set to " + args[1]);
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("lightall")) {
                for (String portal : this.portalRegions) {
                    this.lightPortal(portal);
                }
                sender.sendMessage(ChatColor.GOLD + "Lit all registered portals.");
                return true;
            }

            if (args[0].equalsIgnoreCase("unlightall")) {
                this.unlightPortals();
                sender.sendMessage(ChatColor.GOLD + "Unlit all registered portals.");
                return true;
            }

            if (args[0].equalsIgnoreCase("current")) {
                sender.sendMessage(ChatColor.GOLD + "Current active portal: " + this.currentPortal);
                return true;
            }

        }
        return false;
    }


    public String advancePortal() {
        Integer curIndex = this.portalRegions.indexOf(this.currentPortal);
        Integer nextIndex = curIndex + 1;
        if (nextIndex >= this.portalRegions.size()) {
            nextIndex = 0;
        }
        this.currentPortal = this.portalRegions.get(nextIndex);
        this.getConfig().set("current_portal", this.currentPortal);
        this.saveConfig();
        return this.currentPortal;
    }


    public void lightPortal(String portal) {
        for (String world : this.worldNames) {
            RegionManager regions = getWG().getRegionManager(getServer().getWorld(world));
            if (regions != null) {
                ProtectedRegion region = regions.getRegion(portal);
                if (region != null) {
                    BlockVector minPoint = region.getMinimumPoint();
                    BlockVector maxPoint = region.getMaximumPoint();
                    CuboidRegion cube = new CuboidRegion(new BukkitWorld(getServer().getWorld(world)), minPoint, maxPoint);
                    EditSession session = new EditSession(new BukkitWorld(getServer().getWorld(world)), cube.getArea());
                    try {
                        session.setFastMode(true);
                        session.enableQueue();
                        Set<BaseBlock> from = new HashSet<BaseBlock>();
                        from.add(new BaseBlock(0));
                        if (this.portalOrientation(minPoint, maxPoint) == 0) {
                            session.replaceBlocks(cube, from, new BaseBlock(90));
                        } else {
                            session.replaceBlocks(cube, from, new BaseBlock(90, 0x2));
                        }
                        session.flushQueue();
                    } catch (Exception e) {
                        getLogger().warning("Could not light portal: " + portal);
                    }
                }
            }
        }
    }


    public void unlightPortals() {
        for (String world : this.worldNames) {
            RegionManager regions = getWG().getRegionManager(getServer().getWorld(world));
            if (regions != null) {
                for (String portal : this.portalRegions) {
                    ProtectedRegion region = regions.getRegion(portal);
                    if (region != null) {
                        BlockVector minPoint = region.getMinimumPoint();
                        BlockVector maxPoint = region.getMaximumPoint();
                        CuboidRegion cube = new CuboidRegion(new BukkitWorld(getServer().getWorld(world)), minPoint, maxPoint);
                        EditSession session = new EditSession(new BukkitWorld(getServer().getWorld(world)), cube.getArea());
                        try {
                            session.setFastMode(true);
                            session.enableQueue();
                            Set<BaseBlock> from = new HashSet<BaseBlock>();
                            from.add(new BaseBlock(90));
                            from.add(new BaseBlock(90, 0x1));
                            from.add(new BaseBlock(90, 0x2));
                            session.replaceBlocks(cube, from, new BaseBlock(0));
                            session.flushQueue();
                        } catch (Exception e) {
                            getLogger().warning("Could not unlight portal: " + portal);
                        }
                    }
                }
            }
        }
    }


    public int portalOrientation(BlockVector minPoint, BlockVector maxPoint) {
        // If x coordinates match, portal is facing East/West (1)
        // If z coordinates match, portal is facing North/South (0)
        if (minPoint.getX() == maxPoint.getBlockX()) {
            return 1;
        } else {
            return 0;
        }
    }


    public void loadConfigData() {
        this.currentPortal = getConfig().getString("current_portal");
        this.worldNames = getConfig().getStringList("worlds");
        this.portalRegions = getConfig().getStringList("portals");
    }


    private WorldGuardPlugin getWG() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }
        return (WorldGuardPlugin) plugin;
    }


    private WorldEditPlugin getWE() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return null;
        }
        return (WorldEditPlugin) plugin;
    }


}
