package game;

public class GameState {

    // ── Stats ──────────────────────────────────────────────────────────────
    // Temperature: rises toward 100 (bad). Others fall toward 0 (bad).
    public static float temperature  = 25f;
    public static float hunger       = 100f;
    public static float thirst       = 100f;
    public static float catHunger    = 100f;
    public static float stress       = 0f;

    // ── Timer ──────────────────────────────────────────────────────────────
    public static int momTimerSeconds = 300;   // 5 minutes per day
    public static int day             = 1;
    public static final int MAX_DAYS  = 3;

    // ── Economy ────────────────────────────────────────────────────────────
    public static int coins = 6;

    // ── Environment ────────────────────────────────────────────────────────
    public static boolean acBroken       = false;
    public static boolean powerOut       = false;
    public static int     acSetting      = 2;   // 0=off,1=eco,2=normal,3=blast
    public static boolean windowOpen     = false;
    public static int     windowOpenTicks = 0;
    public static boolean windowBroken   = false;
    public static int     windowBrokenCooldown = 0;
    public static boolean bedroomWindowOpen = false;
    public static boolean kitchenWindowOpen = false;
    public static boolean squirrelPresent = false;
    public static int     squirrelCount   = 1;   // how many squirrels in swarm
    public static int     squirrelTicks   = 0;
    public static boolean roachesPresent      = true;
    public static boolean roachesCleared      = false;
    public static boolean roachesClearedEver  = false; // persists across days
    public static boolean ratsPresent         = false;
    public static boolean ratsClearedToday    = false;

    // ── Inventory ──────────────────────────────────────────────────────────
    public static boolean hasStepStool    = false;
    public static boolean hasWrench       = false;
    public static boolean hasRoachSpray   = false;
    public static int     catFoodScoops   = 3;   // how many feeds left in the bag
    public static boolean hidingInCloset  = false;

    // Item in-world flags (can they still be picked up?)
    public static boolean stepStoolInBedroom  = true;
    public static boolean wrenchInGarage      = true;
    public static boolean roachSprayInDrawer  = true;  // mom's-room dresser drawer

    // ── Upgrade multipliers (persisted across days via shop) ───────────────
    public static float tempRiseRate      = 1.0f;
    public static float hungerFallRate    = 1.0f;
    public static float thirstFallRate    = 1.0f;
    public static float catHungerFallRate = 1.0f;
    public static float stressRiseRate    = 1.0f;

    // ── Illness ────────────────────────────────────────────────────────────
    public static boolean hasCold           = false;
    public static boolean coldMedOrdered    = false;
    public static int     coldMedTicks      = 0;
    public static boolean coldMedArrived    = false;

    // ── Shop upgrades purchased ────────────────────────────────────────────
    public static boolean hasFan            = false;
    public static boolean hasSnackPack      = false;
    public static boolean hasBigBottle      = false;
    public static boolean hasCatTreats      = false;
    public static boolean hasRoachTrap      = false;  // roaches never appear
    public static boolean hasGenerator      = false;
    public static boolean hasKitchenStool   = false;  // step stool always in kitchen
    public static boolean hasChillPills     = false;
    public static boolean hasIcePack        = false;   // one-time big temp drop
    public static boolean hasLaundrysoap   = false;   // laundry takes less time
    public static boolean hasStudyLamp     = false;   // homework faster
    public static boolean hasSquirrelSpray = false;   // squirrel stress halved
    public static boolean hasMouseTrap     = false;   // clears rats
    public static boolean hasMedicineKit  = false;   // always have cold cure available

    // ── Inventory — flashlight ─────────────────────────────────────────────
    public static boolean hasMomFlashlight = false;

    // ── Bathroom single-use ────────────────────────────────────────────────
    public static boolean medicineUsed = false;

