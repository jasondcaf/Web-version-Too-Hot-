# Browser port parity audit

Source baseline: Java sources at repository commit `0f98a6f` (uploaded project), with supplied background artwork added in commit `0390a86`. The original desktop sources and Windows runner are preserved. The Java handlers, rather than older descriptions in the root README, are the behavior reference.

This is a behavior checklist, not a claim that every path has been manually tested. See `README.md` in this folder and test output for validation actually performed.

## Component mapping

| Java component | Browser responsibility | Source behavior to preserve |
| --- | --- | --- |
| `Main.java`, `GameFrame.java` | Browser entry point and screen controller | Menu, game, nightmare, mom arrival, between-day shop, game-over; new game resets upgrades and inventory; fullscreen optional. |
| `Screens.java` | Menu and win/loss views | TOO HOT / “Don't let her find out.”; survive three days; replay and return to menu; final day and reason displayed. |
| `GameState.java` | `src/engine.js` | Single state, inventory/world flags, upgrades, daily reset; 6 starting coins; daily duration counters 300/270/240; temperature 25, hunger/thirst/cat 100, stress 0. |
| `GameLoop.java` | `src/engine.js` | One 500 ms simulation tick; stat decay, environment, random events, chores, phone, friend texts, cat chaos, arrival and loss checks. |
| `HUDPanel.java` | Browser HUD | Temperature, hunger, thirst, cat hunger, stress, phone battery, coins/day/countdown; AC/power/windows/storm/cat alerts; stool/wrench/spray; six core chores and bonus chores. |
| `GamePanel.java` | Room navigation and status messages | Living room, kitchen, garage, bedroom, backyard, mom's room, bathroom, basement and phone; phone remembers previous room. Java uses ground/upstairs/basement navigation strips. |
| `RoomPanel.java` | Shared room styling and responsive controls | Dark navy glass buttons, colored accents, badges and status overlays; outage darkness reduced by flashlight; heat/cold/smoke feedback. |
| `LivingRoomPanel.java` | `src/actions.js` and room UI | Open/close living window (broken slider cooldown disables it); thermostat off/eco/normal/blast; wrench repair consumes wrench and gives 3 coins; shoo squirrels for 1 coin plus stress penalty 10+5×count; TV; vacuum +1; tidy; cushion search 0–5/day; cat-mess cleanup. |
| `KitchenPanel.java` | `src/actions.js` and dishes UI | Stool-gated kitchen drink (+22 or +35 bottle, −3 temperature); stool-gated dish timing game; sweep +1; cat food scoop (+40 cat hunger, consumes one scoop); kitchen window; replace bag only after trash taken out; loose change; delivered food pickup; freezer/ice cream once daily; microwave then heated food +32 hunger; cold food +18 hunger/+5 stress; power outage disables fridge food; tidy. |
| `GaragePanel.java` | `src/actions.js` and roach/fuse UI | Clear 6 roaches in 12s using spray; success consumes spray, gives 2 coins and permanently clears roaches; access wrench after roaches cleared (rats do not block it); mouse trap clears rats (+3 coins, consumes trap); loose change; three fuse memory rounds restore power (+3 coins). |
| `BedroomPanel.java` | `src/actions.js` | Feed cat +50 for 1 scoop when below90 (+2 coins); bedroom window; charge/unplug phone; rest −4 temperature/−10 stress; under-bed search; nap 20 ticks; towel dry then clean clothes; stool pickup; homework progresses merely while in bedroom; tidy. |
| `OutdoorPanel.java` | `src/actions.js` | Take trash out; mail +1; recover escaped cat (+10 hunger); mowing passes +12.5% lawn/+4 temperature; storm/temperature restrictions and streak events; sprinkler once daily −14 temperature/+10 thirst; yard search; water plants +1. |
| `MomsRoomPanel.java` | `src/actions.js` | Dresser supplies spray (not living-room closet as old README says); flashlight during outage; rest −18 stress/−3 temperature; closet hiding reduces stress; nightstand search. |
| `BathroomPanel.java` | `src/actions.js` | Shower once −22 temperature/−10 stress/+6 thirst, then bedroom towel/change chain; unlimited sink drink +18/+30 bottle without stool; cabinet once −28 stress; shelf search; mop +1. |
| `BasementPanel.java` | `src/actions.js` | Laundry: idle→washing→washed→drying→dry→folded; wash consumes one soap; wash/dry each 10 ticks or7 with soap upgrade; delivered detergent +2; dirty clothes permit another load. |
| `PhonePanel.java` | Phone UI, `src/actions.js`, Flappy game | Messages to Mom/friend, pending text reply/ignore, five delivery choices, music genres, phone battery and charging, Flappy Bird. No network account, API, or real purchases. |
| `NightmarePanel.java` | Nightmare memory game | Three circuit rounds at ≥90 temperature; wrong input/time limit is fatal; escape gives +5 coins, temperature70/stress80, resume in living room. |
| `MomArrivalPanel.java` | Arrival screen | Stop game clock; show arriving mom, day assessment and allowance; continue to shop or final ending. |
| `ShopPanel.java` | `src/engine.js` and shop UI | Eight visible items selected by upcoming day, persistent upgrade flags, no repeat purchases, affordability, start next day resets daily state. |
| `AssetManager.java` | Browser asset manifest/loading | Original named room backgrounds/sprites; no preview collection or emulation. Missing art must be reported. |
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
9. Java's delivery UI disables every order if any delivery/medicine/detergent is ordered or a speaker is owned; buying the speaker therefore disables later ordering. Browser deviations, if any, must be explicitly documented.

