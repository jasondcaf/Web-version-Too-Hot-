package game;

import javax.swing.*;
import java.awt.*;

/**
 * Always-visible top strip showing all player stats, phone battery,
 * mom countdown, chores (core + bonus), and coin count.
 */
public class HUDPanel extends JPanel {

    private static final int HEIGHT = 148;   // logical coordinate space height

    private static final Color C_TEMP    = new Color(220, 60, 50);
    private static final Color C_HUNGER  = new Color(180, 110, 50);
    private static final Color C_THIRST  = new Color(60, 130, 210);
    private static final Color C_CAT     = new Color(210, 140, 30);
    private static final Color C_STRESS  = new Color(140, 60, 180);
    private static final Color C_TRACK   = new Color(50, 50, 60);
    private static final Color C_BG      = new Color(20, 20, 30);
    private static final Color C_WARN    = new Color(255, 80, 80);

    public HUDPanel() {
        setPreferredSize(new Dimension(800, HEIGHT));
        setBackground(C_BG);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float _sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float _sy = getHeight() <= 0 ? 1f : getHeight() / (float)HEIGHT;
        g2.scale(_sx, _sy);
        int w = 800;

        // Background
        g2.setColor(C_BG);
        g2.fillRect(0, 0, w, HEIGHT);

        // Bottom separator
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawLine(0, HEIGHT - 1, w, HEIGHT - 1);

        // ── Stat bars (left side) ─────────────────────────────────────────
        int barX = 12, barY = 10, barW = 175, barH = 10, gap = 14;
        drawBar(g2, barX, barY,           barW, barH, "TEMP",   GameState.temperature, 100f, C_TEMP,   true);
        drawBar(g2, barX, barY + gap,     barW, barH, "HUNGER", GameState.hunger,      100f, C_HUNGER, false);
        drawBar(g2, barX, barY + gap * 2, barW, barH, "THIRST", GameState.thirst,      100f, C_THIRST, false);
        drawBar(g2, barX, barY + gap * 3, barW, barH, "CAT",    GameState.catHunger,   100f, C_CAT,    false);
        drawBar(g2, barX, barY + gap * 4, barW, barH, "STRESS", GameState.stress,      100f, C_STRESS, true);

        // ── Phone battery bar ─────────────────────────────────────────────
        int pbY = barY + gap * 5 + 2;
        drawPhoneBattery(g2, barX, pbY, barW, 9);

        // ── Status icons (middle) ─────────────────────────────────────────
        int iconX = 208, iconY = 8;
        drawStatusIcon(g2, iconX,      iconY,      "AC",     !GameState.acBroken,       new Color(60, 180, 220));
        drawStatusIcon(g2, iconX + 52, iconY,      "POWER",  !GameState.powerOut,       new Color(255, 220, 60));
        drawStatusIcon(g2, iconX,      iconY + 28, "WINDOW", GameState.windowOpen
                || GameState.bedroomWindowOpen || GameState.kitchenWindowOpen,          new Color(60, 200, 120));
        drawStatusIcon(g2, iconX + 52, iconY + 28, "SQUIRL", GameState.squirrelPresent, new Color(220, 120, 60));
        drawStatusIcon(g2, iconX,      iconY + 56, "STORM",  GameState.thunderstorm,    new Color(100, 80, 220));
        drawStatusIcon(g2, iconX + 52, iconY + 56, "CATS",   GameState.isCatAngry(),    new Color(220, 100, 60));

        // Inventory pills
        drawPill(g2, iconX,       iconY + 80, "STOOL",  GameState.hasStepStool);
        drawPill(g2, iconX + 52,  iconY + 80, "WRENCH", GameState.hasWrench);
        drawPill(g2, iconX + 104, iconY + 80, "SPRAY",  GameState.hasRoachSpray);

        // ── Timer (right side) ────────────────────────────────────────────
        int timerX = w - 178;
        int mins   = GameState.momTimerSeconds / 60;
        int secs   = GameState.momTimerSeconds % 60;
        String timeStr = String.format("%02d:%02d", mins, secs);

        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        boolean urgent = GameState.momTimerSeconds < 60;
        g2.setColor(urgent ? C_WARN : new Color(220, 220, 220));
        g2.drawString(timeStr, timerX, 38);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(new Color(150, 150, 170));
        g2.drawString("until mom arrives", timerX, 50);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(200, 200, 220));
        g2.drawString("DAY " + GameState.day + " / " + GameState.MAX_DAYS, timerX, 64);

        g2.setColor(new Color(255, 215, 60));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.drawString("★ " + GameState.coins + " coins", timerX, 80);

