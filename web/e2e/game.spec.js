import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

// Development hooks only set up conditions and speed up waiting. Outcomes in
// these tests are earned through rendered controls, including the minigames.
const errors = new WeakMap();
const room = (id) => `[data-room="${id}"]`;
const action = (id) => `[data-action="${id}"]`;
async function press(page, selector, touch = false) {
  // Use the same floor buttons a player uses. A floor change only switches
  // navigation; the room changes when its room button is pressed.
  const requestedRoom = typeof selector === 'string' && selector.match(/^\[data-room="([A-Z_]+)"\]$/)?.[1];
  if (requestedRoom && !await page.locator(selector).isVisible()) {
    const desiredFloor = ['MOMS_ROOM','BATHROOM'].includes(requestedRoom) ? 'UPSTAIRS' : requestedRoom === 'BASEMENT' ? 'BASEMENT' : 'GROUND';
    const ground = page.locator('[data-floor="GROUND"]');
    if (await ground.isVisible()) {
      if (touch) await ground.tap(); else await ground.click();
    }
    if (desiredFloor !== 'GROUND') {
      const floor = page.locator(`[data-floor="${desiredFloor}"]`);
      if (touch) await floor.tap(); else await floor.click();
    }
  }
  const control = typeof selector === 'string' ? page.locator(selector) : selector;
  if (touch) await control.tap(); else await control.click();
}
async function state(page, key) { return page.evaluate(k => window.__tooHot.state[k], key); }
async function setup(page, patch) { await page.evaluate(p => window.__tooHot.set(p), patch); }
async function ticks(page, n = 1) { await page.evaluate(count => window.__tooHot.tick(count), n); }
async function start(page, touch = false) {
  await press(page, '[data-command="new"]', touch);
  await expect(page.locator('#room-title')).toHaveText('Living Room');
}
async function safe(page) {
  await setup(page, {temperature:25,stress:0,hunger:100,thirst:100,catHunger:100,acBreakCooldown:10000,powerOutCooldown:10000});
}

test.beforeEach(async ({ page }) => {
  const pageErrors=[];
  errors.set(page,pageErrors);
  page.on('pageerror', e => pageErrors.push(e.message));
  // A seeded stable value prevents random storms from making UI tests flaky;
  // random-event probabilities are covered by the deterministic unit suite.
  await page.addInitScript(() => { Math.random = () => .5; });
  await page.clock.install({time:new Date('2026-11-04T12:00:00Z')});
  await page.clock.pauseAt(new Date('2026-11-04T13:00:00Z'));
  await page.goto('/');
  await expect(page.getByRole('heading',{name:'TOO HOT',exact:true})).toBeVisible();
});
test.afterEach(async ({ page }) => { expect(errors.get(page)).toEqual([]); });

test('menu, room navigation, touchscreen targets, care, keyboard and pause', async ({page}, info) => {
  const touch=!!info.project.use.hasTouch;
  await press(page,'[data-command="credits"]',touch);
  await expect(page.getByRole('dialog',{name:'Artwork credits'})).toContainText('CC BY 4.0');
  await press(page,'[data-command="close"]',touch);
  await press(page,'[data-command="help"]',touch);
  await expect(page.getByRole('dialog',{name:'How to play'})).toBeVisible();
  await press(page,'[data-command="close"]',touch);
  await start(page,touch);
  await expect(page.locator('#timer')).toHaveText('05:00');
  await expect(page.locator('#room-art img')).toHaveCount(1);
  await expect.poll(()=>page.locator('#room-art img').evaluate(image=>image.complete && image.naturalWidth>0)).toBe(true);
  await press(page,room('BEDROOM'),touch);
  await press(page,action('stool'),touch);
  expect(await state(page,'hasStepStool')).toBe(true);
  await setup(page,{thirst:30,temperature:35});
  await press(page,room('KITCHEN'),touch);
  await press(page,action('drink-kitchen'),touch);
  expect(await state(page,'thirst')).toBe(52);
  expect(await state(page,'temperature')).toBe(32);
  for(const [id,title] of [['GARAGE','Garage'],['OUTSIDE','Backyard'],['MOMS_ROOM','Mom’s Room'],['BATHROOM','Bathroom'],['BASEMENT','Laundry Room']]) {
    await press(page,room(id),touch);
    await expect(page.locator('#room-title')).toHaveText(title);
  }
  await page.keyboard.press('4');
  await expect(page.locator('#room-title')).toHaveText('Bedroom');
  await page.keyboard.press('p');
  await expect(page.locator('#room-title')).toHaveText('Phone');
  await press(page,'[data-command="put-phone"]',touch);
  await expect(page.locator('#room-title')).toHaveText('Bedroom');
  await press(page,'#pause-button',touch);
  await expect(page.getByRole('dialog',{name:'Paused'})).toBeVisible();
  const remaining=await state(page,'momTimerSeconds');
  await page.clock.runFor(2000);
  expect(await state(page,'momTimerSeconds')).toBe(remaining);
  await press(page,'[data-command="close"]',touch);
  await page.clock.runFor(500);
  expect(await state(page,'momTimerSeconds')).toBe(remaining-1);
  await press(page,room('LIVING_ROOM'),touch);
  const targets=await page.locator('.room-nav button,.action').evaluateAll(els=>els.map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height})));
  expect(targets.every(t=>t.w>=44 && t.h>=44)).toBe(true);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.screenshot({path:info.outputPath('living-room.png'),fullPage:true});
  if(touch) {
    await page.setViewportSize({width:768,height:1024});
    await expect(page.locator('#room-title')).toBeVisible();
    await press(page,room('BATHROOM'),touch);
    await press(page,action('drink-bathroom'),touch);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  }
});

