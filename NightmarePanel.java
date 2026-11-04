package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.Timer;

/**
 * The Nightmare Realm.
 * When temperature maxes out you're dragged here.
 * Survive 3 rounds of "Complete the Circuit" to escape.
 */
public class NightmarePanel extends JPanel {

    private static final int   ROUNDS_TO_ESCAPE = 3;
    private static final int   TIMER_SECONDS    = 20;
    private static final Random RNG = new Random();

    // ── Circuit node positions for each round size ────────────────────────
    private static final int[][][] NODE_POSITIONS = {
        // 4 nodes
        {{230,180},{560,180},{560,330},{230,330}},
        // 5 nodes
        {{400,110},{580,230},{510,360},{290,360},{220,230}},
        // 6 nodes
        {{400,100},{560,170},{565,315},{400,385},{235,315},{235,170}},
    };

    // ── Game state ────────────────────────────────────────────────────────
    private int     round       = 1;
    private int     numNodes;
    private int[]   nodeX, nodeY;
    private char[]  nodeLabels;
    private int[]   sequence;          // correct click order (indices into nodes)
    private int     playerStep;        // how many nodes player has clicked correctly
    private boolean showingSequence;
    private int     seqStep;           // current step being flashed
    private int     litNode = -1;      // which node is lit during sequence flash
    private boolean playerTurn;
    private int     secondsLeft;
    private String  statusMsg = "";
    private Color   statusColor = Color.WHITE;

    // Wire segments: list of int[2] = {fromNodeIdx, toNodeIdx}
    private final List<int[]> wires = new ArrayList<>();

    private Timer seqTimer, countdownTimer, ambientTimer;
    private float catBreathPhase = 0f;

