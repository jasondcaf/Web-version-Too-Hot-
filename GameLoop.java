package game;

import javax.swing.Timer;
import java.util.Random;

/**
 * Runs at 2 ticks per second (500 ms).
 * All stat decay, random events, and win/lose checks live here.
 */
public class GameLoop {

    private static final int    TICK_MS   = 500;
    private static final Random RNG       = new Random();

    private static Timer timer;

    // ── Start / Stop ──────────────────────────────────────────────────────
    public static void start() {
        if (timer != null) timer.stop();
        timer = new Timer(TICK_MS, e -> tick());
        timer.start();
    }

    public static void stop() {
        if (timer != null) timer.stop();
    }

    // ── Main tick ─────────────────────────────────────────────────────────
    private static void tick() {
        if (GameState.gameOver || GameState.inNightmare) return;

        tickMomTimer();
        tickStats();
        tickEnvironment();
        tickRandomEvents();
        tickChores();
        tickPhone();
        tickEvents();
        tickMittens();
        checkWinLose();

        GameFrame.getInstance().getGamePanel().refresh();
    }

    // ── Mom countdown ─────────────────────────────────────────────────────
    private static void tickMomTimer() {
        GameState.momTimerSeconds = Math.max(0, GameState.momTimerSeconds - 1);
    }

    // ── Stat decay ────────────────────────────────────────────────────────
    private static void tickStats() {
        // Cold: raises temp and stress rise rates by 25%
        float coldMult = GameState.hasCold ? 1.25f : 1.0f;

        // Temperature rises; rate affected by AC setting, power, window, stress, day
        float tempRise = 0.25f * GameState.tempRiseRate * coldMult;
        if (GameState.acBroken || GameState.powerOut) {
            tempRise += 0.4f;  // no cooling at all
        } else {
            switch (GameState.acSetting) {
                case 0: tempRise += 0.35f;  break; // AC off — house heats up extra
                case 1: tempRise -= 0.1f;   break; // eco — slight cooling
                case 2: /* normal */         break; // default
                case 3: tempRise -= 0.45f;  break; // blast — aggressive cooling
            }
        }
        if (GameState.windowOpen)       tempRise -= 0.2f;   // living room window
        if (GameState.bedroomWindowOpen) tempRise -= 0.1f;  // bedroom window
        if (GameState.kitchenWindowOpen) tempRise -= 0.1f;  // kitchen window
        if (GameState.stress > 50f)     tempRise += 0.2f;   // stress = body heat
        tempRise += (GameState.day - 1) * 0.1f;             // harder each day
        // Last 2 minutes: heat spike
        if (GameState.momTimerSeconds > 0 && GameState.momTimerSeconds < 120) {
            float progress = 1f - (GameState.momTimerSeconds / 120f);
            tempRise += 0.65f * progress;
        }
        GameState.temperature = clamp(GameState.temperature + tempRise, 0f, 100f);

        // Hunger falls
        float hungerFall = 0.35f * GameState.hungerFallRate;
        GameState.hunger = clamp(GameState.hunger - hungerFall, 0f, 100f);

        // Thirst falls faster than hunger
        float thirstFall = 0.45f * GameState.thirstFallRate;
        GameState.thirst = clamp(GameState.thirst - thirstFall, 0f, 100f);

        // Cat hunger falls
        float catFall = 0.25f * GameState.catHungerFallRate;
        GameState.catHunger = clamp(GameState.catHunger - catFall, 0f, 100f);

        // Music (phone): reduces stress while playing; speaker makes it more effective
        if (GameState.musicPlaying && GameState.phoneBattery > 0f) {
            float musicBoost = GameState.hasSpeaker ? 0.7f : 0.42f;
            GameState.stress       = clamp(GameState.stress - musicBoost, 0f, 100f);
            GameState.phoneBattery = clamp(GameState.phoneBattery - 0.2f, 0f, 100f);
            if (GameState.phoneBattery <= 0f) GameState.musicPlaying = false;
        }

        // TV reduces stress while watching; nap reduces stress + temp
        if (GameState.tvWatching && !GameState.hidingInCloset) {
            GameState.stress = clamp(GameState.stress - 1.5f, 0f, 100f);
        }
        if (GameState.napping) {
            GameState.stress      = clamp(GameState.stress - 2.0f, 0f, 100f);
            GameState.temperature = clamp(GameState.temperature - 0.4f, 0f, 100f);
            if (GameState.napTicksLeft > 0) {
                GameState.napTicksLeft--;
                if (GameState.napTicksLeft == 0) {
                    GameState.napping = false;
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("You woke up from your nap. Feeling better!");
                    GameFrame.getInstance().getGamePanel().goToRoom("BEDROOM");
                }
            }
        }

        // Smoke alarm raises stress massively
        if (GameState.smokeAlarmActive) {
            GameState.stress = clamp(GameState.stress + 3f, 0f, 100f);
        }

        // Thunderstorm: open windows let rain in → stress
        if (GameState.thunderstorm) {
            boolean anyOpen = GameState.windowOpen || GameState.bedroomWindowOpen || GameState.kitchenWindowOpen;
            if (anyOpen) GameState.stress = clamp(GameState.stress + 0.6f, 0f, 100f);
        }

        // Friend text ignore penalty
        if (GameState.friendTextIgnores >= 3 && !GameState.friendTextPending) {
            GameState.stress = clamp(GameState.stress + 0.3f, 0f, 100f);
        }

        // Cold illness: extra stress each tick
        if (GameState.hasCold) {
            GameState.stress = clamp(GameState.stress + 0.5f * coldMult, 0f, 100f);
        }

        // Being outside during thunderstorm → huge stress + cold risk
        if (GameState.thunderstorm && "OUTSIDE".equals(GameState.currentRoom)) {
            GameState.stress = clamp(GameState.stress + 2.5f, 0f, 100f);
            if (!GameState.hasCold && RNG.nextFloat() < 0.04f) {
                GameState.hasCold = true;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("You got caught in the storm! You've come down with a cold...");
            }
        }

        // Still wet from shower → clammy stress + slight chill
        if (GameState.needsTowelDry) {
            GameState.stress      = clamp(GameState.stress + 0.4f, 0f, 100f);
            GameState.temperature = clamp(GameState.temperature - 0.15f, 0f, 100f);
        }
        // Still in dirty clothes → slow stress build
        if (GameState.needsClothesChange) {
            GameState.stress = clamp(GameState.stress + 0.3f, 0f, 100f);
        }

        // Stress: rises from squirrel/rats, decays faster while hiding
        if (GameState.hidingInCloset) {
            GameState.stress = clamp(GameState.stress - 1.2f, 0f, 100f);
        } else if (GameState.squirrelPresent) {
            float squirrelStress = 1.2f * GameState.squirrelCount * GameState.stressRiseRate
                                 * (GameState.hasSquirrelSpray ? 0.5f : 1.0f);
            GameState.stress = clamp(GameState.stress + squirrelStress, 0f, 100f);
        } else if (GameState.ratsPresent) {
            GameState.stress = clamp(GameState.stress + 0.8f * GameState.stressRiseRate, 0f, 100f);
        } else {
            GameState.stress = clamp(GameState.stress - 0.3f, 0f, 100f);
        }

        // Angry cat raises stress only if not hiding
        if (GameState.isCatAngry() && !GameState.hidingInCloset) {
            GameState.stress = clamp(GameState.stress + 0.5f, 0f, 100f);
        }
    }