test('original item chain, real roach targets, AC repair and timed dish controls', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch);
  await press(page,room('MOMS_ROOM'),touch); await press(page,action('spray'),touch);
  await press(page,room('GARAGE'),touch);
  await expect(page.locator(action('wrench'))).toBeDisabled();
  await press(page,action('roaches'),touch);
  await expect(page.getByRole('dialog',{name:'Clear the roaches'})).toBeVisible();
  // The supplied roach_ai artwork must render in the actual minigame, rather
  // than leaving the vector fallback visible underneath a failed image.
  await expect(page.locator('.th-mini-roach img')).toHaveCount(6);
  await expect.poll(()=>page.locator('.th-mini-roach img').evaluateAll(images=>images.every(img=>img.complete&&img.naturalWidth>0&&new URL(img.src).pathname.endsWith('/assets/roach_ai.png')))).toBe(true);
  await expect.poll(()=>page.locator('.th-mini-roach svg').evaluateAll(fallbacks=>fallbacks.every(svg=>{
    const style=getComputedStyle(svg);
    return style.display==='none'||style.visibility==='hidden'||Number(style.opacity)===0||!svg.getClientRects().length;
  }))).toBe(true);
  for(let i=1;i<=6;i++) await press(page,page.getByRole('button',{name:`Spray roach ${i}`,exact:true}),touch);
  await expect(page.locator('#modal')).toHaveCount(0);
  expect(await state(page,'roachesClearedEver')).toBe(true);
  await press(page,action('wrench'),touch);
  await setup(page,{acBroken:true});
  await press(page,room('LIVING_ROOM'),touch); await press(page,action('fix-ac'),touch);
  expect(await state(page,'acBroken')).toBe(false);
  expect(await state(page,'hasWrench')).toBe(false);
  await press(page,room('BEDROOM'),touch); await press(page,action('stool'),touch);
  await press(page,room('KITCHEN'),touch); await press(page,action('dishes'),touch);
  await expect(page.getByRole('dialog',{name:'Wash the dishes'})).toBeVisible();
  await press(page,'.th-mini-scrub',touch); // genuine miss at the start resets hits
  expect(await state(page,'dishScrubs')).toBe(0);
  await page.clock.runFor(560); // twenty 28ms increments put the needle at 50%
  const needle=await page.locator('.th-mini-needle').evaluate(e=>parseFloat(e.style.left));
  expect(needle).toBeGreaterThanOrEqual(35); expect(needle).toBeLessThanOrEqual(65);
  for(let i=0;i<4;i++) await press(page,'.th-mini-scrub',touch);
  await expect(page.locator('#modal')).toHaveCount(0);
  expect(await state(page,'choreDishes')).toBe(true);
});

