package io.github.mcredwallhp.clockblocked;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import java.util.*;

public final class ClockBlocked extends JavaPlugin implements Listener {


    String currentPortal;
    String currentGroup;
    List<String> worldNames;
    List<String> portalRegions;
    ConfigurationSection portalGroups;
    Boolean multimode;
    Boolean restartmode;
    int timerDuration;
    long lastCycleTime;


    @Override
    public void onEnable() {

        this.saveDefaultConfig();
        this.loadConfigData();
        this.getServer().getPluginManager().registerEvents(this, this);

        this.unlightPortals();
        if (!this.multimode) {
            this.lightPortal(this.currentPortal);
        } else {
            this.lightPortalGroup(this.currentGroup);
        }

        if (!this.restartmode) {
            this.startPortalTimer();
        }

    }


    @Override
    public void onDisable() {
        // Advance portal on disable
        // This way rev start isn't off by one, and crashes won't advance it
        Bukkit.getScheduler().cancelTasks(this);
        if (!this.restartmode) return;
        if (!this.multimode) {
            this.advancePortal();
        } else {
            this.advancePortalGroup();
        }
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
                sender.sendMessage(ChatColor.GOLD + "current - " + ChatColor.WHITE + "Check current active portal or group.");
                sender.sendMessage(ChatColor.GOLD + "cycle - " + ChatColor.WHITE + "Advance active portal or group by one.");
                sender.sendMessage(ChatColor.GOLD + "list - " + ChatColor.WHITE + "List registered portals and groups.");
                sender.sendMessage(ChatColor.GOLD + "light <portal> - " + ChatColor.WHITE + "Make a specific portal active.");
                sender.sendMessage(ChatColor.GOLD + "lightall - " + ChatColor.WHITE + "Open all registerd portals.");
                sender.sendMessage(ChatColor.GOLD + "lightgroup <name> - " + ChatColor.WHITE + "Activate all portals in a group.");
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
                if (this.multimode) {
                    String groupStr = "";
                    List<String> groups = new ArrayList<String>();
                    groups.addAll(this.portalGroups.getKeys(false));
                    for (String g : groups) {
                        groupStr += g + ", ";
                    }
                    sender.sendMessage(ChatColor.GOLD + "ClockBlocked Groups: " + ChatColor.RESET + groupStr);
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("cycle")) {
                if (!this.multimode) {
                    this.advancePortal();
                    this.unlightPortals();
                    this.lightPortal(this.currentPortal);
                    sender.sendMessage(ChatColor.GOLD + "Lit portal " + this.currentPortal);
                } else {
                    this.advancePortalGroup();
                    this.unlightPortals();
                    this.lightPortalGroup(this.currentGroup);
                    sender.sendMessage(ChatColor.GOLD + "Lit portals in group " + this.currentGroup);
                }
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

            if (args[0].equalsIgnoreCase("lightgroup")) {
                if (!this.multimode) {
                    sender.sendMessage(ChatColor.RED + "Multimode is not enabled.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.GOLD + "Usage: /clockblocked lightgroup <name>");
                    return true;
                }
                if (!this.portalGroups.contains(args[1])) {
                    sender.sendMessage(ChatColor.RED + "That portal group was not found.");
                    return true;
                }
                this.currentGroup = args[1];
                this.getConfig().set("current_group", args[1]);
                this.saveConfig();
                this.unlightPortals();
                this.lightPortalGroup(args[1]);
                sender.sendMessage(ChatColor.GOLD + "Current portal group set to " + args[1]);
                return true;
            }

            if (args[0].equalsIgnoreCase("unlightall")) {
                this.unlightPortals();
                sender.sendMessage(ChatColor.GOLD + "Unlit all registered portals.");
                return true;
            }

            if (args[0].equalsIgnoreCase("current")) {
                if (!this.multimode) {
                    sender.sendMessage(ChatColor.GOLD + "Current active portal: " + this.currentPortal);
                } else {
                    sender.sendMessage(ChatColor.GOLD + "Current active group: " + this.currentGroup);
                }
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


    public String advancePortalGroup() {
        List<String> groups = new ArrayList<String>();
        groups.addAll(this.portalGroups.getKeys(false));
        Integer curIndex = groups.indexOf(this.currentGroup);
        Integer nextIndex = curIndex + 1;
        if (nextIndex >= groups.size()) {
            nextIndex = 0;
        }
        this.currentGroup = groups.get(nextIndex);
        this.getConfig().set("current_group", this.currentGroup);
        this.saveConfig();
        return this.currentGroup;
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


    public void lightPortalGroup(String groupToLight) {
        for (String group : this.portalGroups.getKeys(false)) {
            if (group.equals(groupToLight)) {
                for (Object portal : this.portalGroups.getList(group)) {
                    this.lightPortal(portal.toString());
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


    public void startPortalTimer() {
        // Cycle the portals every x minutes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new CycleTask(this), 1200, 1200);
    }


    public void loadConfigData() {
        this.currentPortal = getConfig().getString("current_portal");
        this.currentGroup = getConfig().getString("current_group");
        this.worldNames = getConfig().getStringList("worlds");
        this.portalRegions = getConfig().getStringList("portals");
        this.portalGroups = getConfig().getConfigurationSection("groups");
        this.multimode = getConfig().getBoolean("multimode");
        this.restartmode = getConfig().getBoolean("restartmode");
        this.timerDuration = getConfig().getInt("timer_duration");
        this.lastCycleTime = getConfig().getLong("last_cycle_time", 0);
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