    // ── Window & squirrel ─────────────────────────────────────────────────
    private static void tickEnvironment() {
        // Window broken cooldown
        if (GameState.windowBrokenCooldown > 0) {
            GameState.windowBrokenCooldown--;
            if (GameState.windowBrokenCooldown == 0) {
                GameState.windowBroken = false;
            }
        }

        // Any open window can attract squirrels
        boolean anyWindowOpen = GameState.windowOpen || GameState.bedroomWindowOpen || GameState.kitchenWindowOpen;
        if (anyWindowOpen) {
            GameState.windowOpenTicks++;
            if (GameState.windowOpenTicks > 8 && !GameState.squirrelPresent) {
                float chance = Math.min(0.10f, 0.015f + GameState.windowOpenTicks * 0.004f);
                if (RNG.nextFloat() < chance) {
                    GameState.squirrelPresent = true;
                    GameState.squirrelCount   = 1 + RNG.nextInt(3); // 1-3 squirrels
                    GameState.squirrelTicks   = 0;
                    // Chance to break window slider
                    if (GameState.windowOpen && RNG.nextFloat() < 0.25f) {
                        GameState.windowBroken           = true;
                        GameState.windowBrokenCooldown   = 24; // 12 seconds
                        GameFrame.getInstance().getGamePanel()
                                .showMessage(GameState.squirrelCount + " squirrel(s) in the house! The window slider broke!");
                    } else {
                        GameFrame.getInstance().getGamePanel()
                                .showMessage(GameState.squirrelCount + " squirrel(s) climbed through a window!");
                    }
                }
            }
        } else {
            GameState.windowOpenTicks = 0;
        }

        if (GameState.squirrelPresent) {
            GameState.squirrelTicks++;
            if (GameState.squirrelTicks > 30) {
                GameState.squirrelPresent = false;
                GameState.squirrelCount   = 1;
                GameState.squirrelTicks   = 0;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("The squirrels left on their own... eventually.");
            }
        }

        // Cooldown ticking
        if (GameState.acBreakCooldown  > 0) GameState.acBreakCooldown--;
        if (GameState.powerOutCooldown > 0) GameState.powerOutCooldown--;
    }