async function observeCircuit(page) {
  await page.evaluate(()=>{
    window.__flashes=[];
    window.__flashObserver?.disconnect();
    window.__flashObserver=new MutationObserver(records=>{
      for(const m of records) {
        if(m.target.matches('.th-mini-node') && m.target.classList.contains('lit') && !(m.oldValue||'').split(/\s+/).includes('lit')) window.__flashes.push(m.target.textContent);
      }
    });
    window.__flashObserver.observe(document.querySelector('#minigame'),{subtree:true,attributes:true,attributeFilter:['class'],attributeOldValue:true});
  });
}

test('fuse-box circuits restore power through observed flashes and actual buttons', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch); await safe(page);
  await setup(page,{powerOut:true});
  await press(page,room('GARAGE'),touch); await press(page,action('circuit'),touch);
  for(let round=1;round<=3;round++) {
    await expect(page.locator('.th-mini-head h2')).toHaveText(`Fuse box · Round ${round}/3`);
    await observeCircuit(page);
    await page.clock.runFor(6000);
    await expect(page.locator('.th-mini-status')).toContainText('Your turn');
    const flashes=await page.evaluate(()=>{window.__flashObserver.disconnect();return window.__flashes;});
    expect(flashes).toHaveLength(round+2);
    for(const label of flashes) await press(page,page.getByRole('button',{name:`Circuit node ${label}`,exact:true}),touch);
    if(round<3) await page.clock.runFor(1000);
  }
  await expect(page.locator('#modal')).toHaveCount(0);
  expect(await state(page,'powerOut')).toBe(false);
});

test('nightmare circuits escape, wrong nodes lose, and restart clears state', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch); await safe(page);
  await setup(page,{temperature:91}); await ticks(page);
  await expect(page.getByRole('heading',{name:'THE NIGHTMARE REALM'})).toBeVisible();
  const remaining=await state(page,'momTimerSeconds');
  for(let round=1;round<=3;round++) {
    await expect(page.locator('#night-round')).toHaveText(`ROUND ${round} / 3`);
    await page.clock.runFor(6000);
    await expect(page.locator('#night-instruction')).toContainText('YOUR TURN');
    const labels=(await page.locator('#night-path').textContent()).replace('PATH: ','').split(' → ');
    expect(labels).toHaveLength(round+3);
    for(const label of labels) await press(page,page.locator('#night-nodes button').filter({hasText:new RegExp(`^${label}$`)}),touch);
    await page.clock.runFor(round===3?2000:1500);
  }
  await expect(page.locator('#room-title')).toHaveText('Living Room');
  expect(await state(page,'momTimerSeconds')).toBeGreaterThanOrEqual(remaining-1);
  expect(await state(page,'inNightmare')).toBe(false);
  expect(await state(page,'coins')).toBe(11);
  await setup(page,{temperature:91,stress:0}); await ticks(page);
  await page.clock.runFor(6000);
  const first=(await page.locator('#night-path').textContent()).replace('PATH: ','').split(' → ')[0];
  const wrong=first==='A'?'B':'A';
  await press(page,page.locator('#night-nodes button').filter({hasText:new RegExp(`^${wrong}$`)}),touch);
  await page.clock.runFor(2000);
  await expect(page.locator('#ending-title')).toHaveText('GAME OVER');
  await expect(page.locator('#ending-reason')).toContainText('Fire Demon Cat');
  await press(page,'[data-command="new"]',touch);
  expect(await state(page,'day')).toBe(1); expect(await state(page,'coins')).toBe(6);
});

test('arrival, allowance, day-specific shop, daily resets and three-day win', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch);
  for(let day=1;day<=3;day++) {
    await safe(page);
    await setup(page,{momTimerSeconds:1,choreDishes:true,choreTrashOut:true,choreTrashBagIn:true,choreCleanLR:true,choreCleanKitchen:true,choreCleanBedroom:true,choreHouseClean:true,choreLawnMowed:true,choreHomework:true,laundryState:5});
    await ticks(page);
    await expect(page.locator('.arrival-screen h1')).toContainText('home');
    expect(await state(page,'pendingAllowance')).toBe(8+day*2);
    await press(page,'[data-command="arrival"]',touch);
    if(day<3) {
      await expect(page.getByRole('heading',{name:'Upgrade shop'})).toBeVisible();
      if(day===1) {
        await expect(page.locator('[data-buy="lamp"]')).toHaveCount(0);
        await press(page,'[data-buy="fan"]',touch);
        await expect(page.locator('[data-buy="fan"]')).toBeDisabled();
      }
      await press(page,'[data-command="next-day"]',touch);
      await expect(page.locator('#day-label')).toHaveText(`DAY ${day+1} / 3`);
      expect(await state(page,'hasFan')).toBe(true);
      expect(await state(page,'choreDishes')).toBe(false);
      expect(await state(page,'hunger')).toBe(100);
      expect(await state(page,'acBroken')).toBe(true);
      expect(await state(page,'powerOut')).toBe(day+1===3);
    }
  }
  await expect(page.locator('#ending-title')).toHaveText('MOM NEVER KNEW!');
  await expect(page.locator('#ending-reason')).toContainText('all 3 days');
});

