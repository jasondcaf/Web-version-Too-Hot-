# Browser port parity audit

Source baseline: Java sources at repository commit `0f98a6f` (uploaded project), with background collection artwork added in commit `0390a86` and the original runtime images subsequently supplied in `rooms.zip`. The original desktop sources and Windows runner are preserved. The Java handlers, rather than older descriptions in the root README, are the behavior reference.

The user subsequently supplied the current desktop files. All attached Java source files matched this baseline byte for byte, and disassembly of all 49 uploaded class files matched the corresponding compiled source. There is no evidence that the port used older Java logic. The earlier visual mismatch came from layout changes introduced by the browser port and from artwork that was not available in the browser checkout. The layout restoration described below addresses those port changes; it does not imply an update to the desktop rules.

This is a behavior checklist, not a claim that every path has been manually tested. See `README.md` in this folder and test output for validation actually performed.

## Component mapping

| Java component | Browser responsibility | Source behavior to preserve |
| --- | --- | --- |
| `Main.java`, `GameFrame.java` | Browser entry point and screen controller | Menu, game, nightmare, mom arrival, between-day shop, game-over; new game resets upgrades and inventory; fullscreen optional. |
| `Screens.java` | Menu and win/loss views | TOO HOT / “Don't let her find out.”; survive three days; replay and return to menu; final day and reason displayed. |
| `GameState.java` | `src/engine.js` | Single state, inventory/world flags, upgrades, daily reset; 6 starting coins; daily duration counters 300/270/240; temperature 25, hunger/thirst/cat 100, stress 0. |
| `GameLoop.java` | `src/engine.js` | One 500 ms simulation tick; stat decay, environment, random events, chores, phone, friend texts, cat chaos, arrival and loss checks. |
| `HUDPanel.java` | `src/main.js`, `src/source-layout.css` | Temperature, hunger, thirst, cat hunger, stress, phone battery, coins/day/countdown; AC/power/windows/storm/cat alerts; stool/wrench/spray; six core chores and bonus chores; restored compact stats-left/status-middle/timer-right arrangement. |
| `GamePanel.java` | Room navigation and status messages | Living room, kitchen, garage, bedroom, backyard, mom's room, bathroom, basement and phone; phone remembers previous room. Ground/upstairs/basement navigation strips are restored. A floor switch changes the navigation strip without moving to a different room. |
| `RoomPanel.java` and room button layouts | `src/room-layout.js`, `src/source-layout.css`, shared room art | Dark navy glass buttons, colored accents, badges and status overlays; source-derived 800 × 455 button geometry shares the room-art coordinate system; outage darkness reduced by flashlight; heat/cold/smoke feedback. |
| `LivingRoomPanel.java` | `src/actions.js` and room UI | Open/close living window (broken slider cooldown disables it); thermostat off/eco/normal/blast; wrench repair consumes wrench and gives 3 coins; shoo squirrels for 1 coin plus stress penalty 10+5×count; TV; vacuum +1; tidy; cushion search 0–5/day; cat-mess cleanup. |
| `KitchenPanel.java` | `src/actions.js` and dishes UI | Stool-gated kitchen drink (+22 or +35 bottle, −3 temperature); stool-gated dish timing game; sweep +1; cat food scoop (+40 cat hunger, consumes one scoop); kitchen window; replace bag only after trash taken out; loose change; delivered food pickup; freezer/ice cream once daily; microwave then heated food +32 hunger; cold food +18 hunger/+5 stress; power outage disables fridge food; tidy. |
| `GaragePanel.java` | `src/actions.js` and roach/fuse UI | Clear 6 roaches in 12s using spray; success consumes spray, gives 2 coins and permanently clears roaches; access wrench after roaches cleared (rats do not block it); mouse trap clears rats (+3 coins, consumes trap); loose change; three fuse memory rounds restore power (+3 coins). |
| `BedroomPanel.java` | `src/actions.js` | Feed cat +50 for 1 scoop when below90 (+2 coins); bedroom window; charge/unplug phone; rest −4 temperature/−10 stress; under-bed search; nap 20 ticks; towel dry then clean clothes; stool pickup; homework progresses merely while in bedroom; tidy. |
| `OutdoorPanel.java` | `src/actions.js` | Take trash out; mail +1; recover escaped cat (+10 hunger); mowing passes +12.5% lawn/+4 temperature; storm/temperature restrictions and streak events; sprinkler once daily −14 temperature/+10 thirst; yard search; water plants +1. |
| `MomsRoomPanel.java` | `src/actions.js` | Dresser supplies spray (not living-room closet as old README says); flashlight during outage; rest −18 stress/−3 temperature; closet hiding reduces stress; nightstand search. |
| `BathroomPanel.java` | `src/actions.js` | Shower once −22 temperature/−10 stress/+6 thirst, then bedroom towel/change chain; unlimited sink drink +18/+30 bottle without stool; cabinet once −28 stress; shelf search; mop +1. |
| `BasementPanel.java` | `src/actions.js` | Laundry: idle→washing→washed→drying→dry→folded; wash consumes one soap; wash/dry each 10 ticks or7 with soap upgrade; delivered detergent +2; dirty clothes permit another load. |
| `PhonePanel.java` | `src/phone-view.js`, `src/actions.js`, Flappy game | Separate Mom/friend contact and conversation pages, pending text reply/ignore, delivery dropdown and order button with five choices, music genres, phone battery and charging, Flappy Bird. No network account, API, or real purchases. |
| `NightmarePanel.java` | Nightmare memory game | Three circuit rounds at ≥90 temperature; wrong input/time limit is fatal; escape gives +5 coins, temperature70/stress80, resume in living room. |
| `MomArrivalPanel.java` | `src/interlude-art.js` and arrival screen | Stop game clock; show the Java-derived illustrated house/car/mom scene, day assessment and allowance; continue to shop or final ending. |
| `ShopPanel.java` | `src/engine.js`, `src/interlude-art.js` and shop UI | Restored Java-derived television framing, with eight visible items selected by upcoming day, persistent upgrade flags, no repeat purchases, affordability, start next day resets daily state. |
| `AssetManager.java` | `scripts/asset-files.mjs`, asset manifest and provenance | Canonical room backgrounds/sprites before collection aliases; Windows-compatible case-insensitive lookup; selected source paths in `assets/sources.json`; no preview collection or emulation. Missing art is reported. |
| `GenerateCloset.java`, `GenerateCloset2.java` | Desktop asset generator utilities | Preserved tools, not gameplay screens. Browser uses the supplied closet image when present. |

