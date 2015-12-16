package io.github.mcredwallhp.clockblocked;

import java.util.Date;

class CycleTask implements Runnable {

    ClockBlocked plugin;
    long duration;

    public CycleTask(ClockBlocked plugin) {
        this.plugin = plugin;
        this.duration = plugin.timerDuration * (1000*60);
    }

    public void run() {
        Date now = new Date();
        long diff = now.getTime() - plugin.lastCycleTime;
        if (diff > duration) {
            plugin.unlightPortals();
            if (!plugin.multimode) {
                plugin.advancePortal();
                plugin.lightPortal(plugin.currentPortal);
            } else {
                plugin.advancePortalGroup();
                plugin.lightPortalGroup(plugin.currentGroup);
            }
            plugin.lastCycleTime = now.getTime();
            plugin.getConfig().set("last_cycle_time", plugin.lastCycleTime);
        }
    }

}