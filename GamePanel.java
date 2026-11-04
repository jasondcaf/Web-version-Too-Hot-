package game;

import javax.swing.*;
import java.awt.*;
import javax.swing.Timer;
import java.util.Random;

public class GamePanel extends JPanel {

    private final HUDPanel       hud;
    private final CardLayout     roomLayout;
    private final JPanel         roomContainer;
    private JLabel               statusLabel;

    // Nav rows (ground floor, upstairs, basement)
    private final JPanel groundFloorNav;
    private final JPanel upstairsNav;
    private final JPanel basementNav;
    private final JPanel navWrapper;    // holds all rows, swaps visibility

    // Room panels
    private final LivingRoomPanel livingRoom;
    private final KitchenPanel    kitchen;
    private final GaragePanel     garage;
    private final BedroomPanel    bedroom;
    private final OutdoorPanel    outdoor;
    private final MomsRoomPanel   momsRoom;
    private final BathroomPanel   bathroom;
    private final BasementPanel   basement;
    private final PhonePanel      phone;

    // Status message auto-hide timer
    private Timer statusTimer;

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // ── HUD ───────────────────────────────────────────────────────────
        hud = new HUDPanel();
        add(hud, BorderLayout.NORTH);

        // ── Room area ─────────────────────────────────────────────────────
        roomLayout    = new CardLayout();
        roomContainer = new JPanel(roomLayout);
        roomContainer.setBackground(Color.BLACK);

        livingRoom = new LivingRoomPanel();
        kitchen    = new KitchenPanel();
        garage     = new GaragePanel();
        bedroom    = new BedroomPanel();
        outdoor    = new OutdoorPanel();
        momsRoom   = new MomsRoomPanel();
        bathroom   = new BathroomPanel();
        basement   = new BasementPanel();
        phone      = new PhonePanel();

        roomContainer.add(livingRoom, "LIVING_ROOM");
        roomContainer.add(kitchen,    "KITCHEN");
        roomContainer.add(garage,     "GARAGE");
        roomContainer.add(bedroom,    "BEDROOM");
        roomContainer.add(outdoor,    "OUTSIDE");
        roomContainer.add(momsRoom,   "MOMS_ROOM");
        roomContainer.add(bathroom,   "BATHROOM");
        roomContainer.add(basement,   "BASEMENT");
        roomContainer.add(phone,      "PHONE");

        add(roomContainer, BorderLayout.CENTER);

        // ── Navigation wrapper ────────────────────────────────────────────
        navWrapper = new JPanel(new BorderLayout());
        navWrapper.setBackground(new Color(12, 12, 20));
        navWrapper.setPreferredSize(new Dimension(800, 68));