## Exact minigame rules

- **Dishes:** needle bounces between0 and1 on 28ms frames, initially .025/frame. Scrub succeeds in inclusive green interval [.35,.65]. Four hits completes dishes. Each hit raises speed by .007 (maximum .09). Misses exceeding hits reset hits, misses and speed. Give up exits; next attempt resets progress.
- **Roaches:** six stationary, randomly positioned 44×44 logical targets, twelve seconds. Failure permits retry without consuming spray. Success grants wrench access, consumes spray, +2 coins, and sets `roachesClearedEver`; rats can appear from day2.
- **Garage fuse:** three rounds with5/6/7 nodes and randomly sampled sequences of3/4/5 nodes (repeats allowed). Demonstration: initial700ms delay,500ms lit/350ms dark. Twenty seconds for each input round. Wrong node or timeout repeats the same sequence; cancel leaves power out. Completing all rounds restores power and grants3 coins.
- **Nightmare circuit:** three rounds of4/5/6 nodes. Each sequence is a shuffled permutation of every node. Initial900ms delay,700ms per node,600ms pause;20 seconds per input round. Wrong node or timeout loses the game. Unlike the garage, no retry/cancel.
- **Mowing:** eight passes at12.5%/pass, +4 temperature/pass; cannot start at≥82, forced stop at≥85; cannot start in thunderstorm. After5 consecutive passes,35% event chance: squirrel, +20 stress, or−15 thirst. Event resets streak.
- **Flappy Bird:** 33ms physics frames; gravity .42, flap velocity−7.2, bird x48/radius9; pipe width28/gap76/speed2, spawn every90 frames at remainder10. Flap uses .5 phone battery. Passing a pipe increments local score; death restarts only this minigame. No coin reward in Java.

