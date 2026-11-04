package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ShopPanel extends JPanel {

    // ── Logical canvas size ──────────────────────────────────────────────────
    private static final int LW = 800;
    private static final int LH = 620;

    // ── TV geometry ──────────────────────────────────────────────────────────
    private static final int SCR_X = 78;
    private static final int SCR_Y = 46;
    private static final int SCR_W = 644;
    private static final int SCR_H = 394;

    private static final int HDR_H = 54;
    private static final int FTR_H = 46;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final Color GOLD      = new Color(255, 215, 60);
    private static final Color ACCENT    = new Color(90, 55, 175);
    private static final Color BOUGHT_C  = new Color(55, 185, 90);
    private static final Color CANT_C    = new Color(85, 85, 105);

    // ── Full item pool (12 items) — shown 8 per day ───────────────────────────
    private static final Object[][] ALL_ITEMS = {
        // Slot 0-7: always available from day 1
        {"Portable Fan",       "Temp rises 30% slower",        8,  "fan"},
        {"Snack Pack",         "Hunger drops 25% slower",      6,  "snack"},
        {"Big Water Bottle",   "Each drink restores more",     6,  "bottle"},
        {"Cat Treats",         "Cat hunger 30% slower",        5,  "treats"},
        {"Chill Pills",        "Stress rises 25% slower",      7,  "chill"},
        {"Kitchen Step Stool", "Stool always in kitchen",      5,  "stool"},
        // Slot 6-7: appear day 2+
        {"Roach Trap",         "Roaches never appear",        10,  "roach"},
        {"Backup Generator",   "Power outages less frequent",  9,  "gen"},
        // Slot 8-9: appear day 2+
        {"Ice Pack",           "One-time -15 temp drop",       8,  "ice"},
        {"Laundry Detergent",  "Laundry chore takes less time",5,  "soap"},
        // Slot 10-11: appear day 3 only
        {"Study Lamp",         "Homework 30% faster",          6,  "lamp"},
        {"Squirrel Spray",     "Squirrel stress cut in half",  7,  "squirt"},
        // Slot 12: appear day 2+ (mouse trap for rats)
        {"Mouse Trap",         "Clears rats from garage",      6,  "mousetrap"},
        // Slot 13: medicine kit — always have cold cure on hand
        {"Medicine Kit",       "Cure colds anytime, forever",  6,  "medkit"},
    };

    // Indices visible each day (always shows 8 items)
    private static final int[][] DAY_SLOTS = {
        {0,1,2,3,4,5,6,13},      // day 1 (Medicine Kit replaces Generator — gen more useful day 2+)
        {0,1,2,6,7,8,9,12},      // day 2 (includes mouse trap)
        {2,3,4,5,7,10,11,13},    // day 3 (Medicine Kit back for late buyers)
    };

    private Object[][] getItems() {
        int dayIdx = Math.max(0, Math.min(GameState.day - 1, DAY_SLOTS.length - 1));
        int[] slots = DAY_SLOTS[dayIdx];
        Object[][] result = new Object[8][];
        for (int i = 0; i < 8; i++) result[i] = ALL_ITEMS[slots[i]];
        return result;
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private String dayText   = "";
    private String coinsText = "";
    private String continueText = "Start Day 1 →";

    private float  tvAnim      = 0f;
    private Timer  animTimer;

    private int    hoveredCard     = -1;
    private boolean hoveredContinue = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ShopPanel() {
        setLayout(null);
        setBackground(new Color(12, 10, 22));

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                float lx = e.getX() * LW / (float) getWidth();
                float ly = e.getY() * LH / (float) getHeight();
                updateHover(lx, ly);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                float lx = e.getX() * LW / (float) getWidth();
                float ly = e.getY() * LH / (float) getHeight();
                handleClick(lx, ly);
            }
        });
    }

    // ── Hover tracking ────────────────────────────────────────────────────────
    private void updateHover(float lx, float ly) {
        int prev = hoveredCard;
        boolean prevC = hoveredContinue;

        hoveredCard = -1;
        for (int i = 0; i < 8; i++) {
            int[] r = cardRect(i);
            if (lx >= r[0] && lx < r[0] + r[2] && ly >= r[1] && ly < r[1] + r[3]) {
                hoveredCard = i;
                break;
            }
        }

        int[] cb = continueButtonRect();
        hoveredContinue = (lx >= cb[0] && lx < cb[0] + cb[2] && ly >= cb[1] && ly < cb[1] + cb[3]);

        if (hoveredCard != prev || hoveredContinue != prevC) repaint();
    }

    // ── Click handling ────────────────────────────────────────────────────────
    private void handleClick(float lx, float ly) {
        // Check item cards – buy button is bottom 24 px of card
        for (int i = 0; i < 8; i++) {
            int[] r = cardRect(i);
            if (lx >= r[0] && lx < r[0] + r[2]) {
                int buyTop = r[1] + r[3] - 24;
                if (ly >= buyTop && ly < r[1] + r[3]) {
                    tryBuy(i);
                    return;
                }
            }
        }
        // Check continue button
        int[] cb = continueButtonRect();
        if (lx >= cb[0] && lx < cb[0] + cb[2] && ly >= cb[1] && ly < cb[1] + cb[3]) {
            startNextDay();
        }
    }

    // ── Card geometry ─────────────────────────────────────────────────────────
    private int[] cardRect(int i) {
        int col = i % 4;
        int row = i / 4;
        int cw = (SCR_W - 12) / 4 - 4;   // ≈152
        int ch = (SCR_H - HDR_H - FTR_H - 8) / 2 - 4;  // ≈138
        int x = SCR_X + 4 + col * (cw + 4);
        int y = SCR_Y + HDR_H + 4 + row * (ch + 4);
        return new int[]{x, y, cw, ch};
    }

    // ── Continue button rect ──────────────────────────────────────────────────
    private int[] continueButtonRect() {
        int x = SCR_X + SCR_W / 2 - 130;
        int y = SCR_Y + SCR_H - FTR_H + 8;
        return new int[]{x, y, 260, 30};
    }

    // ── Buy logic ─────────────────────────────────────────────────────────────
    private void tryBuy(int i) {
        Object[][] items = getItems();
        String key  = (String)  items[i][3];
        int    cost = (Integer) items[i][2];
        if (!isPurchased(key) && GameState.coins >= cost) {
            GameState.coins -= cost;
            applyPurchase(key);
            coinsText = "★ " + GameState.coins + " coins";
            repaint();
        }
    }

    // ── onShow ────────────────────────────────────────────────────────────────
    public void onShow() {
        dayText      = "YOUR ROOM  —  Day " + (GameState.day - 1) + " done!";
        coinsText    = "★ " + GameState.coins + " coins";
        continueText = "Start Day " + GameState.day + " →";
        tvAnim       = 0f;
        hoveredCard  = -1;
        hoveredContinue = false;

        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = new Timer(16, null);
        animTimer.addActionListener(e -> {
            tvAnim += 0.02f;
            if (tvAnim >= 1.0f) {
                tvAnim = 1.0f;
                animTimer.stop();
            }
            repaint();
        });
        animTimer.start();
    }

    // ── paintComponent ────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Scale to logical 800×620
        float sx = getWidth()  / (float) LW;
        float sy = getHeight() / (float) LH;
        g2.scale(sx, sy);

        drawBackground(g2);
        drawTVBody(g2);
        drawTVScreen(g2);
        drawTVBottomBar(g2);
        drawTVStand(g2);
        drawTVOnEffect(g2);
        drawTVChrome(g2);

        g2.dispose();
    }

    // ── Background ────────────────────────────────────────────────────────────
    private void drawBackground(Graphics2D g2) {
        BufferedImage bg = AssetManager.getRoomScaled("bedroom", LW, LH);
        if (bg != null) {
            g2.drawImage(bg, 0, 0, null);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(25, 22, 42), 0, LH, new Color(14, 12, 24));
            g2.setPaint(gp);
            g2.fillRect(0, 0, LW, LH);
            g2.setPaint(null);
        }
        // Dark overlay to focus attention on TV
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, LW, LH);
    }

    // ── TV outer body ─────────────────────────────────────────────────────────
    private void drawTVBody(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 16, new Color(55, 55, 65), 0, 510, new Color(28, 28, 38));
        g2.setPaint(gp);
        g2.fillRoundRect(55, 16, 690, 494, 22, 22);
        g2.setPaint(null);

        // Subtle border
        g2.setColor(new Color(70, 70, 85));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(55, 16, 690, 494, 22, 22);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── TV screen area ────────────────────────────────────────────────────────
    private void drawTVScreen(Graphics2D g2) {
        // Screen black base
        g2.setColor(new Color(8, 8, 14));
        g2.fillRect(SCR_X, SCR_Y, SCR_W, SCR_H);

        if (tvAnim < 0.10f) {
            // Just a thin white line – handled in drawTVOnEffect
            return;
        }

        // Header strip
        drawScreenHeader(g2);

        // Item grid
        Object[][] items = getItems();
        for (int i = 0; i < 8; i++) drawItemCard(g2, i, items[i]);

        // Footer / continue button
        drawFooter(g2);

        // Scanlines (only when mostly visible)
        if (tvAnim > 0.5f) drawScanlines(g2);
    }

    // ── Screen header strip ───────────────────────────────────────────────────
    private void drawScreenHeader(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(SCR_X, SCR_Y, new Color(30, 10, 60),
                SCR_X, SCR_Y + HDR_H, new Color(18, 6, 38));
        g2.setPaint(gp);
        g2.fillRect(SCR_X, SCR_Y, SCR_W, HDR_H);
        g2.setPaint(null);

        // Bottom accent line of header
        g2.setColor(new Color(100, 60, 200, 180));
        g2.fillRect(SCR_X, SCR_Y + HDR_H - 2, SCR_W, 2);

        // Day text (left)
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(200, 180, 255));
        g2.drawString(dayText, SCR_X + 8, SCR_Y + 16);

        // Coins text (right)
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(GOLD);
        FontMetrics fm = g2.getFontMetrics();
        int cw = fm.stringWidth(coinsText);
        g2.drawString(coinsText, SCR_X + SCR_W - cw - 8, SCR_Y + 16);

        // "ITEM SHOP" subtitle centred
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(230, 210, 255));
        fm = g2.getFontMetrics();
        String title = "ITEM SHOP";
        int tw = fm.stringWidth(title);
        g2.drawString(title, SCR_X + (SCR_W - tw) / 2, SCR_Y + HDR_H - 10);
    }

    // ── Item card ─────────────────────────────────────────────────────────────
    private void drawItemCard(Graphics2D g2, int i, Object[] item) {
        int[] r    = cardRect(i);
        int   cx   = r[0];
        int   cy   = r[1];
        int   cw   = r[2];
        int   ch   = r[3];

        String key     = (String)  item[3];
        String name    = (String)  item[0];
        String desc    = (String)  item[1];
        int    cost    = (Integer) item[2];
        boolean bought = isPurchased(key);
        boolean canBuy = !bought && GameState.coins >= cost;
        boolean hover  = (hoveredCard == i);

        // Card background
        Color bgTop, bgBot;
        if (bought) {
            bgTop = new Color(18, 42, 24);
            bgBot = new Color(12, 28, 18);
        } else if (canBuy) {
            bgTop = hover ? new Color(38, 28, 72) : new Color(28, 20, 56);
            bgBot = hover ? new Color(28, 18, 58) : new Color(20, 14, 44);
        } else {
            bgTop = new Color(20, 20, 30);
            bgBot = new Color(14, 14, 22);
        }
        GradientPaint bgGp = new GradientPaint(cx, cy, bgTop, cx, cy + ch, bgBot);
        g2.setPaint(bgGp);
        g2.fillRoundRect(cx, cy, cw, ch, 8, 8);
        g2.setPaint(null);

        // Border
        Color border = bought ? new Color(55, 185, 90)
                : (canBuy ? (hover ? new Color(130, 80, 240) : ACCENT) : new Color(42, 40, 60));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(hover && !bought ? 1.5f : 1.0f));
        g2.drawRoundRect(cx, cy, cw - 1, ch - 1, 8, 8);
        g2.setStroke(new BasicStroke(1f));

        // Icon area (top 40 px)
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(cx + 3, cy + 3, cw - 6, 40, 5, 5);
        drawItemIcon(g2, key, cx + cw / 2, cy + 23);

        // Name
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        Color nameColor = bought ? BOUGHT_C : (canBuy ? new Color(220, 215, 255) : new Color(130, 128, 155));
        g2.setColor(nameColor);
        String nameStr = bought ? "✓ " + name : name;
        FontMetrics fm = g2.getFontMetrics();
        int nameW = fm.stringWidth(nameStr);
        // Truncate if needed
        if (nameW > cw - 6) {
            while (nameStr.length() > 3 && fm.stringWidth(nameStr + "…") > cw - 6)
                nameStr = nameStr.substring(0, nameStr.length() - 1);
            nameStr += "…";
        }
        g2.drawString(nameStr, cx + (cw - fm.stringWidth(nameStr)) / 2, cy + 56);

        // Description
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(new Color(130, 128, 158));
        fm = g2.getFontMetrics();
        String descStr = desc;
        if (fm.stringWidth(descStr) > cw - 6) {
            while (descStr.length() > 3 && fm.stringWidth(descStr + "…") > cw - 6)
                descStr = descStr.substring(0, descStr.length() - 1);
            descStr += "…";
        }
        g2.drawString(descStr, cx + (cw - fm.stringWidth(descStr)) / 2, cy + 68);

        // Cost
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        Color costColor = bought ? BOUGHT_C : (canBuy ? GOLD : CANT_C);
        g2.setColor(costColor);
        String costStr = bought ? "Purchased" : "★ " + cost + " coins";
        fm = g2.getFontMetrics();
        g2.drawString(costStr, cx + (cw - fm.stringWidth(costStr)) / 2, cy + 82);

        // BUY button (bottom 24 px of card)
        int btnX = cx + 4;
        int btnY = cy + ch - 24;
        int btnW = cw - 8;
        int btnH = 20;

        Color btnBg;
        String btnText;
        Color btnTextColor;
        if (bought) {
            btnBg       = new Color(30, 60, 35);
            btnText     = "OWNED";
            btnTextColor = BOUGHT_C;
        } else if (canBuy) {
            btnBg       = hover ? new Color(110, 70, 210) : ACCENT;
            btnText     = "BUY";
            btnTextColor = Color.WHITE;
        } else {
            btnBg       = new Color(32, 30, 48);
            btnText     = "LOCKED";
            btnTextColor = new Color(70, 68, 90);
        }
        g2.setColor(btnBg);
        g2.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
        g2.setColor(btnTextColor);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        fm = g2.getFontMetrics();
        g2.drawString(btnText, btnX + (btnW - fm.stringWidth(btnText)) / 2,
                btnY + (btnH + fm.getAscent() - fm.getDescent()) / 2 - 1);
    }

    // ── Footer / continue button ──────────────────────────────────────────────
    private void drawFooter(Graphics2D g2) {
        // Footer strip
        g2.setColor(new Color(14, 6, 30));
        g2.fillRect(SCR_X, SCR_Y + SCR_H - FTR_H, SCR_W, FTR_H);

        // Top separator
        g2.setColor(new Color(80, 40, 160, 160));
        g2.fillRect(SCR_X, SCR_Y + SCR_H - FTR_H, SCR_W, 1);

        // Continue button
        int[] cb = continueButtonRect();
        Color btnBg = hoveredContinue ? new Color(110, 70, 210) : new Color(75, 48, 165);
        g2.setColor(btnBg);
        g2.fillRoundRect(cb[0], cb[1], cb[2], cb[3], 8, 8);

        // Button border
        g2.setColor(hoveredContinue ? new Color(160, 120, 255) : new Color(120, 80, 220));
        g2.setStroke(new BasicStroke(hoveredContinue ? 1.5f : 1.0f));
        g2.drawRoundRect(cb[0], cb[1], cb[2] - 1, cb[3] - 1, 8, 8);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(continueText);
        g2.drawString(continueText, cb[0] + (cb[2] - tw) / 2,
                cb[1] + (cb[3] + fm.getAscent() - fm.getDescent()) / 2 - 1);
    }

    // ── TV bottom bar ─────────────────────────────────────────────────────────
    private void drawTVBottomBar(Graphics2D g2) {
        g2.setColor(new Color(35, 35, 45));
        g2.fillRoundRect(60, 447, 680, 68, 18, 18);

        // Power LED (green when on, dim when animating)
        float ledBrightness = Math.min(tvAnim * 2f, 1.0f);
        int   ledG = (int) (60 + 130 * ledBrightness);
        g2.setColor(new Color(0, ledG, 0));
        g2.fillOval(386, 458, 8, 8);
        // LED glow
        if (tvAnim > 0.5f) {
            g2.setColor(new Color(0, 255, 60, 60));
            g2.fillOval(382, 454, 16, 16);
        }

        // Speaker grille lines
        g2.setColor(new Color(50, 50, 60));
        g2.setStroke(new BasicStroke(1.0f));
        for (int x = 120; x < 340; x += 5) {
            g2.drawLine(x, 456, x, 500);
        }
        for (int x = 460; x < 680; x += 5) {
            g2.drawLine(x, 456, x, 500);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    // ── TV stand ─────────────────────────────────────────────────────────────
    private void drawTVStand(Graphics2D g2) {
        // Pole
        GradientPaint poleGp = new GradientPaint(350, 0, new Color(50, 50, 60), 450, 0, new Color(35, 35, 45));
        g2.setPaint(poleGp);
        g2.fillRoundRect(350, 510, 100, 20, 6, 6);
        g2.setPaint(null);

        // Base
        GradientPaint baseGp = new GradientPaint(260, 0, new Color(55, 55, 65), 540, 0, new Color(30, 30, 40));
        g2.setPaint(baseGp);
        g2.fillRoundRect(260, 528, 280, 18, 8, 8);
        g2.setPaint(null);

        // Stand border
        g2.setColor(new Color(70, 70, 80));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(350, 510, 100, 20, 6, 6);
        g2.drawRoundRect(260, 528, 280, 18, 8, 8);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── TV on animation effect ────────────────────────────────────────────────
    private void drawTVOnEffect(Graphics2D g2) {
        if (tvAnim >= 0.60f) return; // Fully visible

        if (tvAnim < 0.10f) {
            // Black screen + thin white horizontal line at center
            g2.setColor(new Color(8, 8, 14));
            g2.fillRect(SCR_X, SCR_Y, SCR_W, SCR_H);
            int lineY = SCR_Y + SCR_H / 2;
            int lineH = Math.max(1, (int)(tvAnim * 20));
            g2.setColor(Color.WHITE);
            g2.fillRect(SCR_X, lineY - lineH / 2, SCR_W, lineH);
        } else if (tvAnim < 0.20f) {
            // White flash fading out
            float t     = (tvAnim - 0.10f) / 0.10f; // 0→1
            int   alpha = (int) (255 * (1f - t));
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRect(SCR_X, SCR_Y, SCR_W, SCR_H);
        } else {
            // 0.20–0.60: fade from black overlay (decreasing alpha)
            float t     = (tvAnim - 0.20f) / 0.40f; // 0→1
            int   alpha = (int) (255 * (1f - t));
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRect(SCR_X, SCR_Y, SCR_W, SCR_H);
        }
    }

    // ── Scanlines ─────────────────────────────────────────────────────────────
    private void drawScanlines(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 35));
        for (int y = SCR_Y; y < SCR_Y + SCR_H; y += 3) {
            g2.drawLine(SCR_X, y, SCR_X + SCR_W, y);
        }
    }

    // ── TV chrome reflection ──────────────────────────────────────────────────
    private void drawTVChrome(Graphics2D g2) {
        // Subtle diagonal gradient highlight on top-left quadrant of TV screen
        GradientPaint gp = new GradientPaint(
                SCR_X, SCR_Y, new Color(255, 255, 255, 25),
                SCR_X + SCR_W / 2, SCR_Y + SCR_H / 2, new Color(255, 255, 255, 0));
        g2.setPaint(gp);
        g2.fillRect(SCR_X, SCR_Y, SCR_W / 2, SCR_H / 2);
        g2.setPaint(null);
    }

    // ── Item icons (exact copy from original) ─────────────────────────────────
    private void drawItemIcon(Graphics2D g2, String key, int cx, int cy) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (key) {
            case "fan":
                g2.setColor(new Color(100, 180, 220));
                for (int a = 0; a < 4; a++) {
                    g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double rad = Math.toRadians(a * 90 + 20);
                    g2.drawLine(cx, cy, cx + (int)(Math.cos(rad) * 13), cy + (int)(Math.sin(rad) * 13));
                }
                g2.setStroke(new BasicStroke(1));
                g2.setColor(new Color(60, 140, 180));
                g2.fillOval(cx - 4, cy - 4, 8, 8);
                break;
            case "snack":
                g2.setColor(new Color(220, 180, 40));
                g2.fillRoundRect(cx - 8, cy - 12, 16, 22, 4, 4);
                g2.setColor(new Color(200, 50, 50));
                g2.fillRoundRect(cx - 7, cy - 10, 14, 6, 3, 3);
                g2.setColor(new Color(255, 220, 60));
                g2.fillRoundRect(cx - 7, cy - 2, 14, 10, 3, 3);
                break;
            case "bottle":
                g2.setColor(new Color(80, 180, 230));
                g2.fillRoundRect(cx - 5, cy - 12, 10, 22, 5, 5);
                g2.setColor(new Color(60, 150, 200, 120));
                g2.fillRoundRect(cx - 4, cy - 5, 8, 12, 4, 4);
                g2.setColor(new Color(160, 200, 220));
                g2.fillRoundRect(cx - 3, cy - 14, 6, 5, 2, 2);
                break;
            case "treats":
                g2.setColor(new Color(230, 180, 130));
                g2.fillOval(cx - 7, cy - 4, 14, 12);
                g2.fillOval(cx - 9, cy - 10, 7, 7);
                g2.fillOval(cx - 1, cy - 11, 7, 7);
                g2.fillOval(cx + 2, cy - 4, 7, 7);
                g2.setColor(new Color(210, 150, 110));
                g2.fillOval(cx - 5, cy - 2, 10, 8);
                break;
            case "roach":
                g2.setColor(new Color(140, 120, 80));
                g2.fillRoundRect(cx - 9, cy - 4, 18, 12, 3, 3);
                g2.setColor(new Color(100, 85, 55));
                g2.setStroke(new BasicStroke(1.2f));
                for (int x = cx - 6; x <= cx + 6; x += 4) g2.drawLine(x, cy - 4, x, cy + 8);
                g2.setStroke(new BasicStroke(1));
                g2.setColor(new Color(220, 60, 60));
                g2.drawString("✕", cx - 4, cy - 6);
                break;
            case "gen":
                g2.setColor(new Color(70, 130, 80));
                g2.fillRoundRect(cx - 10, cy - 9, 20, 16, 3, 3);
                g2.setColor(new Color(90, 90, 90));
                g2.fillRect(cx - 4, cy - 12, 8, 4);
                g2.setColor(GOLD);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("⚡", cx - 7, cy + 5);
                break;
            case "stool":
                g2.setColor(new Color(180, 140, 80));
                g2.fillRect(cx - 10, cy - 8, 20, 5);
                g2.fillRect(cx - 10, cy + 5, 20, 5);
                g2.setColor(new Color(155, 118, 62));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(cx - 6, cy - 3, cx - 6, cy + 5);
                g2.drawLine(cx + 6, cy - 3, cx + 6, cy + 5);
                g2.setStroke(new BasicStroke(1));
                break;
            case "chill":
                g2.setColor(new Color(80, 160, 220));
                g2.fillOval(cx - 10, cy - 6, 12, 12);
                g2.setColor(new Color(220, 80, 80));
                g2.fillOval(cx - 2, cy - 6, 12, 12);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.fillOval(cx - 10, cy - 6, 12, 6);
                break;
            case "ice":
                g2.setColor(new Color(180, 230, 255));
                g2.fillOval(cx - 8, cy - 8, 16, 16);
                g2.setColor(new Color(100, 190, 240));
                g2.setStroke(new BasicStroke(1.5f));
                for (int a = 0; a < 6; a++) {
                    double rad = Math.toRadians(a * 60);
                    g2.drawLine(cx, cy, cx + (int)(Math.cos(rad)*10), cy + (int)(Math.sin(rad)*10));
                }
                g2.setStroke(new BasicStroke(1));
                break;
            case "soap":
                g2.setColor(new Color(220, 240, 255));
                g2.fillRoundRect(cx - 7, cy - 11, 14, 20, 4, 4);
                g2.setColor(new Color(130, 200, 240));
                g2.fillRoundRect(cx - 6, cy - 8, 12, 6, 3, 3);
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillOval(cx - 3, cy - 10, 5, 4);
                break;
            case "lamp":
                g2.setColor(new Color(255, 220, 80));
                g2.fillOval(cx - 8, cy - 12, 16, 14);
                g2.setColor(new Color(200, 160, 50));
                g2.fillRect(cx - 2, cy + 2, 4, 10);
                g2.fillRect(cx - 8, cy + 10, 16, 3);
                g2.setColor(new Color(255, 255, 200, 100));
                for (int r = 0; r < 3; r++)
                    g2.fillOval(cx - 12 + r*2, cy - 16 + r*2, 24 - r*4, 20 - r*4);
                break;
            case "squirt":
                g2.setColor(new Color(180, 220, 130));
                g2.fillRoundRect(cx - 9, cy - 4, 18, 10, 3, 3);
                g2.setColor(new Color(100, 170, 240, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawArc(cx + 4, cy - 12, 14, 12, 20, 140);
                g2.drawArc(cx + 7, cy - 8, 10, 8, 20, 140);
                g2.setStroke(new BasicStroke(1));
                break;
            case "mousetrap":
                // Spring bar
                g2.setColor(new Color(160, 140, 100));
                g2.fillRect(cx - 11, cy + 2, 22, 5);
                // Spring wire
                g2.setColor(new Color(180, 180, 180));
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cx - 9, cy - 12, 18, 18, 0, 180);
                g2.setStroke(new BasicStroke(1));
                // Bait platform
                g2.setColor(new Color(200, 170, 80));
                g2.fillOval(cx - 4, cy - 2, 8, 5);
                // Cheese dot
                g2.setColor(new Color(240, 210, 60));
                g2.fillOval(cx - 2, cy - 1, 4, 3);
                break;
        }
        g2.setStroke(new BasicStroke(1));
    }

    // ── Purchase helpers ──────────────────────────────────────────────────────
    private boolean isPurchased(String key) {
        switch (key) {
            case "fan":    return GameState.hasFan;
            case "snack":  return GameState.hasSnackPack;
            case "bottle": return GameState.hasBigBottle;
            case "treats": return GameState.hasCatTreats;
            case "roach":  return GameState.hasRoachTrap;
            case "gen":    return GameState.hasGenerator;
            case "stool":  return GameState.hasKitchenStool;
            case "chill":  return GameState.hasChillPills;
            case "ice":    return GameState.hasIcePack;
            case "soap":   return GameState.hasLaundrysoap;
            case "lamp":   return GameState.hasStudyLamp;
            case "squirt":     return GameState.hasSquirrelSpray;
            case "mousetrap":  return GameState.hasMouseTrap;
            case "medkit":     return GameState.hasMedicineKit;
            default:           return false;
        }
    }

    private void applyPurchase(String key) {
        switch (key) {
            case "fan":       GameState.hasFan          = true; break;
            case "snack":     GameState.hasSnackPack    = true; break;
            case "bottle":    GameState.hasBigBottle    = true; break;
            case "treats":    GameState.hasCatTreats    = true; break;
            case "roach":     GameState.hasRoachTrap    = true; break;
            case "gen":       GameState.hasGenerator    = true; break;
            case "stool":     GameState.hasKitchenStool = true; break;
            case "chill":     GameState.hasChillPills   = true; break;
            case "ice":       GameState.hasIcePack      = true;
                              GameState.temperature = GameLoop.clamp(GameState.temperature - 15f, 0, 100);
                              break;
            case "soap":      GameState.hasLaundrysoap  = true; break;
            case "lamp":      GameState.hasStudyLamp    = true; break;
            case "squirt":    GameState.hasSquirrelSpray = true; break;
            case "mousetrap": GameState.hasMouseTrap    = true; break;
            case "medkit":    GameState.hasMedicineKit  = true; break;
        }
    }

    // ── Start next day ────────────────────────────────────────────────────────
    private void startNextDay() {
        GameState.resetForDay();
        GameFrame.getInstance().showScreen(GameFrame.GAME);
    }
}
