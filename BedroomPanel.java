package game;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BedroomPanel extends RoomPanel {

    private static BufferedImage catImg    = null;
    private static BufferedImage catBowlImg = null;
    static {
        catImg     = AssetManager.getRoom("cat");
        catBowlImg = AssetManager.getRoom("cat_bowl");
    }

    public BedroomPanel() {
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

    // Layout:
    //   Left  (cat): x=22,  w=195  rows: y=300(feed), 342(window), 384(phone charge)
    //   Center(bed): x=305, w=190  rows: y=300(rest), 342(under bed), 384(nap/dry/change)
    //   Right (desk):x=583, w=195  rows: y=300(homework), 342(step stool), 384(tidy bedroom)
    private void buildButtons() {
        // ── LEFT — Cat / Window / Phone ───────────────────────────────────
        // Row 1: Feed Mittens
        if (GameState.catFoodScoops > 0 && GameState.catHunger < 90f) {
            JButton feedBtn = makeButton("Feed Mittens (" + GameState.catFoodScoops + ")",
                    bx(22), by(300), bx(195), by(36));
            feedBtn.setForeground(new Color(255, 200, 80));
            feedBtn.addActionListener(e -> {
                GameState.catHunger     = Math.min(100f, GameState.catHunger + 50f);
                GameState.catFoodScoops = Math.max(0, GameState.catFoodScoops - 1);
                GameLoop.rewardAction(2, "Mittens purrs. Good human.");
                onEnter();
            });
            add(feedBtn);
        } else if (GameState.catFoodScoops <= 0) {
            JButton noFood = makeButton("No Cat Food Left", bx(22), by(300), bx(195), by(36));
            noFood.setEnabled(false);
            add(noFood);
        } else {
            JButton full = makeButton("Mittens Is Full", bx(22), by(300), bx(195), by(36));
            full.setEnabled(false);
            add(full);
        }

        // Row 2: Bedroom Window
        String bwLabel = GameState.bedroomWindowOpen ? "Close Bedroom Window" : "Open Bedroom Window";
        JButton bwBtn = makeButton(bwLabel, bx(22), by(342), bx(195), by(34));
        bwBtn.setForeground(new Color(160, 210, 255));
        bwBtn.addActionListener(e -> {
            GameState.bedroomWindowOpen = !GameState.bedroomWindowOpen;
            onEnter();
            GameFrame.getInstance().getGamePanel().showMessage(
                    GameState.bedroomWindowOpen ? "Bedroom window open — nice breeze." : "Bedroom window closed.");
        });
        add(bwBtn);

        // Row 3: Phone charging
        if (GameState.phoneBattery >= 100f) {
            JButton full2 = makeButton("Phone: Fully Charged", bx(22), by(382), bx(195), by(30));
            full2.setEnabled(false);
            add(full2);
        } else if (!GameState.phoneCharging) {
            String warnSuffix = GameState.phoneChargeCount >= 3 ? " (high power risk!)" : "";
            JButton chargeBtn = makeButton("Charge Phone" + warnSuffix, bx(22), by(382), bx(195), by(30));
            chargeBtn.setForeground(GameState.phoneChargeCount >= 3
                    ? new Color(255, 130, 50) : new Color(130, 255, 150));
            chargeBtn.addActionListener(e -> {
                GameState.phoneCharging = true;
                onEnter();
                String msg = "Phone charging... " + (GameState.phoneChargeCount >= 3
                        ? "WARNING: high power-out risk!" : "(charge #" + (GameState.phoneChargeCount + 1) + ")");
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(chargeBtn);
        } else {
            int pct = (int)GameState.phoneBattery;
            JButton chargingBtn = makeButton("Charging... " + pct + "%", bx(22), by(382), bx(195), by(30));
            chargingBtn.setForeground(new Color(100, 220, 130));
            chargingBtn.addActionListener(e -> {
                GameState.phoneCharging = false;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Phone unplugged.");
            });
            add(chargingBtn);
        }

        // ── CENTER — Bed area ─────────────────────────────────────────────
        // Row 1: Rest on bed
        JButton restBtn = makeButton("Rest on Bed", bx(305), by(300), bx(190), by(36));
        restBtn.setForeground(new Color(180, 200, 255));
        restBtn.addActionListener(e -> {
            GameState.temperature = GameLoop.clamp(GameState.temperature - 4f, 0, 100);
            GameState.stress      = GameLoop.clamp(GameState.stress - 10f, 0, 100);
            GameFrame.getInstance().getGamePanel()
                    .showMessage("A quick rest... temperature and stress down a bit.");
        });
        add(restBtn);

        // Row 2: Check under bed
        if (!GameState.looseChangeFound_Bedroom) {
            JButton changeBtn = makeButton("Check Under Bed", bx(305), by(342), bx(190), by(34));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6);
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_Bedroom = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) under the bed!" : "Nothing under the bed.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }

        // Row 3: Shower chain steps take priority; otherwise nap
        if (GameState.needsTowelDry) {
            JButton dryBtn = makeButton("Dry Off (towel)", bx(305), by(382), bx(190), by(30));
            dryBtn.setForeground(new Color(180, 220, 255));
            dryBtn.addActionListener(e -> {
                GameState.needsTowelDry      = false;
                GameState.needsClothesChange = true;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Dried off! Now change into clean clothes.");
            });
            add(dryBtn);
        } else if (GameState.needsClothesChange) {
            JButton changeBtn2 = makeButton("Change Clothes", bx(305), by(382), bx(190), by(30));
            changeBtn2.setForeground(new Color(200, 255, 180));
            changeBtn2.addActionListener(e -> {
                GameState.needsClothesChange = false;
                GameState.dirtyClothesAdded  = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Changed! Old clothes in laundry basket (basement).");
            });
            add(changeBtn2);
        } else if (!GameState.napping) {
            JButton napBtn = makeButton("Take a Nap", bx(305), by(382), bx(190), by(30));
            napBtn.setForeground(new Color(160, 180, 255));
            napBtn.addActionListener(e -> {
                GameState.napping      = true;
                GameState.napTicksLeft = 20;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Napping... stress and heat fading. Auto-wake in 10s.");
            });
            add(napBtn);
        } else {
            JButton nappingBtn = makeButton("Napping... (" + GameState.napTicksLeft/2 + "s)", bx(305), by(382), bx(190), by(30));
            nappingBtn.setEnabled(false);
            nappingBtn.setForeground(new Color(140, 160, 220));
            add(nappingBtn);
        }

        // ── RIGHT — Desk area ─────────────────────────────────────────────
        // Row 1: Homework
        if (!GameState.choreHomework) {
            int pct = Math.min(100, GameState.choreHomeworkTicks * 5);
            String hwLabel = pct > 0 ? "Homework (" + pct + "%)" : "Do Homework";
            JButton hwBtn = makeButton(hwLabel, bx(583), by(300), bx(195), by(36));
            hwBtn.setForeground(new Color(255, 220, 120));
            hwBtn.addActionListener(e -> GameFrame.getInstance().getGamePanel()
                    .showMessage("Stay in the bedroom — homework fills automatically!"));
            add(hwBtn);
        } else {
            JButton hwDone = makeButton("Homework ✓", bx(583), by(300), bx(195), by(36));
            hwDone.setEnabled(false);
            add(hwDone);
        }

        // Row 2: Step stool
        if (GameState.stepStoolInBedroom) {
            JButton stoolBtn = makeButton("Grab Step Stool", bx(583), by(342), bx(195), by(34));
            stoolBtn.setForeground(new Color(180, 255, 200));
            stoolBtn.addActionListener(e -> {
                GameState.hasStepStool       = true;
                GameState.stepStoolInBedroom = false;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Got the step stool! Now you can reach the kitchen sink.");
            });
            add(stoolBtn);
        } else if (GameState.hasStepStool) {
            JButton taken = makeButton("Stool: Carrying", bx(583), by(342), bx(195), by(34));
            taken.setEnabled(false);
            add(taken);
        }

        // Row 3: Tidy bedroom
        if (!GameState.choreCleanBedroom) {
            JButton tidyBtn = makeButton("Tidy Bedroom", bx(583), by(382), bx(195), by(30));
            tidyBtn.setForeground(new Color(255, 220, 120));
            tidyBtn.addActionListener(e -> {
                GameState.choreCleanBedroom = true;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Bedroom is spotless!");
            });
            add(tidyBtn);
        } else {
            JButton tidyDone = makeButton("Bedroom Clean ✓", bx(583), by(382), bx(195), by(30));
            tidyDone.setEnabled(false);
            add(tidyDone);
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

        drawBg(g2, "bedroom", W, H);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // Draw Mittens
        drawCat(g2, 55, H - 185);

        // Homework progress bar (when active)
        if (!GameState.choreHomework && GameState.choreHomeworkTicks > 0) {
            int pct = Math.min(100, GameState.choreHomeworkTicks * 5);
            int bw = 160, bh = 12;
            int bx2 = bx(580), by2 = by(258);
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(bx2, by2, bw, bh, 4, 4);
            g2.setColor(new Color(255, 200, 50));
            g2.fillRoundRect(bx2, by2, bw * pct / 100, bh, 4, 4);
        }

        // Badges
        if (GameState.powerOut)
            drawBadge(g2, "POWER OUT", W - 165, 10, new Color(155, 95, 0, 220), Color.WHITE);
        if (GameState.choreHomework)
            drawBadge(g2, "Homework ✓", 10, 10, new Color(20, 120, 45, 210), Color.WHITE);
        if (GameState.napping)
            drawBadge(g2, "Napping... " + GameState.napTicksLeft/2 + "s", W/2 - 60, 10,
                    new Color(60, 60, 160, 220), Color.WHITE);
        if (GameState.phoneCharging) {
            int pct2 = (int)GameState.phoneBattery;
            drawBadge(g2, "Phone charging " + pct2 + "%", 10, 36,
                    new Color(30, 120, 50, 220), Color.WHITE);
        }
        if ("BEDROOM".equals(GameState.mittensMessRoom))
            drawBadge(g2, "Mittens made a mess here!", 10, H - 115,
                    new Color(160, 60, 160, 220), Color.WHITE);

        g2.dispose();
    }

    private void drawCat(Graphics2D g2, int x, int y) {
        boolean angry = GameState.isCatAngry();
        int catW = 105, catH = 105;

        if (catImg != null) {
            // Draw rounded clip so the photo blends nicely
            java.awt.geom.RoundRectangle2D clip =
                new java.awt.geom.RoundRectangle2D.Float(x, y - catH, catW, catH, 14, 14);
            g2.setClip(clip);
            g2.drawImage(catImg, x, y - catH, catW, catH, null);
            g2.setClip(null);

            // Red angry overlay
            if (angry) {
                g2.setColor(new Color(180, 0, 0, 90));
                g2.fill(clip);
            }

            // Label above
            g2.setFont(new Font("SansSerif", angry ? Font.BOLD : Font.PLAIN, 10));
            g2.setColor(angry ? new Color(220, 80, 80) : new Color(100, 200, 100));
            String lbl = angry ? "FEED ME >:(" : "purrr...";
            g2.drawString(lbl, x + 4, y - catH - 5);
        } else {
            // G2D fallback
            Color body  = angry ? new Color(180, 80, 60)  : new Color(200, 170, 130);
            Color inner = angry ? new Color(220, 120, 100) : new Color(230, 200, 170);
            g2.setColor(body);
            g2.fillOval(x, y, 60, 50);
            g2.fillOval(x + 35, y - 18, 38, 35);
            int[] exL = {x+38,x+43,x+50}; int[] eyL = {y-18,y-36,y-18};
            g2.fillPolygon(exL, eyL, 3);
            int[] exR = {x+55,x+60,x+68};
            g2.fillPolygon(exR, eyL, 3);
            g2.setColor(inner);
            int[] iexL = {x+40,x+43,x+48}; int[] ieyL = {y-20,y-31,y-20};
            g2.fillPolygon(iexL, ieyL, 3);
            g2.fillOval(x + 15, y + 10, 30, 30);
            g2.setFont(new Font("SansSerif", angry ? Font.BOLD : Font.PLAIN, 10));
            g2.setColor(angry ? new Color(220,80,80) : new Color(100,150,100));
            g2.drawString(angry ? "FEED ME >:(" : "purrr...", x - 10, y - 42);
        }

        // Food bowl (use asset image if available)
        int bowlX = x + catW - 8, bowlY = y;
        if (catBowlImg != null) {
            g2.drawImage(catBowlImg, bowlX, bowlY, 44, 32, null);
            // Dim if empty
            if (GameState.catFoodScoops <= 0) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillRoundRect(bowlX, bowlY, 44, 32, 4, 4);
            }
        } else {
            g2.setColor(new Color(200, 60, 60));
            g2.fillOval(bowlX, bowlY + 4, 35, 18);
            g2.setColor(new Color(180, 40, 40));
            g2.drawOval(bowlX, bowlY + 4, 35, 18);
            if (GameState.catFoodScoops > 0) {
                g2.setColor(new Color(160, 130, 90));
                g2.fillOval(bowlX + 5, bowlY + 8, 25, 10);
            }
        }
    }
}