    // ── Random events ─────────────────────────────────────────────────────
    private static void tickRandomEvents() {
        float diffMult = 1f + (GameState.day - 1) * 0.3f; // harder on later days

        // AC breaks
        if (!GameState.acBroken && GameState.acBreakCooldown == 0) {
            if (RNG.nextFloat() < 0.008f * diffMult) {
                GameState.acBroken        = true;
                GameState.acBreakCooldown = 80;
                // Wrench reappears in garage each time AC breaks
                GameState.wrenchInGarage  = true;
                GameState.hasWrench       = false;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("The AC just stopped working! A wrench appeared in the garage.");
            }
        }

        // Power outage — blast AC and repeated phone charging raise the risk; storm triples it
        float powerOutChance = 0.004f * diffMult;
        if (GameState.acSetting == 3 && !GameState.acBroken) powerOutChance *= 3.0f;
        if (GameState.phoneCharging && GameState.phoneChargeCount >= 3) powerOutChance *= 2.0f;
        if (GameState.thunderstorm) powerOutChance *= 3.0f; // storms = much higher power-out risk
        if (!GameState.powerOut && GameState.powerOutCooldown == 0) {
            if (RNG.nextFloat() < powerOutChance) {
                GameState.powerOut        = true;
                GameState.powerOutCooldown = 160;
                int duration = GameState.hasGenerator ? 10 : 30;
                GameState.acBreakCooldown = Math.max(GameState.acBreakCooldown, duration);
                GameFrame.getInstance().getGamePanel()
                        .showMessage("POWER OUT! Head to the garage fuse box.");
            }
        }

        // Rats appear day 2+ after roaches were ever cleared, if no trap bought
        if (!GameState.ratsPresent && !GameState.ratsClearedToday
                && GameState.roachesClearedEver && GameState.day >= 2 && !GameState.hasMouseTrap) {
            if (RNG.nextFloat() < 0.005f * diffMult) {
                GameState.ratsPresent = true;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Rats! They moved in after the roaches left. Get a mouse trap!");
            }
        }

        // Thunderstorm event
        if (!GameState.thunderstorm) {
            if (RNG.nextFloat() < 0.003f * diffMult) {
                GameState.thunderstorm      = true;
                GameState.thunderstormTicks = 80 + RNG.nextInt(80); // 40-80s
                // Rain automatically waters outdoor plants
                boolean plantsJustWatered = !GameState.choreWaterPlants;
                GameState.choreWaterPlants = true;
                String plantMsg = plantsJustWatered ? " Rain watered the outdoor plants! +1 coin." : "";
                if (plantsJustWatered) GameState.coins++;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Thunderstorm!" + plantMsg + " Power may go out!");
            }
        } else {
            GameState.thunderstormTicks--;
            if (GameState.thunderstormTicks <= 0) {
                GameState.thunderstorm = false;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("The storm passed. You can go outside again.");
            }
        }

        // If power out too long, schedule auto-restore
        // (handled: player goes to garage to fix it)
    }

