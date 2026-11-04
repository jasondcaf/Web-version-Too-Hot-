package game;

import javax.swing.*;
import java.awt.*;
import javax.swing.Timer;

public class MomArrivalPanel extends JPanel {

    // Animation phases
    private static final int PHASE_CAR_IN   = 0;
    private static final int PHASE_MOM_WALK = 1;
    private static final int PHASE_ARRIVED  = 2;

    private Timer   animTimer;
    private int     frame      = 0;
    private int     phase      = PHASE_CAR_IN;
    private float   carX       = 980f;
    private float   momX       = 940f;
    private float   coinAlpha  = 0f;
    private float   twinkle    = 0f;
    private JButton actionBtn;

    public MomArrivalPanel() {
        setLayout(null);
        setBackground(Color.BLACK);
        buildButton();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) { repositionButton(); }
        });
    }

    private int ax(int v) { int w = getWidth();  return w <= 0 ? v : Math.round(v * w / 800f); }
    private int ay(int v) { int h = getHeight(); return h <= 0 ? v : Math.round(v * h / 620f); }

    private void repositionButton() {
        if (actionBtn != null) actionBtn.setBounds(ax(270), ay(510), ax(260), ay(54));
    }

    private void buildButton() {
        actionBtn = new JButton("Go to Your Room!") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                        getModel().isRollover() ? new Color(72, 148, 72) : new Color(40, 108, 45),
                        0, getHeight(), new Color(22, 72, 28));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setPaint(null);
                g2.setColor(new Color(115, 228, 125));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 14, 14);
                g2.setStroke(new BasicStroke(1));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 17));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth()-fm.stringWidth(t))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
        };
        actionBtn.setBounds(ax(270), ay(510), ax(260), ay(54));
        actionBtn.setOpaque(false); actionBtn.setContentAreaFilled(false);
        actionBtn.setBorderPainted(false); actionBtn.setFocusPainted(false);
        actionBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionBtn.setVisible(false);
        actionBtn.addActionListener(e -> {
            if (animTimer != null) animTimer.stop();
            if (GameState.lastDayDone) {
                GameState.gameOverReason = "You survived all 3 days! Mom never knew.";
                GameFrame.getInstance().showScreen(GameFrame.GAMEOVER);
            } else {
                GameFrame.getInstance().showScreen(GameFrame.SHOP);
            }
        });
        add(actionBtn);
    }

    public void onShow() {
        frame     = 0;
        phase     = PHASE_CAR_IN;
        carX      = 980f;
        momX      = 940f;
        coinAlpha = 0f;
        twinkle   = 0f;
        actionBtn.setText(GameState.lastDayDone ? "Thanks mom! (Finish)" : "Go to Your Room!");
        actionBtn.setVisible(false);

        GameState.coins += GameState.pendingAllowance;

        if (animTimer != null) animTimer.stop();
        animTimer = new Timer(30, e -> {
            frame++;
            twinkle += 0.07f;

            if (phase == PHASE_CAR_IN) {
                if (carX > 495f) { carX -= 7.5f; }
                else { phase = PHASE_MOM_WALK; momX = carX + 55f; }
            } else if (phase == PHASE_MOM_WALK) {
                if (momX > 305f) { momX -= 4f; }
                else             { phase = PHASE_ARRIVED; }
            } else {
                coinAlpha = Math.min(1f, coinAlpha + 0.028f);
                if (coinAlpha >= 1f && !actionBtn.isVisible()) actionBtn.setVisible(true);
            }
            repaint();
        });
        animTimer.start();
    }

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

        drawBackground(g2, W, H);
        drawHouse(g2, W, H);
        drawStreetLamp(g2, W, H);
        drawDriveway(g2, W, H);
        drawRoad(g2, W, H);

        // Car visible in all phases (parked after phase 0)
        drawCar(g2, (int)carX, H - 122);

        // Mom visible in phases 1 and 2
        if (phase == PHASE_MOM_WALK || phase == PHASE_ARRIVED) {
            drawMom(g2, (int)momX, H - 232);
        }
        if (phase == PHASE_ARRIVED) {
            drawSpeechBubble(g2, (int)momX, H - 232);
        }

        drawTitle(g2, W, H);

        if (coinAlpha > 0f) drawCoins(g2, W, H);

        g2.dispose();
    }

    // ── Scene layers ──────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g2, int W, int H) {
        // Deep night sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(8, 6, 32),
                0, H - 140, new Color(28, 16, 60));
        g2.setPaint(sky);
        g2.fillRect(0, 0, W, H - 140);
        g2.setPaint(null);

        // Warm horizon glow near ground
        GradientPaint glow = new GradientPaint(0, H - 200, new Color(200, 130, 40, 0),
                0, H - 140, new Color(200, 130, 40, 55));
        g2.setPaint(glow);
        g2.fillRect(0, H - 200, W, 60);
        g2.setPaint(null);

        // Stars
        int[][] stars = {
            {38,22},{82,54},{158,18},{232,44},{318,28},{405,52},{488,18},{565,42},{632,26},{714,55},{768,35},
            {62,88},{128,72},{205,105},{288,85},{372,108},{455,88},{528,62},{602,95},{678,72},{748,98},
            {18,138},{95,118},{175,148},{260,122},{345,155},{428,135},{510,112},{588,145},{662,128},{740,118}
        };
        for (int[] s : stars) {
            float t = (float)Math.sin(twinkle + s[0] * 0.09f);
            int alpha = (int)(155 + t * 85);
            g2.setColor(new Color(255, 255, 255, Math.max(50, Math.min(255, alpha))));
            int sz = (s[0] % 5 == 0) ? 3 : (s[0] % 3 == 0 ? 2 : 2);
            g2.fillOval(s[0], s[1], sz, sz);
            // Occasional 4-point star sparkle
            if (s[0] % 7 == 0 && alpha > 200) {
                g2.setColor(new Color(255, 255, 255, 100));
                g2.drawLine(s[0]+sz/2, s[1]-3, s[0]+sz/2, s[1]+sz+3);
                g2.drawLine(s[0]-3, s[1]+sz/2, s[0]+sz+3, s[1]+sz/2);
            }
        }

        // Moon with crescent shadow and halo
        int mx = W - 108, my = 24;
        g2.setColor(new Color(255, 248, 195, 22));
        g2.fillOval(mx - 24, my - 14, 96, 96);
        g2.setColor(new Color(255, 248, 195, 14));
        g2.fillOval(mx - 36, my - 24, 120, 120);
        g2.setColor(new Color(238, 232, 185));
        g2.fillOval(mx, my, 60, 60);
        g2.setColor(new Color(16, 10, 44));            // shadow bites into moon
        g2.fillOval(mx + 10, my, 56, 56);
        g2.setColor(new Color(195, 190, 148, 70));     // crater
        g2.fillOval(mx + 6, my + 34, 14, 11);
        g2.setColor(new Color(195, 190, 148, 50));
        g2.fillOval(mx + 18, my + 20, 9, 8);

        // Lawn
        GradientPaint lawn = new GradientPaint(0, H - 140, new Color(22, 48, 22),
                0, H, new Color(12, 30, 12));
        g2.setPaint(lawn);
        g2.fillRect(0, H - 140, W, 140);
        g2.setPaint(null);
    }

    private void drawRoad(Graphics2D g2, int W, int H) {
        // Sidewalk
        g2.setColor(new Color(128, 122, 112));
        g2.fillRect(0, H - 76, W, 20);
        g2.setColor(new Color(108, 102, 92));
        g2.drawLine(0, H - 76, W, H - 76);
        g2.drawLine(0, H - 56, W, H - 56);
        // Sidewalk slabs
        g2.setColor(new Color(98, 92, 82, 140));
        for (int xi = 0; xi < W; xi += 145) g2.drawLine(xi, H - 76, xi, H - 56);

        // Road
        g2.setColor(new Color(32, 32, 36));
        g2.fillRect(0, H - 56, W, 56);
        // Yellow centre dashes
        g2.setColor(new Color(200, 180, 48, 150));
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[]{32, 22}, 0));
        g2.drawLine(0, H - 28, W, H - 28);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawDriveway(Graphics2D g2, int W, int H) {
        int[] dx = {388, 570, W, W};
        int[] dy = {H - 140, H - 140, H - 76, H - 76};
        g2.setColor(new Color(108, 100, 90));
        g2.fillPolygon(dx, dy, 4);
        g2.setColor(new Color(88, 80, 72, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(420, H - 140, W - 110, H - 76);
        g2.drawLine(510, H - 140, W - 28,  H - 76);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawStreetLamp(Graphics2D g2, int W, int H) {
        int lx = 680, groundY = H - 140;
        // Light cone on ground
        g2.setColor(new Color(255, 200, 80, 32));
        g2.fillOval(lx - 70, groundY - 22, 140, 50);
        // Pole
        g2.setColor(new Color(55, 52, 46));
        g2.fillRect(lx - 4, groundY - 175, 8, 175);
        // Pole cap / arm
        g2.fillRect(lx - 34, groundY - 175, 38, 7);
        // Housing
        g2.setColor(new Color(44, 42, 38));
        g2.fillRoundRect(lx - 40, groundY - 192, 28, 20, 5, 5);
        // Bulb glow
        g2.setColor(new Color(255, 225, 110, 200));
        g2.fillOval(lx - 37, groundY - 189, 22, 14);
        g2.setColor(new Color(255, 235, 160, 80));
        g2.fillOval(lx - 52, groundY - 205, 52, 44);
    }

    private void drawHouse(Graphics2D g2, int W, int H) {
        int hx = 85, hy = H - 368, hw = 330, hh = 230;

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 65));
        g2.fillRect(hx + 12, hy + hh - 6, hw + 14, 14);

        // ── Garage wing ────────────────────────────────────────────────────
        int gx = hx + hw, gy = hy + 50, gw = 118, gh = 180;
        g2.setColor(new Color(188, 178, 158));
        g2.fillRect(gx, gy, gw, gh);
        // Garage door
        g2.setColor(new Color(148, 140, 125));
        g2.fillRect(gx + 8, gy + 28, gw - 16, 128);
        g2.setColor(new Color(128, 120, 105));
        for (int p = 0; p < 4; p++) {
            g2.drawRect(gx + 8, gy + 28 + p * 32, gw - 16, 32);
            g2.drawLine(gx + 8 + (gw - 16) / 2, gy + 28 + p * 32,
                        gx + 8 + (gw - 16) / 2, gy + 28 + p * 32 + 32);
        }
        // Garage light
        g2.setColor(new Color(255, 215, 100, 85));
        g2.fillOval(gx + gw/2 - 8, gy + 12, 16, 14);

        // ── Main house walls ───────────────────────────────────────────────
        g2.setColor(new Color(212, 200, 178));
        g2.fillRect(hx, hy, hw, hh);

        // Brick texture rows
        g2.setColor(new Color(188, 175, 152, 75));
        for (int row = 0; row < 7; row++) {
            int rowY = hy + 10 + row * 30;
            int off  = (row % 2 == 0) ? 0 : 35;
            for (int col = off - 35; col < hw + 35; col += 68) {
                g2.drawRoundRect(hx + col, rowY, 60, 24, 3, 3);
            }
        }

        // ── Roof ───────────────────────────────────────────────────────────
        g2.setColor(new Color(62, 40, 24));
        int[] rX = {hx - 24, hx + hw + 24, hx + hw/2};
        int[] rY = {hy, hy, hy - 120};
        g2.fillPolygon(rX, rY, 3);
        // Roof shadow side (right face)
        g2.setColor(new Color(40, 24, 12));
        int[] r2X = {hx + hw/2, hx + hw + 24, hx + hw/2 + 18};
        int[] r2Y = {hy - 120, hy, hy - 110};
        g2.fillPolygon(r2X, r2Y, 3);
        // Roof outline
        g2.setColor(new Color(82, 58, 36));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawPolygon(rX, rY, 3);
        g2.setStroke(new BasicStroke(1));
        // Fascia board along eaves
        g2.setColor(new Color(98, 72, 48));
        g2.fillRect(hx - 24, hy - 8, hw + 48, 12);
        // Ridge cap
        g2.setColor(new Color(105, 78, 50));
        g2.fillRect(hx + hw/2 - 10, hy - 125, 20, 12);

        // ── Chimney ────────────────────────────────────────────────────────
        int chx = hx + hw - 72;
        g2.setColor(new Color(135, 88, 68));
        g2.fillRect(chx, hy - 95, 36, 112);
        g2.setColor(new Color(152, 105, 82));
        g2.fillRect(chx - 4, hy - 104, 44, 14);
        // Smoke puffs
        int sf = frame % 8;
        g2.setColor(new Color(165, 162, 175, 50 + sf * 4));
        g2.fillOval(chx - 2, hy - 135 - sf * 4, 26 + sf, 24 + sf);
        g2.setColor(new Color(145, 142, 158, 35));
        g2.fillOval(chx + 2, hy - 160 - sf * 3, 20 + sf, 18 + sf);

        // ── Windows ────────────────────────────────────────────────────────
        drawWindow(g2, hx + 18,      hy + 42, 70, 62);
        drawWindow(g2, hx + hw - 88, hy + 42, 70, 62);
        drawWindow(g2, hx + hw/2 - 26, hy - 52, 52, 44);  // attic window

        // ── Front door ─────────────────────────────────────────────────────
        int dx2 = hx + hw/2 - 43;
        int dy2 = hy + hh - 150;
        // Door frame (moulding)
        g2.setColor(new Color(100, 70, 42));
        g2.fillRoundRect(dx2 - 5, dy2 - 5, 96, 158, 7, 0);
        // Door body (dark walnut)
        g2.setColor(new Color(52, 28, 14));
        g2.fillRoundRect(dx2, dy2, 86, 153, 5, 0);
        // Raised panels
        g2.setColor(new Color(38, 18, 8));
        g2.fillRoundRect(dx2 + 7, dy2 + 8,  34, 52, 4, 4);
        g2.fillRoundRect(dx2 + 45, dy2 + 8, 34, 52, 4, 4);
        g2.fillRoundRect(dx2 + 7, dy2 + 70, 72, 52, 4, 4);
        g2.setColor(new Color(68, 42, 22));
        g2.drawRoundRect(dx2 + 7, dy2 + 8,  34, 52, 4, 4);
        g2.drawRoundRect(dx2 + 45, dy2 + 8, 34, 52, 4, 4);
        g2.drawRoundRect(dx2 + 7, dy2 + 70, 72, 52, 4, 4);
        // Knob
        g2.setColor(new Color(215, 172, 58));
        g2.fillOval(dx2 + 69, dy2 + 88, 13, 13);
        g2.setColor(new Color(175, 135, 38));
        g2.drawOval(dx2 + 69, dy2 + 88, 13, 13);
        // Transom window
        g2.setColor(new Color(255, 225, 148, 110));
        g2.fillRect(dx2, dy2 - 28, 86, 24);
        g2.setColor(new Color(100, 70, 42));
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(dx2, dy2 - 28, 86, 24);
        g2.drawLine(dx2 + 28, dy2 - 28, dx2 + 28, dy2);
        g2.drawLine(dx2 + 57, dy2 - 28, dx2 + 57, dy2);
        g2.setStroke(new BasicStroke(1));

        // Porch light + glow
        g2.setColor(new Color(255, 205, 80, 85));
        g2.fillOval(dx2 - 38, dy2 - 62, 90, 68);
        g2.setColor(new Color(255, 205, 80));
        g2.fillOval(dx2 + 35, dy2 - 56, 12, 12);

        // House outline
        g2.setColor(new Color(148, 135, 115));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(hx, hy, hw, hh);
        g2.setStroke(new BasicStroke(1));

        // Wall sconce lights
        g2.setColor(new Color(255, 210, 120, 70));
        g2.fillOval(hx + 18 - 10, hy + 42 - 12, 30, 22);
        g2.fillOval(hx + hw - 88 + 50, hy + 42 - 12, 30, 22);
    }

    private void drawWindow(Graphics2D g2, int x, int y, int w, int h) {
        // Warm interior glow spilling out
        g2.setColor(new Color(255, 210, 120, 48));
        g2.fillRect(x - 6, y - 6, w + 12, h + 12);
        // Glass — warm amber-yellow (evening light inside)
        GradientPaint glass = new GradientPaint(x, y, new Color(255, 218, 135, 210),
                x + w, y + h, new Color(215, 162, 58, 195));
        g2.setPaint(glass);
        g2.fillRect(x, y, w, h);
        g2.setPaint(null);
        // Thin curtain edges
        g2.setColor(new Color(218, 175, 98, 90));
        g2.fillRect(x, y, 11, h);
        g2.fillRect(x + w - 11, y, 11, h);
        // Window frame
        g2.setColor(new Color(222, 210, 185));
        g2.setStroke(new BasicStroke(3.5f));
        g2.drawRect(x, y, w, h);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(x + w/2, y, x + w/2, y + h);
        g2.drawLine(x, y + h/2, x + w, y + h/2);
        g2.setStroke(new BasicStroke(1));
        // Sill
        g2.setColor(new Color(200, 188, 165));
        g2.fillRect(x - 4, y + h, w + 8, 7);
        // Glass reflection highlight
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRect(x + 2, y + 2, w/3, h/3);
    }

    private void drawCar(Graphics2D g2, int x, int y) {
        // Ground shadow
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(x, y + 52, 215, 20);

        // Body — rich red
        g2.setColor(new Color(175, 35, 35));
        g2.fillRoundRect(x, y + 24, 212, 40, 9, 9);
        // Roof cab
        int[] capX = {x + 20, x + 192, x + 178, x + 34};
        int[] capY = {y + 24, y + 24, y - 8, y - 8};
        g2.fillPolygon(capX, capY, 4);
        // Body highlight / crease line
        g2.setColor(new Color(215, 68, 68));
        g2.fillRect(x, y + 30, 212, 8);

        // Windshield + side windows
        g2.setColor(new Color(148, 204, 228, 175));
        int[] wX = {x + 37, x + 175, x + 170, x + 42};
        int[] wY = {y + 24, y + 24, y - 5, y - 5};
        g2.fillPolygon(wX, wY, 4);
        g2.setColor(new Color(128, 185, 210, 155));
        g2.fillRoundRect(x + 6,   y - 6, 26, 28, 4, 4);
        g2.fillRoundRect(x + 180, y - 6, 26, 28, 4, 4);
        // Window frames
        g2.setColor(new Color(42, 32, 28));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x + 6,   y - 6, 26, 28, 4, 4);
        g2.drawRoundRect(x + 180, y - 6, 26, 28, 4, 4);
        g2.setStroke(new BasicStroke(1));

        // Wheels
        drawWheel(g2, x + 42,  y + 62);
        drawWheel(g2, x + 165, y + 62);

        // Tail lights (left side = rear)
        g2.setColor(new Color(255, 72, 72, 175));
        g2.fillRoundRect(x - 5, y + 30, 8, 24, 3, 3);
        g2.setColor(new Color(255, 55, 55, 70));
        g2.fillOval(x - 18, y + 22, 22, 40);

        // Headlights (right side = front)
        g2.setColor(new Color(255, 252, 205, 210));
        g2.fillRoundRect(x + 209, y + 30, 8, 18, 3, 3);
        g2.setColor(new Color(255, 248, 185, 90));
        g2.fillOval(x + 195, y + 20, 24, 32);

        // Door seams + handle
        g2.setColor(new Color(138, 22, 22));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(x + 105, y + 24, x + 105, y + 64);
        g2.drawLine(x + 34,  y + 40, x + 176, y + 40);
        g2.setStroke(new BasicStroke(1));
        g2.setColor(new Color(215, 172, 65));
        g2.fillRoundRect(x + 120, y + 36, 18, 5, 2, 2);
        g2.fillRoundRect(x + 68,  y + 36, 18, 5, 2, 2);

        // Bumpers
        g2.setColor(new Color(55, 45, 40));
        g2.fillRoundRect(x - 6, y + 56, 14, 8, 3, 3);
        g2.fillRoundRect(x + 204, y + 56, 14, 8, 3, 3);
    }

    private void drawWheel(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(18, 18, 18));
        g2.fillOval(cx - 22, cy - 22, 44, 44);
        // Rim
        GradientPaint rim = new GradientPaint(cx - 16, cy - 16, new Color(200, 195, 182),
                cx + 16, cy + 16, new Color(155, 150, 138));
        g2.setPaint(rim);
        g2.fillOval(cx - 16, cy - 16, 32, 32);
        g2.setPaint(null);
        // Hub
        g2.setColor(new Color(135, 130, 118));
        g2.fillOval(cx - 8, cy - 8, 16, 16);
        // Spokes (animated spin while car is moving)
        g2.setColor(new Color(215, 210, 198));
        float rot = (phase == PHASE_CAR_IN) ? frame * 0.18f : 0f;
        for (int s = 0; s < 5; s++) {
            double a = rot + s * Math.PI * 2 / 5;
            int sx = (int)(cx + Math.cos(a) * 12);
            int sy = (int)(cy + Math.sin(a) * 12);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, cy, sx, sy);
        }
        g2.setStroke(new BasicStroke(1));
    }

    private void drawMom(Graphics2D g2, int x, int y) {
        float walk = (float)Math.sin(frame * 0.22);
        int   sw   = (int)(walk * 11);
        int   lw   = (int)(walk * 15);

        // Ground shadow
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fillOval(x - 24, y + 132, 64, 15);

        // ── Back leg ──────────────────────────────────────────────────────
        g2.setColor(new Color(40, 22, 14));
        g2.fillRoundRect(x - 5, y + 98, 14, 44, 5, 5);
        g2.fillRoundRect(x - 14, y + 138, 26, 9, 4, 4);

        // ── Skirt (A-line) ────────────────────────────────────────────────
        g2.setColor(new Color(148, 55, 102));
        int[] skX = {x - 22, x + 30, x + 44, x - 38};
        int[] skY = {y + 60, y + 60, y + 108, y + 108};
        g2.fillPolygon(skX, skY, 4);
        // Skirt fold highlight
        g2.setColor(new Color(168, 72, 118));
        g2.fillRect(x - 10, y + 60, 10, 48);
        g2.setColor(new Color(122, 42, 85));
        g2.fillRect(x + 10, y + 60, 10, 48);

        // ── Front leg ─────────────────────────────────────────────────────
        g2.setColor(new Color(50, 30, 18));
        g2.fillRoundRect(x + 2, y + 96 - lw/2, 14, 44, 5, 5);
        g2.fillRoundRect(x - 4, y + 137 - lw/2, 26, 10, 4, 4);

        // ── Torso / blouse ────────────────────────────────────────────────
        g2.setColor(new Color(210, 205, 220));   // light periwinkle blouse
        g2.fillRoundRect(x - 20, y + 26, 50, 40, 9, 9);
        g2.setColor(new Color(188, 184, 202));   // shading
        g2.fillRect(x + 6, y + 26, 14, 40);
        // Collar
        g2.setColor(new Color(230, 228, 238));
        g2.fillOval(x - 4, y + 22, 18, 14);
        // Belt at waist
        g2.setColor(new Color(125, 45, 85));
        g2.fillRect(x - 22, y + 58, 52, 7);
        g2.setColor(new Color(205, 162, 52));
        g2.fillOval(x + 3, y + 59, 9, 6);

        // ── Back arm (carries groceries) ──────────────────────────────────
        g2.setColor(new Color(218, 180, 148));
        g2.fillRoundRect(x + 22, y + 28 - sw, 13, 40, 5, 5);
        // Grocery bag
        g2.setColor(new Color(205, 190, 152));
        g2.fillRoundRect(x + 26, y + 50 - sw, 32, 36, 5, 5);
        g2.setColor(new Color(160, 145, 108));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawArc(x + 30, y + 42 - sw, 18, 14, 0, 180);
        g2.setStroke(new BasicStroke(1));
        // Bag label stripe
        g2.setColor(new Color(88, 145, 88));
        g2.fillRect(x + 26, y + 62 - sw, 32, 8);
        // Groceries sticking out
        g2.setColor(new Color(65, 140, 60));
        g2.fillOval(x + 28, y + 46 - sw, 14, 11);
        g2.fillOval(x + 38, y + 44 - sw, 12, 9);
        // Baguette
        g2.setColor(new Color(210, 168, 90));
        g2.fillRoundRect(x + 50, y + 38 - sw, 8, 24, 3, 3);

        // ── Front arm ─────────────────────────────────────────────────────
        g2.setColor(new Color(218, 180, 148));
        g2.fillRoundRect(x - 24, y + 28 + sw, 13, 38, 5, 5);
        g2.fillOval(x - 24, y + 62 + sw, 13, 12);

        // ── Head ──────────────────────────────────────────────────────────
        // Neck
        g2.setColor(new Color(218, 180, 148));
        g2.fillRect(x + 1, y + 14, 10, 15);
        // Head oval
        g2.setColor(new Color(225, 188, 155));
        g2.fillOval(x - 17, y - 38, 46, 54);
        // Face shading (side)
        g2.setColor(new Color(200, 165, 132));
        g2.fillOval(x + 10, y - 30, 16, 40);

        // ── Hair ──────────────────────────────────────────────────────────
        g2.setColor(new Color(52, 28, 10));
        g2.fillArc(x - 18, y - 40, 48, 34, 0, 180);
        g2.fillRoundRect(x - 18, y - 32, 9, 34, 4, 4);
        g2.fillRoundRect(x + 21, y - 32, 9, 30, 4, 4);
        // Loose strands + bun
        g2.fillOval(x + 17, y - 52, 20, 18);
        g2.setColor(new Color(80, 48, 18));
        g2.fillArc(x - 12, y - 38, 24, 20, 15, 150);

        // ── Face features ─────────────────────────────────────────────────
        // Eyes
        g2.setColor(new Color(28, 18, 10));
        g2.fillOval(x - 8, y - 18, 8, 8);
        g2.fillOval(x + 8, y - 18, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 7, y - 16, 3, 3);
        g2.fillOval(x + 9, y - 16, 3, 3);
        // Eyebrows
        g2.setColor(new Color(42, 22, 8));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x - 10, y - 24, x - 3, y - 26);
        g2.drawLine(x + 8,  y - 26, x + 15, y - 24);
        g2.setStroke(new BasicStroke(1));
        // Nose
        g2.setColor(new Color(188, 150, 120));
        g2.fillOval(x + 2, y - 8, 7, 6);
        // Smile
        g2.setColor(new Color(168, 80, 80));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(x - 4, y - 4, 18, 10, 200, 140);
        g2.setStroke(new BasicStroke(1));
        // Cheeks
        g2.setColor(new Color(230, 138, 130, 85));
        g2.fillOval(x - 13, y - 7, 13, 9);
        g2.fillOval(x + 12, y - 7, 13, 9);
        // Earring
        g2.setColor(new Color(215, 172, 55));
        g2.fillOval(x - 20, y - 16, 7, 7);
        g2.setColor(new Color(175, 135, 35));
        g2.drawOval(x - 20, y - 16, 7, 7);
    }

    private void drawSpeechBubble(Graphics2D g2, int mx, int my) {
        String speech = GameState.lastDayDone
                ? "\"So proud of you!  ♥\"" : "\"You better have cleaned up!\"";
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(speech) + 30;
        int bh = 40;
        int bx = mx - bw / 2;
        int by = my - 118;

        // Soft drop shadow
        g2.setColor(new Color(0, 0, 0, 48));
        g2.fillRoundRect(bx + 3, by + 3, bw, bh, 15, 15);
        // Bubble body
        g2.setColor(new Color(255, 255, 255, 245));
        g2.fillRoundRect(bx, by, bw, bh, 15, 15);
        g2.setColor(new Color(155, 152, 172));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bx, by, bw, bh, 15, 15);
        g2.setStroke(new BasicStroke(1));
        // Tail pointing down to mom's head
        int[] tx = {mx - 6, mx + 8, mx + 1};
        int[] ty = {by + bh, by + bh, my - 42};
        g2.setColor(new Color(255, 255, 255, 245));
        g2.fillPolygon(tx, ty, 3);
        g2.setColor(new Color(155, 152, 172));
        g2.drawLine(mx - 6, by + bh, mx + 1, my - 42);
        g2.drawLine(mx + 8, by + bh, mx + 1, my - 42);
        // Text
        g2.setColor(new Color(40, 36, 60));
        g2.drawString(speech, bx + 15, by + 26);
    }

    private void drawTitle(Graphics2D g2, int W, int H) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 38));
        String title = "MOM IS HOME!";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(title)) / 2;
        // Glow layers
        for (int off = 6; off >= 1; off--) {
            g2.setColor(new Color(220, 55, 55, 28));
            g2.drawString(title, tx - off, 68 - off);
            g2.drawString(title, tx + off, 68 + off);
        }
        // Shadow
        g2.setColor(new Color(0, 0, 0, 115));
        g2.drawString(title, tx + 2, 70);
        // Main
        g2.setColor(new Color(255, 82, 82));
        g2.drawString(title, tx, 68);
        // Top shine
        g2.setColor(new Color(255, 168, 168, 100));
        g2.drawString(title, tx, 67);

        // Day sub-label
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        String sub = "Day " + (GameState.day - 1) + " complete!";
        FontMetrics sfm = g2.getFontMetrics();
        int sx = (W - sfm.stringWidth(sub)) / 2;
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawString(sub, sx + 1, 92);
        g2.setColor(new Color(255, 215, 80));
        g2.drawString(sub, sx, 91);
    }

    private void drawCoins(Graphics2D g2, int W, int H) {
        Composite orig = g2.getComposite();

        // Allowance label banner
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.min(coinAlpha * 0.9f, 0.9f)));
        g2.setColor(new Color(25, 18, 5, 190));
        g2.fillRoundRect(W/2 - 150, H - 250, 300, 42, 12, 12);
        g2.setColor(new Color(215, 175, 55));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(W/2 - 150, H - 250, 300, 42, 12, 12);
        g2.setStroke(new BasicStroke(1));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.min(coinAlpha, 1f)));
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        String label = "+" + GameState.pendingAllowance + " allowance coins!";
        FontMetrics lfm = g2.getFontMetrics();
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(label, (W - lfm.stringWidth(label))/2 + 1, H - 221);
        g2.setColor(new Color(255, 218, 58));
        g2.drawString(label, (W - lfm.stringWidth(label))/2, H - 222);

        // Coin row with bounce
        int numCoins = Math.min(GameState.pendingAllowance, 12);
        int startX   = W/2 - (numCoins * 32) / 2;
        int baseY    = H - 192;

        for (int i = 0; i < numCoins; i++) {
            double bounce = Math.sin(twinkle * 1.6 + i * 0.55) * 7;
            int cx = startX + i * 32;
            int cy = (int)(baseY + bounce);

            // Coin shadow
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.min(coinAlpha * 0.38f, 0.38f)));
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(cx + 2, cy + 28, 24, 8);

            // Coin body with gradient
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    Math.min(coinAlpha, 1f)));
            GradientPaint cg = new GradientPaint(cx, cy, new Color(255, 235, 85),
                    cx + 25, cy + 25, new Color(195, 148, 18));
            g2.setPaint(cg);
            g2.fillOval(cx, cy, 26, 26);
            g2.setPaint(null);
            // Coin edge
            g2.setColor(new Color(175, 132, 16));
            g2.drawOval(cx, cy, 26, 26);
            // Inner ring
            g2.setColor(new Color(218, 188, 45, 120));
            g2.drawOval(cx + 3, cy + 3, 20, 20);
            // $ symbol
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(140, 100, 8));
            g2.drawString("$", cx + 9, cy + 18);
            // Shine spot
            g2.setColor(new Color(255, 255, 255, 130));
            g2.fillOval(cx + 4, cy + 3, 9, 8);
        }

        g2.setComposite(orig);
    }
}