    // ── Loose change (one find per room per day) ───────────────────────────
    public static boolean looseChangeFound_LivingRoom = false;
    public static boolean looseChangeFound_Kitchen    = false;
    public static boolean looseChangeFound_Bedroom    = false;
    public static boolean looseChangeFound_Garage     = false;
    public static boolean looseChangeFound_Outside    = false;
    public static boolean looseChangeFound_MomsRoom   = false;
    public static boolean looseChangeFound_Bathroom   = false;
    public static boolean sprinklerUsed               = false;

    // ── Chores ────────────────────────────────────────────────────────────
    public static boolean choreDishes        = false;   // dishes washed
    public static int     dishScrubs         = 0;       // 0-4, mini-game progress
    public static boolean choreTrashOut      = false;   // trash taken outside
    public static boolean choreTrashBagIn    = false;   // new bag put in after
    public static boolean choreCleanLR       = false;   // living room tidied
    public static boolean choreCleanKitchen  = false;   // kitchen tidied
    public static boolean choreCleanBedroom  = false;   // bedroom tidied
    public static boolean choreHouseClean    = false;   // all 3 rooms tidied
    public static float   choreLawnPct       = 0f;      // 0-100
    public static boolean choreLawnMowed     = false;   // lawn >= 100%
    public static boolean choreHomework      = false;   // homework done
    public static int     choreHomeworkTicks = 0;       // ticks at desk (need 20)
    public static int     dishMisses         = 0;       // X marks in dish minigame

    // Laundry chore (basement): 0=idle,1=washing,2=wash done,3=drying,4=dry done,5=folded
    public static int laundryState     = 0;
    public static int laundryWashTicks = 0;
    public static int laundryDryTicks  = 0;

    // Shower chain: shower → dry with towel → change clothes → dirty laundry
    public static boolean showeredToday     = false;
    public static boolean needsTowelDry     = false;
    public static boolean needsClothesChange= false;
    public static boolean dirtyClothesAdded = false;  // shower laundry added this day

    // Microwave: food must be heated before eating for full benefit
    public static boolean foodMicrowaved    = false;  // reset when eaten

    /** Returns number of fully completed chores (0-6). */
    public static int choreCount() {
        int n = 0;
        if (choreDishes)                       n++;
        if (choreTrashOut && choreTrashBagIn)  n++;
        if (choreHouseClean)                   n++;
        if (choreLawnMowed)                    n++;
        if (choreHomework)                     n++;
        if (laundryState >= 5)                 n++;   // laundry folded & put away
        return n;
    }

    public static final int TOTAL_CHORES = 6;

    // ── Allowance / mom arrival ────────────────────────────────────────────
    public static int     pendingAllowance = 0;
    public static boolean lastDayDone      = false;

    // ── Navigation ─────────────────────────────────────────────────────────
    public static String currentRoom = "LIVING_ROOM";

    // ── Game-flow flags ────────────────────────────────────────────────────
    public static boolean gameOver        = false;
    public static boolean won             = false;
    public static String  gameOverReason  = "";
    public static boolean inNightmare     = false;

    // ── Phone ─────────────────────────────────────────────────────────────
    public static float   phoneBattery      = 80f;   // 0-100, drains 1/tick
    public static boolean phoneCharging     = false;
    public static int     phoneChargeTicks  = 0;     // ticks remaining in charge cycle
    public static int     phoneChargeCount  = 0;     // total charges this day
    public static boolean deliveryOrdered   = false;
    public static int     deliveryTicks     = 0;     // countdown to arrival (20)
    public static boolean deliveryArrived   = false;
    public static String  deliveryItemType  = "Food"; // "Food","Drinks","Speaker"
    public static boolean drinkArrived      = false;
    public static boolean hasSpeaker        = false;
    public static boolean musicPlaying      = false;
    public static String  musicGenre        = "";
    public static String  roomBeforePhone   = "LIVING_ROOM";

    // ── Laundry detergent (consumed each wash start) ──────────────────────
    public static int     laundryDetergent   = 2;    // uses remaining today
    public static boolean detergentOrdered   = false;
    public static int     detergentTicks     = 0;
    public static boolean detergentArrived   = false;

