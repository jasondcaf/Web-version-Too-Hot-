package game;

import javax.swing.*;
import java.awt.*;

public class BasementPanel extends RoomPanel {

    private static java.awt.image.BufferedImage LAUNDRY_BG = null;
    static { LAUNDRY_BG = AssetManager.getRoom("laundry_room"); }

    public BasementPanel() {
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

    private void buildButtons() {

        switch (GameState.laundryState) {
            case 0: { // Idle — start washer (costs 1 detergent)
                if (GameState.laundryDetergent > 0) {
                    JButton startBtn = makeButton(
                            "Put Laundry in Washer (soap: " + GameState.laundryDetergent + ")",
                            bx(210), by(290), bx(380), by(38));
                    startBtn.setForeground(new Color(180, 220, 255));
                    startBtn.addActionListener(e -> {
                        GameState.laundryState    = 1;
                        GameState.laundryWashTicks = 0;
                        GameState.laundryDetergent--;
                        onEnter();
                        GameFrame.getInstance().getGamePanel()
                                .showMessage("Washer running! Come back when it's done.");
                    });
                    add(startBtn);
                } else {
                    JButton noSoapBtn = makeButton("Out of Detergent! Order via phone (2 coins)",
                            bx(170), by(290), bx(460), by(38));
                    noSoapBtn.setEnabled(false);
                    noSoapBtn.setForeground(new Color(255, 120, 80));
                    add(noSoapBtn);
                }
                break;
            }
            case 1: { // Washing in progress
                JButton waitBtn = makeButton("Washer running... (" +
                        Math.min(100, GameState.laundryWashTicks * 10) + "%)",
                        bx(260), by(290), bx(280), by(38));
                waitBtn.setEnabled(false);
                add(waitBtn);
                break;
            }
            case 2: { // Wash done — move to dryer
                JButton dryBtn = makeButton("Move to Dryer!", bx(260), by(290), bx(280), by(38));
                dryBtn.setForeground(new Color(255, 210, 100));
                dryBtn.addActionListener(e -> {
                    GameState.laundryState    = 3;
                    GameState.laundryDryTicks = 0;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Dryer running! Come back when it's done.");
                });
                add(dryBtn);
                break;
            }
            case 3: { // Drying in progress
                JButton dryWaitBtn = makeButton("Dryer running... (" +
                        Math.min(100, GameState.laundryDryTicks * 10) + "%)",
                        bx(260), by(290), bx(280), by(38));
                dryWaitBtn.setEnabled(false);
                add(dryWaitBtn);
                break;
            }
            case 4: { // Dry done — fold and put away
                JButton foldBtn = makeButton("Fold & Put Away Clothes!", bx(220), by(290), bx(360), by(38));
                foldBtn.setForeground(new Color(140, 255, 160));
                foldBtn.addActionListener(e -> {
                    GameState.laundryState = 5;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Laundry done and put away! Chore complete.");
                });
                add(foldBtn);
                break;
            }
            case 5: { // Previous load done
                JButton doneBtn = makeButton("Laundry Done ✓", bx(260), by(290), bx(280), by(38));
                doneBtn.setEnabled(false);
                add(doneBtn);
                // If new dirty clothes were added (from shower), allow starting another load
                if (GameState.dirtyClothesAdded) {
                    JButton newLoadBtn = makeButton("Wash New Load", bx(260), by(340), bx(280), by(38));
                    newLoadBtn.setForeground(new Color(180, 220, 255));
                    newLoadBtn.addActionListener(e -> {
                        GameState.laundryState     = 0;
                        GameState.laundryWashTicks  = 0;
                        GameState.laundryDryTicks   = 0;
                        GameState.dirtyClothesAdded = false;
                        onEnter();
                        GameFrame.getInstance().getGamePanel()
                                .showMessage("Ready to wash the new dirty clothes!");
                    });
                    add(newLoadBtn);
                }
                break;
            }
        }

        // Detergent pickup (delivered by phone)
        if (GameState.detergentArrived) {
            JButton pickupBtn = makeButton("Pick Up Detergent (+2 uses)!", bx(50), by(390), bx(240), by(36));
            pickupBtn.setForeground(new Color(100, 220, 255));
            pickupBtn.addActionListener(e -> {
                GameState.laundryDetergent += 2;
                GameState.detergentOrdered  = false;
                GameState.detergentArrived  = false;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Got the detergent! +" + 2 + " washes available.");
            });
            add(pickupBtn);
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

        drawBasement(g2, W, H);

        // Show dirty clothes basket if a new load is waiting
        if (GameState.dirtyClothesAdded && (GameState.laundryState == 0 || GameState.laundryState == 5)) {
            drawBadge(g2, "Dirty clothes in basket! Start the wash.", 10, 36,
                    new Color(180, 100, 20, 220), Color.WHITE);
        }
        // Detergent arrived badge
        if (GameState.detergentArrived) {
            drawBadge(g2, "Detergent arrived — pick it up!", 10, 62,
                    new Color(30, 130, 200, 220), Color.WHITE);
        }
        // Detergent count warning
        if (GameState.laundryDetergent == 0 && !GameState.detergentArrived && !GameState.detergentOrdered) {
            drawBadge(g2, "No detergent left! Order via Phone.", 10, 88,
                    new Color(160, 40, 20, 220), Color.WHITE);
        }

        // Dark strip for buttons
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, H - 120, W, 120);

        // Laundry progress info
        if (GameState.laundryState >= 1 && GameState.laundryState <= 4) {
            String phase = GameState.laundryState <= 2 ? "WASHING" : "DRYING";
            int ticks = GameState.laundryState == 1 ? GameState.laundryWashTicks
                    : (GameState.laundryState == 3 ? GameState.laundryDryTicks : 10);
            int pct = Math.min(100, ticks * 10);
            // Progress bar
            int bw = 300, bh = 16, bx2 = W/2 - bw/2, by2 = H - 145;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(bx2 - 2, by2 - 2, bw + 4, bh + 4, 6, 6);
            g2.setColor(new Color(30, 50, 80));
            g2.fillRoundRect(bx2, by2, bw, bh, 4, 4);
            g2.setColor(GameState.laundryState <= 2 ? new Color(60, 150, 230) : new Color(255, 180, 50));
            g2.fillRoundRect(bx2, by2, bw * pct / 100, bh, 4, 4);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            String lbl = phase + ": " + pct + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, bx2 + (bw - fm.stringWidth(lbl))/2, by2 + 12);
        }

        // Badges
        if (GameState.powerOut)
            drawBadge(g2, "POWER OUT", W - 165, 10, new Color(155, 95, 0, 220), Color.WHITE);
        if (GameState.laundryState >= 5)
            drawBadge(g2, "Laundry Done ✓", 10, 10, new Color(20, 120, 45, 210), Color.WHITE);

        g2.dispose();
    }