    public NightmarePanel() {
        setLayout(null);
        setBackground(new Color(10, 5, 15));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!playerTurn) return;
                float lx = e.getX() * 800f / Math.max(1, getWidth());
                float ly = e.getY() * 620f / Math.max(1, getHeight());
                checkNodeClick((int) lx, (int) ly);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { repaint(); }
        });
    }

    // ── Called when nightmare screen becomes visible ───────────────────────
    public void onShow() {
        round = 1;
        catBreathPhase = 0f;
        stopAllTimers();

        ambientTimer = new Timer(80, e -> { catBreathPhase += 0.12f; repaint(); });
        ambientTimer.start();

        startRound();
    }

    private void startRound() {
        numNodes = 3 + round;  // round1=4, round2=5, round3=6
        int posIdx = round - 1;
        nodeX = new int[numNodes];
        nodeY = new int[numNodes];
        nodeLabels = new char[numNodes];
        for (int i = 0; i < numNodes; i++) {
            nodeX[i] = NODE_POSITIONS[posIdx][i][0];
            nodeY[i] = NODE_POSITIONS[posIdx][i][1];
            nodeLabels[i] = (char)('A' + i);
        }

        // Build random sequence
        sequence = new int[numNodes];
        int[] pool = new int[numNodes];
        for (int i = 0; i < numNodes; i++) pool[i] = i;
        for (int i = numNodes - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            int tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp;
        }
        System.arraycopy(pool, 0, sequence, 0, numNodes);

        playerStep = 0;
        playerTurn = false;
        litNode = -1;
        wires.clear();
        showingSequence = true;
        seqStep = 0;
        statusMsg = "WATCH the circuit path...";
        statusColor = new Color(180, 140, 220);
        repaint();

        // Delay then flash sequence
        Timer delay = new Timer(900, e -> flashNext());
        delay.setRepeats(false);
        delay.start();
    }

    private void flashNext() {
        if (seqStep > 0) {
            // Unlight previous
            litNode = -1;
            repaint();
        }
        if (seqStep >= numNodes) {
            // Sequence done — let player go
            showingSequence = false;
            Timer pause = new Timer(600, e -> beginPlayerTurn());
            pause.setRepeats(false);
            pause.start();
            return;
        }
        // Light next node
        litNode = sequence[seqStep];
        seqStep++;
        repaint();
        seqTimer = new Timer(700, e -> {
            seqTimer.stop();
            flashNext();
        });
        seqTimer.setRepeats(false);
        seqTimer.start();
    }

    private void beginPlayerTurn() {
        playerTurn = true;
        secondsLeft = TIMER_SECONDS;
        litNode = -1;
        statusMsg = "YOUR TURN — click nodes in the shown order!";
        statusColor = new Color(220, 220, 80);
        repaint();

        countdownTimer = new Timer(1000, e -> {
            secondsLeft--;
            repaint();
            if (secondsLeft <= 0) {
                countdownTimer.stop();
                onWrong("Time's up! The demon cat strikes...");
            }
        });
        countdownTimer.start();
    }

    private void checkNodeClick(int lx, int ly) {
        for (int i = 0; i < numNodes; i++) {
            int dx = lx - nodeX[i], dy = ly - nodeY[i];
            if (dx*dx + dy*dy <= 22*22) {
                // Hit node i
                if (i == sequence[playerStep]) {
                    // Correct!
                    if (playerStep > 0) wires.add(new int[]{sequence[playerStep-1], i});
                    litNode = i;
                    repaint();
                    playerStep++;
                    Timer unlit = new Timer(220, ev -> { litNode = -1; repaint(); });
                    unlit.setRepeats(false); unlit.start();

                    if (playerStep == numNodes) {
                        // Round complete
                        playerTurn = false;
                        if (countdownTimer != null) countdownTimer.stop();
                        wires.add(new int[]{sequence[numNodes-1], sequence[0]}); // close circuit
                        if (round >= ROUNDS_TO_ESCAPE) {
                            statusMsg = "CIRCUIT COMPLETE! Escaping...";
                            statusColor = new Color(80, 255, 140);
                            repaint();
                            Timer t = new Timer(1600, ev -> escapeNightmare());
                            t.setRepeats(false); t.start();
                        } else {
                            round++;
                            statusMsg = "Circuit complete! Round " + round + " incoming...";
                            statusColor = new Color(160, 220, 160);
                            repaint();
                            Timer t = new Timer(1200, ev -> startRound());
                            t.setRepeats(false); t.start();
                        }
                    }
                } else {
                    // Wrong node
                    onWrong("Wrong node! The demon cat claims you...");
                }
                return;
            }
        }
    }

    private void onWrong(String msg) {
        playerTurn = false;
        if (countdownTimer != null) countdownTimer.stop();
        statusMsg = msg;
        statusColor = new Color(220, 60, 60);
        repaint();
        Timer t = new Timer(1600, e -> loseNightmare());
        t.setRepeats(false); t.start();
    }

    private void escapeNightmare() {
        stopAllTimers();
        GameLoop.resumeAfterNightmare();
    }

    private void loseNightmare() {
        stopAllTimers();
        GameState.gameOver       = true;
        GameState.won            = false;
        GameState.gameOverReason = "The Fire Demon Cat consumed your soul.\nShould've kept it cooler.";
        GameFrame.getInstance().showScreen(GameFrame.GAMEOVER);
    }

    private void stopAllTimers() {
        if (seqTimer      != null) seqTimer.stop();
        if (countdownTimer!= null) countdownTimer.stop();
        if (ambientTimer  != null) ambientTimer.stop();
    }

    // ── Painting ──────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);

        float _sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float _sy = getHeight() <= 0 ? 1f : getHeight() / 620f;
        g2.scale(_sx, _sy);
        int W = 800, H = 620;

        // ── Background ────────────────────────────────────────────────────
        GradientPaint bgGrad = new GradientPaint(W/2f, H, new Color(65, 8, 3), W/2f, 0, new Color(8, 3, 18));
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, W, H);

        float pulse = (float)(0.5 + 0.5 * Math.sin(catBreathPhase));
        int glowAlpha = (int)(35 + pulse * 30);
        g2.setColor(new Color(200, 50, 10, glowAlpha));
        g2.fillOval(W/2 - 220, 30, 440, 380);
        g2.setColor(new Color(240, 100, 20, glowAlpha / 2));
        g2.fillOval(W/2 - 140, 60, 280, 240);

        drawLavaFloor(g2, W, H);
        drawFlames(g2, W, H);

        // Ember particles
        long t = System.currentTimeMillis();
        for (int i = 0; i < 18; i++) {
            double px = (i * 53 + Math.sin(t * 0.0008 + i * 0.9) * 55 + 400) % W;
            double py = H - 60 - ((t * 0.025 + i * 36) % (H - 110));
            float a = 0.3f + (float)Math.abs(Math.sin(t * 0.002 + i)) * 0.5f;
            g2.setColor(new Color(1f, 0.42f + (float)(i % 3) * 0.1f, 0.08f, a));
            int sz = 3 + (i % 3) * 2;
            g2.fillOval((int)px, (int)py, sz, sz);
        }

        // Stalactites
        g2.setColor(new Color(6, 3, 12, 200));
        for (int x = 0; x < W; x += 38) {
            int drop = 20 + (int)(Math.sin(x * 0.18 + 1.2) * 14 + 14);
            g2.fillPolygon(new int[]{x, x+19, x+38}, new int[]{0, drop, 0}, 3);
        }

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        String title = "THE NIGHTMARE REALM";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(255, 60, 10, 60));
        g2.drawString(title, tx - 2, 55);
        g2.drawString(title, tx + 2, 55);
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(title, tx + 2, 57);
        g2.setColor(new Color(240, 70, 50));
        g2.drawString(title, tx, 55);

        // Fire Demon Cat (small, top-center — game area takes center)
        float breath = (float) Math.sin(catBreathPhase) * 2;
        drawFireDemonCat(g2, W/2 - 45, 55, breath, 0.55f);

        // ── Circuit game area ─────────────────────────────────────────────
        drawCircuitBoard(g2, W, H);

        g2.dispose();
    }

    private void drawCircuitBoard(Graphics2D g2, int W, int H) {
        if (nodeX == null) return;

        // Circuit board background panel
        g2.setColor(new Color(5, 22, 10, 210));
        g2.fillRoundRect(85, 155, 630, 280, 12, 12);
        g2.setColor(new Color(0, 80, 30, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(85, 155, 630, 280, 12, 12);
        g2.setStroke(new BasicStroke(1));

        // PCB grid lines
        g2.setColor(new Color(0, 60, 20, 50));
        for (int x = 100; x < 715; x += 30) g2.drawLine(x, 155, x, 435);
        for (int y = 165; y < 435; y += 30) g2.drawLine(85, y, 715, y);

        // Round label
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        g2.setColor(new Color(0, 200, 80, 200));
        g2.drawString("ROUND " + round + " / " + ROUNDS_TO_ESCAPE, 95, 178);

        // Sequence path label
        if (sequence != null) {
            StringBuilder pathStr = new StringBuilder("PATH:  ");
            for (int i = 0; i < numNodes; i++) {
                pathStr.append(nodeLabels[sequence[i]]);
                if (i < numNodes - 1) pathStr.append(" → ");
            }
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(showingSequence
                    ? new Color(180, 255, 160, 220)
                    : (playerTurn ? new Color(80, 130, 80, 140) : new Color(80, 130, 80, 80)));
            g2.drawString(pathStr.toString(), 95, 195);
        }

        // Draw completed wires
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int[] wire : wires) {
            int ax = nodeX[wire[0]], ay = nodeY[wire[0]];
            int bx = nodeX[wire[1]], by = nodeY[wire[1]];
            g2.setColor(new Color(0, 255, 100, 200));
            g2.drawLine(ax, ay, bx, by);
            // Glow
            g2.setColor(new Color(0, 255, 100, 60));
            g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(ax, ay, bx, by);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g2.setStroke(new BasicStroke(1));

        // Draw nodes
        for (int i = 0; i < numNodes; i++) {
            boolean lit = (i == litNode);
            int cx = nodeX[i], cy = nodeY[i];
            int r = 22;

            // Glow if lit
            if (lit) {
                g2.setColor(new Color(80, 255, 150, 80));
                g2.fillOval(cx - r - 8, cy - r - 8, (r + 8)*2, (r + 8)*2);
            }
            // Node circle
            g2.setColor(lit ? new Color(60, 240, 120) : new Color(12, 55, 25));
            g2.fillOval(cx - r, cy - r, r*2, r*2);
            // Node border
            g2.setColor(lit ? new Color(120, 255, 170) : new Color(0, 140, 55));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r, cy - r, r*2, r*2);
            g2.setStroke(new BasicStroke(1));
            // Label
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2.setColor(lit ? Color.WHITE : new Color(100, 220, 130));
            FontMetrics fm = g2.getFontMetrics();
            String lbl = String.valueOf(nodeLabels[i]);
            g2.drawString(lbl, cx - fm.stringWidth(lbl)/2, cy + fm.getAscent()/2 - 2);

            // Small circuit trace dot
            g2.setColor(new Color(0, 180, 60, 100));
            g2.fillOval(cx - 4, cy - 4, 8, 8);
        }

        // Timer bar
        if (playerTurn && secondsLeft > 0) {
            int barW = 560, barH = 10;
            int barX = (W - barW) / 2, barY = 445;
            g2.setColor(new Color(20, 60, 25));
            g2.fillRoundRect(barX, barY, barW, barH, 5, 5);
            float pct = secondsLeft / (float) TIMER_SECONDS;
            Color barCol = pct > 0.5f ? new Color(0, 210, 80) : (pct > 0.25f ? new Color(220, 200, 0) : new Color(230, 50, 30));
            g2.setColor(barCol);
            g2.fillRoundRect(barX, barY, (int)(barW * pct), barH, 5, 5);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(new Color(180, 255, 180));
            g2.drawString(secondsLeft + "s", barX + barW + 6, barY + 9);
        }

        // Status message
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(statusColor);
        FontMetrics sfm = g2.getFontMetrics();
        g2.drawString(statusMsg, (W - sfm.stringWidth(statusMsg)) / 2, 475);
    }

    // ── Background helpers (unchanged from original) ──────────────────────
    private void drawLavaFloor(Graphics2D g2, int W, int H) {
        GradientPaint lava = new GradientPaint(0, H - 65, new Color(180, 40, 5, 180),
                0, H, new Color(220, 80, 10, 80));
        g2.setPaint(lava);
        g2.fillRect(0, H - 65, W, 65);
        g2.setPaint(null);
        g2.setColor(new Color(20, 8, 5));
        for (int x = 0; x < W + 80; x += 95) {
            int jitter = (int)(Math.sin(x * 0.31) * 4);
            g2.fillRect(x + 2, H - 60 + jitter, 90, 55);
        }
        g2.setColor(new Color(240, 80, 10, 140));
        g2.setStroke(new BasicStroke(1.5f));
        for (int x = 95; x < W; x += 95) {
            int jitter = (int)(Math.sin(x * 0.31) * 4);
            g2.drawLine(x, H - 60 + jitter, x, H);
        }
        g2.drawLine(0, H - 60, W, H - 60);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawFlames(Graphics2D g2, int W, int H) {
        long t = System.currentTimeMillis();
        for (int x = 0; x < W; x += 22) {
            double hh = 30 + Math.sin(t * 0.002 + x * 0.06) * 18;
            int fh = (int) hh;
            g2.setColor(new Color(200, 70, 8, 140));
            g2.fillPolygon(new int[]{x-4,x+11,x+26}, new int[]{H-60,H-60-fh,H-60}, 3);
        }
        for (int x = 0; x < W; x += 16) {
            double hh = 22 + Math.sin(t * 0.003 + x * 0.09 + 1.0) * 14;
            int fh = (int) hh;
            g2.setColor(new Color(255, 170, 20, 185));
            g2.fillPolygon(new int[]{x+2,x+8,x+14}, new int[]{H-60,H-60-fh,H-60}, 3);
        }
    }

    private void drawFireDemonCat(Graphics2D g2, int x, int y, float breath, float scale) {
        // Scaled-down version of the demon cat for the top area
        int s = (int)(scale * 100);
        g2.setColor(new Color(180, 50, 20));
        g2.fillOval(x, y + (int)(0.6f*s) + (int)breath, (int)(1.4f*s), (int)(1.1f*s));
        g2.fillOval(x + (int)(0.35f*s), y + (int)(0.2f*s) + (int)breath, (int)(0.9f*s), (int)(0.8f*s));
        // Horns
        g2.setColor(new Color(120, 20, 10));
        g2.fillPolygon(new int[]{x+(int)(.45f*s),x+(int)(.38f*s),x+(int)(.6f*s)},
                new int[]{y+(int)(.2f*s)+(int)breath,y-(int)(.2f*s)+(int)breath,y+(int)(.2f*s)+(int)breath},3);
        g2.fillPolygon(new int[]{x+(int)(.95f*s),x+(int)(1.22f*s),x+(int)(1.05f*s)},
                new int[]{y+(int)(.2f*s)+(int)breath,y-(int)(.2f*s)+(int)breath,y+(int)(.2f*s)+(int)breath},3);
        // Eyes
        g2.setColor(new Color(255, 200, 30));
        g2.fillOval(x+(int)(.5f*s), y+(int)(.42f*s)+(int)breath, (int)(.18f*s), (int)(.18f*s));
        g2.fillOval(x+(int)(.9f*s), y+(int)(.42f*s)+(int)breath, (int)(.18f*s), (int)(.18f*s));
        g2.setColor(new Color(255, 200, 30, 60));
        g2.fillOval(x+(int)(.44f*s), y+(int)(.36f*s)+(int)breath, (int)(.3f*s), (int)(.3f*s));
        g2.fillOval(x+(int)(.84f*s), y+(int)(.36f*s)+(int)breath, (int)(.3f*s), (int)(.3f*s));
        // Mittens label
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(new Color(255, 100, 50, 200));
        g2.drawString("Mittens???", x + (int)(.25f*s), y + (int)(2.1f*s));
    }
}