    // ── Win / Lose checks ─────────────────────────────────────────────────
    private static void checkWinLose() {
        // Win: mom timer hits zero
        if (GameState.momTimerSeconds <= 0) {
            stop();
            GameState.won = true;
            GameState.coins += calcDayBonus();

            // Allowance: only paid if ALL chores done; otherwise 2 coins per completed chore
            int done = GameState.choreCount();
            if (done >= GameState.TOTAL_CHORES) {
                GameState.pendingAllowance = 8 + GameState.day * 2; // day1=10, day2=12, day3=14
            } else {
                GameState.pendingAllowance = done * 2; // 2 coins per completed chore (partial)
            }

            if (GameState.day < GameState.MAX_DAYS) {
                GameState.lastDayDone = false;
                GameState.day++;
            } else {
                GameState.lastDayDone = true;
            }
            GameFrame.getInstance().showScreen(GameFrame.MOM_ARRIVAL);
            return;
        }

        // Stress maxed out
        if (GameState.stress >= 100f) {
            endGame("You stressed out completely before mom got home.");
            return;
        }

        // Nightmare trigger
        if (GameState.isOverheating() && !GameState.inNightmare) {
            stop();
            GameState.inNightmare = true;
            GameFrame.getInstance().showScreen(GameFrame.NIGHTMARE);
            return;
        }

        // Death conditions
        if (GameState.temperature >= 100f) {
            endGame("You overheated. The heat got you.");
        } else if (GameState.hunger <= 0f) {
            endGame("You starved before mom got home.");
        } else if (GameState.thirst <= 0f) {
            endGame("You were too dehydrated to go on.");
        }
    }

    private static void endGame(String reason) {
        stop();
        GameState.gameOver       = true;
        GameState.won            = false;
        GameState.gameOverReason = reason;
        GameFrame.getInstance().showScreen(GameFrame.GAMEOVER);
    }

    // ── Called after nightmare escape ─────────────────────────────────────
    public static void resumeAfterNightmare() {
        GameState.inNightmare = false;
        GameState.temperature = 70f;   // brought back down to survivable
        GameState.stress      = 80f;   // still shaken
        GameState.coins      += 5;     // reward for escaping
        start();
        GameFrame.getInstance().showScreen(GameFrame.GAME);
        GameFrame.getInstance().getGamePanel()
                .showMessage("You escaped the nightmare... for now. +5 coins!");
    }

    // ── Chore ticks (called each game tick) ──────────────────────────────
    private static void tickChores() {
        // Homework
        if ("BEDROOM".equals(GameState.currentRoom) && !GameState.choreHomework) {
            GameState.choreHomeworkTicks++;
            int homeworkNeeded = GameState.hasStudyLamp ? 12 : 20;
            if (GameState.choreHomeworkTicks >= homeworkNeeded) {
                GameState.choreHomework = true;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Homework done!");
                GameFrame.getInstance().getGamePanel().goToRoom("BEDROOM");
            }
        }
        // House-clean aggregate check
        if (GameState.choreCleanLR && GameState.choreCleanKitchen && GameState.choreCleanBedroom) {
            GameState.choreHouseClean = true;
        }
        // Laundry washer timer (state 1 = washing)
        int laundryTicks = GameState.hasLaundrysoap ? 7 : 10;
        if (GameState.laundryState == 1) {
            GameState.laundryWashTicks++;
            if (GameState.laundryWashTicks >= laundryTicks) {
                GameState.laundryState = 2; // wash done
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Laundry is done washing! Head to the basement to move it to the dryer.");
                if ("BASEMENT".equals(GameState.currentRoom))
                    GameFrame.getInstance().getGamePanel().goToRoom("BASEMENT");
            }
        }
        // Laundry dryer timer (state 3 = drying)
        if (GameState.laundryState == 3) {
            GameState.laundryDryTicks++;
            if (GameState.laundryDryTicks >= laundryTicks) {
                GameState.laundryState = 4; // dry done
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Laundry is dry! Head to the basement to fold and put it away.");
                if ("BASEMENT".equals(GameState.currentRoom))
                    GameFrame.getInstance().getGamePanel().goToRoom("BASEMENT");
            }
        }
    }

    // ── Phone battery & delivery ──────────────────────────────────────────
    private static void tickPhone() {
        // Battery drains 1 per tick (0.5/sec). TV/games drain faster.
        float drain = 1.0f;
        if (GameState.tvWatching) drain = 1.4f;
        GameState.phoneBattery = clamp(GameState.phoneBattery - drain, 0f, 100f);

        // Charging
        if (GameState.phoneCharging && GameState.phoneBattery < 100f) {
            GameState.phoneBattery = clamp(GameState.phoneBattery + 4f, 0f, 100f);
            GameState.phoneChargeTicks++;
            if (GameState.phoneBattery >= 100f) {
                GameState.phoneCharging    = false;
                GameState.phoneChargeTicks = 0;
                GameState.phoneChargeCount++;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Phone fully charged! (" + GameState.phoneChargeCount + " charges today)");
            }
        }

        // Delivery countdown (food or drinks)
        if (GameState.deliveryOrdered && !GameState.deliveryArrived) {
            GameState.deliveryTicks--;
            if (GameState.deliveryTicks <= 0) {
                GameState.deliveryArrived = true;
                if ("Drinks".equals(GameState.deliveryItemType)) {
                    GameState.thirst    = clamp(GameState.thirst + 50f, 0f, 100f);
                    GameState.drinkArrived = true;
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Drinks delivered! You grabbed them at the door. Thirst way up!");
                } else {
                    GameFrame.getInstance().getGamePanel()
                            .showMessage("Delivery arrived! Pick up your food in the kitchen.");
                }
            }
        }

        // Detergent delivery countdown
        if (GameState.detergentOrdered && !GameState.detergentArrived) {
            GameState.detergentTicks--;
            if (GameState.detergentTicks <= 0) {
                GameState.detergentArrived = true;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Detergent delivered! Pick it up in the basement.");
            }
        }

        // Cold medicine delivery countdown
        if (GameState.coldMedOrdered && !GameState.coldMedArrived) {
            GameState.coldMedTicks--;
            if (GameState.coldMedTicks <= 0) {
                GameState.coldMedArrived = true;
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Cold medicine arrived! Open your phone to take it.");
            }
        }
    }

