package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;

public class GaragePanel extends RoomPanel {

    private static java.awt.image.BufferedImage ROACH_IMG = null;
    private static java.awt.image.BufferedImage RAT_IMG   = null;
    static {
        ROACH_IMG = AssetManager.getRoom("roach_ai");   // roach_preview_5_ai_generated.png
        RAT_IMG   = AssetManager.getRoom("rat");        // drop rat.png in assets/rooms/ to enable
    }

    // Roach minigame
    private boolean minigameActive = false;
    private final List<JButton> roachBtns = new ArrayList<>();
    private int roachesLeft = 0;
    private Timer roachTimer;
    private int roachSeconds = 0;
    private JLabel roachTimerLabel;
    private static final int ROACH_COUNT    = 6;
    private static final int ROACH_TIME_SEC = 12;
    private static final Random RNG = new Random();

    // ── Circuit fuse-box minigame ─────────────────────────────────────────
    private boolean circuitActive     = false;
    private int     circuitRound      = 0;   // 0=round1, 1=round2, 2=round3
    private int[]   circuitSeq        = new int[0];
    private int     circuitStep       = 0;   // demo index
    private int     circuitInputStep  = 0;   // player input index
    private boolean circuitPlayerTurn = false;
    private int     circuitFlashNode  = -1;  // node lit during demo (-1=off)
    private int     circuitTimerSec   = 0;
    private boolean circuitWrong      = false;
    private int     circuitWrongNode  = -1;
    private Timer   circuitFlashTimer;
    private Timer   circuitCountdownTimer;
    private JLabel  circuitStatus;
    private JLabel  circuitTimerLabel;

    // Node positions [round][node][x,y] in 800×455 logical space
    private static final int[][][] NODE_POS = {
        {{200,155},{400,110},{595,175},{530,315},{280,330}},                     // round 1 – 5 nodes
        {{175,145},{365,95},{590,145},{645,280},{430,355},{210,295}},            // round 2 – 6 nodes
        {{215,125},{415,85},{610,130},{670,255},{570,360},{340,378},{155,268}},  // round 3 – 7 nodes
    };
    private static final int NODE_R = 22;
    private static final int CIRCUIT_TIMEOUT = 20;

