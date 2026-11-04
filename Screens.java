package game;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

// ─────────────────────────────────────────────────────────────────────────────
//  MenuPanel — Title screen
// ─────────────────────────────────────────────────────────────────────────────
class MenuPanel extends JPanel {

    private float glowPhase = 0f;
    private Timer anim;

    public MenuPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(15, 15, 28));

        // Transparent center panel holds buttons using BoxLayout so they always show
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));

        btnPanel.add(Box.createVerticalGlue());

        JButton newGame = makeMenuBtn("NEW GAME");
        newGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        newGame.addActionListener(e -> {
            GameState.newGame();
            GameFrame.getInstance().showScreen(GameFrame.GAME);
        });
        btnPanel.add(newGame);
        btnPanel.add(Box.createVerticalStrut(18));

        JButton quit = makeMenuBtn("QUIT");
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);
        quit.addActionListener(e -> System.exit(0));
        btnPanel.add(quit);

        btnPanel.add(Box.createVerticalGlue());

        add(btnPanel, BorderLayout.CENTER);

        anim = new Timer(50, e -> { glowPhase += 0.06f; repaint(); });
        anim.start();
    }

    public void onShow() { repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int W = 800, H = 620;
        float _sx = getWidth()  <= 0 ? 1f : getWidth()  / 800f;
        float _sy = getHeight() <= 0 ? 1f : getHeight() / 620f;
        g2.scale(_sx, _sy);

        // ── Real background image (house entrance / exterior) ─────────────
        java.awt.image.BufferedImage bg = AssetManager.getRoomScaled("menu", W, H);
        if (bg != null) {
            g2.drawImage(bg, 0, 0, null);
        } else {
            GradientPaint gp2 = new GradientPaint(W/2f, 0, new Color(20, 18, 40), W/2f, H, new Color(8, 8, 18));
            g2.setPaint(gp2); g2.fillRect(0, 0, W, H); g2.setPaint(null);
        }

        // Dark scrim so text is readable over the photo
        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRect(0, 0, W, H);

        // Glowing ambient halo behind title
        float alpha = 0.35f + (float) Math.sin(glowPhase) * 0.12f;
        g2.setColor(new Color(1f, 0.28f, 0.08f, alpha));
        g2.fillOval(W/2 - 200, 60, 400, 220);

        // ── Title ────────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 52));
        String t1 = "TOO HOT";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(t1)) / 2;
        // shadow
        g2.setColor(new Color(0, 0, 0, 130));
        g2.drawString(t1, tx + 3, 168);
        // glow
        g2.setColor(new Color(255, 100, 50, 80));
        g2.drawString(t1, tx - 2, 163);
        g2.drawString(t1, tx + 2, 163);
        // main
        g2.setColor(new Color(255, 95, 55));
        g2.drawString(t1, tx, 165);

        // ── Subtitle ─────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.ITALIC, 18));
        g2.setColor(new Color(220, 195, 255, 210));
        String t2 = "Don't let her find out.";
        fm = g2.getFontMetrics();
        g2.drawString(t2, (W - fm.stringWidth(t2)) / 2, 202);

        // ── Instructions ─────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.setColor(new Color(190, 185, 210, 185));
        String[] lines = {
            "Keep your temperature, hunger & thirst under control.",
            "Feed the cat.  Fix the AC.  Don't let the squirrels in.",
            "If you overheat... Mittens won't be Mittens anymore.",
            "Survive 3 days.  Mom must never know."
        };
        for (int i = 0; i < lines.length; i++) {
            fm = g2.getFontMetrics();
            g2.drawString(lines[i], (W - fm.stringWidth(lines[i])) / 2, 246 + i * 24);
        }
        g2.dispose();
    }

    private JButton makeMenuBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(70, 50, 110) : new Color(45, 30, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(130, 90, 200));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                g2.setColor(new Color(220, 200, 240));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth()-fm.stringWidth(text))/2, (getHeight()+fm.getAscent())/2-2);
            }
        };
        btn.setPreferredSize(new Dimension(200, 48));
        btn.setMaximumSize(new Dimension(200, 48));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GameOverPanel — Win or lose screen with stats
// ─────────────────────────────────────────────────────────────────────────────
class GameOverPanel extends JPanel {

    private JLabel titleLabel;
    private JLabel reasonLabel;
    private JLabel statsLabel;

    public GameOverPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(12, 10, 22));
        buildUI();
    }

    private void buildUI() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(10, 20, 10, 20);
        c.anchor = GridBagConstraints.CENTER;

        titleLabel = new JLabel("", JLabel.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        add(titleLabel, c);

        reasonLabel = new JLabel("", JLabel.CENTER);
        reasonLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
        reasonLabel.setForeground(new Color(180, 180, 220));
        add(reasonLabel, c);

        statsLabel = new JLabel("", JLabel.CENTER);
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statsLabel.setForeground(new Color(150, 150, 190));
        add(statsLabel, c);

        // Buttons row
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btns.setOpaque(false);

        JButton playAgain = makeBtn("Play Again");
        playAgain.addActionListener(e -> {
            GameState.newGame();
            GameFrame.getInstance().showScreen(GameFrame.GAME);
        });
        JButton menu = makeBtn("Main Menu");
        menu.addActionListener(e -> {
            GameState.newGame();
            GameFrame.getInstance().showScreen(GameFrame.MENU);
        });
        btns.add(playAgain);
        btns.add(menu);
        add(btns, c);
    }

    public void onShow() {
        if (GameState.won) {
            titleLabel.setText("MOM NEVER KNEW!");
            titleLabel.setForeground(new Color(80, 220, 120));
        } else {
            titleLabel.setText("GAME OVER");
            titleLabel.setForeground(new Color(220, 60, 60));
        }
        // Reason may contain \n — wrap in html
        reasonLabel.setText("<html><center>" +
                GameState.gameOverReason.replace("\n", "<br>") +
                "</center></html>");

        statsLabel.setText(String.format(
                "<html><center>Day %d reached &nbsp;|&nbsp; Temp: %.0f&deg; &nbsp;|&nbsp; Coins left: %d</center></html>",
                GameState.day, GameState.temperature, GameState.coins));
        repaint();
    }

    private JButton makeBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(55, 45, 90) : new Color(35, 28, 65));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(100, 80, 160));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.setColor(new Color(210, 200, 240));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth()-fm.stringWidth(text))/2, (getHeight()+fm.getAscent())/2-2);
            }
        };
        btn.setPreferredSize(new Dimension(160, 42));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
