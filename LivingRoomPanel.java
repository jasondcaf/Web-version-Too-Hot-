package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LivingRoomPanel extends RoomPanel {

    private static java.awt.image.BufferedImage SQUIRREL_IMG = null;
    static { SQUIRREL_IMG = AssetManager.getRoom("squirrel"); }

    private JButton windowBtn, acBtn, squirrelBtn;

    public LivingRoomPanel() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onEnter(); }
        });
        onEnter();
    }

    private int bx(int v) { int w = getWidth();  return w <= 0 ? v : Math.round(v * w / 800f); }
    private int by(int v) { int h = getHeight(); return h <= 0 ? v : Math.round(v * h / 455f); }

    @Override
    public void onEnter() {
        removeAll();
        buildButtons();
        revalidate();
        repaint();
    }

    private void buildButtons() {
        // Layout:
        //   Row 0 (y=300): Squirrel alert OR TV/Vacuum/Plants row
        //   Row 1 (y=342): Window | Tidy LR | Check Cushions | AC

        // ── Row 0 — Squirrel (full-width) OR activity row ─────────────────
        if (GameState.squirrelPresent) {
            String shooLabel = "SHOO " + GameState.squirrelCount + " SQUIRREL"
                             + (GameState.squirrelCount > 1 ? "S!" : "!");
            squirrelBtn = makeButton(shooLabel, bx(200), by(300), bx(400), by(38));
            squirrelBtn.setForeground(new Color(255, 130, 30));
            squirrelBtn.addActionListener(e -> {
                int count = GameState.squirrelCount; // capture before reset
                GameState.squirrelPresent = false;
                GameState.squirrelCount   = 1;
                GameState.squirrelTicks   = 0;
                float stressPenalty = 10f + count * 5f;
                GameState.stress = GameLoop.clamp(GameState.stress + stressPenalty, 0, 100);
                GameLoop.rewardAction(1, "Squirrels shoo'd!");
                onEnter();
            });
            add(squirrelBtn);
        } else {
            // TV button
            String tvLabel = GameState.tvWatching ? "Stop Watching TV" : "Watch TV / Chill";
            JButton tvBtn = makeButton(tvLabel, bx(18), by(300), bx(192), by(36));
            tvBtn.setForeground(GameState.tvWatching ? new Color(255, 180, 80) : new Color(200, 200, 255));
            tvBtn.addActionListener(e -> {
                GameState.tvWatching = !GameState.tvWatching;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage(
                    GameState.tvWatching ? "Chilling in front of the TV. Stress fading..."
                                         : "Turned off the TV.");
            });
            add(tvBtn);

            // Vacuum (bonus chore)
            if (!GameState.choreVacuum) {
                JButton vacBtn = makeButton("Vacuum", bx(218), by(300), bx(178), by(36));
                vacBtn.setForeground(new Color(255, 220, 120));
                vacBtn.addActionListener(e -> {
                    GameState.choreVacuum = true;
                    GameState.coins++;
                    onEnter();
                    GameFrame.getInstance().getGamePanel().showMessage("Vacuumed the living room! +1 coin.");
                });
                add(vacBtn);
            } else {
                JButton vacDone = makeButton("Vacuumed ✓", bx(218), by(300), bx(178), by(36));
                vacDone.setEnabled(false);
                add(vacDone);
            }

            // Mittens mess clean-up (if mess is in LR — re-tidy button already reappears automatically)
            if ("LIVING_ROOM".equals(GameState.mittensMessRoom)) {
                JButton messBtn = makeButton("Clean Mittens' Mess", bx(590), by(300), bx(192), by(36));
                messBtn.setForeground(new Color(255, 120, 200));
                messBtn.addActionListener(e -> {
                    GameState.mittensMessRoom = "";
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Cleaned up Mittens' mess. Now re-tidy the room!");
                });
                add(messBtn);
            }
        }

        // ── Row 1 — 4-column action strip ────────────────────────────────
        // Window (x=18, w=170)
        if (GameState.windowBroken) {
            windowBtn = makeButton("Window Broken (" + (GameState.windowBrokenCooldown / 2) + "s)",
                    bx(18), by(342), bx(170), by(36));
            windowBtn.setEnabled(false);
            windowBtn.setForeground(new Color(180, 90, 90));
        } else {
            String wLabel = GameState.windowOpen ? "Close Window" : "Open Window";
            windowBtn = makeButton(wLabel, bx(18), by(342), bx(170), by(36));
            windowBtn.setForeground(new Color(100, 180, 255));
            windowBtn.addActionListener(e -> {
                GameState.windowOpen = !GameState.windowOpen;
                if (!GameState.windowOpen) GameState.windowOpenTicks = 0;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage(
                        GameState.windowOpen ? "Window open — breeze cools you down!" : "Window closed.");
            });
        }
        add(windowBtn);

        // Tidy Living Room (x=200, w=165) — chore
        if (!GameState.choreCleanLR) {
            JButton tidyBtn = makeButton("Tidy Living Room", bx(200), by(342), bx(165), by(36));
            tidyBtn.setForeground(new Color(255, 220, 120));
            tidyBtn.addActionListener(e -> {
                GameState.choreCleanLR = true;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Living room is tidy!");
            });
            add(tidyBtn);
        }

        // Check Cushions (x=380, w=165) — loose change 0-5
        if (!GameState.looseChangeFound_LivingRoom) {
            JButton changeBtn = makeButton("Check Cushions", bx(380), by(342), bx(165), by(36));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6); // 0-5
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_LivingRoom = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) in the cushions!" : "Nothing in the cushions today.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }

        // AC fix/status (x=558, w=224)
        if (GameState.acBroken) {
            String label = GameState.hasWrench ? "Fix AC (wrench)" : "AC Broken (need wrench)";
            acBtn = makeButton(label, bx(558), by(342), bx(224), by(36));
            acBtn.setForeground(GameState.hasWrench ? new Color(100, 255, 130) : new Color(255, 100, 100));
            if (GameState.hasWrench) {
                acBtn.addActionListener(e -> {
                    GameState.acBroken  = false;
                    GameState.hasWrench = false;
                    GameLoop.rewardAction(3, "AC fixed!");
                    onEnter();
                });
            }
        } else {
            acBtn = makeButton("AC Running ✓", bx(558), by(342), bx(224), by(36));
            acBtn.setEnabled(false);
        }
        add(acBtn);

        // ── Thermostat row (x=18, w=762, y=384) ─────────────────────────
        if (!GameState.acBroken && !GameState.powerOut) {
            String[] settingNames = {"OFF", "Eco", "Normal", "BLAST"};
            Color[]  settingColors = {new Color(200,100,100), new Color(100,200,140),
                                      new Color(100,180,255), new Color(255,200,60)};
            int btnW = 180, btnH = 30, startX = 20;
            for (int s = 0; s < 4; s++) {
                final int setting = s;
                String lbl = "AC: " + settingNames[s] + (s == GameState.acSetting ? " ✓" : "");
                JButton tBtn = makeButton(lbl, bx(startX + s * (btnW + 8)), by(384), bx(btnW), by(btnH));
                tBtn.setForeground(settingColors[s]);
                if (s == GameState.acSetting) tBtn.setEnabled(false);
                else tBtn.addActionListener(e -> {
                    GameState.acSetting = setting;
                    String warn = setting == 3 ? " WARNING: High power-out risk!" : "";
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("AC set to " + settingNames[setting] + "." + warn);
                    onEnter();
                });
                add(tBtn);
            }
        }

        // ── Check Cushions — coin RNG 0-5 ────────────────────────────────
        // (already added above — handled in same block)
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);

        float _sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float _sy = getHeight() <= 0 ? 1f : getHeight() / 455f;
        g2.scale(_sx, _sy);
        int W = 800, H = 455;

        // ── Real background image ──────────────────────────────────────────
        drawBg(g2, "living_room", W, H);

        // ── Semi-transparent bottom strip so buttons are readable ──────────
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // ── Status badges ──────────────────────────────────────────────────
        int bady = 10;
        if (GameState.powerOut) {
            drawBadge(g2, "POWER OUT", W - 195, bady, new Color(155, 95, 0, 220), Color.WHITE);
            bady += 28;
        }
        if (GameState.acBroken) {
            drawBadge(g2, "AC  BROKEN", W - 195, bady, new Color(165, 28, 28, 220), Color.WHITE);
        } else if (!GameState.powerOut) {
            drawBadge(g2, "AC  ✓", W - 105, bady, new Color(18, 110, 45, 200), Color.WHITE);
        }
        if (GameState.windowOpen) {
            drawBadge(g2, "Window OPEN", 10, 10, new Color(25, 90, 150, 210), Color.WHITE);
        }
        if (GameState.windowBroken) {
            drawBadge(g2, "Window Slider BROKEN", 10, GameState.windowOpen ? 36 : 10,
                    new Color(160, 40, 40, 220), Color.WHITE);
        }
        // AC setting indicator
        if (!GameState.acBroken && !GameState.powerOut) {
            String[] names = {"OFF","Eco","Normal","BLAST"};
            Color[]  cols  = {new Color(220,80,80,200), new Color(60,180,100,200),
                               new Color(80,160,240,200), new Color(255,190,40,200)};
            drawBadge(g2, "AC: " + names[GameState.acSetting], W - 135, bady,
                    cols[GameState.acSetting], Color.WHITE);
        }
        // ── Squirrel swarm ────────────────────────────────────────────────
        if (GameState.squirrelPresent) {
            int[] squirrelXs = {490, 400, 560, 350};
            int[] squirrelYs = {H-235, H-200, H-215, H-190};
            for (int s = 0; s < Math.min(GameState.squirrelCount, 4); s++)
                drawSquirrel(g2, squirrelXs[s], squirrelYs[s]);
        }

        // ── House clean badge ─────────────────────────────────────────────
        if (GameState.choreCleanLR)
            drawBadge(g2, "LR Tidied ✓", 10, GameState.windowOpen ? 36 : 10,
                    new Color(20, 120, 45, 210), Color.WHITE);

        // ── Mittens mess badge ────────────────────────────────────────────
        if ("LIVING_ROOM".equals(GameState.mittensMessRoom))
            drawBadge(g2, "Mittens made a mess here!", 10, H - 115,
                    new Color(160, 60, 160, 220), Color.WHITE);

        // ── TV watching badge ─────────────────────────────────────────────
        if (GameState.tvWatching)
            drawBadge(g2, "Watching TV...", W/2 - 60, 10, new Color(50, 50, 140, 210), Color.WHITE);

        g2.dispose();
    }

    // ── Closet hide view ──────────────────────────────────────────────────
    private void drawClosetInterior(Graphics2D g2, int W, int H) {
        g2.setColor(new Color(10, 6, 4));
        g2.fillRect(0, 0, W, H);
        GradientPaint crack = new GradientPaint(0, 0, new Color(255, 235, 180, 185), 90, 0, new Color(0, 0, 0, 0));
        g2.setPaint(crack);
        g2.fillRect(0, 0, 90, H);
        g2.setPaint(null);
        g2.setColor(new Color(255, 240, 200, 225));
        g2.fillRect(0, 0, 3, H);
        g2.setColor(new Color(28, 18, 12));
        g2.fillRoundRect(110, 25, 58, H - 55, 8, 8);
        g2.fillRoundRect(200, 48, 50, H - 75, 8, 8);
        g2.fillRoundRect(295, 22, 55, H - 48, 8, 8);
        g2.fillRoundRect(400, 40, 48, H - 65, 8, 8);
        g2.setColor(new Color(75, 65, 55));
        g2.fillRect(95, 16, 310, 8);
        for (int hx = 125; hx < 405; hx += 52) {
            g2.setColor(new Color(105, 92, 72));
            g2.setStroke(new BasicStroke(2));
            g2.drawArc(hx - 6, 8, 12, 14, 0, 180);
            g2.setStroke(new BasicStroke(1));
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 21));
        g2.setColor(new Color(255, 230, 180, 200));
        String msg = "Hiding in the closet...";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H - 118);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g2.setColor(new Color(180, 170, 150, 165));
        String sub = "Stress is fading.  The cat can't find you here.";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (W - fm.stringWidth(sub)) / 2, H - 92);
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillRect(0, 0, W, H);
    }

    // ── Closet door drawn on the wall ─────────────────────────────────────
    private void drawClosetDoor(Graphics2D g2, int x, int y) {
        int dw = 70, dh = 155;
        // Door frame shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 3, y + 3, dw, dh, 4, 4);
        // Door body
        GradientPaint dp = new GradientPaint(x, y, new Color(110, 85, 62), x + dw, y + dh, new Color(80, 58, 38));
        g2.setPaint(dp);
        g2.fillRoundRect(x, y, dw, dh, 4, 4);
        g2.setPaint(null);
        // Door panels (raised look)
        g2.setColor(new Color(125, 98, 72));
        g2.fillRoundRect(x + 6, y + 8,  dw - 12, (dh/2) - 12, 3, 3);
        g2.fillRoundRect(x + 6, y + dh/2 + 4, dw - 12, (dh/2) - 12, 3, 3);
        g2.setColor(new Color(65, 45, 28));
        g2.drawRoundRect(x + 6, y + 8,  dw - 12, (dh/2) - 12, 3, 3);
        g2.drawRoundRect(x + 6, y + dh/2 + 4, dw - 12, (dh/2) - 12, 3, 3);
        // Door knob
        g2.setColor(new Color(200, 170, 90));
        g2.fillOval(x + dw - 16, y + dh/2 - 5, 10, 10);
        g2.setColor(new Color(160, 130, 60));
        g2.drawOval(x + dw - 16, y + dh/2 - 5, 10, 10);
        // Frame
        g2.setColor(new Color(55, 38, 22));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(x, y, dw, dh, 4, 4);
        g2.setStroke(new BasicStroke(1f));
        // Small label if item inside
        if (GameState.roachSprayInDrawer) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(new Color(255, 220, 80, 220));
            g2.drawString("!", x + dw/2 - 2, y - 5);
        }
    }

    private void drawSquirrel(Graphics2D g2, int x, int y) {
        int sw = 95, sh = 95;
        if (SQUIRREL_IMG != null) {
            // Draw watercolor squirrel image (PNG has transparent background)
            g2.drawImage(SQUIRREL_IMG, x, y - sh, sw, sh, null);
        } else {
            // G2D fallback
            g2.setColor(new Color(145, 105, 62));
            g2.fillOval(x, y, 40, 30);
            g2.fillOval(x + 28, y - 10, 22, 20);
            g2.setColor(new Color(165, 128, 75));
            g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(x - 15, y - 5, 35, 40, 200, 180);
            g2.setStroke(new BasicStroke(1));
            g2.setColor(Color.BLACK);
            g2.fillOval(x + 38, y - 4, 5, 5);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 36, y + 7, 5, 6);
        }
        // "SQUIRREL!" badge above
        drawBadge(g2, "SQUIRREL!", x - 5, y - sh - 8, new Color(200, 80, 0, 220), Color.WHITE);
    }
}
