package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class KitchenPanel extends RoomPanel {

    // ── Dishes mini-game state ─────────────────────────────────────────────
    private boolean dishMiniActive   = false;
    private float   dishNeedle       = 0f;
    private float   dishDir          = 1f;
    private float   dishSpeed        = 0.025f;   // increases on each hit
    private Timer   dishTimer;
    private int     dishFeedback     = 0;         // 1=hit, -1=miss, 0=none
    private int     dishFeedbackTick = 0;

    public KitchenPanel() {
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
        if (!dishMiniActive) buildButtons();
        else                 buildDishMiniGame();
        revalidate();
        repaint();
    }

    // ── Normal kitchen buttons ─────────────────────────────────────────────
    // Left  (x=18,  w=185): Sink/Dishes/Sweep
    // Center(x=215, w=180): Cat food / Window / Drawer or Trash Bag
    // Right (x=603, w=185): Freezer / Microwave / Eat / Tidy
    private void buildButtons() {
        boolean canReachSink = GameState.hasStepStool || GameState.hasKitchenStool;

        // ── LEFT — Sink / Dishes / Sweep ─────────────────────────────────
        // Row 1: Drink Water
        if (canReachSink) {
            JButton drinkBtn = makeButton("Drink Water", bx(18), by(300), bx(185), by(34));
            drinkBtn.setForeground(new Color(100, 200, 255));
            drinkBtn.addActionListener(e -> {
                float amount = GameState.hasBigBottle ? 35f : 22f;
                GameState.thirst      = GameLoop.clamp(GameState.thirst + amount, 0, 100);
                GameState.temperature = GameLoop.clamp(GameState.temperature - 3f, 0, 100);
                GameFrame.getInstance().getGamePanel().showMessage("Drank some water. Refreshing!");
            });
            add(drinkBtn);
        } else {
            JButton noReach = makeButton("Sink (need stool)", bx(18), by(300), bx(185), by(34));
            noReach.setEnabled(false);
            noReach.setForeground(new Color(200, 100, 100));
            add(noReach);
        }

        // Row 2: Wash Dishes
        if (!GameState.choreDishes) {
            JButton dishBtn = makeButton("Wash Dishes", bx(18), by(340), bx(185), by(34));
            dishBtn.setForeground(new Color(180, 220, 255));
            if (!canReachSink) dishBtn.setEnabled(false);
            else dishBtn.addActionListener(e -> startDishMiniGame());
            add(dishBtn);
        } else {
            JButton done = makeButton("Dishes ✓", bx(18), by(340), bx(185), by(34));
            done.setEnabled(false);
            add(done);
        }

        // Row 3: Sweep Kitchen (bonus chore)
        if (!GameState.choreSweepKitchen) {
            JButton sweepBtn = makeButton("Sweep Kitchen", bx(18), by(380), bx(185), by(32));
            sweepBtn.setForeground(new Color(255, 220, 120));
            sweepBtn.addActionListener(e -> {
                GameState.choreSweepKitchen = true;
                GameState.coins++;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Kitchen swept! +1 coin.");
            });
            add(sweepBtn);
        } else {
            JButton done2 = makeButton("Swept ✓", bx(18), by(380), bx(185), by(32));
            done2.setEnabled(false);
            add(done2);
        }

        // ── CENTER — Cat food / Window / Misc ─────────────────────────────
        // Row 1: Cat food
        if (GameState.catFoodScoops > 0) {
            JButton catBtn = makeButton("Cat Food (" + GameState.catFoodScoops + ")",
                    bx(215), by(300), bx(180), by(34));
            catBtn.setForeground(new Color(255, 200, 80));
            catBtn.addActionListener(e -> {
                GameState.catFoodScoops--;
                GameState.catHunger = GameLoop.clamp(GameState.catHunger + 40f, 0, 100);
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Fed Mittens! (" + GameState.catFoodScoops + " scoops left)");
            });
            add(catBtn);
        } else {
            JButton noFood = makeButton("Cat Food Empty", bx(215), by(300), bx(180), by(34));
            noFood.setEnabled(false);
            add(noFood);
        }

        // Row 2: Kitchen Window toggle
        String kwLabel = GameState.kitchenWindowOpen ? "Close Kitchen Window" : "Open Kitchen Window";
        JButton kwBtn = makeButton(kwLabel, bx(215), by(340), bx(180), by(34));
        kwBtn.setForeground(new Color(140, 200, 255));
        kwBtn.addActionListener(e -> {
            GameState.kitchenWindowOpen = !GameState.kitchenWindowOpen;
            onEnter();
            GameFrame.getInstance().getGamePanel().showMessage(
                    GameState.kitchenWindowOpen ? "Kitchen window open." : "Kitchen window closed.");
        });
        add(kwBtn);

        // Row 3: Put Trash Bag In (priority) or Junk Drawer
        if (GameState.choreTrashOut && !GameState.choreTrashBagIn) {
            JButton bagBtn = makeButton("Put Trash Bag In", bx(215), by(380), bx(180), by(32));
            bagBtn.setForeground(new Color(255, 160, 80));
            bagBtn.addActionListener(e -> {
                GameState.choreTrashBagIn = true;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("New trash bag in! Chore complete.");
            });
            add(bagBtn);
        } else if (!GameState.looseChangeFound_Kitchen) {
            JButton changeBtn = makeButton("Junk Drawer", bx(215), by(380), bx(180), by(32));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6);
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_Kitchen = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) in the junk drawer!" : "Junk drawer is empty.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }

        // ── RIGHT — Freezer / Microwave / Eat / Tidy ─────────────────────
        // Row 0 (small): Delivery pick-up (priority) or Search Freezer
        if (GameState.deliveryArrived && GameState.hunger < 95f) {
            JButton pickupBtn = makeButton("Pick Up Delivery!", bx(603), by(283), bx(185), by(30));
            pickupBtn.setForeground(new Color(120, 255, 160));
            pickupBtn.addActionListener(e -> {
                GameState.hunger        = GameLoop.clamp(GameState.hunger + 40f, 0, 100);
                GameState.deliveryArrived = false;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Ate the delivery food. Hunger restored!");
            });
            add(pickupBtn);
        } else if (!GameState.iceCreamFound) {
            JButton freezerBtn = makeButton("Search Freezer", bx(603), by(283), bx(185), by(30));
            freezerBtn.setForeground(new Color(160, 220, 255));
            freezerBtn.addActionListener(e -> {
                GameState.iceCreamFound = true;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Found ice cream hidden in the back of the freezer!");
            });
            add(freezerBtn);
        } else if (!GameState.iceCreamEaten) {
            JButton eatIce = makeButton("Eat Ice Cream!", bx(603), by(283), bx(185), by(30));
            eatIce.setForeground(new Color(220, 160, 255));
            eatIce.addActionListener(e -> {
                GameState.iceCreamEaten = true;
                GameState.hunger        = GameLoop.clamp(GameState.hunger + 25f, 0, 100);
                GameState.thirst        = GameLoop.clamp(GameState.thirst + 10f, 0, 100);
                GameState.temperature   = GameLoop.clamp(GameState.temperature - 5f, 0, 100);
                GameState.stress        = GameLoop.clamp(GameState.stress - 8f, 0, 100);
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Ice cream! Yum. Temp and stress down a bit.");
            });
            add(eatIce);
        } else {
            JButton eaten = makeButton("Ice Cream Gone ✓", bx(603), by(283), bx(185), by(30));
            eaten.setEnabled(false);
            add(eaten);
        }

        // Row 1: Microwave / Eat / Spoiled
        if (!GameState.powerOut) {
            if (!GameState.foodMicrowaved) {
                JButton microBtn = makeButton("Microwave Food", bx(603), by(317), bx(185), by(34));
                microBtn.setForeground(new Color(255, 200, 80));
                microBtn.addActionListener(e -> {
                    GameState.foodMicrowaved = true;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Microwaving... beep! Food's hot. Now eat it.");
                });
                add(microBtn);
            } else {
                JButton eatBtn = makeButton("Eat (heated food)", bx(603), by(317), bx(185), by(34));
                eatBtn.setForeground(new Color(150, 255, 150));
                eatBtn.addActionListener(e -> {
                    GameState.hunger         = GameLoop.clamp(GameState.hunger + 32f, 0, 100);
                    GameState.foodMicrowaved = false;
                    GameFrame.getInstance().getGamePanel().showMessage("Hot food! Hunger restored.");
                });
                add(eatBtn);
            }

            // Row 2: Eat Cold
            JButton coldEatBtn = makeButton("Eat Cold (less)", bx(603), by(356), bx(185), by(30));
            coldEatBtn.setForeground(new Color(130, 200, 130));
            coldEatBtn.addActionListener(e -> {
                GameState.hunger         = GameLoop.clamp(GameState.hunger + 18f, 0, 100);
                GameState.stress         = GameLoop.clamp(GameState.stress + 5f, 0, 100);
                GameState.foodMicrowaved = false;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Cold leftovers... less satisfying. Stress slightly up.");
            });
            add(coldEatBtn);
        } else {
            JButton spoiled = makeButton("Food Spoiled ✗", bx(603), by(317), bx(185), by(34));
            spoiled.setEnabled(false);
            add(spoiled);
        }

        // Row 3: Tidy Kitchen
        if (!GameState.choreCleanKitchen) {
            JButton tidyBtn = makeButton("Tidy Kitchen", bx(603), by(390), bx(185), by(28));
            tidyBtn.setForeground(new Color(255, 220, 120));
            tidyBtn.addActionListener(e -> {
                GameState.choreCleanKitchen = true;
                GameLoop.rewardAction(0, "Kitchen tidied!");
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Kitchen is spotless!");
            });
            add(tidyBtn);
        } else {
            JButton done3 = makeButton("Kitchen Clean ✓", bx(603), by(390), bx(185), by(28));
            done3.setEnabled(false);
            add(done3);
        }
    }

    // ── Dishes mini-game ──────────────────────────────────────────────────
    private void startDishMiniGame() {
        dishMiniActive   = true;
        dishNeedle       = 0f;
        dishDir          = 1f;
        dishSpeed        = 0.025f;
        dishFeedback     = 0;
        dishFeedbackTick = 0;
        GameState.dishScrubs = 0;
        GameState.dishMisses = 0;
        if (dishTimer != null) dishTimer.stop();
        dishTimer = new Timer(28, e -> {
            dishNeedle += dishDir * dishSpeed;
            if (dishNeedle >= 1f) { dishNeedle = 1f; dishDir = -1f; }
            if (dishNeedle <= 0f) { dishNeedle = 0f; dishDir =  1f; }
            if (dishFeedbackTick > 0) dishFeedbackTick--;
            repaint();
        });
        dishTimer.start();
        onEnter();
    }

    private void buildDishMiniGame() {
        JButton back = makeButton("Give Up", bx(18), by(388), bx(100), by(32));
        back.addActionListener(e -> {
            if (dishTimer != null) dishTimer.stop();
            dishMiniActive = false;
            onEnter();
        });
        add(back);

        JButton scrub = makeButton("SCRUB!", bx(310), by(356), bx(180), by(48));
        scrub.setFont(new Font("SansSerif", Font.BOLD, 18));
        scrub.addActionListener(e -> {
            if (dishNeedle >= 0.35f && dishNeedle <= 0.65f) {
                // Hit!
                dishFeedback = 1;
                GameState.dishScrubs++;
                dishSpeed = Math.min(dishSpeed + 0.007f, 0.09f);
                if (GameState.dishScrubs >= 4) {
                    if (dishTimer != null) dishTimer.stop();
                    dishMiniActive = false;
                    GameState.choreDishes = true;
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Dishes are clean! +4 coins at day end.");
                    return;
                }
            } else {
                // Miss!
                dishFeedback = -1;
                GameState.dishMisses++;
                if (GameState.dishMisses > GameState.dishScrubs) {
                    // Fail — too many misses relative to hits
                    GameState.dishScrubs = 0;
                    GameState.dishMisses = 0;
                    dishSpeed = 0.025f;
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Too many misses! Starting over...");
                }
            }
            dishFeedbackTick = 8;
            repaint();
        });
        add(scrub);
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

        drawBg(g2, "kitchen", W, H);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(0, H - 80, W, 80);

        // ── Badges ────────────────────────────────────────────────────────
        int badY = 10;
        if (GameState.powerOut) {
            drawBadge(g2, "POWER OUT", W - 165, badY, new Color(155, 95, 0, 220), Color.WHITE);
        }
        if (GameState.kitchenWindowOpen) {
            drawBadge(g2, "Kitchen Window OPEN", 10, badY, new Color(25, 90, 150, 210), Color.WHITE);
            badY += 28;
        }
        if (GameState.choreTrashOut && !GameState.choreTrashBagIn) {
            drawBadge(g2, "Need: put trash bag in!", 10, badY, new Color(180, 90, 20, 220), Color.WHITE);
            badY += 28;
        }
        if (GameState.iceCreamFound && !GameState.iceCreamEaten) {
            drawBadge(g2, "Ice cream in freezer!", 10, badY, new Color(140, 60, 200, 210), Color.WHITE);
        }
        if (GameState.deliveryArrived) {
            drawBadge(g2, "Delivery here! Check freezer area.", W/2 - 120, 10,
                    new Color(30, 140, 30, 220), Color.WHITE);
        }
        if (GameState.choreDishes) {
            drawBadge(g2, "Dishes ✓", W - 165, 36, new Color(20, 120, 45, 210), Color.WHITE);
        }
        if (!GameState.mittensMessRoom.isEmpty() && "KITCHEN".equals(GameState.mittensMessRoom)) {
            drawBadge(g2, "Mittens made a mess here!", 10, H - 115,
                    new Color(160, 60, 160, 220), Color.WHITE);
        }

        // ── Dishes mini-game overlay ──────────────────────────────────────
        if (dishMiniActive) drawDishMiniGame(g2, W, H);

        g2.dispose();
    }

    private void drawDishMiniGame(Graphics2D g2, int W, int H) {
        // Panel background
        g2.setColor(new Color(0, 0, 0, 165));
        g2.fillRoundRect(100, 45, 600, 300, 16, 16);
        g2.setColor(new Color(60, 130, 200, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(100, 45, 600, 300, 16, 16);
        g2.setStroke(new BasicStroke(1));

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(new Color(180, 220, 255));
        String title = "Wash the Dishes!";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (W - fm.stringWidth(title)) / 2, 80);

        // Speed indicator
        int speedPct = Math.max(0, Math.min(100, (int)((dishSpeed - 0.025f) / (0.09f - 0.025f) * 100f)));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(speedPct > 60 ? new Color(255, 120, 60) : new Color(140, 200, 255));
        String speedLbl = speedPct == 0 ? "Speed: Normal" : "Speed +" + speedPct + "%";
        g2.drawString(speedLbl, W - 225, 80);

        // ── Green hit dots ──────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(150, 150, 180));
        g2.drawString("Hits:", 130, 110);
        for (int i = 0; i < 4; i++) {
            boolean done = i < GameState.dishScrubs;
            g2.setColor(done ? new Color(60, 200, 120) : new Color(45, 45, 65));
            g2.fillOval(190 + i * 34, 97, 22, 22);
            g2.setColor(done ? new Color(40, 160, 100) : new Color(38, 38, 58));
            g2.drawOval(190 + i * 34, 97, 22, 22);
        }

        // ── Red miss X marks ────────────────────────────────────────────
        if (GameState.dishMisses > 0) {
            g2.setColor(new Color(150, 75, 75));
            g2.drawString("Misses:", 420, 110);
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            for (int i = 0; i < Math.min(GameState.dishMisses, 6); i++) {
                g2.setColor(new Color(220, 50, 50));
                g2.drawString("✗", 486 + i * 20, 112);
            }
        }

        // Warning — one more miss = reset
        if (GameState.dishMisses > 0 && GameState.dishMisses >= GameState.dishScrubs) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(255, 100, 100));
            g2.drawString("One more miss = RESET!", 130, 128);
        }

        // Instructions
        g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
        g2.setColor(new Color(155, 155, 195));
        g2.drawString("Hit SCRUB! when the needle is in the green zone", 130, 143);

        // Track bar
        int barX = 130, barY = 155, barW = 540, barH = 30;
        g2.setColor(new Color(28, 28, 42));
        g2.fillRoundRect(barX, barY, barW, barH, 8, 8);
        g2.setColor(new Color(195, 45, 45, 180));
        g2.fillRoundRect(barX, barY, (int)(barW * 0.35f), barH, 8, 8);
        g2.fillRoundRect(barX + (int)(barW * 0.65f), barY, (int)(barW * 0.35f), barH, 8, 8);
        g2.setColor(new Color(50, 195, 80, 200));
        g2.fillRect(barX + (int)(barW * 0.35f), barY, (int)(barW * 0.30f), barH);
        g2.setColor(new Color(75, 75, 105));
        g2.drawRoundRect(barX, barY, barW, barH, 8, 8);

        // Needle
        int needleX = barX + (int)(dishNeedle * barW);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(needleX, barY - 6, needleX, barY + barH + 6);
        g2.setStroke(new BasicStroke(1));
        g2.fillOval(needleX - 5, barY - 8, 10, 10);

        // Zone labels
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(200, 75, 75));
        g2.drawString("MISS", barX + 10, barY + 20);
        g2.drawString("MISS", barX + barW - 34, barY + 20);
        g2.setColor(new Color(50, 215, 90));
        g2.drawString("SCRUB ZONE", barX + (int)(barW * 0.39f), barY + 20);

        // Feedback text
        if (dishFeedbackTick > 0) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            if (dishFeedback == 1) {
                g2.setColor(new Color(80, 255, 120));
                String ok = "NICE!";
                fm = g2.getFontMetrics();
                g2.drawString(ok, (W - fm.stringWidth(ok))/2, 230);
            } else {
                g2.setColor(new Color(255, 80, 80));
                String ms = "MISS!";
                fm = g2.getFontMetrics();
                g2.drawString(ms, (W - fm.stringWidth(ms))/2, 230);
            }
        }
    }
}
