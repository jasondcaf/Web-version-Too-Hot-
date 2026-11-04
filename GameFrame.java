package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameFrame extends JFrame {

    // ── Singleton ──────────────────────────────────────────────────────────
    private static GameFrame instance;

    public static GameFrame getInstance() {
        if (instance == null) instance = new GameFrame();
        return instance;
    }

    // ── Screen name constants ──────────────────────────────────────────────
    public static final String MENU        = "MENU";
    public static final String GAME        = "GAME";
    public static final String NIGHTMARE   = "NIGHTMARE";
    public static final String MOM_ARRIVAL = "MOM_ARRIVAL";
    public static final String SHOP        = "SHOP";
    public static final String GAMEOVER    = "GAMEOVER";

    // ── Panels ────────────────────────────────────────────────────────────
    private final CardLayout   cardLayout;
    private final JPanel       mainPanel;

    private final MenuPanel       menuPanel;
    private final GamePanel       gamePanel;
    private final NightmarePanel  nightmarePanel;
    private final MomArrivalPanel momArrivalPanel;
    private final ShopPanel       shopPanel;
    private final GameOverPanel   gameOverPanel;

    private boolean fullscreen    = false;
    private String  currentScreen = MENU;

    private GameFrame() {
        super("Too Hot");
        instance = this;  // set early so panels can call getInstance() during init
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 620);
        setLocationRelativeTo(null);
        setResizable(true);

        cardLayout = new CardLayout();
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setBackground(Color.BLACK);

        menuPanel       = new MenuPanel();
        gamePanel       = new GamePanel();
        nightmarePanel  = new NightmarePanel();
        momArrivalPanel = new MomArrivalPanel();
        shopPanel       = new ShopPanel();
        gameOverPanel   = new GameOverPanel();

        mainPanel.add(menuPanel,       MENU);
        mainPanel.add(gamePanel,       GAME);
        mainPanel.add(nightmarePanel,  NIGHTMARE);
        mainPanel.add(momArrivalPanel, MOM_ARRIVAL);
        mainPanel.add(shopPanel,       SHOP);
        mainPanel.add(gameOverPanel,   GAMEOVER);

        add(mainPanel);

    }

    // ── Fullscreen toggle ─────────────────────────────────────────────────
    public void toggleFullscreen() {
        if (!fullscreen) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            fullscreen = true;
        } else {
            setExtendedState(JFrame.NORMAL);
            setSize(800, 620);
            setLocationRelativeTo(null);
            fullscreen = false;
        }
        SwingUtilities.invokeLater(() -> showScreen(currentScreen));
    }

    public boolean isFullscreen() { return fullscreen; }

    // ── Screen switching ──────────────────────────────────────────────────
    public void showScreen(String name) {
        currentScreen = name;
        cardLayout.show(mainPanel, name);
        switch (name) {
            case MENU:        menuPanel.onShow();        break;
            case GAME:        gamePanel.onShow();        break;
            case NIGHTMARE:   nightmarePanel.onShow();   break;
            case MOM_ARRIVAL: momArrivalPanel.onShow();  break;
            case SHOP:        shopPanel.onShow();        break;
            case GAMEOVER:    gameOverPanel.onShow();    break;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────
    public GamePanel      getGamePanel()      { return gamePanel; }
    public NightmarePanel getNightmarePanel() { return nightmarePanel; }
    public ShopPanel      getShopPanel()      { return shopPanel; }
}
