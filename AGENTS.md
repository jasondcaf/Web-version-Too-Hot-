# Too Hot: Cross-platform browser port

## Mission
Port the existing Java Swing game **Too Hot** to a functional HTML5/browser game that runs in Safari on iPadOS and modern desktop browsers, WITHOUT Shadow PC, a Java runtime in the browser, or emulation. Preserve the original Java game. Do not substitute a mockup for working gameplay.

## Source of truth
- Source is in this directory, with `Main.java`, `GameFrame.java`, `GameState.java`, `GameLoop.java`, room panels and other panels.
- `GameFrame.java` defines MENU, GAME, NIGHTMARE, MOM_ARRIVAL, SHOP, GAMEOVER screens; examine `Screens.java` for menu/game-over definitions.
- `GameState.java` is authoritative for stats, progression, inventory, timers, economy, chores, minigames, events, win/loss logic and upgrades. Audit the full file and related panel handlers; do not invent mechanics.
- `assets/rooms/` contains the actual backgrounds and sprites. Keep image attribution/credits intact; don't indiscriminately bundle all `assets/preview` exploratory downloads.
- Existing Java uses Swing/AWT custom rendering, so it cannot simply be served to Safari. Port game rules and screen/UI behavior to JavaScript/TypeScript, using Canvas and/or HTML/CSS.

## Non-negotiable requirements
1. Leave all original `.java` files, `run.bat`, and existing assets unchanged unless needed for an independently justified bug fix. Keep new code inside `web/`.
2. Prioritize functional parity with the Java game: room traversal, HUD, day timer, temperature/hunger/thirst/stress/cat hunger, chores, inventory, objects, shop, nightmare, mom arrival, game-over and other endings. Preserve relevant game values, resource costs, event rules and pacing. Create a parity checklist mapping old Java components to new modules.
3. iPad usability: responsive landscape-first layout, pointer/touch buttons with comfortable hit areas, no hover-only gameplay, scroll-safe overlays, scaling for retina screens, and Safari-compatible audio activated by user gesture if necessary. Desktop mouse/keyboard must also work. Allow fullscreen where supported but do not require it.
4. Deliver a locally launchable and deployable static web project. Prefer Vite + plain TypeScript/JavaScript unless a framework is clearly beneficial. Document `npm install`, `npm run dev`, `npm run build`, and hosting on HTTPS for iPad access.
5. Do NOT claim feature parity until actually tested. Add unit tests for critical state transitions and progression; build project and run tests. If browser automation available, test key flows and iPad viewport sizes. Document any remaining limitations.
6. Preserve existing room art, style, gameplay and screen structure instead of designing an unrelated new game. Optimize asset loading; prefer source `assets/rooms` references, not huge unneeded preview art.
7. No Java compiler/runtime on iPad is needed. No Shadow PC or streaming dependency.

## Execution plan
- Inspect all Java files and README first; document all navigation, actions and win/loss paths.
- Create `web/` setup and asset pipeline; get main menu, first playable room, HUD, navigation and time-based stats working; verify with build/test.
- Continue porting the other rooms, interactions, inventory, events, shop, nightmares and endings in vertical slices, running tests at each step.
- Deliver a README with screenshots or test results if possible, feature-parity checklist, a succinct list of any incomplete mechanics and how to run on iPad (same Wi-Fi local dev URL or deploy HTTPS).

## Coding cautions
- Java source uses `package game;` even though source files sit in the project root. Port based on classes, not folder inference.
- Preserve consistent single-source state and simulation ticks; avoid timers accelerating after screen changes.
- Avoid stretched/cropped hitboxes by mapping input coordinates through the same transformation used for drawing.
- Persist saves only if compatible with game design; do not change original progression rules without identifying it as an enhancement.
- Commit changes in logical steps if git is available.