## Artwork availability

The Java-source upload initially contained no `assets/` directory. Commit `0390a86` subsequently supplied eleven original Spiral Atlas house backgrounds as root-level JPEGs, plus their original credits and license. The browser asset pipeline selects the relevant images rather than bundling unrelated preview art. Files in `assets/rooms/` with exact Java runtime names take precedence when supplied later.

| Browser/Java asset name | Supplied file used |
| --- | --- |
| `living_room` | `lounge.jpg` |
| `bedroom` | `single bedroom.jpg` |
| `moms_room` | `master bedroom.jpg` |
| `kitchen` | `kitchen.jpg` |
| `bathroom` | `bathroom.jpg` |
| `outdoor` | `outside1.jpg` |
| `menu` | `outside2.jpg` |

Still missing: `garage` and `laundry_room` backgrounds, plus original sprites/overlays `cat`, `cat_bowl`, `squirrel`, `closet_interior`, `roach_ai`, `rat`, `sprinkler_on`, `sprinkler_off` (fallback name `sprinkler`). Those locations use explicitly identified Java-style drawn fallbacks. The fallback art preserves functioning controls and mechanics; it does not establish original visual parity. Supply the remaining images under `assets/rooms/` and rebuild to replace fallbacks without changing the game rules.

Background credits: **Spiral Atlas Visual Novel House Backgrounds ©2023 by Spiral Atlas**, licensed under [CC BY4.0](https://creativecommons.org/licenses/by/4.0/). [Full author/model acknowledgements](https://spiralatlas.github.io/credits/). Original `Credits and Information.txt` and `license.txt` are retained and included with the selected browser assets. Browser adaptation maps filenames to room roles and scales images to responsive layouts; JPEG originals are unchanged. The missing sprite downloader mentions a CC0 cat and CC-BY4.0 roach, but no missing sprite is represented as a supplied image.

## Browser presentation and usability differences

- Browser room controls use a responsive action grid and direct room navigation, rather than the Java absolute-positioned controls and separate floor navigation strips. Larger pointer targets support iPad touch without changing action costs or rewards.
- The shop uses readable item cards rather than the Java television illustration. The eight-item day selection and upgrade rules remain the same.
- Mom's arrival uses a simpler CSS house/car/mom animation. The allowance, survival/chore bonus, shop transition and final ending are state-driven; the original illustration and exact animation duration are not reproduced.
- Nightmare circuit nodes and wires use the Java logical positions, scaled to the browser board. Flashing states, three memory rounds, shuffled paths, timeout and fatal mistake rules are preserved. The demon-cat decoration and surrounding illustration are simplified.
- Roach targets are enlarged and distributed to avoid overlapping touch targets. There are still six stationary targets and a twelve-second limit.
- The HUD rearranges survival stats, inventory, six core chores, the five bonus jobs and open-window status into responsive bars, badges and strips rather than reproducing the exact Swing HUD geometry.
- Pause and hidden-tab handling are browser usability enhancements. No save system is added. Music genres retain the Java gameplay effects; the uploaded Java has no audio playback to port.
- Original artwork is reused where supplied. Missing garage/laundry backgrounds and sprites, plus the simplified scenes described above, remain visual limitations; full visual parity is not claimed.

## Verification boundaries

Critical state transitions and action prerequisites are suitable for deterministic unit tests. Browser automation must additionally exercise pointer interactions, focus, responsive layouts, menu→play→arrival→shop→next day, nightmare success/failure, and iPad-sized viewports. A passing build alone does not establish Safari compatibility or complete gameplay parity. Hardware iPad/Safari testing remains distinct from simulated viewport or WebKit checks.

## Recorded validation

- `npm test`: **42 passed, 0 failed, 0 skipped**, independently rerun after the source/art integration. Covers room prerequisites, item consumption, all six chores, food/feeding values, searches/bonus jobs, mowing, phone/order rules, day resets and scoring, three-day survival through real actions, nightmare outcomes, Flappy physics, fuse sequences, and minigame timer visibility/cleanup.
- Browser validation is reported separately in the browser README and final handoff; only completed runs should be counted. Chromium with touch/iPad dimensions is distinct from Safari.
- WebKit installation encountered HTTP403 from the browser-download CDN. **WebKit tests were not run**; actual iPad Safari and Windows hardware browser sessions were not performed in this environment.