        // Mittens escaped alert in timer area
        if (GameState.mittensEscaped) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(255, 140, 50));
            g2.drawString("Mittens escaped!", timerX, 92);
        }

        // Current room label
        g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
        g2.setColor(new Color(120, 120, 140));
        g2.drawString(roomLabel(GameState.currentRoom), timerX - 5, 10);

        // ── Separator ─────────────────────────────────────────────────────
        g2.setColor(new Color(255, 255, 255, 18));
        g2.drawLine(0, 100, w, 100);

        // ── Core chores strip ─────────────────────────────────────────────
        drawCoreChoreSstrip(g2, w);

        // ── Bonus chores strip ────────────────────────────────────────────
        drawBonusChoresStrip(g2, w);

        g2.dispose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void drawBar(Graphics2D g2, int x, int y, int w, int h,
                         String label, float val, float max, Color fill, boolean invertedBad) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(new Color(160, 160, 180));
        g2.drawString(label, x, y + h - 1);

        int labelW = 38;
        int bx = x + labelW, bw = w - labelW;

        g2.setColor(C_TRACK);
        g2.fillRoundRect(bx, y, bw, h, 3, 3);

        float ratio = val / max;
        Color barColor;
        if (invertedBad) {
            if (ratio > 0.85f)      barColor = new Color(220, 30, 30);
            else if (ratio > 0.65f) barColor = new Color(210, 160, 30);
            else                    barColor = fill;
        } else {
            if (ratio < 0.2f)       barColor = new Color(220, 30, 30);
            else if (ratio < 0.4f)  barColor = new Color(210, 160, 30);
            else                    barColor = fill;
        }
        g2.setColor(barColor);
        g2.fillRoundRect(bx, y, (int)(bw * ratio), h, 3, 3);
    }

    private void drawPhoneBattery(Graphics2D g2, int x, int y, int w, int h) {
        float bat = GameState.phoneBattery;
        int labelW = 38;
        int bx = x + labelW, bw = w - labelW;

        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(new Color(140, 160, 140));
        g2.drawString("PHONE", x, y + h - 1);

        g2.setColor(C_TRACK);
        g2.fillRoundRect(bx, y, bw, h, 3, 3);

        Color fillCol = bat > 50 ? new Color(50, 200, 90)
                      : bat > 25 ? new Color(220, 185, 40)
                      : new Color(210, 50, 50);
        if (GameState.phoneCharging) fillCol = new Color(80, 180, 255);
        g2.setColor(fillCol);
        g2.fillRoundRect(bx, y, (int)(bw * bat / 100f), h, 3, 3);

        // Flash warning when dead
        if (bat <= 5f) {
            long t = System.currentTimeMillis();
            if ((t / 500) % 2 == 0) {
                g2.setColor(new Color(255, 60, 60, 180));
                g2.fillRoundRect(bx, y, bw, h, 3, 3);
            }
        }

        if (GameState.friendTextPending) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(new Color(100, 200, 255));
            g2.drawString("*text!", bx + bw + 3, y + h);
        }
    }

    private void drawStatusIcon(Graphics2D g2, int x, int y, String label, boolean good, Color onColor) {
        g2.setColor(good ? onColor : new Color(75, 75, 88));
        g2.fillRoundRect(x, y, 46, 20, 5, 5);
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(good ? Color.BLACK : new Color(130, 130, 140));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        g2.drawString(label, x + (46 - tw) / 2, y + 14);
    }

    private void drawPill(Graphics2D g2, int x, int y, String label, boolean has) {
        g2.setColor(has ? new Color(60, 200, 100, 180) : new Color(55, 55, 68));
        g2.fillRoundRect(x, y, 48, 14, 7, 7);
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.setColor(has ? Color.BLACK : new Color(90, 90, 104));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(label);
        g2.drawString(label, x + (48 - tw) / 2, y + 10);
    }

    private String roomLabel(String r) {
        switch (r) {
            case "LIVING_ROOM": return "Living Room";
            case "KITCHEN":     return "Kitchen";
            case "GARAGE":      return "Garage";
            case "BEDROOM":     return "Bedroom";
            case "OUTSIDE":     return "Backyard";
            case "MOMS_ROOM":   return "Mom's Room";
            case "BATHROOM":    return "Bathroom";
            case "BASEMENT":    return "Basement";
            case "PHONE":       return "Phone";
            default:            return r;
        }
    }

    private void drawCoreChoreSstrip(Graphics2D g2, int w) {
        int sy = 102, sh = 21;

        g2.setColor(new Color(14, 14, 24));
        g2.fillRect(0, sy, w, sh);
        g2.setColor(new Color(255, 255, 255, 16));
        g2.drawLine(0, sy, w, sy);

        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(new Color(130, 125, 158));
        g2.drawString("CHORES", 6, sy + 14);

        String[][] chores = {
            {"Dishes",   GameState.choreDishes    ? "✓" : (GameState.dishScrubs > 0 ? "…" : "○")},
            {"Trash",    (GameState.choreTrashOut && GameState.choreTrashBagIn) ? "✓"
                         : (GameState.choreTrashOut ? "½" : "○")},
            {"Clean",    GameState.choreHouseClean ? "✓"
                         : ((GameState.choreCleanLR ? 1:0)+(GameState.choreCleanKitchen ? 1:0)
                            +(GameState.choreCleanBedroom ? 1:0)) > 0
                           ? ((GameState.choreCleanLR?1:0)+(GameState.choreCleanKitchen?1:0)
                              +(GameState.choreCleanBedroom?1:0)) + "/3" : "○"},
            {"Lawn",     GameState.choreLawnMowed  ? "✓"
                         : GameState.choreLawnPct > 0 ? ((int)GameState.choreLawnPct) + "%" : "○"},
            {"Homework", GameState.choreHomework   ? "✓"
                         : GameState.choreHomeworkTicks > 0
                           ? (Math.min(100, GameState.choreHomeworkTicks*5)) + "%" : "○"},
            {"Laundry",  GameState.laundryState >= 5 ? "✓"
                         : GameState.laundryState >= 1
                           ? (GameState.laundryState <= 2 ? "wash" : "dry") : "○"},
        };

        int cx = 58;
        for (String[] chore : chores) {
            boolean done = "✓".equals(chore[1]);
            boolean prog = !done && !"○".equals(chore[1]);
            Color bg = done ? new Color(22, 80, 35) : (prog ? new Color(55, 48, 18) : new Color(20, 19, 33));
            Color fg = done ? new Color(80, 215, 110) : (prog ? new Color(220, 185, 55) : new Color(82, 80, 108));
            g2.setColor(bg);
            g2.fillRoundRect(cx, sy + 2, 105, sh - 4, 5, 5);
            g2.setColor(done ? new Color(45, 140, 70) : (prog ? new Color(120, 100, 25) : new Color(34, 32, 52)));
            g2.drawRoundRect(cx, sy + 2, 105, sh - 4, 5, 5);
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(fg);
            g2.drawString(chore[0], cx + 5, sy + 14);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics fm2 = g2.getFontMetrics();
            g2.setColor(done ? new Color(100, 255, 140) : (prog ? new Color(255, 215, 75) : new Color(55, 52, 78)));
            g2.drawString(chore[1], cx + 105 - fm2.stringWidth(chore[1]) - 4, sy + 14);
            cx += 110;
        }

        // Bonus coin preview
        int n = GameState.choreCount();
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(n > 0 ? new Color(255, 215, 60) : new Color(75, 72, 100));
        g2.drawString("+" + (n * 4) + " coins", cx + 2, sy + 14);
    }

    private void drawBonusChoresStrip(Graphics2D g2, int w) {
        int sy = 125, sh = 20;

        g2.setColor(new Color(12, 12, 22));
        g2.fillRect(0, sy, w, sh);
        g2.setColor(new Color(255, 255, 255, 10));
        g2.drawLine(0, sy, w, sy);

        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.setColor(new Color(100, 95, 125));
        g2.drawString("BONUS", 8, sy + 13);

        String[][] bonus = {
            {"Vacuum",  GameState.choreVacuum      ? "✓" : "○"},
            {"Plants",  GameState.choreWaterPlants ? "✓" : "○"},
            {"Mail",    GameState.choreMailIn       ? "✓" : "○"},
            {"Sweep",   GameState.choreSweepKitchen ? "✓" : "○"},
            {"Mop",     GameState.choreMopBathroom  ? "✓" : "○"},
        };

        int cx = 58;
        for (String[] b : bonus) {
            boolean done = "✓".equals(b[1]);
            g2.setColor(done ? new Color(20, 75, 30) : new Color(18, 18, 30));
            g2.fillRoundRect(cx, sy + 2, 88, sh - 4, 5, 5);
            g2.setColor(done ? new Color(40, 120, 55) : new Color(30, 30, 48));
            g2.drawRoundRect(cx, sy + 2, 88, sh - 4, 5, 5);
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(done ? new Color(80, 210, 100) : new Color(80, 78, 105));
            g2.drawString(b[0], cx + 5, sy + 13);
            if (done) {
                g2.setColor(new Color(100, 255, 130));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("✓", cx + 88 - fm.stringWidth("✓") - 4, sy + 13);
            }
            cx += 93;
        }

        // Bonus coins earned from bonus chores
        int bonusCoins = (GameState.choreVacuum ? 1 : 0) + (GameState.choreWaterPlants ? 1 : 0)
                + (GameState.choreMailIn ? 1 : 0) + (GameState.choreSweepKitchen ? 1 : 0)
                + (GameState.choreMopBathroom ? 1 : 0);
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.setColor(bonusCoins > 0 ? new Color(200, 185, 50) : new Color(65, 62, 88));
        g2.drawString("+" + bonusCoins + " earned", cx + 2, sy + 13);
    }

    public void refresh() { repaint(); }
}
