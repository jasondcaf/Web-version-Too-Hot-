package game;

import javax.swing.*;
import java.awt.*;

public class MomsRoomPanel extends RoomPanel {

    private static java.awt.image.BufferedImage CLOSET_IMG = null;
    static { CLOSET_IMG = AssetManager.getRoom("closet_interior"); }

    public MomsRoomPanel() {
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { onEnter(); }
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

    // Layout (bottom strip):
    //   Left  (dresser): x=22,  w=220   rows: y=300, 342
    //   Right (closet):  x=560, w=220   rows: y=300, 342
    private void buildButtons() {
        // ── If hiding, show closet interior only ──────────────────────────
        if (GameState.hidingInCloset) {
            JButton comeOut = makeButton("Come Out of Closet", bx(300), by(300), bx(200), by(40));
            comeOut.setForeground(new Color(200, 180, 255));
            comeOut.addActionListener(e -> { GameState.hidingInCloset = false; onEnter(); });
            add(comeOut);
            return;
        }

        // ── LEFT col — Dresser ────────────────────────────────────────────

        // Row 1: Roach spray in drawer
        if (GameState.roachSprayInDrawer) {
            JButton drawerBtn = makeButton("Search Dresser Drawer", bx(22), by(300), bx(220), by(36));
            drawerBtn.setForeground(new Color(180, 255, 180));
            drawerBtn.addActionListener(e -> {
                GameState.hasRoachSpray      = true;
                GameState.roachSprayInDrawer = false;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Found roach spray in the dresser drawer! Head to the garage.");
            });
            add(drawerBtn);
        } else if (GameState.hasRoachSpray) {
            JButton got = makeButton("Roach Spray: carrying", bx(22), by(300), bx(220), by(36));
            got.setEnabled(false);
            add(got);
        } else {
            JButton used = makeButton("Drawer (already checked)", bx(22), by(300), bx(220), by(36));
            used.setEnabled(false);
            add(used);
        }

        // Row 2: Flashlight when power is out, otherwise Rest on Bed
        if (GameState.powerOut && !GameState.hasMomFlashlight) {
            JButton flashBtn = makeButton("Search Bottom Drawer", bx(22), by(342), bx(220), by(36));
            flashBtn.setForeground(new Color(255, 220, 80));
            flashBtn.addActionListener(e -> {
                GameState.hasMomFlashlight = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Found a flashlight in the bottom drawer! Much less scary.");
            });
            add(flashBtn);
        } else if (GameState.hasMomFlashlight) {
            JButton taken = makeButton("Flashlight: carrying it", bx(22), by(342), bx(220), by(36));
            taken.setEnabled(false);
            add(taken);
        } else {
            JButton restBtn = makeButton("Rest on Mom's Bed", bx(22), by(342), bx(220), by(36));
            restBtn.setForeground(new Color(200, 210, 255));
            restBtn.addActionListener(e -> {
                GameState.stress      = GameLoop.clamp(GameState.stress - 18f, 0, 100);
                GameState.temperature = GameLoop.clamp(GameState.temperature - 3f, 0, 100);
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Mom's bed is SO comfy... stress way down.");
            });
            add(restBtn);
        }

        // ── RIGHT col — Closet ────────────────────────────────────────────

        // Row 1: Hide in Mom's Closet
        JButton hideBtn = makeButton("Hide in Mom's Closet", bx(560), by(300), bx(220), by(36));
        hideBtn.setForeground(new Color(180, 160, 220));
        hideBtn.addActionListener(e -> {
            GameState.hidingInCloset = true;
            onEnter();
            GameFrame.getInstance().getGamePanel()
                    .showMessage("You hid in Mom's closet. Her clothes smell nice... stress fading.");
        });
        add(hideBtn);

        // Row 2: Loose change on nightstand
        if (!GameState.looseChangeFound_MomsRoom) {
            JButton changeBtn = makeButton("Check Nightstand", bx(560), by(342), bx(220), by(36));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6); // 0-5
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_MomsRoom = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) on mom's nightstand!" : "Nothing on the nightstand.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        float sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float sy = getHeight() <= 0 ? 1f : getHeight() / 455f;
        g2.scale(sx, sy);
        int W = 800, H = 455;

        if (GameState.hidingInCloset) {
            drawClosetInterior(g2, W, H);
            g2.dispose();
            return;
        }

        drawBg(g2, "moms_room", W, H);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // ── Badges ───────────────────────────────────────────────────────
        int badgeY = 10;
        if (GameState.powerOut) {
            drawBadge(g2, "POWER OUT", W - 165, badgeY, new Color(155, 95, 0, 220), Color.WHITE);
        }
        if (GameState.roachSprayInDrawer) {
            drawBadge(g2, "★ Dresser: spray inside!", 10, badgeY, new Color(50, 120, 50, 200), Color.WHITE);
            badgeY += 28;
        }
        if (GameState.powerOut && !GameState.hasMomFlashlight) {
            drawBadge(g2, "★ Flashlight in bottom drawer!", 10, badgeY, new Color(180, 140, 20, 220), Color.WHITE);
        }

        g2.dispose();
    }

    private void drawClosetInterior(Graphics2D g2, int W, int H) {
        if (CLOSET_IMG != null) {
            g2.drawImage(CLOSET_IMG, 0, 0, W, H, null);
        } else {
            // Fallback: plain dark background
            g2.setColor(new Color(10, 6, 4));
            g2.fillRect(0, 0, W, H);
        }
        // Subtle darkening overlay so text pops
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRect(0, 0, W, H);
        // Text overlay
        g2.setFont(new Font("SansSerif", Font.BOLD, 21));
        g2.setColor(new Color(255, 230, 180, 210));
        String msg = "Hiding in Mom's closet...";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H - 118);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g2.setColor(new Color(200, 185, 160, 175));
        String sub = "Stress is fading.  Her clothes smell nice.";
        fm = g2.getFontMetrics();
        g2.drawString(sub, (W - fm.stringWidth(sub)) / 2, H - 92);
    }
}