## State and scoring details

- The timer's variable name says seconds, but Java decrements it **once every500ms**. Therefore day1 lasts150 actual seconds, day2 lasts135, day3 lasts120. Browser pacing follows executable Java behavior.
- Six core chores: dishes; trash **and replacement bag**; all three tidy rooms; lawn; homework; folded laundry. Bonus vacuum/plants/mail/sweep/mop grant1 coin immediately and are not counted among six.
- Daily bonus:5 survival, +3 if temperature<40, +2 if hunger>60, +2 if thirst>60, +1 if no squirrel, +4 per core chore. Allowance:2 per completed chore, or8+2×day if all six complete.
- Arrival wins that tick before stress/nightmare/starvation checks. Otherwise loss precedence is stress≥100, nightmare temperature≥90, temperature100, hunger0, thirst0. Nightmare trigger precedes temperature death, so ordinary overheating enters the nightmare.
- Upgrade multipliers after day reset: fan .7 temperature base rise, snack .75 hunger fall, treats .7 cat hunger fall, chill pills .7 stress rise. Bottle raises drink amounts; stool is available from day start; soap speeds laundry and supplies4 uses; lamp homework12 rather than20 ticks; squirrel spray halves squirrel stress.
- Daily reset keeps coins, purchases, AC setting and permanent roach-clearing history; resets daily inventory/chores/supplies/events. AC starts broken day2+, power out day3+.

## Original source quirks and incomplete mechanics

These are documented to avoid treating old descriptions or unused fields as completed gameplay:

1. `hasGenerator` does not actually reduce outage probability or automatically restore power. Java only uses it to alter a value subsequently applied to an AC-break cooldown.
2. `hasMedicineKit` can be purchased but has no cure action in the uploaded Java. Cold medicine delivery is the implemented cure path.
3. `smokeAlarmActive` starts false and no uploaded handler sets it true; no fanning handler exists. The HUD/loop include dormant smoke-alarm visuals/stress logic.
4. `momPriorityChores` is randomized/displayed but has no effect on scoring or endings.
5. Ice-pack purchase immediately drops temperature15 in the shop; the next daily reset restores temperature25. The flag persists but there is no use-item handler.
6. Phone charging and washer/dryer progress do not check power outage. Music names affect the displayed genre and stress calculation; the Java source does not contain audio playback.
7. Java's direct actions (rest, drinks, etc.) generally have no cooldown; repeating them is allowed.
8. Food pickup clears `deliveryArrived` but leaves `deliveryOrdered`, so Java reasserts arrival next tick. The generic kitchen pickup also does not distinguish food from drinks.
9. Java's delivery UI disables every order if any delivery/medicine/detergent is ordered or a speaker is owned; buying the speaker therefore disables later ordering. The browser preserves this original order gate.

## Artwork availability

The original runtime artwork is now available. The user's `rooms.zip` supplied 26 image files, successfully extracted under `assets/rooms/` and preserved byte for byte. All nine room/menu backgrounds and every sprite/overlay requested by the room renderers are included in the browser build. The build selects 18 runtime images; backup/alternate images and the unused `shop.jpg` remain in the repository without being bundled.

The restored pipeline resolves filenames and the `assets/rooms` directory case-insensitively, matching normal Windows filename behavior. For each runtime name, it prefers:

1. The canonical filename under `<repository>/assets/rooms/`.
2. The canonical filename uploaded directly to the repository root.
3. A supplied root-level house-collection alias, where one is defined.

Within each location, the extension order is `.jpg`, `.png`, `.jpeg`. Actual runtime art takes precedence over the collection aliases, and the current build resolves all 18 images from `assets/rooms/`. Generated `assets/manifest.json` names the served files, and `assets/sources.json` records the repository-relative source selected for every image. The pipeline does not recursively bundle preview downloads or nested source-art collections.

| Browser/Java asset name | Currently selected supplied file |
| --- | --- |
| `menu` | `assets/rooms/menu.jpg` (entrance) |
| `living_room` | `assets/rooms/living_room.jpg` |
| `kitchen` | `assets/rooms/kitchen.jpg` |
| `garage` | `assets/rooms/garage.jpg` |
| `bedroom` | `assets/rooms/bedroom.jpg` |
| `outdoor` | `assets/rooms/outdoor.jpg` |
| `moms_room` | `assets/rooms/moms_room.jpg` |
| `bathroom` | `assets/rooms/bathroom.jpg` |
| `laundry_room` | `assets/rooms/laundry_room.jpg` |

Supplied sprites/overlays are `cat.png`, `cat_bowl.png`, `squirrel.png`, `closet_interior.png`, `roach_ai.png`, `rat.png.png`, `sprinkler_on.png`, `sprinkler_off.png` and fallback `sprinkler.png`. The 493-byte rat file is a valid 48 × 36 indexed PNG, not a failed download. The browser build accommodates its duplicated extension by copying it unchanged to `assets/rat.png`, with provenance retaining `assets/rooms/rat.png.png`. Java's exact loader would miss that doubled filename; the browser accommodation is a rendering difference with no game-rule change.

The menu now uses the uploaded canonical `menu.jpg` (the entrance), replacing the earlier outside2 collection selection. Garage, laundry, closet, animal and sprinkler images now replace the first port's fallback shapes. The Java TV shop continues to use the bedroom background; the archive's `shop.jpg` is not referenced by that source screen.

Java itself searches both `assets/rooms/` and `../assets/rooms/`, relative to the desktop process working directory. The browser pipeline reads the repository's asset folder and canonical root files; it does not search outside the repository. All 26 supplied originals remain unchanged, including unused files and the doubled rat extension.