    // ── Friend texts & smoke alarm ────────────────────────────────────────
    private static void tickEvents() {
        // Friend text countdown
        if (!GameState.friendTextPending) {
            GameState.nextFriendTextIn--;
            if (GameState.nextFriendTextIn <= 0) {
                GameState.friendTextPending = true;
                GameState.nextFriendTextIn  = 80 + RNG.nextInt(60);
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Friend texted you! Check your phone.");
            }
        }

        // Smoke alarm auto-clears after 60 ticks if player ignores it
        if (GameState.smokeAlarmActive) {
            // Each tick without fanning it hurts stress (handled in tickStats)
            // After 60s of alarm, auto-silences with big stress dump
        }
    }

    // ── Mittens chaos & escape ────────────────────────────────────────────
    private static void tickMittens() {
        // Escape: if cat starving and any window open
        if (!GameState.mittensEscaped && GameState.catHunger <= 5f) {
            boolean anyOpen = GameState.windowOpen || GameState.bedroomWindowOpen || GameState.kitchenWindowOpen;
            if (anyOpen && RNG.nextFloat() < 0.08f) {
                GameState.mittensEscaped   = true;
                GameState.catHunger        = 5f;
                GameState.windowOpen       = false;  // auto-close whichever
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Mittens escaped through an open window! Find her outside!");
            }
        }

        // Chaos: if angry, chance to knock things over
        if (GameState.isCatAngry() && !GameState.mittensEscaped) {
            GameState.mittensChaosTimer--;
            if (GameState.mittensChaosTimer <= 0) {
                GameState.mittensChaosTimer = 40 + RNG.nextInt(40);
                // Pick a room to mess up
                String[] rooms = {"LIVING_ROOM", "KITCHEN", "BEDROOM"};
                String target = rooms[RNG.nextInt(rooms.length)];
                GameState.mittensMessRoom = target;
                // Undo clean chore in that room
                switch (target) {
                    case "LIVING_ROOM": GameState.choreCleanLR      = false; GameState.choreHouseClean = false; break;
                    case "KITCHEN":     GameState.choreCleanKitchen = false; GameState.choreHouseClean = false; break;
                    case "BEDROOM":     GameState.choreCleanBedroom = false; GameState.choreHouseClean = false; break;
                }
                String roomName = target.replace("_", " ").toLowerCase();
                GameFrame.getInstance().getGamePanel()
                        .showMessage("Mittens knocked things over in the " + roomName + "! Re-tidy it.");
                // Repaint
                if (target.equals(GameState.currentRoom))
                    GameFrame.getInstance().getGamePanel().goToRoom(target);
            }
        }
    }

    // ── Scoring helper ────────────────────────────────────────────────────
    private static int calcDayBonus() {
        int bonus = 5; // base survival
        if (GameState.temperature < 40f) bonus += 3;
        if (GameState.hunger      > 60f) bonus += 2;
        if (GameState.thirst      > 60f) bonus += 2;
        if (!GameState.squirrelPresent)  bonus += 1;
        // Chore bonus: 4 coins each
        bonus += GameState.choreCount() * 4;
        return bonus;
    }

    // ── Utility ───────────────────────────────────────────────────────────
    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── Coin rewards for player actions ───────────────────────────────────
    public static void rewardAction(int amount, String msg) {
        GameState.coins += amount;
        GameFrame.getInstance().getGamePanel()
                .showMessage(msg + " (+" + amount + " coins)");
    }
}
