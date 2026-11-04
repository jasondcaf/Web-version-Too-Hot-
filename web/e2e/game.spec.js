import { test, expect } from '@playwright/test';

// Development hooks only set up conditions and speed up waiting. Outcomes in
// these tests are earned through rendered controls, including the minigames.
const errors = new WeakMap();
const room = (id) => `[data-room="${id}"]`;
const action = (id) => `[data-action="${id}"]`;
async function press(page, selector, touch = false) {
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

test('phone apps, delivery, music and Flappy touch/keyboard input are functional', async ({page},info) => {
  const touch=!!info.project.use.hasTouch;
  await start(page,touch); await safe(page);
  await press(page,room('PHONE'),touch);
  await press(page,'.phone-tabs [data-phone="MESSAGES"]',touch);
  await setup(page,{stress:40});
  await press(page,action('call-friend'),touch);
  expect(await state(page,'stress')).toBe(25);
  await press(page,'.phone-tabs [data-phone="MUSIC"]',touch);
  await press(page,action('music-Lo-fi'),touch);
  expect(await state(page,'musicPlaying')).toBe(true);
  await press(page,'.phone-tabs [data-phone="DELIVERY"]',touch);
  await press(page,action('order-Food'),touch);
  expect(await state(page,'coins')).toBe(3);
  await ticks(page,20);
  expect(await state(page,'deliveryArrived')).toBe(true);
  await press(page,'.phone-tabs [data-phone="GAMES"]',touch);
  const canvas=page.locator('#flappy canvas');
  await expect(canvas).toBeVisible();
  const battery=await state(page,'phoneBattery');
  await press(page,canvas,touch);
  expect(await state(page,'phoneBattery')).toBeLessThan(battery);
  await page.keyboard.press('Space');
  await press(page,'[data-command="put-phone"]',touch);
  await expect(page.locator('#room-title')).toHaveText('Living Room');
});
