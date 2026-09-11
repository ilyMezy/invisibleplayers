package com.mezy.invisibleplayers.service;

import org.bukkit.Bukkit;

/**
 * The single, lightweight safety-net reconciliation task. Exactly one instance
 * is ever scheduled at a time; lifecycle (start/stop/reschedule) is owned by
 * the main plugin class.
 */
public final class ReconciliationTask implements Runnable {

    private final TabListTracker tabListTracker;

    public ReconciliationTask(TabListTracker tabListTracker) {
        this.tabListTracker = tabListTracker;
    }

    @Override
    public void run() {
        tabListTracker.reconcileAll(Bukkit.getOnlinePlayers());
    }
}
