package game;

import javax.swing.*;
import java.awt.*;

public class BathroomPanel extends RoomPanel {

    public BathroomPanel() {
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

    // Layout (bottom strip, 2 columns):
    //   Left  (shower/sink):    x=22,  w=220   rows: y=300, 342
    //   Right (medicine/shelf): x=558, w=220   rows: y=300, 342
    private void buildButtons() {
        // ── LEFT — Shower chain ───────────────────────────────────────────
        if (!GameState.showeredToday) {
            JButton showerBtn = makeButton("Take a Cold Shower", bx(22), by(300), bx(220), by(36));
            showerBtn.setForeground(new Color(100, 210, 255));
            showerBtn.addActionListener(e -> {
                GameState.temperature    = GameLoop.clamp(GameState.temperature - 22f, 0, 100);
                GameState.stress         = GameLoop.clamp(GameState.stress - 10f, 0, 100);
                GameState.thirst         = GameLoop.clamp(GameState.thirst + 6f, 0, 100);
                GameState.showeredToday  = true;
                GameState.needsTowelDry  = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Cold shower! Temp way down. Now dry off in the bedroom.");
            });
            add(showerBtn);
        } else if (GameState.needsTowelDry) {
            JButton showerDone = makeButton("Shower done - Dry off!", bx(22), by(300), bx(220), by(36));
            showerDone.setEnabled(false);
            showerDone.setForeground(new Color(180, 220, 255));
            add(showerDone);
        } else {
            JButton showerDone = makeButton("Showered ✓", bx(22), by(300), bx(220), by(36));
            showerDone.setEnabled(false);
            add(showerDone);
        }

        // ── LEFT row 2 — Sink ─────────────────────────────────────────────
        JButton sinkBtn = makeButton("Drink From Sink", bx(22), by(342), bx(220), by(36));
        sinkBtn.setForeground(new Color(160, 225, 255));
        sinkBtn.addActionListener(e -> {
            float amount = GameState.hasBigBottle ? 30f : 18f;
            GameState.thirst = GameLoop.clamp(GameState.thirst + amount, 0, 100);
            GameFrame.getInstance().getGamePanel()
                    .showMessage("Drank some water from the bathroom sink.");
        });
        add(sinkBtn);

        // ── RIGHT row 1 — Medicine cabinet ───────────────────────────────
        if (!GameState.medicineUsed) {
            JButton medBtn = makeButton("Medicine Cabinet", bx(558), by(300), bx(220), by(36));
            medBtn.setForeground(new Color(180, 255, 180));
            medBtn.addActionListener(e -> {
                GameState.stress       = GameLoop.clamp(GameState.stress - 28f, 0, 100);
                GameState.medicineUsed = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Took something from the medicine cabinet. Feeling much calmer.");
            });
            add(medBtn);
        } else {
            JButton medDone = makeButton("Cabinet used today", bx(558), by(300), bx(220), by(36));
            medDone.setEnabled(false);
            add(medDone);
        }

        // ── RIGHT row 2 — Loose change on shelf (0-5) ────────────────────
        if (!GameState.looseChangeFound_Bathroom) {
            JButton changeBtn = makeButton("Check Cabinet Shelf", bx(558), by(342), bx(220), by(36));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6); // 0-5
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_Bathroom = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) on the cabinet shelf!" : "Nothing on the shelf today.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }

        // ── LEFT row 3 — Mop Bathroom (bonus chore) ───────────────────────
        if (!GameState.choreMopBathroom) {
            JButton mopBtn = makeButton("Mop Bathroom", bx(22), by(384), bx(220), by(30));
            mopBtn.setForeground(new Color(100, 200, 255));
            mopBtn.addActionListener(e -> {
                GameState.choreMopBathroom = true;
                GameState.coins++;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Bathroom mopped! +1 coin.");
            });
            add(mopBtn);
        } else {
            JButton mopDone = makeButton("Mopped ✓", bx(22), by(384), bx(220), by(30));
            mopDone.setEnabled(false);
            add(mopDone);
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

        drawBg(g2, "bathroom", W, H);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // ── Badges ───────────────────────────────────────────────────────
        if (GameState.powerOut)
            drawBadge(g2, "POWER OUT", W - 165, 10, new Color(155, 95, 0, 220), Color.WHITE);
        if (GameState.medicineUsed)
            drawBadge(g2, "Cabinet used ✓", 10, 10, new Color(40, 110, 40, 210), Color.WHITE);

        g2.dispose();
    }

