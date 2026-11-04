package game;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.RadialGradientPaint;

/**
 * Abstract base for every room panel. Provides:
 *  - paintComponent hook for room art
 *  - helper to create styled action buttons
 *  - navigation row at the bottom
 */
public abstract class RoomPanel extends JPanel {

    protected static final Color BTN_BG     = new Color(10, 10, 18, 195);
    protected static final Color BTN_FG     = new Color(235, 235, 245);
    protected static final Color BTN_BORDER = new Color(130, 130, 180, 200);
    protected static final Color BTN_HOVER  = new Color(40, 40, 70, 215);
    protected static final Color NAV_BG     = new Color(15, 15, 25);

    public RoomPanel() {
        setLayout(null);  // absolute positioning for game feel
        setBackground(Color.BLACK);
    }

    // ── Must implement: room-specific drawing ─────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    /**
     * Draws a power-out darkness overlay on top of all children.
     * Called via paint() so it renders AFTER buttons/components.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (GameState.powerOut) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int alpha = GameState.hasMomFlashlight ? 80 : 160;
            g2.setColor(new Color(0, 0, 20, alpha));
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (GameState.hasMomFlashlight) {
                // Warm flashlight cone centred on the panel
                float cx = getWidth() * 0.5f, cy = getHeight() * 0.55f;
                float r  = getWidth() * 0.42f;
                RadialGradientPaint cone = new RadialGradientPaint(
                    cx, cy, r,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(255, 235, 180, 110),
                                new Color(255, 210, 130, 30),
                                new Color(0, 0, 0, 0)}
                );
                g2.setPaint(cone);
                g2.fillOval((int)(cx - r), (int)(cy - r), (int)(r * 2), (int)(r * 2));
                g2.setPaint(null);
            }
            g2.dispose();
        }
    }

    // ── Must implement: rebuild buttons when room becomes visible ─────────
    public abstract void onEnter();

    // ── Button factory — polished game-style design ───────────────────────
    protected JButton makeButton(String text, int x, int y, int w, int h) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                boolean hover   = getModel().isRollover();
                boolean press   = getModel().isPressed();
                boolean enabled = isEnabled();

                // Accent color comes from the button's foreground if the panel set one
                Color fg  = getForeground();
                boolean hasAccent = fg != null && !fg.equals(Color.BLACK)
                                    && !fg.equals(new Color(235, 235, 245))
                                    && !fg.equals(Color.WHITE);
                Color accent = hasAccent ? fg : new Color(140, 200, 255);

                // Background — dark frosted glass
                int bgAlpha = enabled ? (press ? 230 : hover ? 218 : 200) : 140;
                Color bgTop = press  ? new Color(30,  50,  30, bgAlpha) :
                              hover  ? new Color(38,  55,  85, bgAlpha) :
                              enabled? new Color(12,  14,  28, bgAlpha) :
                                       new Color(22,  22,  34, 130);
                Color bgBot = press  ? new Color(18,  35,  18, bgAlpha) :
                              hover  ? new Color(22,  36,  62, bgAlpha) :
                              enabled? new Color( 6,   6,  18, bgAlpha) :
                                       new Color(14,  14,  22, 110);
                GradientPaint gp = new GradientPaint(0, 0, bgTop, 0, H, bgBot);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, W, H, 10, 10);
                g2.setPaint(null);

                // Left-side accent bar (colored category indicator)
                if (enabled && hasAccent) {
                    Color bar = hover ? accent.brighter() : accent;
                    g2.setColor(new Color(bar.getRed(), bar.getGreen(), bar.getBlue(), hover ? 255 : 210));
                    g2.fillRoundRect(0, 3, 4, H - 6, 3, 3);
                }

                // Outer border
                Color borderC = hover ? accent : (enabled ? new Color(90, 105, 160, 190) : new Color(55, 58, 80, 120));
                g2.setColor(borderC);
                g2.setStroke(new BasicStroke(hover ? 1.6f : 1.1f));
                g2.drawRoundRect(0, 0, W - 1, H - 1, 10, 10);

                // Subtle top-shine highlight
                g2.setColor(new Color(255, 255, 255, hover ? 28 : 18));
                g2.fillRoundRect(2, 2, W - 4, H / 3, 7, 7);

                // Text
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String label = getText();
                int tx = (W - fm.stringWidth(label)) / 2 + (hasAccent ? 2 : 0);
                int ty = (H + fm.getAscent() - fm.getDescent()) / 2;

                // Drop shadow
                g2.setColor(new Color(0, 0, 0, 130));
                g2.drawString(label, tx + 1, ty + 1);

                // Main text color
                Color textC = !enabled ? new Color(95, 100, 120) :
                               hasAccent ? (hover ? accent.brighter() : accent) :
                                           new Color(220, 228, 248);
                g2.setColor(textC);
                g2.drawString(label, tx, ty);

                g2.setStroke(new BasicStroke(1));
                g2.dispose();
            }
        };
        btn.setBounds(x, y, w, h);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Navigation buttons (placed by GamePanel in the nav row) ──────────
    protected static JButton makeNavButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? new Color(50, 50, 70) :
                           getModel().isRollover() ? new Color(45, 45, 65) :
                                                     new Color(25, 25, 40);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(100, 100, 140));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.setColor(new Color(200, 200, 220));
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(text)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, tx, ty);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Draw a simple label over the room ─────────────────────────────────
    protected void drawLabel(Graphics2D g2, String text, int x, int y) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(new Color(255, 255, 255, 210));
        g2.drawString(text, x, y);
    }

    // ── Draw room background image (scales to fill logical 800×455) ───────
    protected void drawBg(Graphics2D g2, String assetName, int W, int H) {
        BufferedImage img = AssetManager.getRoomScaled(assetName, W, H);
        if (img != null) {
            g2.drawImage(img, 0, 0, null);
        } else {
            // fallback gradient if asset is missing
            GradientPaint gp = new GradientPaint(0, 0, new Color(38, 36, 52),
                    0, H, new Color(22, 20, 32));
            g2.setPaint(gp);
            g2.fillRect(0, 0, W, H);
            g2.setPaint(null);
        }
    }

    // ── Status badge (semi-transparent pill) ──────────────────────────────
    protected void drawBadge(Graphics2D g2, String text, int x, int y,
                             Color bg, Color fg) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int pw = fm.stringWidth(text) + 18;
        int ph = 22;
        g2.setColor(bg);
        g2.fillRoundRect(x, y, pw, ph, 10, 10);
        g2.setColor(bg.brighter().brighter());
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y, pw, ph, 10, 10);
        g2.setColor(fg);
        g2.drawString(text, x + 9, y + 15);
    }
}