test('phone contact pages, delivery selector, music and Flappy touch/keyboard input are functional', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch); await safe(page);
  await press(page,room('PHONE'),touch);
  await expect(page.locator('.phone-tabs')).toHaveCount(0);
  await press(page,'[data-phone="MESSAGES"]',touch);
  await expect(page.locator('[data-phone="MSG_MOM"]')).toBeVisible();
  await expect(page.locator('[data-phone="MSG_FRIEND"]')).toBeVisible();
  await expect(page.locator(action('call-friend'))).toHaveCount(0);
  await press(page,'[data-phone="MSG_MOM"]',touch);
  await expect(page.locator(action('call-mom'))).toBeVisible();
  await expect(page.locator(action('text-mom'))).toBeVisible();
  await expect(page.locator(action('call-friend'))).toHaveCount(0);
  await setup(page,{stress:40});
  await press(page,action('text-mom'),touch);
  expect(await state(page,'stress')).toBe(38);
  await press(page,'[data-phone="BACK"]',touch);
  await press(page,'[data-phone="MSG_FRIEND"]',touch);
  await expect(page.locator(action('call-mom'))).toHaveCount(0);
  await setup(page,{stress:40,friendTextPending:true});
  await press(page,action('call-friend'),touch);
  expect(await state(page,'stress')).toBe(25);
  expect(await state(page,'friendTextPending')).toBe(false);
  await press(page,'[data-phone="HOME"]',touch);
  await press(page,'[data-phone="MUSIC"]',touch);
  await press(page,action('music-Lo-fi'),touch);
  expect(await state(page,'musicPlaying')).toBe(true);
  await press(page,'[data-phone="HOME"]',touch);
  await press(page,'[data-phone="DELIVERY"]',touch);
  await expect(page.locator('#phone-delivery')).toBeVisible();
  await page.locator('#phone-delivery').selectOption('order-Food');
  await expect(page.locator('#phone-order')).toHaveAttribute('data-action','order-Food');
  await press(page,'#phone-order',touch);
  expect(await state(page,'coins')).toBe(3);
  await ticks(page,20);
  expect(await state(page,'deliveryArrived')).toBe(true);
  await press(page,'[data-phone="HOME"]',touch);
  await press(page,'[data-phone="GAMES"]',touch);
  const canvas=page.locator('#flappy canvas');
  await expect(canvas).toBeVisible();
  const battery=await state(page,'phoneBattery');
  await press(page,canvas,touch);
  expect(await state(page,'phoneBattery')).toBeLessThan(battery);
  await page.keyboard.press('Space');
  await press(page,'[data-phone="HOME"]',touch);
  await press(page,'[data-phone="BACK"]',touch);
  await expect(page.locator('#room-title')).toHaveText('Living Room');
});

async function visibleRooms(page) {
  return page.locator('.room-nav [data-room]:visible').evaluateAll(buttons=>buttons.map(b=>b.dataset.room).sort());
}
async function assertComfortableNonOverlappingActions(page) {
  const boxes=await page.locator('.room-actions button.action:enabled').evaluateAll(buttons=>buttons.filter(b=>b.getClientRects().length).map(b=>{
    const r=b.getBoundingClientRect();
    return {id:b.dataset.action,x:r.x,y:r.y,w:r.width,h:r.height};
  }));
  for(const b of boxes) {
    expect(b.w,`${b.id} touch width`).toBeGreaterThanOrEqual(44);
    expect(b.h,`${b.id} touch height`).toBeGreaterThanOrEqual(44);
  }
  for(let i=0;i<boxes.length;i++) for(let j=i+1;j<boxes.length;j++) {
    const a=boxes[i],b=boxes[j];
    const intersectionWidth=Math.min(a.x+a.w,b.x+b.w)-Math.max(a.x,b.x);
    const intersectionHeight=Math.min(a.y+a.h,b.y+b.h)-Math.max(a.y,b.y);
    expect(intersectionWidth>1 && intersectionHeight>1,`${a.id} overlaps ${b.id}`).toBe(false);
  }
}

