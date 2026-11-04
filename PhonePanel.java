package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Phone overlay — draws the current room as background and renders a small
 * realistic phone on top so it looks like the player is holding the phone
 * inside whatever room they were in.
 *
 * Apps: Messages (Mom / Friend), Delivery, Music, Flappy Bird
 * Bottom nav: Back  ◁   Home  ○   Charge  ⚡
 */
public class PhonePanel extends RoomPanel {

    // ── Phone screen state ─────────────────────────────────────────────────
    private String screen = "HOME";
    // HOME | MESSAGES | MSG_MOM | MSG_FRIEND | DELIVERY | MUSIC | GAME

    // ── Delivery ───────────────────────────────────────────────────────────
    private String selectedDelivery = "Food";  // current combo selection

    // ── Flappy Bird ────────────────────────────────────────────────────────
    private FlappyBird flappy = null;

    // ── Phone frame constants (logical 800×455 space) ──────────────────────
    // The phone sits right-of-centre; the left ~38% of the screen shows the room.
    static final int PX = 290, PY = 5,  PW = 222, PH = 445;
    // Screen inside the phone frame
    static final int SX = PX + 7, SY = PY + 54, SW = PW - 14, SH = 330;
    // Content area (below 22-px status bar)
    static final int CY = SY + 22;   // content top
    static final int CH = SH - 22;   // content height  ≈ 308
    // Navigation bar
    static final int NV_Y = SY + SH + 3;  // nav bar top  (≈ 392)
    static final int NV_H = 40;