    // ── Weather ───────────────────────────────────────────────────────────
    public static boolean thunderstorm      = false;
    public static int     thunderstormTicks = 0;

    // ── Events ────────────────────────────────────────────────────────────
    public static boolean smokeAlarmActive  = false;
    public static int     smokeAlarmFans    = 0;     // clicks needed to clear (need 5)
    public static boolean friendTextPending = false;
    public static int     friendTextIgnores = 0;
    public static int     nextFriendTextIn  = 90;    // ticks countdown

    // ── TV / Nap ──────────────────────────────────────────────────────────
    public static boolean tvWatching        = false;
    public static boolean napping           = false;
    public static int     napTicksLeft      = 0;     // auto-wake countdown

    // ── Mittens chaos ─────────────────────────────────────────────────────
    public static int     mittensChaosTimer = 50;    // ticks until next chaos check
    public static boolean mittensEscaped    = false;
    public static String  mittensMessRoom   = "";    // room with undone mess

    // ── New bonus chores ──────────────────────────────────────────────────
    public static boolean choreVacuum       = false;
    public static boolean choreWaterPlants  = false;
    public static boolean choreMailIn       = false;
    public static boolean choreSweepKitchen = false;
    public static boolean choreMopBathroom  = false;

    // ── Ice cream (hidden in freezer) ────────────────────────────────────
    public static boolean iceCreamFound     = false;
    public static boolean iceCreamEaten     = false;

    // ── Mom's priority note (2 randomly chosen chore names) ──────────────
    public static String[] momPriorityChores = {"Dishes", "Homework"};

    // ── Event cooldowns (in ticks at 2 ticks/sec) ─────────────────────────
    public static int acBreakCooldown  = 0;  // ticks until next possible break
    public static int powerOutCooldown = 0;