    // ── Shower stall ──────────────────────────────────────────────────────
    private void drawShower(Graphics2D g2, int x, int y) {
        // Stall walls
        g2.setColor(new Color(195, 215, 230));
        g2.fillRoundRect(x, y, 170, 130, 10, 10);
        g2.setColor(new Color(165, 190, 210));
        g2.drawRoundRect(x, y, 170, 130, 10, 10);
        // Tile grid
        g2.setColor(new Color(180, 200, 218, 120));
        g2.setStroke(new BasicStroke(0.5f));
        for (int tx = x + 20; tx < x + 170; tx += 20)
            g2.drawLine(tx, y, tx, y + 130);
        for (int ty = y + 20; ty < y + 130; ty += 20)
            g2.drawLine(x, ty, x + 170, ty);
        g2.setStroke(new BasicStroke(1));
        // Shower head pipe
        g2.setColor(new Color(172, 178, 188));
        g2.fillRect(x + 135, y + 15, 6, 40);
        // Shower head disk
        g2.setColor(new Color(155, 162, 172));
        g2.fillOval(x + 118, y + 50, 40, 14);
        g2.setColor(new Color(130, 138, 150));
        g2.drawOval(x + 118, y + 50, 40, 14);
        // Water droplets
        g2.setColor(new Color(100, 170, 240, 180));
        long t = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            int dx = (int)(Math.abs(Math.sin(t * 0.003 + i)) * 30);
            int dy = (int)(((t * 0.06 + i * 20) % 60));
            g2.fillOval(x + 120 + dx, y + 64 + dy, 3, 5);
        }
        // Drain
        g2.setColor(new Color(130, 135, 145));
        g2.fillOval(x + 72, y + 118, 26, 10);
        g2.setColor(new Color(180, 185, 195));
        g2.drawOval(x + 72, y + 118, 26, 10);
        // Label
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(80, 110, 140, 200));
        g2.drawString("SHOWER", x + 55, y - 6);
    }

    // ── Sink ──────────────────────────────────────────────────────────────
    private void drawSink(Graphics2D g2, int x, int y) {
        // Vanity/counter
        g2.setColor(new Color(220, 225, 230));
        g2.fillRect(x - 10, y + 55, 150, 18);
        g2.setColor(new Color(190, 195, 205));
        g2.drawRect(x - 10, y + 55, 150, 18);
        // Basin
        g2.setColor(new Color(235, 240, 245));
        g2.fillRoundRect(x, y, 130, 60, 14, 14);
        g2.setColor(new Color(205, 215, 225));
        g2.drawRoundRect(x, y, 130, 60, 14, 14);
        // Drain
        g2.setColor(new Color(120, 128, 140));
        g2.fillOval(x + 57, y + 46, 16, 8);
        g2.setColor(new Color(160, 168, 180));
        g2.drawOval(x + 57, y + 46, 16, 8);
        // Faucet body
        g2.setColor(new Color(195, 200, 210));
        g2.fillRoundRect(x + 53, y - 24, 24, 32, 8, 8);
        // Faucet spout
        g2.setColor(new Color(180, 186, 196));
        g2.fillRoundRect(x + 38, y - 6, 54, 10, 6, 6);
        // Handles
        g2.setColor(new Color(200, 205, 215));
        g2.fillOval(x + 28, y - 20, 18, 14);
        g2.fillOval(x + 84, y - 20, 18, 14);
        // Label
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(80, 110, 140, 200));
        g2.drawString("SINK", x + 50, y + 80);
    }

    // ── Medicine cabinet / mirrored cabinet ───────────────────────────────
    private void drawMedicineCabinet(Graphics2D g2, int x, int y) {
        int cw = 165, ch = 125;
        // Cabinet frame
        g2.setColor(new Color(192, 198, 208));
        g2.fillRoundRect(x, y, cw, ch, 5, 5);
        // Mirror face
        GradientPaint mirror = new GradientPaint(
                x + 4, y + 4, new Color(230, 240, 252, 220),
                x + cw - 4, y + ch - 4, new Color(200, 218, 238, 200));
        g2.setPaint(mirror);
        g2.fillRoundRect(x + 4, y + 4, cw - 8, ch - 8, 4, 4);
        g2.setPaint(null);
        // Reflection highlights
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 20, y + 18, x + 65, y + 30);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 20, y + 35, x + 45, y + 42);
        g2.setStroke(new BasicStroke(1));
        // Handle
        g2.setColor(new Color(162, 168, 178));
        g2.fillRoundRect(x + cw - 15, y + ch / 2 - 12, 8, 24, 4, 4);
        g2.setColor(new Color(130, 136, 148));
        g2.drawRoundRect(x + cw - 15, y + ch / 2 - 12, 8, 24, 4, 4);
        // Frame border
        g2.setColor(new Color(142, 148, 160));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, cw, ch, 5, 5);
        g2.setStroke(new BasicStroke(1));
        // Label above
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(80, 110, 140, 200));
        g2.drawString("MEDICINE CABINET", x + 22, y - 6);
        // Red cross for medicine
        if (!GameState.medicineUsed) {
            g2.setColor(new Color(200, 60, 60, 180));
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("+", x + (cw - fm.stringWidth("+")) / 2, y + ch / 2 + 8);
        }
    }
}
