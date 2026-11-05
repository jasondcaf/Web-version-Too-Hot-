# Too Hot — browser edition

A playable, static HTML/CSS/JavaScript port of the uploaded Java Swing game. No Java runtime, streaming service, emulator, account, or backend is required. The desktop Java source and runner are unchanged. New application code lives entirely in `web/`.

![Living room using the supplied original background at an iPad landscape viewport](docs/ipad-landscape.png)

Screenshot captured with Chromium touch emulation, not physical Safari. [View the restored garage](docs/garage.png).

## Run on your computer

Install Node.js 22.12+ (Node 24 LTS recommended), then from a terminal in this repository:

```sh
cd web
npm install
npm run dev
```

Open the address Vite prints, normally port **5173**. The server listens on your network so another device can connect. Subsequent reproducible installs can use `npm ci` with the included lockfile.

```sh
npm test          # deterministic rules, actions, progression and minigame tests
npm run build    # refresh original art copies and build dist/
npm run preview  # serve the production build on port 4173
```

The built `dist/` folder is the complete game. Do not open `index.html` directly with `file://`; serve it over HTTP or HTTPS.

## Open on your iPad

1. Run `npm run dev` on your Windows computer.
2. Put the iPad and computer on the **same Wi-Fi network**.
3. On Windows, run `ipconfig` and find the active Wi-Fi adapter’s IPv4 address (for example, `192.168.1.25`). Allow Node.js through Windows Firewall for **private networks** if prompted.
4. In iPad Safari, enter your computer’s IPv4 address with port `5173`, for example `http://192.168.1.25:5173`.
5. Rotate to landscape. Tap room and action buttons; no hover or physical keyboard is required. You can scroll when using a smaller viewport. Fullscreen is optional and browser-dependent.

Use your computer’s LAN address, not `localhost` on the iPad. Guest Wi-Fi isolation, VPNs, or a firewall may block local device access. A computer running inside a cloud workspace is not on your home Wi-Fi; for that case, use an HTTPS deployment below. No cloud-workspace preview URL is provided by this project.

## Publish online

Use GitHub Pages, Netlify, Cloudflare Pages, or another static HTTPS host:

- **Build directory:** `web`
- **Install command:** `npm ci`
- **Build command:** `npm run build`
- **Publish/output directory:** `web/dist` relative to the repository, or `dist` when the host’s base directory is `web`.

For GitHub Pages, build from `web/` in a GitHub Actions workflow, upload `web/dist` as the Pages artifact, and deploy it through the repository’s Pages settings. For Netlify/Cloudflare, connect the repository and use the settings above. The Vite base is relative (`./`), so a repository subpath is supported. After deployment, open the host-provided **HTTPS** address on Safari and Windows browsers. Publishing has not been performed by this coding task.

Assets must be present during the build. Deploy `dist/`, including its `assets/` directory and credits; deploying just `web/index.html` will not work. There are no server routes or environment secrets.

## Controls and gameplay

- Tap/click rooms and actions. Desktop keyboard: **1–9** selects rooms; **P** opens the phone; **Escape** pauses or closes the current overlay. **Tab/Enter** operate buttons.
- Dishes: tap **SCRUB** or press **Space** while the needle is in the green zone. Four successful hits finish the dishes; too many misses reset progress.
- Roaches: tap all six targets in twelve seconds. Fuse box: watch and repeat three memory sequences; wrong input repeats the round. Fuse keys are **1–7**.
- Nightmare: repeat three circuits using taps or **A–F**. A wrong node or time running out is fatal. The path hint remains visible, as in Java.
- Phone: messages, deliveries, music effects, charging and Flappy Bird. Tap its canvas or use focused **Space/Arrow Up** to flap. Music affects stats; the original Java never played audio, so this port is also silent.
- Get the **stool** from the bedroom to reach the kitchen sink. Get **spray** from Mom’s dresser, clear garage roaches, take the **wrench**, then repair the AC.
- Finish six main chores: dishes, trash plus replacement bag, three tidy rooms, lawn, homework, laundry. Bonus jobs earn coins immediately. The Chores button explains each chain.
- Survive until Mom arrives, collect allowance, shop between days, then survive all three days. All original stat failures and nightmare endings are represented.

## Source parity and deliberate browser differences

See [PARITY.md](PARITY.md) for the Java-to-browser mapping, exact values and original quirks. It is an audit, not a blanket parity claim.

The Java timer decrements one displayed second every **500 ms**. This port retains that executable behavior: days last approximately **150, 135 and 120 real seconds**, excluding nightmare/arrival/shop/pause. The original old README’s “five minutes” description is inaccurate.

The interface restores the Java room-specific button positions in their shared 800×455 coordinate system, ground/upstairs/basement navigation, separate Mom/Friend phone conversations, delivery dropdown, TV shop, and illustrated house/car/Mom arrival. The HUD keeps stacked stats on the left, status indicators in the middle, and countdown on the right. Browser usability changes are explicit pause, hidden-tab suspension (including minigames), 44px touch targets, scrolling on short displays, fullscreen fallback, and a flowing layout on small phones. State remains in memory with no autosave, matching Java’s restart behavior. Responsive scaling, browser fonts and simplified nightmare decoration still prevent a pixel-perfect parity claim.

