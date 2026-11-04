# Mom Comes Home 🌡️🐱

A Java Swing survival game. You're home alone. Mom comes home in 5 minutes.
Keep your temperature, hunger, and thirst under control, feed the cat,
fix the AC, and don't let the squirrels in. Survive 3 days.

---

## How to Compile & Run (VS Code / Terminal)

### Option A — Terminal commands

```bash
# From the MomComesHome/ folder:
mkdir -p out
javac -d out src/game/*.java
java -cp out game.Main
```

### Option B — VS Code Java Extension Pack

1. Install the **Extension Pack for Java** (Microsoft) if you haven't already.
2. Open the `MomComesHome/` folder in VS Code.
3. VS Code will auto-detect the sources. Click **Run** on `Main.java`.

---

## Project Structure

```
MomComesHome/
├── src/
│   └── game/
│       ├── Main.java            — Entry point
│       ├── GameState.java       — All game data (static fields)
│       ├── GameFrame.java       — Main JFrame, CardLayout screen manager
│       ├── GameLoop.java        — javax.swing.Timer tick logic
│       ├── HUDPanel.java        — Stat bars, countdown, coins (custom painted)
│       ├── GamePanel.java       — Room navigation + HUD layout
│       ├── RoomPanel.java       — Abstract base class for rooms
│       ├── LivingRoomPanel.java — Window, AC, squirrel, closet
│       ├── KitchenPanel.java    — Sink (needs step stool), fridge
│       ├── GaragePanel.java     — Fuse box, roach minigame, wrench
│       ├── BedroomPanel.java    — Cat feeding, step stool, rest
│       ├── NightmarePanel.java  — Simon Says fire demon cat escape
│       ├── ShopPanel.java       — Between-day upgrade store
│       └── Screens.java        — MenuPanel + GameOverPanel
└── README.md
```

---

## Gameplay

| Stat       | Gets worse when...                            | How to fix               |
|------------|-----------------------------------------------|--------------------------|
| Temperature | AC broken, power out, window closed, stressed | Fix AC (wrench), open window (briefly), rest |
| Hunger     | Time passing                                  | Eat from fridge (if power on) |
| Thirst     | Time passing (faster than hunger)             | Drink from sink (need step stool) |
| Cat Hunger | Time passing                                  | Feed Mittens (bedroom) |
| Stress     | Squirrel present, cat angry                   | Shoo squirrel, feed cat |

### Item Chain
```
Closet (Living Room) → Roach Spray
Bedroom             → Step Stool  → Kitchen Sink (drink water)
Garage: Roach Spray → Clear Roaches → Wrench
Wrench              → Fix AC (Living Room)
Garage              → Fuse Box (fix power outage)
```

### Nightmare Realm
- Triggers at **90°C** temperature
- Simon Says game against the Fire Demon Cat (Mittens)
- Complete **3 rounds** to escape and return to the game
- Fail = Game Over

### Shop (between days)
- Earn coins by surviving and completing actions
- Buy upgrades that persist across days
- Day 2: AC starts broken. Day 3: AC broken + power out from the start.

---

## Java Swing Concepts Demonstrated

- `JFrame`, `CardLayout`, `BorderLayout`, `GridLayout`, `FlowLayout`
- Custom `paintComponent` with `Graphics2D` (all room art, HUD bars, nightmare)
- `javax.swing.Timer` for game loop (2 ticks/sec)
- `JButton`, `ActionListener`, `MouseListener`
- `JLabel`, `JPanel`, `GridBagLayout` (game over screen)
- `JCheckBox`, `JRadioButton`, `JComboBox` patterns (shop state)
- Inner anonymous classes and lambda expressions
- MVC-adjacent separation: GameState (model), panels (view), GameLoop (controller)