    // ── Constructor ────────────────────────────────────────────────────────
    public PhonePanel() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onEnter(); }
        });
        // Flap / interact with Flappy Bird on click inside game area
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!"GAME".equals(screen)) return;
                float scX = getWidth()  > 0 ? 800f / getWidth()  : 1f;
                float scY = getHeight() > 0 ? 455f / getHeight() : 1f;
                int lx = (int)(e.getX() * scX);
                int ly = (int)(e.getY() * scY);
                if (lx >= SX && lx <= SX + SW && ly >= CY && ly <= SY + SH) {
                    if (flappy != null) { flappy.flap(); repaint(); }
                }
            }
        });
        onEnter();
    }

    /** Called by GamePanel when the player opens the phone from any room. */
    public void resetToHome() {
        if (flappy != null) { flappy.stop(); flappy = null; }
        screen = "HOME";
    }

    private int bx(int v) { int w = getWidth();  return w <= 0 ? v : Math.round(v * w / 800f); }
    private int by(int v) { int h = getHeight(); return h <= 0 ? v : Math.round(v * h / 455f); }

    // ── onEnter ────────────────────────────────────────────────────────────
    @Override
    public void onEnter() {
        // Stop flappy bird when leaving the game screen
        if (!"GAME".equals(screen) && flappy != null) { flappy.stop(); flappy = null; }
        removeAll();
        buildButtons();
        revalidate();
        repaint();
    }

    // ── Button builder ─────────────────────────────────────────────────────
    private void buildButtons() {
        boolean hasPower = GameState.phoneBattery > 5f;

        // ── Always-present nav bar ─────────────────────────────────────────
        int nbBtnW = 58, nbBtnH = 28;
        // Back
        JButton backBtn = phoneBtn("◁  Back", bx(SX + 4), by(NV_Y + 6), bx(nbBtnW), by(nbBtnH));
        backBtn.setForeground(new Color(180, 185, 230));
        backBtn.addActionListener(e -> navigateBack());
        add(backBtn);
        // Home
        JButton homeBtn = phoneBtn("○  Home", bx(SX + SW/2 - nbBtnW/2 + 2), by(NV_Y + 6), bx(nbBtnW), by(nbBtnH));
        homeBtn.setForeground(new Color(180, 185, 230));
        homeBtn.addActionListener(e -> { if (!"HOME".equals(screen)) { screen = "HOME"; onEnter(); } });
        add(homeBtn);
        // Charge / Unplug icon on right
        if (GameState.phoneCharging) {
            JButton unplug = phoneBtn("✕", bx(SX + SW - 30), by(NV_Y + 6), bx(28), by(nbBtnH));
            unplug.setForeground(new Color(80, 210, 255));
            unplug.addActionListener(e -> { GameState.phoneCharging = false; onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Phone unplugged."); });
            add(unplug);
        } else if (GameState.phoneBattery < 100f) {
            JButton charge = phoneBtn("⚡", bx(SX + SW - 30), by(NV_Y + 6), bx(28), by(nbBtnH));
            charge.setForeground(new Color(100, 255, 150));
            charge.addActionListener(e -> {
                GameState.phoneCharging = true; onEnter();
                String msg = GameState.phoneChargeCount >= 3
                    ? "WARNING: Overloading the circuits!" : "Charging... (#" + (GameState.phoneChargeCount+1) + ")";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(charge);
        }

        // ── Screen content ─────────────────────────────────────────────────
        switch (screen) {
            case "HOME":       buildHomeScreen(hasPower);     break;
            case "MESSAGES":   buildMessagesList();            break;
            case "MSG_MOM":    buildMomConvo(hasPower);        break;
            case "MSG_FRIEND": buildFriendConvo(hasPower);     break;
            case "DELIVERY":   buildDeliveryApp(hasPower);     break;
            case "MUSIC":      buildMusicApp(hasPower);        break;
            case "GAME":       buildGameScreen(hasPower);      break;
        }
    }

    // ── HOME SCREEN ────────────────────────────────────────────────────────
    private void buildHomeScreen(boolean hasPower) {
        int iW = 60, iH = 54;  // icon size in logical pixels
        int row1 = CY + 14, row2 = CY + 82, row3 = CY + 150;
        int c1 = SX + 5, c2 = SX + 75, c3 = SX + 145;

        // Messages
        JButton msg = appIcon("Messages", new Color(60, 140, 255), bx(c1), by(row1), bx(iW), by(iH));
        msg.setEnabled(hasPower);
        msg.addActionListener(e -> { screen = "MESSAGES"; onEnter(); });
        add(msg);

        // Delivery
        JButton del = appIcon("Delivery", new Color(255, 150, 40), bx(c2), by(row1), bx(iW), by(iH));
        del.setEnabled(hasPower);
        del.addActionListener(e -> { screen = "DELIVERY"; onEnter(); });
        add(del);

        // Music
        JButton mus = appIcon("Music", new Color(160, 80, 255), bx(c3), by(row1), bx(iW), by(iH));
        if (GameState.musicPlaying) mus.setForeground(new Color(255, 100, 220));
        mus.setEnabled(hasPower);
        mus.addActionListener(e -> { screen = "MUSIC"; onEnter(); });
        add(mus);

        // Games
        JButton gm = appIcon("Games", new Color(60, 200, 90), bx(c1), by(row2), bx(iW), by(iH));
        gm.setEnabled(hasPower);
        gm.addActionListener(e -> { screen = "GAME"; onEnter(); });
        add(gm);

        // Charging status if charging
        if (GameState.phoneBattery >= 100f) {
            JButton full = phoneBtn("Phone: Fully Charged ✓", bx(SX+4), by(row3), bx(SW-8), by(28));
            full.setEnabled(false); full.setForeground(new Color(80, 220, 120));
            add(full);
        }
        // Notification for friend text
        if (GameState.friendTextPending) {
            JButton notif = phoneBtn("📩  New text from Friend!", bx(SX+4), by(row3 + 38), bx(SW-8), by(28));
            notif.setForeground(new Color(100, 255, 180));
            notif.addActionListener(e -> { screen = "MSG_FRIEND"; onEnter(); });
            add(notif);
        }
    }

    // ── MESSAGES LIST ──────────────────────────────────────────────────────
    private void buildMessagesList() {
        int rowH = 44, gap = 6;
        int y1 = CY + 10, y2 = y1 + rowH + gap;

        JButton momBtn = contactRow("Mom", "👩  Mom", new Color(255, 200, 100),
                bx(SX + 4), by(y1), bx(SW - 8), by(rowH));
        momBtn.addActionListener(e -> { screen = "MSG_MOM"; onEnter(); });
        add(momBtn);

        String friendLabel = GameState.friendTextPending ? "👦  Friend  [new!]" : "👦  Friend";
        Color friendCol = GameState.friendTextPending ? new Color(100, 255, 180) : new Color(130, 200, 255);
        JButton frndBtn = contactRow("Friend", friendLabel, friendCol,
                bx(SX + 4), by(y2), bx(SW - 8), by(rowH));
        frndBtn.addActionListener(e -> { screen = "MSG_FRIEND"; onEnter(); });
        add(frndBtn);
    }

    // ── MOM CONVERSATION ───────────────────────────────────────────────────
    private void buildMomConvo(boolean hasPower) {
        int btnW = SW - 8, btnH = 36, btnX = SX + 4;
        int y = CY + 140;

        JButton call = phoneBtn("📞  Call Mom", bx(btnX), by(y), bx(btnW), by(btnH));
        call.setForeground(new Color(255, 200, 100));
        call.setEnabled(hasPower);
        call.addActionListener(e -> {
            GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 6f, 0, 100);
            GameState.stress       = GameLoop.clamp(GameState.stress - 5f, 0, 100);
            onEnter();
            GameFrame.getInstance().getGamePanel().showMessage("Called Mom. She says be good! Stress down.");
        });
        add(call);

        JButton text = phoneBtn("💬  Text Mom", bx(btnX), by(y + btnH + 8), bx(btnW), by(btnH));
        text.setForeground(new Color(200, 200, 255));
        text.setEnabled(hasPower);
        text.addActionListener(e -> {
            GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 2f, 0, 100);
            GameState.stress       = GameLoop.clamp(GameState.stress - 2f, 0, 100);
            onEnter();
            GameFrame.getInstance().getGamePanel().showMessage("Texted Mom. She replied with a heart emoji.");
        });
        add(text);
    }

    // ── FRIEND CONVERSATION ────────────────────────────────────────────────
    private void buildFriendConvo(boolean hasPower) {
        int btnW = SW - 8, btnH = 36, btnX = SX + 4;
        int y = CY + 130;

        JButton call = phoneBtn("📞  Call Friend", bx(btnX), by(y), bx(btnW), by(btnH));
        call.setForeground(new Color(100, 210, 255));
        call.setEnabled(hasPower);
        call.addActionListener(e -> {
            GameState.stress       = GameLoop.clamp(GameState.stress - 15f, 0, 100);
            GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 8f, 0, 100);
            if (GameState.friendTextPending) { GameState.friendTextPending = false; GameState.friendTextIgnores = 0; }
            onEnter();
            GameFrame.getInstance().getGamePanel().showMessage("Called your friend! Big stress drop. 😊");
        });
        add(call);

        if (GameState.friendTextPending) {
            JButton reply = phoneBtn("💬  Reply to Text", bx(btnX), by(y + btnH + 8), bx(btnW), by(btnH));
            reply.setForeground(new Color(100, 255, 180));
            reply.setEnabled(hasPower);
            reply.addActionListener(e -> {
                GameState.friendTextPending = false;
                GameState.friendTextIgnores = 0;
                GameState.stress       = GameLoop.clamp(GameState.stress - 8f, 0, 100);
                GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 3f, 0, 100);
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Replied to your friend! Stress down.");
            });
            add(reply);

            JButton ignore = phoneBtn("🚫  Ignore", bx(btnX), by(y + (btnH + 8)*2), bx(btnW), by(btnH));
            ignore.setForeground(new Color(160, 130, 130));
            ignore.addActionListener(e -> {
                GameState.friendTextPending = false;
                GameState.friendTextIgnores++;
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Ignored... guilt creeping in.");
            });
            add(ignore);
        } else {
            JButton text = phoneBtn("💬  Text Friend", bx(btnX), by(y + btnH + 8), bx(btnW), by(btnH));
            text.setForeground(new Color(160, 200, 255));
            text.setEnabled(hasPower);
            text.addActionListener(e -> {
                GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 2f, 0, 100);
                GameState.stress       = GameLoop.clamp(GameState.stress - 3f, 0, 100);
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Texted your friend. They sent a meme 😂. Stress down.");
            });
            add(text);
        }
    }

    // ── DELIVERY APP ───────────────────────────────────────────────────────
    private void buildDeliveryApp(boolean hasPower) {
        int btnW = SW - 8, btnX = SX + 4;
        int y = CY + 14;

        // Dropdown (ComboBox)
        String[] items = {
            "Food — 3 coins",
            "Drinks — 2 coins",
            "Cold Medicine — 2 coins",
            "Bluetooth Speaker — 4 coins",
            "Detergent — 2 coins"
        };
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBounds(bx(btnX), by(y), bx(btnW), by(30));
        combo.setBackground(new Color(28, 30, 55));
        combo.setForeground(new Color(210, 220, 255));
        combo.setFont(new Font("SansSerif", Font.BOLD, 10));
        // Sync with saved selection
        for (int i = 0; i < items.length; i++) {
            if (items[i].startsWith(selectedDelivery)) { combo.setSelectedIndex(i); break; }
        }
        combo.addActionListener(e -> {
            String sel = (String) combo.getSelectedItem();
            if (sel != null) selectedDelivery = sel.split(" —")[0];
        });
        add(combo);

        // Order button
        int cost = costOf(selectedDelivery);
        boolean canOrder = hasPower && GameState.coins >= cost && !GameState.deliveryOrdered
                && !isColdMedOrdering() && !isSpeakerBought() && !isDetOrdering();
        JButton orderBtn = phoneBtn("Order  (" + cost + " coins)", bx(btnX), by(y + 40), bx(btnW), by(36));
        orderBtn.setForeground(new Color(255, 175, 60));
        orderBtn.setEnabled(canOrder);
        orderBtn.addActionListener(e -> placeOrder());
        add(orderBtn);

        // Status labels below the button (y+86 onward)
        int sy2 = y + 86;
        if (GameState.deliveryOrdered && !GameState.deliveryArrived) {
            JButton wait = phoneBtn("🚚  En route... (" + GameState.deliveryTicks/2 + "s)",
                    bx(btnX), by(sy2), bx(btnW), by(28));
            wait.setEnabled(false); wait.setForeground(new Color(255, 200, 100));
            add(wait);
        } else if (GameState.deliveryArrived && "Food".equals(GameState.deliveryItemType)) {
            JButton arr = phoneBtn("✅  Food in Kitchen!", bx(btnX), by(sy2), bx(btnW), by(28));
            arr.setEnabled(false); arr.setForeground(new Color(100, 255, 130));
            add(arr);
        }
        if (GameState.detergentOrdered && !GameState.detergentArrived) {
            JButton dw = phoneBtn("🚚  Detergent en route... (" + GameState.detergentTicks/2 + "s)",
                    bx(btnX), by(sy2 + 34), bx(btnW), by(28));
            dw.setEnabled(false); dw.setForeground(new Color(100, 200, 255));
            add(dw);
        } else if (GameState.detergentArrived) {
            JButton da = phoneBtn("✅  Detergent in Basement!", bx(btnX), by(sy2 + 34), bx(btnW), by(28));
            da.setEnabled(false); da.setForeground(new Color(100, 255, 130));
            add(da);
        }
        if (GameState.coldMedOrdered && !GameState.coldMedArrived) {
            JButton mw = phoneBtn("🚚  Medicine en route... (" + GameState.coldMedTicks/2 + "s)",
                    bx(btnX), by(sy2 + 68), bx(btnW), by(28));
            mw.setEnabled(false); mw.setForeground(new Color(140, 200, 255));
            add(mw);
        } else if (GameState.coldMedArrived) {
            JButton ma = phoneBtn("✅  Medicine Arrived! (take it here)", bx(btnX), by(sy2 + 68), bx(btnW), by(28));
            ma.setForeground(new Color(100, 255, 220));
            ma.addActionListener(ev -> {
                if (GameState.hasCold) { GameState.hasCold = false; GameState.coldMedArrived = false; }
                onEnter();
                GameFrame.getInstance().getGamePanel().showMessage("Took the cold medicine. Feeling better!");
            });
            add(ma);
        }
        if (GameState.hasSpeaker) {
            JButton sp = phoneBtn("🔊  Speaker active — music boost!", bx(btnX), by(sy2 + 102), bx(btnW), by(28));
            sp.setEnabled(false); sp.setForeground(new Color(200, 120, 255));
            add(sp);
        }
    }

    private int costOf(String item) {
        switch (item) {
            case "Drinks":             return 2;
            case "Cold Medicine":      return 2;
            case "Detergent":          return 2;
            case "Bluetooth Speaker":  return 4;
            default:                   return 3; // Food
        }
    }
    private boolean isColdMedOrdering() { return GameState.coldMedOrdered; }
    private boolean isSpeakerBought()   { return GameState.hasSpeaker; }
    private boolean isDetOrdering()     { return GameState.detergentOrdered; }

    private void placeOrder() {
        int cost = costOf(selectedDelivery);
        if (GameState.coins < cost) return;
        GameState.coins         -= cost;
        GameState.phoneBattery   = GameLoop.clamp(GameState.phoneBattery - 4f, 0, 100);
        switch (selectedDelivery) {
            case "Food":
                GameState.deliveryOrdered  = true;
                GameState.deliveryItemType = "Food";
                GameState.deliveryTicks    = 20;
                GameFrame.getInstance().getGamePanel().showMessage("Food ordered! Arrives at the kitchen in ~10s.");
                break;
            case "Drinks":
                GameState.deliveryOrdered  = true;
                GameState.deliveryItemType = "Drinks";
                GameState.deliveryTicks    = 16;
                GameFrame.getInstance().getGamePanel().showMessage("Drinks ordered! Coming in ~8s.");
                break;
            case "Cold Medicine":
                GameState.coldMedOrdered = true;
                GameState.coldMedTicks   = 15;
                GameFrame.getInstance().getGamePanel().showMessage("Medicine ordered! Coming in ~8s.");
                break;
            case "Bluetooth Speaker":
                GameState.hasSpeaker = true;
                GameFrame.getInstance().getGamePanel().showMessage("Speaker delivered instantly! Music is more effective now.");
                break;
            case "Detergent":
                GameState.detergentOrdered = true;
                GameState.detergentTicks   = 20;
                GameFrame.getInstance().getGamePanel().showMessage("Detergent ordered! Arrives at basement in ~10s.");
                break;
        }
        onEnter();
    }

    // ── MUSIC APP ─────────────────────────────────────────────────────────
    private void buildMusicApp(boolean hasPower) {
        int btnW = SW - 8, btnH = 38, btnX = SX + 4, gap = 8;
        int y = CY + 14;

        String[] genres = {"🎸  Pop", "🎤  Hip-Hop", "🎻  Classical", "🎧  Lo-fi"};
        String[] keys   = {"Pop", "Hip-Hop", "Classical", "Lo-fi"};
        Color[]  cols   = {
            new Color(255, 100, 150),
            new Color(120, 80, 255),
            new Color(80, 180, 255),
            new Color(180, 255, 160)
        };
        for (int i = 0; i < genres.length; i++) {
            final String g = keys[i];
            boolean playing = GameState.musicPlaying && g.equals(GameState.musicGenre);
            String label = playing ? genres[i] + "  ♫ playing" : genres[i];
            JButton gb = phoneBtn(label, bx(btnX), by(y + i * (btnH + gap)), bx(btnW), by(btnH));
            gb.setForeground(playing ? cols[i].brighter() : cols[i]);
            gb.setEnabled(hasPower);
            if (playing) {
                // Click again to stop
                gb.addActionListener(e -> {
                    GameState.musicPlaying = false;
                    GameState.musicGenre   = "";
                    onEnter();
                    GameFrame.getInstance().getGamePanel().showMessage("Music stopped.");
                });
            } else {
                gb.addActionListener(e -> {
                    GameState.musicPlaying = true;
                    GameState.musicGenre   = g;
                    GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 2f, 0, 100);
                    onEnter();
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Now playing: " + g + (GameState.hasSpeaker ? " 🔊 Speaker boost!" : ""));
                });
            }
            add(gb);
        }
    }

    // ── GAME (Flappy Bird) ─────────────────────────────────────────────────
    private void buildGameScreen(boolean hasPower) {
        if (!hasPower) {
            JButton dead = phoneBtn("No battery — charge the phone!", bx(SX+4), by(CY+100), bx(SW-8), by(36));
            dead.setEnabled(false); dead.setForeground(new Color(200, 80, 80));
            add(dead);
            return;
        }
        // Start flappy if not running
        if (flappy == null) {
            flappy = new FlappyBird();
            flappy.start();
        }
        // Hint label at top of game area
        JLabel hint = new JLabel("Tap the screen to flap!", JLabel.CENTER);
        hint.setBounds(bx(SX), by(CY), bx(SW), by(16));
        hint.setFont(new Font("SansSerif", Font.ITALIC, 8));
        hint.setForeground(new Color(180, 200, 255, 180));
        add(hint);
    }

    // ── Navigation helper ──────────────────────────────────────────────────
    private void navigateBack() {
        switch (screen) {
            case "HOME":
                GameFrame.getInstance().getGamePanel().goToRoom(GameState.roomBeforePhone);
                break;
            case "MESSAGES":
                screen = "HOME";     onEnter(); break;
            case "MSG_MOM": case "MSG_FRIEND":
                screen = "MESSAGES"; onEnter(); break;
            default:
                screen = "HOME";     onEnter(); break;
        }
    }

    // ── Button factories ───────────────────────────────────────────────────
    /** Small phone-style button (plain dark background). */
    private JButton phoneBtn(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                boolean hover = getModel().isRollover(), press = getModel().isPressed();
                boolean en    = isEnabled();
                Color bg = !en ? new Color(20,20,40,160) :
                           press ? new Color(50,60,100,220) :
                           hover ? new Color(42,50,88,220) :
                                   new Color(28,32,62,210);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, W, H, 8, 8);
                Color border = hover ? new Color(120,140,220) : new Color(60,70,120,180);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, W-1, H-1, 8, 8);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (W - fm.stringWidth(getText())) / 2;
                int ty = (H + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(!en ? new Color(90,95,120) : getForeground());
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        b.setBounds(x, y, w, h);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(new Color(210, 215, 255));
        return b;
    }

    /** App icon button (rounded tile with label). */
    private JButton appIcon(String label, Color accent, int x, int y, int w, int h) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int W = getWidth(), H = getHeight();
                boolean hover = getModel().isRollover(), press = getModel().isPressed();
                boolean en = isEnabled();
                // Tile background
                Color bg = !en ? new Color(20,20,35,150) :
                           press ? accent.darker().darker() :
                           hover ? accent.darker() : new Color(accent.getRed()/4, accent.getGreen()/4, accent.getBlue()/4 + 20, 220);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, W, H, 12, 12);
                // Accent border
                g2.setColor(en ? (hover ? accent : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140))
                               : new Color(50,55,80,120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, W-1, H-1, 12, 12);
                // Icon (large emoji-style circle)
                g2.setColor(en ? accent : new Color(80,80,100,160));
                g2.fillOval(W/2-12, 5, 24, 24);
                // Label
                g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(!en ? new Color(90,95,120) : Color.WHITE);
                g2.drawString(label, (W - fm.stringWidth(label))/2, H - 7);
                // Notification dot
                if ("Messages".equals(label) && GameState.friendTextPending) {
                    g2.setColor(new Color(255, 60, 60));
                    g2.fillOval(W - 14, 3, 11, 11);
                }
                if ("Music".equals(label) && GameState.musicPlaying) {
                    g2.setColor(new Color(100, 255, 200));
                    g2.fillOval(W - 14, 3, 11, 11);
                }
                g2.dispose();
            }
        };
        b.setBounds(x, y, w, h);
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Contact row button (name + subtle background). */
    private JButton contactRow(String name, String label, Color fg, int x, int y, int w, int h) {
        JButton b = phoneBtn(label, x, y, w, h);
        b.setForeground(fg);
        return b;
    }

    // ── PaintComponent ─────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        float sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float sy = getHeight() <= 0 ? 1f : getHeight() / 455f;
        g2.scale(sx, sy);
        int W = 800, H = 455;

        // ── 1. Room background (shows around the phone) ────────────────────
        drawBg(g2, roomBgAsset(), W, H);

        // Semi-dark tint on room so phone stands out
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRect(0, 0, PX - 2, H);
        g2.fillRect(PX + PW + 2, 0, W - PX - PW - 2, H);

        // ── 2. Phone body ──────────────────────────────────────────────────
        drawPhoneBody(g2);

        // ── 3. Phone screen background (OLED dark) ─────────────────────────
        g2.setColor(new Color(10, 11, 22));
        g2.fillRoundRect(SX, SY, SW, SH, 6, 6);

        // ── 4. Status bar ──────────────────────────────────────────────────
        drawStatusBar(g2);

        // ── 5. Screen title ────────────────────────────────────────────────
        drawScreenTitle(g2);

        // ── 6. App content (painted parts) ────────────────────────────────
        drawAppContent(g2);

        // ── 7. Nav bar background ──────────────────────────────────────────
        g2.setColor(new Color(16, 18, 38, 220));
        g2.fillRoundRect(SX, NV_Y, SW, NV_H, 4, 4);
        g2.setColor(new Color(50, 55, 100, 140));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(SX, NV_Y, SX + SW, NV_Y);
        g2.setStroke(new BasicStroke(1));

        g2.dispose();
    }

    private String roomBgAsset() {
        switch (GameState.roomBeforePhone) {
            case "KITCHEN":   return "kitchen";
            case "GARAGE":    return "garage";
            case "BEDROOM":   return "bedroom";
            case "OUTSIDE":   return "outdoor";
            case "MOMS_ROOM": return "moms_room";
            case "BATHROOM":  return "bathroom";
            case "BASEMENT":  return "basement";
            default:          return "living_room";
        }
    }

    private void drawPhoneBody(Graphics2D g2) {
        // Shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(PX + 5, PY + 6, PW, PH, 28, 28);
        // Body gradient
        GradientPaint bodyGP = new GradientPaint(PX, PY, new Color(38, 40, 62),
                PX + PW, PY + PH, new Color(22, 24, 40));
        g2.setPaint(bodyGP); g2.fillRoundRect(PX, PY, PW, PH, 28, 28); g2.setPaint(null);
        // Border / bezel
        g2.setColor(new Color(70, 78, 120, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(PX, PY, PW, PH, 28, 28);
        g2.setStroke(new BasicStroke(1));
        // Camera pill notch
        g2.setColor(new Color(18, 20, 36));
        g2.fillRoundRect(PX + PW/2 - 22, PY + 9, 44, 14, 10, 10);
        g2.setColor(new Color(14, 55, 90));
        g2.fillOval(PX + PW/2 - 5, PY + 11, 10, 10);
        g2.setColor(new Color(30, 100, 140, 160));
        g2.fillOval(PX + PW/2 - 3, PY + 13, 6, 6);
        // Side buttons (volume / power)
        g2.setColor(new Color(55, 58, 90));
        g2.fillRoundRect(PX - 3, PY + 80, 5, 28, 3, 3);
        g2.fillRoundRect(PX - 3, PY + 118, 5, 28, 3, 3);
        g2.fillRoundRect(PX + PW - 2, PY + 100, 5, 42, 3, 3);
        // Home bar at bottom
        g2.setColor(new Color(90, 98, 150, 200));
        g2.fillRoundRect(PX + PW/2 - 38, PY + PH - 16, 76, 5, 4, 4);
    }

    private void drawStatusBar(Graphics2D g2) {
        // Black bar
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(SX, SY, SW, 22);
        // Time placeholder
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(200, 210, 240));
        g2.drawString("12:00", SX + 6, SY + 14);
        // Battery icon
        float bat = GameState.phoneBattery;
        Color batCol = bat > 50 ? new Color(60, 220, 100)
                     : bat > 25 ? new Color(230, 200, 40)
                     :            new Color(220, 60, 50);
        if (GameState.phoneCharging) batCol = new Color(80, 200, 255);
        int bw = 30, bh = 12, bx2 = SX + SW - 38, by2 = SY + 5;
        g2.setColor(new Color(40, 42, 70));
        g2.fillRoundRect(bx2, by2, bw, bh, 3, 3);
        g2.setColor(batCol);
        g2.fillRoundRect(bx2, by2, (int)(bw * bat / 100f), bh, 3, 3);
        g2.setColor(new Color(100, 110, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bx2, by2, bw, bh, 3, 3);
        g2.fillRoundRect(bx2 + bw, by2 + 3, 3, bh - 6, 2, 2);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        g2.setColor(Color.WHITE);
        String bLabel = (int)bat + "%";
        g2.drawString(bLabel, bx2 + (bw - g2.getFontMetrics().stringWidth(bLabel))/2, by2 + 9);
        // Music indicator
        if (GameState.musicPlaying) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(180, 120, 255));
            g2.drawString("♫", SX + SW/2 - 4, SY + 14);
        }
    }

    private void drawScreenTitle(Graphics2D g2) {
        String title;
        Color  tColor;
        switch (screen) {
            case "MESSAGES":   title = "Messages";            tColor = new Color(100, 180, 255); break;
            case "MSG_MOM":    title = "Mom";                 tColor = new Color(255, 200, 100); break;
            case "MSG_FRIEND": title = "Friend";              tColor = new Color(100, 220, 255); break;
            case "DELIVERY":   title = "Delivery";            tColor = new Color(255, 165, 60);  break;
            case "MUSIC":      title = "Music";               tColor = new Color(190, 100, 255); break;
            case "GAME":       title = "Flappy Bird";         tColor = new Color(120, 255, 120); break;
            default:           title = "Phone";               tColor = new Color(160, 170, 220); break;
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(title, SX + (SW - fm.stringWidth(title))/2 + 1, CY + 14 + 1);
        g2.setColor(tColor);
        g2.drawString(title, SX + (SW - fm.stringWidth(title))/2, CY + 14);
    }

    private void drawAppContent(Graphics2D g2) {
        switch (screen) {
            case "HOME":       drawHomeContent(g2);    break;
            case "MESSAGES":   drawMessagesContent(g2); break;
            case "MSG_MOM":    drawMomContent(g2);     break;
            case "MSG_FRIEND": drawFriendContent(g2);  break;
            case "DELIVERY":   drawDeliveryContent(g2); break;
            case "MUSIC":      drawMusicContent(g2);   break;
            case "GAME":       drawGameContent(g2);    break;
        }
    }

    private void drawHomeContent(Graphics2D g2) {
        // Wallpaper gradient
        GradientPaint wp = new GradientPaint(SX, CY, new Color(14, 18, 42), SX, SY + SH, new Color(8, 10, 30));
        g2.setPaint(wp); g2.fillRect(SX, CY, SW, SH - 22); g2.setPaint(null);
        // Icon label backgrounds are drawn by button paintComponent
        // Notification badges drawn per appIcon button
    }

    private void drawMessagesContent(Graphics2D g2) {
        g2.setColor(new Color(12, 14, 32));
        g2.fillRect(SX, CY, SW, CH);
        // Separator line between contacts
        g2.setColor(new Color(40, 45, 80));
        g2.drawLine(SX + 4, CY + 62, SX + SW - 4, CY + 62);
    }

    private void drawMomContent(Graphics2D g2) {
        g2.setColor(new Color(12, 14, 32));
        g2.fillRect(SX, CY, SW, CH);
        // Mom avatar circle
        g2.setColor(new Color(255, 200, 100, 180));
        g2.fillOval(SX + SW/2 - 22, CY + 22, 44, 44);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(new Color(30, 20, 10));
        g2.drawString("M", SX + SW/2 - 7, CY + 52);
        // Divider
        g2.setColor(new Color(40, 45, 80));
        g2.drawLine(SX + 4, CY + 80, SX + SW - 4, CY + 80);
        // Example chat bubble
        g2.setColor(new Color(40, 50, 90, 200));
        g2.fillRoundRect(SX + 10, CY + 90, SW - 50, 30, 10, 10);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(new Color(200, 210, 240));
        g2.drawString("Behave! I'll be home soon ❤", SX + 16, CY + 110);
    }

    private void drawFriendContent(Graphics2D g2) {
        g2.setColor(new Color(12, 14, 32));
        g2.fillRect(SX, CY, SW, CH);
        // Friend avatar
        g2.setColor(new Color(100, 200, 255, 180));
        g2.fillOval(SX + SW/2 - 22, CY + 22, 44, 44);
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(new Color(10, 20, 35));
        g2.drawString("F", SX + SW/2 - 7, CY + 52);
        g2.setColor(new Color(40, 45, 80));
        g2.drawLine(SX + 4, CY + 80, SX + SW - 4, CY + 80);
        if (GameState.friendTextPending) {
            g2.setColor(new Color(30, 50, 90, 200));
            g2.fillRoundRect(SX + 10, CY + 90, SW - 50, 30, 10, 10);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            g2.setColor(new Color(100, 255, 180));
            g2.drawString("Yo what's up, u home alone?", SX + 16, CY + 110);
        }
    }

    private void drawDeliveryContent(Graphics2D g2) {
        g2.setColor(new Color(12, 14, 32));
        g2.fillRect(SX, CY, SW, CH);
    }

    private void drawMusicContent(Graphics2D g2) {
        g2.setColor(new Color(12, 14, 32));
        g2.fillRect(SX, CY, SW, CH);
        // Now playing info (if playing)
        if (GameState.musicPlaying) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(180, 130, 255));
            String np = "♫  Now Playing: " + GameState.musicGenre;
            g2.drawString(np, SX + (SW - g2.getFontMetrics().stringWidth(np))/2, CY + 220);
            if (GameState.hasSpeaker) {
                g2.setColor(new Color(130, 255, 200));
                String sp = "🔊 Speaker boost active!";
                g2.drawString(sp, SX + (SW - g2.getFontMetrics().stringWidth(sp))/2, CY + 234);
            }
        }
    }

    private void drawGameContent(Graphics2D g2) {
        // Game area: from CY+20 down to SY+SH (excluding status bar area)
        int gY = CY + 20;
        int gH = SY + SH - gY;
        if (flappy != null) {
            flappy.draw(g2, SX, gY, SW, gH);
        } else {
            g2.setColor(new Color(135, 206, 235));
            g2.fillRect(SX, gY, SW, gH);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString("Loading...", SX + SW/2 - 30, gY + gH/2);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FLAPPY BIRD INNER CLASS
    // ══════════════════════════════════════════════════════════════════════
    private class FlappyBird {

        // Game area dimensions (local coords, set from draw call)
        private int GW = 208, GH = 280;

        // Bird
        private float birdY  = 130f;
        private float birdVY = 0f;
        private boolean alive   = true;
        private boolean started = false;

        // Pipes  [pipeX, topHeight, scored(0/1)]
        private final List<int[]> pipes = new ArrayList<>();

        // Score
        private int score = 0;
        private int bestScore = 0;
        private int frame = 0;

        // Physics
        static final float GRAVITY   = 0.42f;
        static final float FLAP_VEL  = -7.2f;
        static final int   BIRD_X    = 48;
        static final int   BIRD_R    = 9;
        static final int   PIPE_W    = 28;
        static final int   PIPE_GAP  = 76;
        static final int   PIPE_SPEED = 2;

        private Timer timer;

        void start() {
            timer = new Timer(33, e -> update());
            timer.start();
        }

        void stop() {
            if (timer != null) { timer.stop(); timer = null; }
        }

        boolean isRunning() { return timer != null && timer.isRunning(); }

        void flap() {
            if (!alive) { restart(); return; }
            started = true;
            birdVY  = FLAP_VEL;
            // Drain phone battery slightly per flap session
            GameState.phoneBattery = GameLoop.clamp(GameState.phoneBattery - 0.5f, 0f, 100f);
        }

        void restart() {
            birdY    = GH / 2f - 20;
            birdVY   = 0f;
            alive    = true;
            started  = false;
            frame    = 0;
            score    = 0;
            pipes.clear();
        }

        void update() {
            if (!started) { repaint(); return; }
            birdVY += GRAVITY;
            birdY  += birdVY;
            frame++;

            // Spawn a new pipe every 90 frames
            if (frame % 90 == 10) {
                int topH = 24 + (int)(Math.random() * (GH - PIPE_GAP - 48));
                pipes.add(new int[]{GW, topH, 0});
            }
            // Move pipes
            for (int[] p : pipes) p[0] -= PIPE_SPEED;

            // Score
            for (int[] p : pipes) {
                if (p[2] == 0 && p[0] + PIPE_W < BIRD_X) {
                    p[2] = 1; score++;
                    if (score > bestScore) bestScore = score;
                }
            }
            pipes.removeIf(p -> p[0] + PIPE_W < 0);

            // Collision
            if (birdY - BIRD_R < 0 || birdY + BIRD_R > GH - 14) { die(); return; }
            for (int[] p : pipes) {
                if (BIRD_X + BIRD_R > p[0] && BIRD_X - BIRD_R < p[0] + PIPE_W) {
                    if (birdY - BIRD_R < p[1] || birdY + BIRD_R > p[1] + PIPE_GAP) { die(); return; }
                }
            }
            repaint();
        }

        private void die() {
            alive   = false;
            started = false;
            repaint();
        }

        void draw(Graphics2D g2, int ox, int oy, int w, int h) {
            GW = w; GH = h;

            // Sky
            GradientPaint sky = new GradientPaint(ox, oy, new Color(100, 180, 255), ox, oy+h, new Color(180, 220, 255));
            g2.setPaint(sky); g2.fillRect(ox, oy, w, h); g2.setPaint(null);

            // Clouds (static decorative)
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRoundRect(ox + 20,  oy + 18, 36, 16, 14, 14);
            g2.fillRoundRect(ox + 110, oy + 30, 28, 12, 10, 10);

            // Ground
            g2.setColor(new Color(160, 100, 40));
            g2.fillRect(ox, oy + h - 14, w, 14);
            g2.setColor(new Color(80, 175, 55));
            g2.fillRect(ox, oy + h - 14, w, 7);

            // Pipes
            for (int[] p : pipes) {
                int px = ox + p[0];
                // Top pipe
                g2.setColor(new Color(50, 160, 50));
                g2.fillRect(px, oy, PIPE_W, p[1]);
                g2.setColor(new Color(30, 120, 30));
                g2.fillRect(px - 3, oy + p[1] - 14, PIPE_W + 6, 14);
                g2.setColor(new Color(80, 200, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRect(px, oy, PIPE_W, p[1]);
                g2.drawRect(px - 3, oy + p[1] - 14, PIPE_W + 6, 14);
                // Bottom pipe
                int botY = oy + p[1] + PIPE_GAP;
                int botH = (oy + h - 14) - botY;
                if (botH > 0) {
                    g2.setColor(new Color(50, 160, 50));
                    g2.fillRect(px, botY + 14, PIPE_W, botH - 14);
                    g2.setColor(new Color(30, 120, 30));
                    g2.fillRect(px - 3, botY, PIPE_W + 6, 14);
                    g2.setColor(new Color(80, 200, 80));
                    g2.drawRect(px, botY + 14, PIPE_W, botH - 14);
                    g2.drawRect(px - 3, botY, PIPE_W + 6, 14);
                }
                g2.setStroke(new BasicStroke(1));
            }

            // Bird
            int bxL = ox + BIRD_X, byL = oy + (int)birdY;
            // Rotate bird based on velocity
            double rot = Math.toRadians(Math.max(-25, Math.min(35, birdVY * 3)));
            g2.rotate(rot, bxL, byL);
            // Body
            g2.setColor(new Color(255, 210, 50));
            g2.fillOval(bxL - BIRD_R, byL - BIRD_R, BIRD_R*2, BIRD_R*2);
            // Wing
            g2.setColor(new Color(220, 160, 30));
            g2.fillOval(bxL - BIRD_R, byL, 8, 5);
            // Eye
            g2.setColor(Color.WHITE);
            g2.fillOval(bxL + 2, byL - 5, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillOval(bxL + 3, byL - 4, 4, 4);
            // Beak
            int[] bkx = {bxL + BIRD_R - 1, bxL + BIRD_R + 7, bxL + BIRD_R - 1};
            int[] bky = {byL - 3, byL, byL + 3};
            g2.setColor(new Color(255, 130, 0));
            g2.fillPolygon(bkx, bky, 3);
            g2.rotate(-rot, bxL, byL);

            // Score
            g2.setFont(new Font("SansSerif", Font.BOLD, 15));
            g2.setColor(new Color(0,0,0,120));
            g2.drawString("" + score, ox + w/2 - g2.getFontMetrics().stringWidth(""+score)/2 + 1, oy + 24);
            g2.setColor(Color.WHITE);
            g2.drawString("" + score, ox + w/2 - g2.getFontMetrics().stringWidth(""+score)/2, oy + 24);

            // Start / Game-over overlay
            if (!started || !alive) {
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(ox + 16, oy + h/2 - 40, w - 32, 78, 14, 14);
                if (!alive) {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                    g2.setColor(new Color(255, 80, 80));
                    String gover = "Game Over!";
                    g2.drawString(gover, ox + (w - g2.getFontMetrics().stringWidth(gover))/2, oy + h/2 - 16);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.setColor(Color.WHITE);
                    String sc = "Score: " + score + "   Best: " + bestScore;
                    g2.drawString(sc, ox + (w - g2.getFontMetrics().stringWidth(sc))/2, oy + h/2 + 2);
                }
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.setColor(new Color(200, 240, 200));
                String tap = alive ? "Tap screen to start!" : "Tap to play again!";
                g2.drawString(tap, ox + (w - g2.getFontMetrics().stringWidth(tap))/2, oy + h/2 + 22);
            }
        }
    }
}