Known Java quirks are retained and described in PARITY: delivery ordering locks after certain purchases, food pickup reappears, generator/medicine-kit/ice-pack upgrades have limited or unused source effects, priority notes do not affect rewards, and homework/laundry can progress during outages. These have not been silently redesigned.

## Artwork and credits

The uploaded `rooms.zip` has been successfully integrated under the repository's `assets/rooms/`. All 26 supplied images are preserved byte for byte. The browser now includes **all nine original room/menu backgrounds and the supplied sprites**, including the garage and laundry room.

`npm run assets` copies 18 runtime images into generated `public/assets/`, then the production build includes them. It first prefers the actual Java runtime files in `../assets/rooms/`, then exact runtime names uploaded at the repository root, then the earlier root-level collection aliases. Lookup tolerates Windows filename casing. The current build selects every image from `assets/rooms/`; `dist/assets/sources.json` records each original path:

| Game image | Supplied source |
| --- | --- |
| Menu | `assets/rooms/menu.jpg` (entrance) |
| Living room | `assets/rooms/living_room.jpg` |
| Kitchen | `assets/rooms/kitchen.jpg` |
| Garage | `assets/rooms/garage.jpg` |
| Bedroom | `assets/rooms/bedroom.jpg` |
| Mom’s room | `assets/rooms/moms_room.jpg` |
| Bathroom | `assets/rooms/bathroom.jpg` |
| Backyard | `assets/rooms/outdoor.jpg` |
| Laundry room | `assets/rooms/laundry_room.jpg` |

Sprites include the cat, food bowl, squirrel, closet interior, roach, rat and sprinkler states. The valid 48×36 rat PNG was supplied as `rat.png.png`; the build exposes it as `assets/rat.png` and records its original filename in `sources.json`. This filename accommodation affects rendering only and leaves the uploaded file unchanged. Backup/alternate images and the unused `shop.jpg` are preserved in the repository without being copied into the game build. Java's TV shop uses the bedroom background.

Seven canonical backgrounds byte-match the supplied **Spiral Atlas Visual Novel House Backgrounds © 2023 by Spiral Atlas** collection: menu/entrance, living room/lounge, kitchen, bedroom/single bedroom, Mom's room/master bedroom, bathroom and backyard/outside1. That collection is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), with [author/model acknowledgements](https://spiralatlas.github.io/credits/). Its original `Credits and Information.txt` and `license.txt` are copied into the build.

Cat, squirrel and sprinkler files contain Vecteezy creator/source metadata; those exact credits and licensing links are listed in [the game's artwork credits](public/credits.html). The ZIP included no separate author/license records for the garage, laundry room, bowl, closet, roach or rat. The Spiral Atlas notice applies to the matched background collection. Images and embedded watermarks are retained unchanged; filenames are mapped for lookup and images scale at display time. Credits are available from the menu and Help. The build excludes exploratory `assets/preview` downloads.

The user’s newly attached Java files match the original uploaded source byte-for-byte, and all 49 subsequently uploaded compiled classes match a recompile in disassembly. An older Java source version was not established. The initial visual mismatch came from missing runtime images and presentation changes made in the first browser port.

## Browser testing and remaining verification

Completed for the integrated art: **56 unit tests passed**, the production build passed with **18 runtime images**, and **27 browser scenarios passed** (nine desktop Chromium, nine iPad-sized touch Chromium and nine iPad-sized touch WebKit cases), with no JavaScript page errors. Browser checks include source-file provenance, byte equality and image decoding, garage/laundry backgrounds and actual sprites, along with gameplay flows. These results do not include a physical Windows/iPad session.

The automated browser suite uses Playwright, real rendered controls, and development-only hooks to set up rare events and advance long countdowns. The production bundle excludes those hooks. Unit tests include a full three-day action-based simulation with all six daily chores, resource management and shop progression; browser tests separately exercise the actual dish needle, six roach targets and observed fuse/node sequences.

```sh
# Install a test browser if your machine does not have a usable system Chromium:
npx playwright install chromium
npm run test:e2e

# Optional actual Safari-engine check (Linux may need --with-deps):
npx playwright install webkit
# macOS/Linux:
TEST_WEBKIT=1 npm run test:e2e
# Windows PowerShell:
$env:TEST_WEBKIT='1'; npm run test:e2e
```

The default suite runs desktop Chromium and **Chromium with an iPad-sized touch viewport**, which is not Safari. `CHROMIUM_PATH` can select a system executable. The optional project runs actual WebKit; `WEBKIT_PATH` can select a custom executable. All nine WebKit scenarios passed in the current environment. This tests Safari’s browser engine on Linux with an iPad-sized touch viewport, not a physical iPad or the Safari app itself. Test landscape/portrait Safari on a real device after hosting; layout/touch simulation alone does not establish that hardware validation.

This project uses modern browser-native ES modules, Pointer Events, Canvas and CSS; no Java or emulation layer is present. No online deployment or physical Windows/iPad test has been claimed.