        // Status message bar
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(255, 220, 120));
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        navWrapper.add(statusLabel, BorderLayout.NORTH);

        // ── Floor nav switcher (CardLayout) ───────────────────────────────
        CardLayout floorLayout = new CardLayout();
        JPanel floorCards = new JPanel(floorLayout);
        floorCards.setOpaque(false);

        groundFloorNav = buildGroundFloorNav();
        upstairsNav    = buildUpstairsNav();
        basementNav    = buildBasementNav();
        floorCards.add(groundFloorNav, "GROUND");
        floorCards.add(upstairsNav,    "UPSTAIRS");
        floorCards.add(basementNav,    "BASEMENT");
        navWrapper.add(floorCards, BorderLayout.CENTER);

        add(navWrapper, BorderLayout.SOUTH);
    }

    // ── Ground floor navigation row ───────────────────────────────────────
    private JPanel buildGroundFloorNav() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(800, 36));

        JButton toLiving   = RoomPanel.makeNavButton("Living Room");
        JButton toKitchen  = RoomPanel.makeNavButton("Kitchen");
        JButton toGarage   = RoomPanel.makeNavButton("Garage");
        JButton toBedroom  = RoomPanel.makeNavButton("Bedroom");
        JButton toOutside  = RoomPanel.makeNavButton("Backyard");
        JButton toBasement = RoomPanel.makeNavButton("⬇ Basement");
        JButton toPhone    = RoomPanel.makeNavButton("Phone");
        JButton upstairs   = RoomPanel.makeNavButton("⬆ Upstairs");

        for (JButton b : new JButton[]{toLiving, toKitchen, toGarage, toBedroom, toOutside, toBasement}) {
            b.setPreferredSize(new Dimension(88, 28));
            p.add(b);
        }
        toPhone .setPreferredSize(new Dimension(68, 28));
        toPhone.setForeground(new Color(100, 200, 255));
        p.add(toPhone);
        upstairs.setPreferredSize(new Dimension(88, 28));
        p.add(upstairs);

        toLiving  .addActionListener(e -> goToRoom("LIVING_ROOM"));
        toKitchen .addActionListener(e -> goToRoom("KITCHEN"));
        toGarage  .addActionListener(e -> goToRoom("GARAGE"));
        toBedroom .addActionListener(e -> goToRoom("BEDROOM"));
        toOutside .addActionListener(e -> goToRoom("OUTSIDE"));
        toBasement.addActionListener(e -> showFloor("BASEMENT"));
        toPhone   .addActionListener(e -> goToRoom("PHONE"));
        upstairs  .addActionListener(e -> showFloor("UPSTAIRS"));

        return p;
    }

    // ── Basement navigation row ───────────────────────────────────────────
    private JPanel buildBasementNav() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(800, 36));

        JButton toLaundry  = RoomPanel.makeNavButton("Laundry Room");
        JButton groundFloor = RoomPanel.makeNavButton("⬆ Ground Floor");

        toLaundry  .setPreferredSize(new Dimension(130, 28));
        groundFloor.setPreferredSize(new Dimension(120, 28));

        p.add(toLaundry); p.add(groundFloor);

        toLaundry  .addActionListener(e -> goToRoom("BASEMENT"));
        groundFloor.addActionListener(e -> showFloor("GROUND"));

        return p;
    }

    // ── Upstairs navigation row ───────────────────────────────────────────
    private JPanel buildUpstairsNav() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(800, 36));

        JButton toMoms     = RoomPanel.makeNavButton("Mom's Room");
        JButton toBath     = RoomPanel.makeNavButton("Bathroom");
        JButton toPhone2   = RoomPanel.makeNavButton("Phone");
        JButton downstairs = RoomPanel.makeNavButton("⬇ Downstairs");

        toMoms    .setPreferredSize(new Dimension(120, 28));
        toBath    .setPreferredSize(new Dimension(100, 28));
        toPhone2  .setPreferredSize(new Dimension(68, 28));
        toPhone2.setForeground(new Color(100, 200, 255));
        downstairs.setPreferredSize(new Dimension(110, 28));

        p.add(toMoms); p.add(toBath); p.add(toPhone2); p.add(downstairs);

        toMoms    .addActionListener(e -> goToRoom("MOMS_ROOM"));
        toBath    .addActionListener(e -> goToRoom("BATHROOM"));
        toPhone2  .addActionListener(e -> goToRoom("PHONE"));
        downstairs.addActionListener(e -> showFloor("GROUND"));

        return p;
    }

    /** floorKey: "GROUND", "UPSTAIRS", or "BASEMENT" */
    private void showFloor(String floorKey) {
        JPanel cards = (JPanel) groundFloorNav.getParent();
        if (cards != null) {
            ((CardLayout) cards.getLayout()).show(cards, floorKey);
        }
    }

    // ── Room navigation ───────────────────────────────────────────────────
    public void goToRoom(String room) {
        // Save the room we came from before navigating to phone
        if ("PHONE".equals(room) && !"PHONE".equals(GameState.currentRoom))
            GameState.roomBeforePhone = GameState.currentRoom;
        GameState.currentRoom = room;
        roomLayout.show(roomContainer, room);
        switch (room) {
            case "LIVING_ROOM": livingRoom.onEnter(); showFloor("GROUND");    break;
            case "KITCHEN":     kitchen.onEnter();    showFloor("GROUND");    break;
            case "GARAGE":      garage.onEnter();     showFloor("GROUND");    break;
            case "BEDROOM":     bedroom.onEnter();    showFloor("GROUND");    break;
            case "OUTSIDE":     outdoor.onEnter();    showFloor("GROUND");    break;
            case "MOMS_ROOM":   momsRoom.onEnter();   showFloor("UPSTAIRS");  break;
            case "BATHROOM":    bathroom.onEnter();   showFloor("UPSTAIRS");  break;
            case "BASEMENT":    basement.onEnter();   showFloor("BASEMENT");  break;
            case "PHONE":
                phone.resetToHome();
                phone.onEnter();
                showFloor("GROUND");
                break;
        }
        refresh();
    }

    // ── Called by GameLoop every tick ─────────────────────────────────────
    public void refresh() {
        hud.refresh();
        repaintCurrentRoom();
        if (GameState.isHeatDanger()) repaint();
    }

    private void repaintCurrentRoom() {
        switch (GameState.currentRoom) {
            case "LIVING_ROOM": livingRoom.repaint(); break;
            case "KITCHEN":     kitchen.repaint();    break;
            case "GARAGE":      garage.repaint();     break;
            case "BEDROOM":     bedroom.repaint();    break;
            case "OUTSIDE":     outdoor.repaint();    break;
            case "MOMS_ROOM":   momsRoom.repaint();   break;
            case "BATHROOM":    bathroom.repaint();   break;
            case "BASEMENT":    basement.repaint();   break;
            case "PHONE":       phone.repaint();      break;
        }
    }

    // ── Status messages ───────────────────────────────────────────────────
    public void showMessage(String msg) {
        statusLabel.setText(msg);
        if (statusTimer != null) statusTimer.stop();
        statusTimer = new Timer(4000, e -> statusLabel.setText(" "));
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    // ── Heat-haze / fuzz overlay — drawn OVER all children ───────────────
    private static final Random FUZZ_RNG = new Random();

    @Override
    public void paint(Graphics g) {
        super.paint(g);   // draws background, border, and all child panels first
        drawOverlays((Graphics2D) g);
    }

    private void drawOverlays(Graphics2D g2) {
        int W = getWidth(), H = getHeight();

        // ── Smoke alarm flash ─────────────────────────────────────────────
        if (GameState.smokeAlarmActive) {
            long t = System.currentTimeMillis();
            if ((t / 400) % 2 == 0) {
                g2.setColor(new Color(255, 80, 0, 55));
                g2.fillRect(0, 0, W, H);
            }
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(new Color(255, 100, 50, 220));
            String alarmTxt = "SMOKE ALARM! Fan it out (" + GameState.smokeAlarmFans + "/5)";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(alarmTxt, (W - fm.stringWidth(alarmTxt)) / 2, H / 2 - 30);
        }

        // ── Cold indicator ────────────────────────────────────────────────
        if (GameState.hasCold) {
            long t = System.currentTimeMillis();
            if ((t / 600) % 2 == 0) {
                g2.setColor(new Color(100, 160, 255, 28));
                g2.fillRect(0, 0, W, H);
            }
        }

        // ── Heat danger overlay ───────────────────────────────────────────
        if (!GameState.isHeatDanger()) return;
        float danger = (GameState.temperature - 75f) / 25f;

        int tintAlpha = (int)(danger * 70);
        g2.setColor(new Color(200, 30, 0, Math.min(tintAlpha, 80)));
        g2.fillRect(0, 0, W, H);

        if (GameState.temperature >= 80f) {
            float fuzz = (GameState.temperature - 80f) / 20f;
            int pixels = (int)(fuzz * 1200);
            for (int i = 0; i < pixels; i++) {
                int px = FUZZ_RNG.nextInt(W);
                int py = FUZZ_RNG.nextInt(H);
                int br = 150 + FUZZ_RNG.nextInt(105);
                int alpha = (int)(100 + fuzz * 140);
                g2.setColor(new Color(br, FUZZ_RNG.nextInt(60), 0, Math.min(alpha, 230)));
                int sz = 1 + FUZZ_RNG.nextInt(3);
                g2.fillRect(px, py, sz, sz);
            }
            if (GameState.temperature >= 90f) {
                float edge = (GameState.temperature - 90f) / 10f;
                int ea = (int)(edge * 120);
                g2.setColor(new Color(0, 0, 0, Math.min(ea, 130)));
                int bw = (int)(W * 0.18f);
                g2.fillRect(0, 0, bw, H);
                g2.fillRect(W - bw, 0, bw, H);
            }
        }
    }

    // ── Called when screen becomes active ────────────────────────────────
    public void onShow() {
        showFloor("GROUND");
        goToRoom("LIVING_ROOM");
        GameLoop.start();
    }
}