    private void drawBasement(Graphics2D g2, int W, int H) {
        // Use the laundry_room asset if loaded
        if (LAUNDRY_BG != null) {
            g2.drawImage(LAUNDRY_BG, 0, 0, W, H, null);
            // Dark overlay to keep button strip legible
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRect(0, 0, W, H);
            return;
        }
        // Fallback: Concrete floor
        GradientPaint floor = new GradientPaint(0, H - 80, new Color(55, 52, 48),
                0, H, new Color(38, 35, 32));
        g2.setPaint(floor);
        g2.fillRect(0, H - 80, W, 80);
        g2.setPaint(null);
        // Floor cracks
        g2.setColor(new Color(30, 28, 25, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(120, H-70, 210, H-40);
        g2.drawLine(420, H-75, 500, H-35);
        g2.drawLine(650, H-65, 720, H-50);
        g2.setStroke(new BasicStroke(1));

        // Back wall (concrete grey)
        GradientPaint wall = new GradientPaint(0, 0, new Color(62, 58, 55),
                0, H - 80, new Color(48, 44, 40));
        g2.setPaint(wall);
        g2.fillRect(0, 0, W, H - 80);
        g2.setPaint(null);

        // Concrete block texture
        g2.setColor(new Color(40, 36, 33, 80));
        g2.setStroke(new BasicStroke(1f));
        for (int y = 20; y < H - 80; y += 50) {
            g2.drawLine(0, y, W, y);
            for (int x = (y/50 % 2 == 0) ? 0 : 70; x < W; x += 140)
                g2.drawLine(x, y, x, y + 50);
        }
        g2.setStroke(new BasicStroke(1));

        // Overhead pipes
        g2.setColor(new Color(90, 80, 70));
        g2.fillRect(0, 12, W, 14);
        g2.setColor(new Color(70, 60, 55));
        g2.fillRect(0, 12, W, 4);
        for (int x = 80; x < W; x += 180) {
            g2.setColor(new Color(75, 65, 60));
            g2.fillRoundRect(x - 10, 8, 20, 22, 4, 4);
        }
        // Water pipe (blue)
        g2.setColor(new Color(55, 80, 110));
        g2.fillRect(0, 32, W, 8);

        // Bare bulb light
        g2.setColor(new Color(255, 240, 160, 30));
        g2.fillOval(W/2 - 60, 35, 120, 100);
        g2.setColor(new Color(255, 240, 160, 60));
        g2.fillOval(W/2 - 30, 38, 60, 55);
        g2.setColor(new Color(255, 245, 200));
        g2.fillOval(W/2 - 6, 40, 12, 16);
        g2.setColor(new Color(80, 70, 60));
        g2.fillRect(W/2 - 1, 26, 3, 16);

        // ── Washer (left) ─────────────────────────────────────────────────
        drawAppliance(g2, 140, H - 260, true);

        // ── Dryer (right) ─────────────────────────────────────────────────
        drawAppliance(g2, 480, H - 260, false);

        // Labels
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(200, 195, 185));
        g2.drawString("WASHER", 168, H - 95);
        g2.drawString("DRYER", 510, H - 95);
    }

    private void drawAppliance(Graphics2D g2, int x, int y, boolean isWasher) {
        int w = 150, h = 170;
        boolean active = isWasher ? (GameState.laundryState == 1)
                                  : (GameState.laundryState == 3);
        boolean done   = isWasher ? (GameState.laundryState >= 2)
                                  : (GameState.laundryState >= 4);

        // Body
        GradientPaint body = new GradientPaint(x, y, new Color(210, 210, 215),
                x + w, y + h, new Color(175, 175, 182));
        g2.setPaint(body);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setPaint(null);
        g2.setColor(new Color(140, 140, 148));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setStroke(new BasicStroke(1));

        // Top panel
        g2.setColor(new Color(185, 185, 192));
        g2.fillRoundRect(x + 5, y + 5, w - 10, 30, 4, 4);

        // Status light
        Color lightCol = active ? new Color(0, 220, 80) : (done ? new Color(255, 200, 0) : new Color(60, 60, 65));
        g2.setColor(lightCol);
        g2.fillOval(x + w - 22, y + 12, 10, 10);
        if (active) {
            g2.setColor(new Color(0, 255, 100, 80));
            g2.fillOval(x + w - 26, y + 8, 18, 18);
        }

        // Door (circle window)
        g2.setColor(new Color(40, 40, 50));
        g2.fillOval(x + 22, y + 50, 106, 100);
        GradientPaint glass = new GradientPaint(x + 30, y + 55,
                active ? new Color(100, 160, 220, 200) : new Color(30, 30, 40, 220),
                x + 110, y + 140,
                active ? new Color(60, 120, 190, 180) : new Color(20, 20, 30, 180));
        g2.setPaint(glass);
        g2.fillOval(x + 28, y + 56, 94, 88);
        g2.setPaint(null);

        // Spinning drum lines if active
        if (active) {
            long t = System.currentTimeMillis();
            double rot = (t * 0.003) % (2 * Math.PI);
            int dcx = x + 75, dcy = y + 100;
            g2.setColor(new Color(180, 210, 240, 140));
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 3; i++) {
                double a = rot + i * Math.PI * 2 / 3;
                g2.drawLine(dcx, dcy,
                        dcx + (int)(Math.cos(a) * 38),
                        dcy + (int)(Math.sin(a) * 38));
            }
            g2.setStroke(new BasicStroke(1));
        }
        // Done checkmark
        if (done && !active) {
            g2.setColor(new Color(80, 220, 100, 200));
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString("✓", x + 60, y + 112);
        }

        // Door handle
        g2.setColor(new Color(160, 160, 168));
        g2.fillRoundRect(x + 115, y + 92, 14, 22, 4, 4);

        // Knobs
        g2.setColor(new Color(155, 155, 162));
        g2.fillOval(x + 12, y + 10, 18, 18);
        g2.fillOval(x + 34, y + 10, 18, 18);

        // Type label
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(80, 80, 90));
        g2.drawString(isWasher ? "WASH" : "DRY", x + 56, y + 23);
    }
}