Background credits: **Spiral Atlas Visual Novel House Backgrounds © 2023 by Spiral Atlas**, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), with [author/model acknowledgements](https://spiralatlas.github.io/credits/). SHA-256 comparisons identify seven canonical backgrounds as unchanged collection files: menu/entrance, living room/lounge, bedroom/single bedroom, Mom's room/master bedroom, kitchen, bathroom and outdoor/outside1. Original `Credits and Information.txt` and `license.txt` are retained in the build. This attribution covers those matched collection backgrounds.

Embedded XMP identifies the cat, squirrel and sprinkler images' Vecteezy creator IDs, source URLs and licensing page; [public/credits.html](public/credits.html) retains those exact credits. The supplied ZIP contains no separate credits/license documents, and no author/license metadata was found for garage, laundry, bowl, closet, roach or rat. Historical `download_assets.ps1` comments describe possible CC0/CC-BY downloads, but they do not establish the provenance of the uploaded replacements. The closet generator utilities likewise do not prove how the current PNG was produced. No license for unrelated images is inferred from the Spiral Atlas notice. Images, embedded watermarks and source metadata are preserved unchanged; the browser maps filenames and scales at display time.

## Restored presentation and remaining browser differences

- **Room controls:** `src/room-layout.js` transcribes the Java room buttons' 800 × 455 geometry, including the living-room thermostat row, bedroom cat/bed/desk columns, kitchen sink/cat-food/fridge columns, priority slots and state-dependent laundry widths. Buttons and room art share this logical coordinate system. Source-hidden controls remain hidden; disabled information controls remain where Java displays them.
- **Floor navigation:** the ground, upstairs and basement strips replace the first port's flat room list. Selecting Upstairs, Downstairs, Basement or Ground Floor changes the visible navigation strip without implicitly changing the current room, as in `GamePanel.java`.
- **HUD:** the restored compact layout places stacked survival stats on the left, environment and inventory status in the middle, and room/day/countdown/coins on the right. Core and bonus chore strips remain below it. The browser retains its help, pause and optional fullscreen controls.
- **Phone:** the source hierarchy is restored: app home, Messages contact list, separate Mom/friend conversations, delivery selector and order button, music, and Flappy Bird. Back/Home controls navigate the phone pages; underlying rules remain in `actions.js`.
- **Shop and arrival:** the shop again uses a Java-derived TV body, screen and stand over the bedroom. Mom's arrival uses a Java-derived illustrated house, car and walking-mom scene. These are browser translations, not claims of identical rasterization, fonts or every animation frame.
- **Touch and narrow screens:** source-style buttons retain colored accents, borders and logical placement on landscape/tablet/desktop layouts. A taller stage and minimum 44 CSS-pixel action targets support touch. At widths of 600 CSS pixels or less, room actions use a flowing grid to keep controls readable and reachable; that narrow-screen arrangement intentionally differs from Swing.
- **Nightmare:** circuit nodes and wires use the Java logical positions. Flashing states, three memory rounds, shuffled paths, timeout and fatal mistake rules are preserved. The demon-cat decoration and surrounding illustration remain minimal compared with Java.
- **Roaches:** targets are enlarged and distributed to avoid overlapping touch targets. There are still six stationary targets and a twelve-second limit.
- **Browser behavior:** pause and hidden-tab handling are usability enhancements. No save system is added. Music genres retain the Java gameplay effects; the Java source contains no audio playback to port.

The source layout and supplied runtime artwork have been restored. Full pixel-level parity is **not** claimed: browser font metrics and responsive scaling differ, touch targets are enlarged where necessary, and the nightmare illustration remains simplified. These presentation differences do not imply older Java logic.

## Verification boundaries

Critical state transitions and action prerequisites are suitable for deterministic unit tests. Browser automation must additionally exercise pointer interactions, focus, responsive layouts, menu→play→arrival→shop→next day, nightmare success/failure, and iPad-sized viewports. A passing build alone does not establish Safari compatibility or complete gameplay parity. Hardware iPad/Safari testing remains distinct from simulated viewport or WebKit checks.

## Recorded validation

- `npm test`: **56 passed, 0 failed**. This comprises the original 42 rule/action/physics/timer tests, eight source-room-layout tests, three asset-pipeline tests and three phone navigation/contact/delivery tests. Layout tests cover source coordinate landmarks, shared priority slots, state-dependent dimensions, bounds, non-overlap and reachable action geometry. Asset tests cover canonical precedence, case-insensitive Windows filenames, provenance and avoiding unintended preview collection copying.
- `npm run build`: passed with **18 runtime images**. All 26 source images match their uploaded archive bytes. Asset/browser checks verify source provenance, decoded images and actual garage/laundry/sprite rendering.
- `npm run test:e2e`: **18 Chromium cases passed**: nine desktop and nine iPad-sized touch cases, with no JavaScript page errors. These exercise room/floor navigation, actual item-chain and roach/dish/fuse interactions, nightmare success/failure, arrival/shop/next-day/final-win transitions, phone conversations/delivery/music/Flappy input, source-positioned controls, touch targets and integrated art.
- These are current local-build results. They do not establish that an older hosted deployment or previously downloaded archive has been replaced.
- WebKit iPad-touch project: **9 passed**, with no JavaScript page errors. Together with the 18 Chromium cases, **27 browser scenarios passed**. This runs Safari’s browser engine on Linux at an iPad touch viewport; physical iPad Safari and Windows hardware sessions remain unperformed.