test('original floor navigation and source-positioned room controls stay usable', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch);
  await expect.poll(()=>visibleRooms(page)).toEqual(['BEDROOM','GARAGE','KITCHEN','LIVING_ROOM','OUTSIDE','PHONE']);
  await press(page,'[data-floor="UPSTAIRS"]',touch);
  expect(await state(page,'currentRoom')).toBe('LIVING_ROOM');
  await expect(page.locator('#room-title')).toHaveText('Living Room');
  await expect.poll(()=>visibleRooms(page)).toEqual(['BATHROOM','MOMS_ROOM','PHONE']);
  await press(page,'[data-floor="GROUND"]',touch);
  await press(page,'[data-floor="BASEMENT"]',touch);
  expect(await state(page,'currentRoom')).toBe('LIVING_ROOM');
  await expect.poll(()=>visibleRooms(page)).toEqual(['BASEMENT']);
  await press(page,room('BASEMENT'),touch);
  expect(await state(page,'currentRoom')).toBe('BASEMENT');
  await press(page,room('KITCHEN'),touch);
  await expect(page.locator('.room-actions')).toHaveClass(/source-room-actions/);
  await expect(page.locator(action('drink-kitchen'))).toHaveAttribute('data-source-x','18');
  await expect(page.locator(action('feed-kitchen'))).toHaveAttribute('data-source-x','215');
  await expect(page.locator(action('freezer'))).toHaveAttribute('data-source-x','603');
  for(const id of ['LIVING_ROOM','KITCHEN','BEDROOM','GARAGE','MOMS_ROOM','BATHROOM','BASEMENT','OUTSIDE']) {
    await press(page,room(id),touch);
    await assertComfortableNonOverlappingActions(page);
  }
  await press(page,room('KITCHEN'),touch);
  await page.screenshot({path:info.outputPath('source-kitchen-layout.png'),fullPage:true});
});


const originalBackgrounds = ['menu','living_room','kitchen','garage','bedroom','outdoor','moms_room','bathroom','laundry_room'];
const originalSprites = ['cat','cat_bowl','squirrel','closet_interior','roach_ai','rat','sprinkler_on','sprinkler_off','sprinkler'];
const repositoryRoot = fileURLToPath(new URL('../../',import.meta.url));
const sha256 = data => createHash('sha256').update(data).digest('hex');

async function assertBackground(page, container, name, manifest) {
  const background=page.locator(`${container} > img[data-room-background="${name}"]`);
  await expect(background).toHaveCount(1);
  await expect(background).toHaveAttribute('src',new RegExp(`(?:^|/)${manifest[name].replace(/[.*+?^${}()|[\]\\]/g,'\\$&')}$`));
  await expect.poll(()=>background.evaluate(image=>image.complete&&image.naturalWidth>0&&image.naturalHeight>0)).toBe(true);
  await expect(page.locator(container).getByText(/art not supplied/i)).toHaveCount(0);
  await expect(page.locator(`${container} [data-art-fallback]`)).toHaveAttribute('display','none');
}
async function assertSprite(page,name,manifest,minimum=1) {
  const sprite=page.locator(`#room-art image[data-original-sprite="${name}"]`);
  expect(await sprite.count()).toBeGreaterThanOrEqual(minimum);
  const hrefs=await sprite.evaluateAll(images=>images.map(image=>image.getAttribute('href')));
  for(const href of hrefs) expect(new URL(href,page.url()).pathname).toBe(`/${manifest[name]}`);
}