    public GaragePanel() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { onEnter(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (circuitActive && circuitPlayerTurn)
                    checkCircuitClick(e.getX(), e.getY());
            }
        });
        onEnter();
    }

    private int bx(int v) { int w = getWidth();  return w <= 0 ? v : Math.round(v * w / 800f); }
    private int by(int v) { int h = getHeight(); return h <= 0 ? v : Math.round(v * h / 455f); }

    @Override
    public void onEnter() {
        if (minigameActive || circuitActive) return;
        removeAll();
        roachBtns.clear();
        buildButtons();
        revalidate();
        repaint();
    }

    private void buildButtons() {
        // ── LEFT — Workbench (coin RNG 0-5) ──────────────────────────────
        if (!GameState.looseChangeFound_Garage) {
            JButton changeBtn = makeButton("Search Workbench", bx(22), by(300), bx(190), by(36));
            changeBtn.setForeground(new Color(200, 220, 160));
            changeBtn.addActionListener(e -> {
                int found = (int)(Math.random() * 6); // 0-5
                if (found > 0) GameState.coins += found;
                GameState.looseChangeFound_Garage = true;
                onEnter();
                String msg = found > 0 ? "Found " + found + " coin(s) on the workbench!" : "Workbench is empty.";
                GameFrame.getInstance().getGamePanel().showMessage(msg);
            });
            add(changeBtn);
        }

        // Wrench
        if (GameState.wrenchInGarage && GameState.roachesCleared) {
            JButton wrenchBtn = makeButton("Pick Up Wrench", bx(22), by(342), bx(190), by(36));
            wrenchBtn.setForeground(new Color(180, 200, 255));
            wrenchBtn.addActionListener(e -> {
                GameState.hasWrench      = true;
                GameState.wrenchInGarage = false;
                onEnter();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Got the wrench! Head to the Living Room to fix the AC.");
            });
            add(wrenchBtn);
        } else if (GameState.wrenchInGarage) {
            JButton blocked = makeButton("Wrench (clear roaches first)", bx(22), by(342), bx(190), by(36));
            blocked.setEnabled(false);
            add(blocked);
        } else if (GameState.hasWrench) {
            JButton carrying = makeButton("Wrench: Carrying", bx(22), by(342), bx(190), by(36));
            carrying.setEnabled(false);
            add(carrying);
        }

        // ── CENTER — Roach / Rat situation ───────────────────────────────
        if (GameState.roachesPresent && !GameState.roachesCleared) {
            if (GameState.hasRoachSpray) {
                JButton sprayBtn = makeButton("Use Roach Spray!", bx(230), by(300), bx(280), by(38));
                sprayBtn.setForeground(new Color(255, 160, 80));
                sprayBtn.addActionListener(e -> startRoachMinigame());
                add(sprayBtn);
            } else {
                JButton needSpray = makeButton("Roaches! (need spray from Mom's room)", bx(230), by(300), bx(280), by(38));
                needSpray.setEnabled(false);
                add(needSpray);
            }
        } else if (GameState.ratsPresent && !GameState.ratsClearedToday) {
            if (GameState.hasMouseTrap) {
                JButton trapBtn = makeButton("Place Mouse Trap!", bx(230), by(300), bx(280), by(38));
                trapBtn.setForeground(new Color(255, 120, 60));
                trapBtn.addActionListener(e -> {
                    GameState.ratsPresent     = false;
                    GameState.ratsClearedToday = true;
                    GameState.hasMouseTrap    = false;
                    GameLoop.rewardAction(3, "Rats gone! Garage is clear.");
                    onEnter();
                });
                add(trapBtn);
            } else {
                JButton needTrap = makeButton("RATS! (buy Mouse Trap from shop)", bx(230), by(300), bx(280), by(38));
                needTrap.setEnabled(false);
                needTrap.setForeground(new Color(255, 80, 80));
                add(needTrap);
            }
        } else if (GameState.roachesCleared) {
            JButton cleared = makeButton("Roaches ✓ Cleared", bx(230), by(300), bx(280), by(38));
            cleared.setEnabled(false);
            add(cleared);
        }

        // ── RIGHT — Fuse box (only when power is out) ─────────────────────
        if (GameState.powerOut) {
            JButton fuseBtn = makeButton("Open Fuse Box", bx(560), by(300), bx(220), by(38));
            fuseBtn.setForeground(new Color(255, 200, 80));
            fuseBtn.addActionListener(e -> startCircuitGame());
            add(fuseBtn);
        }
    }

    // ── Circuit game ──────────────────────────────────────────────────────
    private void startCircuitGame() {
        circuitActive     = true;
        circuitRound      = 0;
        circuitPlayerTurn = false;
        circuitWrong      = false;
        removeAll();
        roachBtns.clear();

        circuitStatus = new JLabel("Watch the circuit!", JLabel.CENTER);
        circuitStatus.setForeground(new Color(100, 255, 180));
        circuitStatus.setFont(new Font("Monospaced", Font.BOLD, 15));
        circuitStatus.setBounds(bx(240), by(16), bx(320), by(26));
        add(circuitStatus);

        circuitTimerLabel = new JLabel("", JLabel.CENTER);
        circuitTimerLabel.setForeground(new Color(255, 80, 80));
        circuitTimerLabel.setFont(new Font("Monospaced", Font.BOLD, 15));
        circuitTimerLabel.setBounds(bx(660), by(16), bx(100), by(26));
        add(circuitTimerLabel);

        JButton cancel = makeButton("✕ Cancel", bx(10), by(13), bx(80), by(26));
        cancel.setForeground(new Color(200, 100, 100));
        cancel.addActionListener(e -> cancelCircuit());
        add(cancel);

        revalidate();
        repaint();
        startCircuitRound();
    }

    private void startCircuitRound() {
        int nodeCount = NODE_POS[circuitRound].length;
        int seqLen = 3 + circuitRound; // round 1=3, round 2=4, round 3=5
        circuitSeq = new int[seqLen];
        for (int i = 0; i < seqLen; i++) circuitSeq[i] = RNG.nextInt(nodeCount);
        circuitStep       = 0;
        circuitInputStep  = 0;
        circuitPlayerTurn = false;
        circuitFlashNode  = -1;
        circuitWrong      = false;
        circuitWrongNode  = -1;
        circuitStatus.setText("Round " + (circuitRound + 1) + "/3 — Watch the sequence!");
        circuitTimerLabel.setText("");
        repaint();
        playCircuitDemo();
    }

    private void playCircuitDemo() {
        if (circuitFlashTimer != null) circuitFlashTimer.stop();
        circuitFlashNode = -1;
        // Alternate: light node (500ms) → dark gap (300ms) → next node
        final boolean[] isLit = {false};
        circuitFlashTimer = new Timer(700, null);
        circuitFlashTimer.addActionListener(e -> {
            if (isLit[0]) {
                // currently lit → turn off
                circuitFlashNode = -1;
                isLit[0] = false;
                circuitFlashTimer.setDelay(350);
                circuitStep++;
                if (circuitStep >= circuitSeq.length) {
                    circuitFlashTimer.stop();
                    circuitFlashNode  = -1;
                    circuitPlayerTurn = true;
                    circuitInputStep  = 0;
                    circuitTimerSec   = CIRCUIT_TIMEOUT;
                    circuitStatus.setText("Your turn!  Click " + circuitSeq.length + " nodes in order");
                    circuitTimerLabel.setText(circuitTimerSec + "s");
                    startCircuitCountdown();
                }
            } else {
                // currently dark → light next node
                if (circuitStep < circuitSeq.length) {
                    circuitFlashNode = circuitSeq[circuitStep];
                    isLit[0] = true;
                    circuitFlashTimer.setDelay(500);
                }
            }
            repaint();
        });
        circuitFlashTimer.setInitialDelay(700);
        circuitFlashTimer.start();
    }

    private void startCircuitCountdown() {
        if (circuitCountdownTimer != null) circuitCountdownTimer.stop();
        circuitCountdownTimer = new Timer(1000, e -> {
            circuitTimerSec--;
            circuitTimerLabel.setText(circuitTimerSec + "s");
            if (circuitTimerSec <= 0) {
                circuitCountdownTimer.stop();
                circuitPlayerTurn = false;
                circuitTimerLabel.setText("");
                circuitStatus.setText("Time's up! Showing sequence again...");
                Timer retry = new Timer(900, ev -> { circuitStep = 0; playCircuitDemo(); });
                retry.setRepeats(false);
                retry.start();
            }
            repaint();
        });
        circuitCountdownTimer.start();
    }

    private void checkCircuitClick(int mx, int my) {
        if (circuitRound >= NODE_POS.length) return;
        float sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float sy = getHeight() <= 0 ? 1f : getHeight() / 455f;
        int[][] nodes = NODE_POS[circuitRound];
        for (int i = 0; i < nodes.length; i++) {
            int nx = Math.round(nodes[i][0] * sx);
            int ny = Math.round(nodes[i][1] * sy);
            int nr = Math.round(NODE_R * ((sx + sy) / 2f));
            int dx = mx - nx, dy = my - ny;
            if (dx*dx + dy*dy <= nr*nr) {
                handleNodeClick(i);
                return;
            }
        }
    }

    private void handleNodeClick(int idx) {
        if (idx == circuitSeq[circuitInputStep]) {
            // Correct — flash node green briefly
            circuitFlashNode = idx;
            repaint();
            Timer dim = new Timer(180, e -> { circuitFlashNode = -1; repaint(); });
            dim.setRepeats(false);
            dim.start();
            circuitInputStep++;
            if (circuitInputStep >= circuitSeq.length) {
                // Round done
                if (circuitCountdownTimer != null) circuitCountdownTimer.stop();
                circuitPlayerTurn = false;
                circuitRound++;
                if (circuitRound >= 3) {
                    circuitActive  = false;
                    GameState.powerOut = false;
                    circuitStatus.setText("⚡ POWER RESTORED!");
                    circuitTimerLabel.setText("");
                    GameLoop.rewardAction(3, "Fuse box fixed! Power's back.");
                    Timer done = new Timer(1200, e -> onEnter());
                    done.setRepeats(false);
                    done.start();
                } else {
                    circuitStatus.setText("Round " + circuitRound + " done! Get ready...");
                    circuitTimerLabel.setText("");
                    Timer next = new Timer(950, e -> startCircuitRound());
                    next.setRepeats(false);
                    next.start();
                }
                repaint();
            }
        } else {
            // Wrong — flash red, retry
            circuitWrong      = true;
            circuitWrongNode  = idx;
            circuitPlayerTurn = false;
            if (circuitCountdownTimer != null) circuitCountdownTimer.stop();
            circuitTimerLabel.setText("");
            circuitStatus.setText("Wrong node! Watch the sequence again...");
            repaint();
            Timer retry = new Timer(950, e -> {
                circuitWrong     = false;
                circuitWrongNode = -1;
                circuitStep      = 0;
                playCircuitDemo();
            });
            retry.setRepeats(false);
            retry.start();
        }
    }

    private void cancelCircuit() {
        if (circuitFlashTimer     != null) circuitFlashTimer.stop();
        if (circuitCountdownTimer != null) circuitCountdownTimer.stop();
        circuitActive    = false;
        circuitFlashNode = -1;
        circuitWrong     = false;
        onEnter();
    }

    // ── Roach minigame ────────────────────────────────────────────────────
    private void startRoachMinigame() {
        minigameActive = true;
        roachesLeft    = ROACH_COUNT;
        roachSeconds   = ROACH_TIME_SEC;
        removeAll();
        roachBtns.clear();

        roachTimerLabel = new JLabel("Time: " + roachSeconds + "s");
        roachTimerLabel.setForeground(new Color(255, 100, 50));
        roachTimerLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        roachTimerLabel.setBounds(bx(340), by(10), bx(120), by(30));
        add(roachTimerLabel);

        JLabel inst = new JLabel("Click all the roaches!");
        inst.setForeground(new Color(220, 220, 200));
        inst.setFont(new Font("SansSerif", Font.BOLD, 14));
        inst.setBounds(bx(290), by(40), bx(220), by(25));
        add(inst);

        for (int i = 0; i < ROACH_COUNT; i++) spawnRoach();

        roachTimer = new Timer(1000, e -> {
            roachSeconds--;
            if (roachTimerLabel != null) roachTimerLabel.setText("Time: " + roachSeconds + "s");
            if (roachSeconds <= 0) endRoachMinigame(false);
        });
        roachTimer.start();

        revalidate();
        repaint();
    }

    private void spawnRoach() {
        int rx = bx(80) + RNG.nextInt(Math.max(1, bx(580)));
        int ry = by(80) + RNG.nextInt(Math.max(1, by(250)));
        final int angle = RNG.nextInt(360);
        JButton r = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,   RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.rotate(Math.toRadians(angle), cx, cy);
                if (ROACH_IMG != null) {
                    int dw = getWidth(), dh = getHeight();
                    g2.drawImage(ROACH_IMG, cx - dw/2, cy - dh/2, dw, dh, null);
                } else {
                    // Fallback
                    g2.setColor(new Color(55, 30, 10));
                    g2.fillOval(cx - 10, cy - 5, 20, 10);
                    g2.setColor(new Color(40, 20, 5));
                    g2.fillOval(cx + 8, cy - 4, 8, 8);
                }
                g2.dispose();
            }
        };
        r.setBounds(rx, ry, bx(44), by(44));
        r.setOpaque(false); r.setContentAreaFilled(false);
        r.setBorderPainted(false); r.setFocusPainted(false);
        r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        r.addActionListener(e -> {
            remove(r);
            roachBtns.remove(r);
            roachesLeft--;
            revalidate();
            repaint();
            if (roachesLeft <= 0) endRoachMinigame(true);
        });
        roachBtns.add(r);
        add(r);
    }

    private void endRoachMinigame(boolean success) {
        if (roachTimer != null) roachTimer.stop();
        minigameActive = false;
        if (success) {
            GameState.roachesPresent     = false;
            GameState.roachesCleared     = true;
            GameState.roachesClearedEver = true; // persists to day 2 → rats appear
            GameState.hasRoachSpray      = false;
            GameLoop.rewardAction(2, "Roaches cleared! Wrench is now accessible.");
        } else {
            GameFrame.getInstance().getGamePanel()
                    .showMessage("Ran out of time! Roaches scattered... try again.");
        }
        onEnter();
    }

    // ── Paint ─────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        float sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float sy = getHeight() <= 0 ? 1f : getHeight() / 455f;
        g2.scale(sx, sy);
        int W = 800, H = 455;

        if (circuitActive) {
            drawCircuitBoard(g2, W, H);
        } else {
            drawBg(g2, "garage", W, H);
            // ── Thunderstorm rain visible through garage door ──────────────
            if (GameState.thunderstorm) drawGarageRain(g2, W, H);
            g2.setColor(new Color(0, 0, 0, 80));
            g2.fillRect(0, H - 80, W, 80);
            if (GameState.powerOut)
                drawBadge(g2, "POWER OUT — fix the fuse box!", W/2 - 120, 10, new Color(155, 95, 0, 220), Color.WHITE);
            if (GameState.thunderstorm)
                drawBadge(g2, "THUNDERSTORM — Stay inside!", W/2 - 110, 36, new Color(60, 40, 140, 220), Color.WHITE);
            if (GameState.ratsPresent && !GameState.ratsClearedToday) {
                drawRats(g2, W, H);
                drawBadge(g2, "RATS! Get a Mouse Trap", 10, 36, new Color(160, 40, 20, 230), Color.WHITE);
            }
        }
        g2.dispose();
    }

    // ── Circuit board renderer ─────────────────────────────────────────────
    private void drawCircuitBoard(Graphics2D g2, int W, int H) {
        // PCB background
        GradientPaint bg = new GradientPaint(0, 0, new Color(12, 28, 18), 0, H, new Color(6, 16, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);
        g2.setPaint(null);

        // Grid texture
        g2.setColor(new Color(20, 55, 30, 60));
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x < W; x += 20) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 20) g2.drawLine(0, y, W, y);
        g2.setStroke(new BasicStroke(1));

        if (circuitRound >= NODE_POS.length) return;
        int[][] nodes = NODE_POS[circuitRound];

        // Draw wires between consecutive sequence nodes (completed so far)
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < circuitInputStep - 1; i++) {
            int a = circuitSeq[i], b = circuitSeq[i + 1];
            g2.setColor(new Color(60, 255, 140, 180));
            g2.drawLine(nodes[a][0], nodes[a][1], nodes[b][0], nodes[b][1]);
        }

        // Draw static trace lines (faint green, visual only)
        g2.setColor(new Color(30, 100, 50, 50));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < nodes.length - 1; i++)
            g2.drawLine(nodes[i][0], nodes[i][1], nodes[(i+1) % nodes.length][0], nodes[(i+1) % nodes.length][1]);
        g2.setStroke(new BasicStroke(1));

        // Draw nodes
        for (int i = 0; i < nodes.length; i++) {
            int nx = nodes[i][0], ny = nodes[i][1];
            boolean isFlash  = (i == circuitFlashNode);
            boolean isWrong  = (circuitWrong && i == circuitWrongNode);
            boolean isDone   = (circuitPlayerTurn || circuitInputStep > 0) && isAlreadyClicked(i);

            // Outer glow
            if (isFlash) {
                g2.setColor(new Color(80, 255, 200, 60));
                g2.fillOval(nx - NODE_R - 8, ny - NODE_R - 8, (NODE_R + 8) * 2, (NODE_R + 8) * 2);
                g2.setColor(new Color(80, 255, 200, 120));
                g2.fillOval(nx - NODE_R - 4, ny - NODE_R - 4, (NODE_R + 4) * 2, (NODE_R + 4) * 2);
            } else if (isWrong) {
                g2.setColor(new Color(255, 50, 50, 80));
                g2.fillOval(nx - NODE_R - 6, ny - NODE_R - 6, (NODE_R + 6) * 2, (NODE_R + 6) * 2);
            }

            // Node body
            Color bodyCol;
            if (isFlash)      bodyCol = new Color(100, 255, 200);
            else if (isWrong) bodyCol = new Color(255, 60, 60);
            else if (isDone)  bodyCol = new Color(50, 180, 100);
            else              bodyCol = new Color(22, 60, 38);

            GradientPaint gp = new GradientPaint(
                    nx - NODE_R, ny - NODE_R, bodyCol.brighter(),
                    nx + NODE_R, ny + NODE_R, bodyCol.darker());
            g2.setPaint(gp);
            g2.fillOval(nx - NODE_R, ny - NODE_R, NODE_R * 2, NODE_R * 2);
            g2.setPaint(null);

            // Border
            g2.setColor(isFlash ? new Color(160, 255, 220) : new Color(50, 140, 80));
            g2.setStroke(new BasicStroke(isFlash ? 2.5f : 1.5f));
            g2.drawOval(nx - NODE_R, ny - NODE_R, NODE_R * 2, NODE_R * 2);
            g2.setStroke(new BasicStroke(1));

            // Index label
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(isFlash ? new Color(10, 30, 20) : new Color(100, 200, 140));
            String lbl = String.valueOf(i + 1);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, nx - fm.stringWidth(lbl)/2, ny + fm.getAscent()/2 - 1);
        }

        // Timer bar
        if (circuitPlayerTurn && circuitTimerSec > 0) {
            int bw = W - 40, bh = 10, bx2 = 20, by2 = H - 30;
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRoundRect(bx2 - 2, by2 - 2, bw + 4, bh + 4, 4, 4);
            g2.setColor(new Color(15, 40, 22));
            g2.fillRoundRect(bx2, by2, bw, bh, 4, 4);
            float pct = (float) circuitTimerSec / CIRCUIT_TIMEOUT;
            Color barCol = pct > 0.5f ? new Color(60, 220, 100)
                         : pct > 0.25f ? new Color(220, 200, 40)
                         : new Color(220, 60, 40);
            g2.setColor(barCol);
            g2.fillRoundRect(bx2, by2, (int)(bw * pct), bh, 4, 4);
        }

        // Round indicator dots
        int dotY = H - 18;
        for (int r = 0; r < 3; r++) {
            int dotX = W / 2 - 24 + r * 24;
            if (r < circuitRound)
                g2.setColor(new Color(60, 220, 100));
            else if (r == circuitRound)
                g2.setColor(new Color(100, 200, 255));
            else
                g2.setColor(new Color(30, 60, 40));
            g2.fillOval(dotX, dotY, 12, 12);
            g2.setColor(new Color(60, 140, 80));
            g2.drawOval(dotX, dotY, 12, 12);
        }

        // Scanline overlay for atmosphere
        g2.setColor(new Color(0, 0, 0, 22));
        for (int y = 0; y < H; y += 4)
            g2.fillRect(0, y, W, 2);
    }

    private boolean isAlreadyClicked(int nodeIdx) {
        for (int i = 0; i < circuitInputStep; i++)
            if (circuitSeq[i] == nodeIdx) return true;
        return false;
    }

    /** Rain seen through the open garage door during a thunderstorm */
    private void drawGarageRain(Graphics2D g2, int W, int H) {
        long t = System.currentTimeMillis();
        // Dark storm tint (less intense indoors)
        g2.setColor(new Color(15, 15, 50, 70));
        g2.fillRect(0, 0, W, H);
        // Occasional lightning flash
        if ((t / 1800) % 10 == 0) {
            g2.setColor(new Color(200, 220, 255, 40));
            g2.fillRect(0, 0, W, H);
        }
        // Rain streaks — lighter than outdoor since we're inside
        g2.setColor(new Color(140, 170, 220, 65));
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int phase = (int)((t / 45) % H);
        for (int col = 10; col < W; col += 26) {
            int offset = (col * 7 + phase) % H;
            g2.drawLine(col + 7, offset - 16, col, offset);
            int offset2 = (col * 7 + phase + H / 2) % H;
            g2.drawLine(col + 7, offset2 - 16, col, offset2);
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawRats(Graphics2D g2, int W, int H) {
        long t = System.currentTimeMillis();
        int[] ratXs = {80, 200, 340};
        for (int i = 0; i < 3; i++) {
            int rx = ratXs[i] + (int)(Math.sin(t * 0.003 + i * 2.1) * 18);
            int ry = H - 148 + (int)(Math.cos(t * 0.004 + i * 1.7) * 6);
            drawRat(g2, rx, ry, (t / 200 + i) % 2 == 0);
        }
    }

    private void drawRat(Graphics2D g2, int x, int y, boolean legPhase) {
        if (RAT_IMG != null) {
            // Flip every other phase so it looks like it's running
            int dw = 52, dh = 34;
            if (legPhase) {
                g2.drawImage(RAT_IMG, x, y, dw, dh, null);
            } else {
                // Horizontal flip
                g2.drawImage(RAT_IMG, x + dw, y, -dw, dh, null);
            }
            return;
        }
        // Fallback: drawn shapes
        g2.setColor(new Color(90, 78, 68));
        g2.fillOval(x, y, 28, 14);
        g2.fillOval(x + 22, y - 3, 14, 11);
        g2.setColor(new Color(160, 100, 100));
        g2.fillOval(x + 28, y - 8, 7, 7);
        g2.setColor(new Color(220, 30, 30));
        g2.fillOval(x + 32, y - 2, 3, 3);
        g2.setColor(new Color(130, 100, 80));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(x - 14, y + 2, 20, 18, legPhase ? 30 : 10, 150);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(70, 60, 50));
        g2.setStroke(new BasicStroke(1.5f));
        if (legPhase) {
            g2.drawLine(x + 6, y + 13, x + 4, y + 20);
            g2.drawLine(x + 14, y + 13, x + 16, y + 20);
        } else {
            g2.drawLine(x + 6, y + 13, x + 8, y + 20);
            g2.drawLine(x + 14, y + 13, x + 12, y + 20);
        }
        g2.setStroke(new BasicStroke(1));
    }
}
