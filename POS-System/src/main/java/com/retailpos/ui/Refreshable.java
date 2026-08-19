package com.retailpos.ui;

/**
 * Panels that can be auto-refreshed implement this interface.
 * MainFrame calls {@link #refreshData()} on the visible panel
 * whenever relevant data changes (sync complete, new sale, etc.).
 *
 * {@link #getRefreshIntervalSeconds()} controls how often the
 * background timer will call refresh when this panel is visible.
 * Return 0 to opt out of timer-based refresh.
 */
public interface Refreshable {

    /** Reload / refresh data shown in this panel. Must be safe to call on the EDT. */
    void refreshData();

    /**
     * How often (in seconds) MainFrame should auto-refresh this panel while visible.
     * 0 = no timer-based refresh (still refreshed on sync events).
     */
    default int getRefreshIntervalSeconds() { return 60; }

    /**
     * Return a short human-readable description for the status bar,
     * e.g. "Dashboard — live". Used when the panel becomes visible.
     */
    default String getPanelDescription() { return ""; }
}
