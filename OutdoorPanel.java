package game;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class OutdoorPanel extends RoomPanel {

    private static final Random RNG = new Random();
    private static java.awt.image.BufferedImage SPRINKLER_ON_IMG  = null;
    private static java.awt.image.BufferedImage SPRINKLER_OFF_IMG = null;
    static {
        SPRINKLER_ON_IMG  = AssetManager.getRoom("sprinkler_on");
        SPRINKLER_OFF_IMG = AssetManager.getRoom("sprinkler_off");
        if (SPRINKLER_ON_IMG == null)  SPRINKLER_ON_IMG  = AssetManager.getRoom("sprinkler");
    }

    // Water splash animation after running through
    private boolean sprinklerRunAnim = false;
    private int     sprinklerAnimTick = 0;
    private javax.swing.Timer sprinklerTimer = null;

    // ── Lawn mowing mini-game state ────────────────────────────────────────
    private boolean mowingActive = false;
    private int     mowStreak    = 0;   // consecutive mow-button presses this session

    public OutdoorPanel() {
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

    // Layout (bottom strip, 3 columns):
    //   Left   (trash/mail): x=22,  w=195  rows: y=300, 342
    //   Center (mow):        x=235, w=195  rows: y=300, 342
    //   Right  (sprinkler):  x=448, w=210  rows: y=300, 342
    private void buildButtons() {
        // ── LEFT — Take out trash / Bring in mail ─────────────────────────
        if (!GameState.choreTrashOut) {
            JButton trashBtn = makeButton("Take Out Trash", bx(22), by(300), bx(195), by(36));
            trashBtn.setForeground(new Color(255, 200, 100));
            trashBtn.addActionListener(e -> {
                GameState.choreTrashOut = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Trash is out! Now put a new bag in (go to Kitchen).");
            });
            add(trashBtn);
        } else if (!GameState.choreTrashBagIn) {
            JButton reminder = makeButton("Trash out ✓ — put bag in!", bx(22), by(300), bx(195), by(36));
            reminder.setEnabled(false);
            add(reminder);
        }

        // Mail chore (bonus)
        if (!GameState.choreMailIn) {
            JButton mailBtn = makeButton("Bring In Mail", bx(22), by(342), bx(195), by(36));
            mailBtn.setForeground(new Color(200, 220, 160));
            mailBtn.addActionListener(e -> {
                GameState.choreMailIn = true;
                GameState.coins++;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Mail's in! +1 coin.");
            });
            add(mailBtn);
        } else {
            JButton mailDone = makeButton("Mail In ✓", bx(22), by(342), bx(195), by(36));
            mailDone.setEnabled(false);
            add(mailDone);
        }

        // Find Mittens (when escaped)
        if (GameState.mittensEscaped) {
            JButton mittenBtn = makeButton("Find Mittens!", bx(22), by(384), bx(195), by(30));
            mittenBtn.setForeground(new Color(255, 150, 50));
            mittenBtn.addActionListener(e -> {
                GameState.mittensEscaped = false;
                GameState.catHunger     = GameLoop.clamp(GameState.catHunger + 10f, 0, 100);
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Found Mittens! She was hiding under the porch.");
            });
            add(mittenBtn);
        }

        // ── CENTER — Mow the lawn ─────────────────────────────────────────
        if (!GameState.choreLawnMowed) {
            if (GameState.thunderstorm) {
                JButton noMow = makeButton("Can't Mow — Thunderstorm!", bx(235), by(300), bx(195), by(36));
                noMow.setEnabled(false);
                noMow.setForeground(new Color(180, 160, 80));
                add(noMow);
            } else if (!mowingActive) {
                JButton startMow = makeButton("Start Mowing", bx(235), by(300), bx(195), by(36));
                startMow.setForeground(new Color(180, 255, 130));
                startMow.addActionListener(e -> {
                    if (GameState.temperature >= 82f) {
                        GameFrame.getInstance().getGamePanel()
                                .showMessage("Too hot to mow right now! Cool down first.");
                        return;
                    }
                    mowingActive = true;
                    mowStreak = 0;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Started mowing. Each pass heats you up — don't overdo it!");
                });
                add(startMow);
            } else {
                // Mowing in progress
                JButton mowBtn = makeButton("MOW!", bx(235), by(300), bx(195), by(36));
                mowBtn.setForeground(new Color(130, 255, 100));
                mowBtn.addActionListener(e -> doMow());
                add(mowBtn);

                JButton stopMow = makeButton("Stop Mowing", bx(235), by(342), bx(195), by(36));
                stopMow.addActionListener(e -> {
                    mowingActive = false;
                    mowStreak = 0;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Stopped mowing. Lawn at " + (int)GameState.choreLawnPct + "%.");
                });
                add(stopMow);
            }
        } else {
            JButton done = makeButton("Lawn Mowed ✓", bx(235), by(300), bx(195), by(36));
            done.setEnabled(false);
            add(done);
        }

        // ── RIGHT — Sprinkler ─────────────────────────────────────────────
        if (!GameState.sprinklerUsed) {
            JButton sprinklerBtn = makeButton("Run Through Sprinkler", bx(448), by(300), bx(210), by(36));
            sprinklerBtn.setForeground(new Color(100, 200, 255));
            sprinklerBtn.addActionListener(e -> {
                GameState.temperature = GameLoop.clamp(GameState.temperature - 14f, 0, 100);
                GameState.thirst      = GameLoop.clamp(GameState.thirst + 10f, 0, 100);
                GameState.sprinklerUsed = true;
                startSprinklerAnim();
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("You ran through the sprinkler! Temp way down.");
            });
            add(sprinklerBtn);
        } else {
            JButton off = makeButton("Sprinkler used ✓", bx(448), by(300), bx(210), by(36));
            off.setEnabled(false);
            add(off);
        }

        // ── RIGHT row 2 — Search yard (coin RNG 0-5) ─────────────────────
        if (!GameState.looseChangeFound_Outside) {
            JButton searchBtn = makeButton("Search the Yard", bx(448), by(342), bx(210), by(36));
            searchBtn.setForeground(new Color(200, 220, 160));
            searchBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6); // 0-5
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_Outside = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) in the grass!" : "Nothing in the yard today.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(searchBtn);
        }

        // ── RIGHT row 3 — Water outdoor plants ───────────────────────────
        if (!GameState.choreWaterPlants) {
            JButton waterBtn = makeButton("Water Outdoor Plants", bx(448), by(384), bx(210), by(30));
            waterBtn.setForeground(new Color(100, 220, 100));
            waterBtn.addActionListener(e -> {
                GameState.choreWaterPlants = true;
                GameState.coins++;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Watered the outdoor plants! +1 coin.");
            });
            add(waterBtn);
        } else {
            JButton plantDone = makeButton("Outdoor Plants ✓", bx(448), by(384), bx(210), by(30));
            plantDone.setEnabled(false);
            add(plantDone);
        }
    }

    private void doMow() {
        if (GameState.temperature >= 85f) {
            mowingActive = false;
            mowStreak = 0;
            onEnter();
            GameFrame.getInstance().getGamePanel()
                    .showMessage("Too hot! You had to stop mowing. Cool down first.");
            return;
        }
        // Progress lawn
        GameState.choreLawnPct = Math.min(100f, GameState.choreLawnPct + 12.5f);
        // Heat penalty per mow
        GameState.temperature = GameLoop.clamp(GameState.temperature + 4f, 0, 100);
        mowStreak++;
        // After 5+ consecutive passes, random event chance
        if (mowStreak >= 5 && RNG.nextFloat() < 0.35f) {
            triggerMowEvent();
        }
        if (GameState.choreLawnPct >= 100f) {
            GameState.choreLawnMowed = true;
            mowingActive = false;
            mowStreak = 0;
            onEnter();
            GameFrame.getInstance().getGamePanel()
                    .showMessage("Lawn's done! +4 coins at day end.");
        } else {
            onEnter();
            GameFrame.getInstance().getGamePanel()
                    .showMessage("Mowing... " + (int)GameState.choreLawnPct + "% done. Temp +" + 4);
        }
    }

    private void triggerMowEvent() {
        int evt = RNG.nextInt(3);
        switch (evt) {
            case 0:
                GameState.squirrelPresent = true;
                GameState.squirrelTicks = 0;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("A squirrel ran out of the grass!");
                break;
            case 1:
                GameState.stress = GameLoop.clamp(GameState.stress + 20f, 0, 100);
                GameFrame.getInstance().getGamePanel()
                        .showMessage("The mower hit a rock! Stress way up.");
                break;
            case 2:
                GameState.thirst = GameLoop.clamp(GameState.thirst - 15f, 0, 100);
                GameFrame.getInstance().getGamePanel()
                        .showMessage("It's hot out here. You're getting dehydrated!");
                break;
        }
        mowStreak = 0; // reset streak after event
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

        drawBg(g2, "outdoor", W, H);

        // ── Sprinkler overlay ─────────────────────────────────────────────
        drawSprinkler(g2, 30, H - 220);

        // ── Temperature display (outdoor thermometer) ─────────────────────
        drawOutdoorTemp(g2, W - 105, 30);

        // ── Dark strip for buttons ────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // ── Lawn progress bar (if mowing started) ────────────────────────
        if (GameState.choreLawnPct > 0 && !GameState.choreLawnMowed) {
            int bw = 220, bh = 18, bx2 = W/2 - bw/2, by2 = H - 105;
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(bx2 - 2, by2 - 2, bw + 4, bh + 4, 6, 6);
            g2.setColor(new Color(30, 80, 20));
            g2.fillRoundRect(bx2, by2, bw, bh, 4, 4);
            g2.setColor(new Color(80, 200, 60));
            g2.fillRoundRect(bx2, by2, (int)(bw * GameState.choreLawnPct / 100f), bh, 4, 4);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            String lbl = "Lawn: " + (int)GameState.choreLawnPct + "%";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, bx2 + (bw - fm.stringWidth(lbl))/2, by2 + 13);
        }

        // ── Thunderstorm rain overlay ─────────────────────────────────────
        if (GameState.thunderstorm) {
            drawRain(g2, W, H);
        }

        // Status badges
        int bdY = 10;
        if (GameState.powerOut) {
            drawBadge(g2, "POWER OUT", 10, bdY, new Color(155, 95, 0, 220), Color.WHITE);
            bdY += 28;
        }
        if (GameState.thunderstorm) {
            drawBadge(g2, "THUNDERSTORM — Stay Inside!", W/2 - 120, 10,
                    new Color(80, 60, 160, 220), Color.WHITE);
        }
        if (GameState.choreLawnMowed)
            drawBadge(g2, "Lawn ✓", 10, bdY, new Color(20, 120, 45, 210), Color.WHITE);
        if (GameState.mittensEscaped)
            drawBadge(g2, "Mittens escaped! Find her outside!", W/2 - 130, 36,
                    new Color(200, 80, 30, 220), Color.WHITE);

        g2.dispose();
    }

    private void startSprinklerAnim() {
        sprinklerRunAnim = true;
        sprinklerAnimTick = 0;
        if (sprinklerTimer != null) sprinklerTimer.stop();
        sprinklerTimer = new javax.swing.Timer(60, e -> {
            sprinklerAnimTick++;
            repaint();
            if (sprinklerAnimTick > 30) {
                sprinklerRunAnim = false;
                ((javax.swing.Timer)e.getSource()).stop();
            }
        });
        sprinklerTimer.start();
    }

    private void drawSprinkler(Graphics2D g2, int x, int y) {
        if (!GameState.sprinklerUsed) {
            // ACTIVE: show sprinkler_on image (has water baked in)
            if (SPRINKLER_ON_IMG != null) {
                g2.drawImage(SPRINKLER_ON_IMG, x, y, 92, 70, null);
            } else {
                drawSprinklerHead(g2, x, y, true);
            }
        } else {
            // USED: show sprinkler_off image (no water)
            if (SPRINKLER_OFF_IMG != null) {
                g2.drawImage(SPRINKLER_OFF_IMG, x, y, 72, 56, null);
            } else {
                drawSprinklerHead(g2, x, y, false);
            }
        }

        // Water splash animation overlay when player just ran through
        if (sprinklerRunAnim) {
            float alpha = Math.max(0f, 1f - sprinklerAnimTick / 30f);
            g2.setColor(new Color(100, 180, 255, (int)(alpha * 200)));
            long t = System.currentTimeMillis();
            for (int i = 0; i < 18; i++) {
                double angle = Math.PI * 2 * i / 18.0 + t * 0.004;
                int r = 28 + sprinklerAnimTick * 2;
                int dx = (int)(Math.cos(angle) * r);
                int dy = (int)(Math.sin(angle) * r * 0.5);
                int ds = 4 + (int)(Math.random() * 5);
                g2.fillOval(x + 46 + dx - ds/2, y + 35 + dy - ds/2, ds, ds);
            }
        }
    }

    private void drawSprinklerHead(Graphics2D g2, int x, int y, boolean active) {
        int cx = x + 30, cy = y + 50;
        g2.setColor(new Color(60, 100, 60));
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx, cy, cx, cy + 28);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(75, 115, 75));
        g2.fillRoundRect(cx - 14, cy - 8, 28, 14, 5, 5);
        g2.fillRoundRect(cx - 5, cy - 20, 10, 16, 4, 4);
        g2.setColor(new Color(50, 90, 50));
        g2.fillOval(cx - 18, cy - 4, 8, 6);
        g2.fillOval(cx + 10, cy - 4, 8, 6);
        if (!active) {
            g2.setColor(new Color(110, 75, 30, 120));
            g2.fillOval(cx - 5, cy - 3, 4, 4);
            g2.fillOval(cx + 3, cy, 3, 3);
        }
    }

    private void drawRain(Graphics2D g2, int W, int H) {
        long t = System.currentTimeMillis();
        // Dark storm overlay
        g2.setColor(new Color(20, 20, 60, 100));
        g2.fillRect(0, 0, W, H);
        // Lightning flash occasionally
        if ((t / 1800) % 10 == 0) {
            g2.setColor(new Color(200, 220, 255, 60));
            g2.fillRect(0, 0, W, H);
        }
        // Rain streaks
        g2.setColor(new Color(140, 170, 220, 100));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int phase = (int)((t / 40) % H);
        for (int col = 0; col < W; col += 22) {
            int offset = (col * 7 + phase) % H;
            g2.drawLine(col + 8, offset - 18, col, offset);
            int offset2 = (col * 7 + phase + H/2) % H;
            g2.drawLine(col + 8, offset2 - 18, col, offset2);
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawOutdoorTemp(Graphics2D g2, int x, int y) {
        int tw = 88, th = 62;
        // Panel background
        g2.setColor(new Color(10, 8, 20, 200));
        g2.fillRoundRect(x, y, tw, th, 10, 10);
        g2.setColor(new Color(80, 80, 120, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, tw, th, 10, 10);
        g2.setStroke(new BasicStroke(1));

        // Temp value
        float t = GameState.temperature;
        Color tempColor = t < 50 ? new Color(100, 220, 255)
                        : t < 70 ? new Color(255, 220, 60)
                        : t < 85 ? new Color(255, 140, 40)
                        : new Color(255, 60, 60);
        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.setColor(tempColor);
        String val = String.format("%.0f", t) + "°";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(val, x + (tw - fm.stringWidth(val)) / 2, y + 36);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(new Color(160, 160, 200));
        String lbl = "HEAT INDEX";
        fm = g2.getFontMetrics();
        g2.drawString(lbl, x + (tw - fm.stringWidth(lbl)) / 2, y + 52);
    }
}