test('all original runtime assets retain canonical provenance and all nine backgrounds decode in their rooms', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  const manifestResponse=await page.request.get('/assets/manifest.json');
  const sourcesResponse=await page.request.get('/assets/sources.json');
  expect(manifestResponse.ok()).toBe(true);expect(sourcesResponse.ok()).toBe(true);
  const manifest=await manifestResponse.json(), sources=await sourcesResponse.json();
  expect(Object.keys(manifest).sort()).toEqual([...originalBackgrounds,...originalSprites].sort());
  expect(Object.keys(sources).sort()).toEqual(Object.keys(manifest).sort());
  for(const name of Object.keys(manifest)) {
    // Verify the browser's bytes against the actual imported desktop runtime
    // files, rather than accepting a provenance label for a different picture.
    expect(sources[name]).toMatch(/^assets\/rooms\/[^/]+$/);
    const expectedSource=`assets/rooms/${name}${name==='rat'?'.png.png':originalBackgrounds.includes(name)?'.jpg':'.png'}`;
    expect(sources[name]).toBe(expectedSource);
    const response=await page.request.get(`/${manifest[name]}`);
    expect(response.ok(),name).toBe(true);
    expect(sha256(await response.body()),name).toBe(sha256(readFileSync(path.join(repositoryRoot,sources[name]))));
  }
  expect(manifest.rat).toBe('assets/rat.png');
  const decoded=await page.evaluate(async assets=>Promise.all(Object.entries(assets).map(async([name,url])=>{
    const image=new Image();image.src=url;await image.decode();
    return {name,width:image.naturalWidth,height:image.naturalHeight};
  })),manifest);
  expect(decoded).toHaveLength(18);
  for(const image of decoded){expect(image.width,image.name).toBeGreaterThan(0);expect(image.height,image.name).toBeGreaterThan(0);}
  await assertBackground(page,'#menu-art','menu',manifest);
  await start(page,touch);
  for(const[id,name]of [['LIVING_ROOM','living_room'],['KITCHEN','kitchen'],['GARAGE','garage'],['BEDROOM','bedroom'],['OUTSIDE','outdoor'],['MOMS_ROOM','moms_room'],['BATHROOM','bathroom'],['BASEMENT','laundry_room']]) {
    await press(page,room(id),touch);
    await assertBackground(page,'#room-art',name,manifest);
    if(id==='GARAGE'||id==='BASEMENT')await page.screenshot({path:info.outputPath(`original-${name}.png`),fullPage:true});
  }
});

test('supplied character and object sprites, closet PNG and phone prior-room art replace illustration fallbacks', async({page},info)=>{
  const touch=!!info.project.use.hasTouch;
  const manifest=await(await page.request.get('/assets/manifest.json')).json();
  await start(page,touch);
  await press(page,room('BEDROOM'),touch);
  await assertSprite(page,'cat',manifest);await assertSprite(page,'cat_bowl',manifest);
  await setup(page,{squirrelPresent:true,squirrelCount:2});
  await press(page,room('LIVING_ROOM'),touch);await assertSprite(page,'squirrel',manifest,2);
  await setup(page,{squirrelPresent:false,ratsPresent:true,ratsClearedToday:false,roachesPresent:false,roachesCleared:true});
  await press(page,room('GARAGE'),touch);await assertSprite(page,'rat',manifest);
  await press(page,room('OUTSIDE'),touch);await assertSprite(page,'sprinkler_on',manifest);
  await press(page,action('sprinkler'),touch);await assertSprite(page,'sprinkler_off',manifest);
  await press(page,room('MOMS_ROOM'),touch);
  await press(page,action('closet'),touch);
  await assertBackground(page,'#room-art','closet_interior',manifest);
  await press(page,action('leave-closet'),touch);
  await assertBackground(page,'#room-art','moms_room',manifest);
  // The source phone paints the prior room background alone: it must not keep
  // bedroom cat sprites or a hidden-closet scene when opened from those rooms.
  await press(page,room('BEDROOM'),touch);
  await press(page,room('PHONE'),touch);
  expect(await state(page,'roomBeforePhone')).toBe('BEDROOM');
  await assertBackground(page,'#room-art','bedroom',manifest);
  await expect(page.locator('#room-art [data-original-sprite]')).toHaveCount(0);
  await press(page,'[data-phone="HOME"]',touch);await press(page,'[data-phone="BACK"]',touch);
  await expect(page.locator('#room-title')).toHaveText('Bedroom');
  await assertSprite(page,'cat',manifest);
  await page.screenshot({path:info.outputPath('original-bedroom-sprites.png'),fullPage:true});
});