    // ── Day-level reset (keeps upgrades & coins) ───────────────────────────
    public static void resetForDay() {
        temperature  = 25f;
        hunger       = 100f;
        thirst       = 100f;
        catHunger    = 100f;
        stress       = 0f;
        momTimerSeconds = Math.max(180, 300 - (day - 1) * 30); // shrinks per day

        acBroken        = (day >= 2);           // already broken on day 2+
        powerOut        = (day >= 3);           // power out on day 3 start
        windowOpen       = false;
        windowOpenTicks  = 0;
        windowBroken     = false;
        windowBrokenCooldown = 0;
        bedroomWindowOpen = false;
        kitchenWindowOpen = false;
        squirrelPresent  = false;
        squirrelCount    = 1;
        squirrelTicks    = 0;
        // If roaches were manually cleared on a prior day, don't bring them back
        roachesPresent   = !hasRoachTrap && !roachesClearedEver;
        roachesCleared   = hasRoachTrap || roachesClearedEver;
        // Rats appear from day 2 if roaches were ever cleared (manually or via trap)
        ratsPresent      = (day >= 2 && roachesClearedEver && !hasMouseTrap);
        ratsClearedToday = false;

        hasStepStool    = hasKitchenStool;
        hasWrench       = false;
        hasRoachSpray   = false;
        catFoodScoops   = 3;
        hidingInCloset  = false;

        stepStoolInBedroom = !hasKitchenStool;
        wrenchInGarage     = true;
        roachSprayInDrawer = true;

        hasMomFlashlight = false;
        medicineUsed     = false;
        showeredToday    = false;
        needsTowelDry    = false;
        needsClothesChange = false;
        dirtyClothesAdded  = false;
        foodMicrowaved   = false;

        phoneBattery     = 80f;
        phoneCharging    = false;
        phoneChargeTicks = 0;
        phoneChargeCount = 0;
        deliveryOrdered  = false;
        deliveryTicks    = 0;
        deliveryArrived  = false;
        deliveryItemType = "Food";
        drinkArrived     = false;
        hasSpeaker       = false;
        musicPlaying     = false;
        musicGenre       = "";

        laundryDetergent = hasLaundrysoap ? 4 : 2;
        detergentOrdered = false;
        detergentTicks   = 0;
        detergentArrived = false;

        thunderstorm      = false;
        thunderstormTicks = 0;

        smokeAlarmActive = false;
        smokeAlarmFans   = 0;
        friendTextPending = false;
        friendTextIgnores = 0;
        nextFriendTextIn  = 80 + (int)(Math.random() * 40);

        hasCold        = false;
        coldMedOrdered = false;
        coldMedTicks   = 0;
        coldMedArrived = false;

        tvWatching   = false;
        napping      = false;
        napTicksLeft = 0;

        mittensChaosTimer = 40 + (int)(Math.random() * 30);
        mittensEscaped    = false;
        mittensMessRoom   = "";

        choreVacuum       = false;
        choreWaterPlants  = false;
        choreMailIn       = false;
        choreSweepKitchen = false;
        choreMopBathroom  = false;

        iceCreamFound = false;
        iceCreamEaten = false;
        dishMisses    = 0;

        // Randomise mom's 2 priority chores
        String[] allChoreNames = {"Dishes","Trash","Clean Rooms","Lawn","Homework","Laundry"};
        java.util.List<String> pool = new java.util.ArrayList<>(java.util.Arrays.asList(allChoreNames));
        java.util.Collections.shuffle(pool);
        momPriorityChores = new String[]{pool.get(0), pool.get(1)};

        looseChangeFound_LivingRoom = false;
        looseChangeFound_Kitchen    = false;
        looseChangeFound_Bedroom    = false;
        looseChangeFound_Garage     = false;
        looseChangeFound_Outside    = false;
        looseChangeFound_MomsRoom   = false;
        looseChangeFound_Bathroom   = false;
        sprinklerUsed               = false;

        choreDishes = false;       dishScrubs = 0;
        choreTrashOut = false;     choreTrashBagIn = false;
        choreCleanLR = false;      choreCleanKitchen = false;
        choreCleanBedroom = false; choreHouseClean = false;
        choreLawnPct = 0f;         choreLawnMowed = false;
        choreHomework = false;     choreHomeworkTicks = 0;
        laundryState = 0;          laundryWashTicks = 0;    laundryDryTicks = 0;

        currentRoom  = "LIVING_ROOM";
        gameOver     = false;
        won          = false;
        gameOverReason = "";
        inNightmare  = false;

        acBreakCooldown  = 60;
        powerOutCooldown = 120;

        // Reapply upgrade multipliers
        tempRiseRate      = hasFan        ? 0.7f : 1.0f;
        hungerFallRate    = hasSnackPack  ? 0.75f : 1.0f;
        thirstFallRate    = 1.0f;          // big bottle helps drink more, not slow fall
        catHungerFallRate = hasCatTreats  ? 0.7f : 1.0f;
        stressRiseRate    = hasChillPills ? 0.7f : 1.0f;
    }

    // ── Full new-game reset ────────────────────────────────────────────────
    public static void newGame() {
        day            = 1;
        coins          = 6;
        lastDayDone    = false;
        pendingAllowance = 0;
        hasFan = hasSnackPack = hasBigBottle = hasCatTreats = false;
        hasRoachTrap = hasGenerator = hasKitchenStool = hasChillPills = false;
        hasIcePack = hasLaundrysoap = hasStudyLamp = hasSquirrelSpray = false;
        hasMouseTrap = false;
        hasMedicineKit = false;
        roachesClearedEver = false;
        acSetting = 2;
        phoneChargeCount = 0;
        resetForDay();
    }

    // ── Convenience helpers ────────────────────────────────────────────────
    public static boolean isOverheating()  { return temperature >= 90f; }
    public static boolean isHeatDanger()   { return temperature >= 75f; }
    public static boolean isStarving()     { return hunger  <= 20f; }
    public static boolean isDehydrated()   { return thirst  <= 20f; }
    public static boolean isCatAngry()     { return catHunger <= 20f; }
}
