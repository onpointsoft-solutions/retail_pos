package com.retailpos.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Wraps a JTabbedPane and plays a fast fade + directional slide whenever
 * the selected tab changes.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>On tab-change we snapshot the outgoing panel into a BufferedImage.</li>
 *   <li>A Swing Timer advances a progress float 0→1 over {@code DURATION_MS}.</li>
 *   <li>During animation the content area is replaced by an overlay that
 *       alpha-composites the old snapshot sliding out while the real new panel
 *       fades in behind it.</li>
 *   <li>When progress reaches 1 the overlay is removed and the real layout
 *       takes over — no extra components remain in the hierarchy.</li>
 * </ol>
 *
 * The host is itself a JPanel with BorderLayout; the JTabbedPane sits inside
 * it with its content area customised via a tab-change listener.
 */
public class AnimatedTabHost extends JPanel {

    private static final int   DURATION_MS = 160;   // total animation time
    private static final int   TICK_MS     = 12;    // ~83 fps
    private static final float SLIDE_PX    = 22f;   // max pixel slide distance

    private final JTabbedPane tabs;
    private final JPanel      contentWrapper;       // holds whichever component is "live"

    // animation state
    private Timer         animTimer;
    private BufferedImage oldSnapshot;
    private long          animStart;
    private int           direction = 1;            // +1 = new tab is to the right, -1 = left
    private int           lastIndex = -1;
    private boolean       animating = false;

    public AnimatedTabHost(JTabbedPane tabs) {
        super(new BorderLayout());
        setOpaque(false);
        this.tabs = tabs;

        // Pull the tab bar out of the regular JTabbedPane and put it on the
        // LEFT side of this host; the content lives in contentWrapper.
        contentWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        contentWrapper.setOpaque(false);

        // We keep the original JTabbedPane for its left-side tab strip only;
        // we intercept selection changes to drive our own content swap.
        add(tabs, BorderLayout.CENTER);

        tabs.addChangeListener(e -> {
            int newIndex = tabs.getSelectedIndex();
            if (newIndex < 0 || newIndex == lastIndex) return;
            direction    = newIndex > lastIndex ? 1 : -1;
            lastIndex    = newIndex;
            triggerAnimation();
        });
    }

    // ── Animation entry point ─────────────────────────────────────────────────

    private void triggerAnimation() {
        Component newComp = tabs.getSelectedComponent();
        if (newComp == null) return;

        // Snapshot the entire tabs content area before Swing swaps it
        Rectangle bounds = tabs.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) return;

        // Grab screenshot of the currently displayed content region
        oldSnapshot = snapshotComponent(tabs);

        // Stop any running animation
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animating = true;
        animStart = System.currentTimeMillis();

        // Install glass overlay on root pane
        JRootPane root = SwingUtilities.getRootPane(tabs);
        if (root == null) { animating = false; return; }

        AnimationOverlay overlay = new AnimationOverlay(root, tabs, oldSnapshot, direction);
        root.setGlassPane(overlay);
        overlay.setVisible(true);

        animTimer = new Timer(TICK_MS, (ActionEvent ev) -> {
            float progress = Math.min(1f,
                (System.currentTimeMillis() - animStart) / (float) DURATION_MS);
            overlay.setProgress(progress);
            if (progress >= 1f) {
                animTimer.stop();
                animating = false;
                overlay.setVisible(false);
                root.setGlassPane(new JPanel()); // clear glass
                tabs.repaint();
            }
        });
        animTimer.start();
    }

    // ── Snapshot helper ───────────────────────────────────────────────────────

    private static BufferedImage snapshotComponent(JComponent c) {
        int w = Math.max(c.getWidth(),  1);
        int h = Math.max(c.getHeight(), 1);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        return img;
    }

    // ── Overlay panel (glass pane) ────────────────────────────────────────────

    /**
     * Transparent glass pane that draws the fading/sliding old snapshot
     * on top of the already-swapped new panel content.
     */
    static class AnimationOverlay extends JPanel {
        private final JRootPane   root;
        private final JComponent  target;         // the JTabbedPane
        private final BufferedImage snapshot;
        private final int         dir;
        private float             progress = 0f;

        AnimationOverlay(JRootPane root, JComponent target,
                         BufferedImage snapshot, int dir) {
            super(null);
            setOpaque(false);
            this.root     = root;
            this.target   = target;
            this.snapshot = snapshot;
            this.dir      = dir;
        }

        void setProgress(float p) {
            this.progress = p;
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            if (snapshot == null || progress >= 1f) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_SPEED);

            // Map target bounds into glass-pane coordinate space
            Point origin = SwingUtilities.convertPoint(target, 0, 0, root.getGlassPane());
            int x = origin.x, y = origin.y;
            int w = target.getWidth(), h = target.getHeight();

            // Ease function: cubic ease-out
            float t = easeOut(progress);

            // Old snapshot slides out and fades
            float alpha = 1f - t;
            float slideX = dir * SLIDE_PX * t;

            AlphaComposite ac = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, Math.max(0f, alpha));
            g.setComposite(ac);
            g.drawImage(snapshot, (int)(x + slideX), y, w, h, null);

            g.dispose();
        }

        /** Cubic ease-out: fast start, gentle landing. */
        private static float easeOut(float t) {
            float inv = 1f - t;
            return 1f - inv * inv * inv;
        }
    }
}
