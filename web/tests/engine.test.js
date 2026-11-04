import test from 'node:test';
import assert from 'node:assert/strict';
import { createGame, resetForDay, tick, choreCount, dayBonus, finishArrival, availableShopItems, purchase, startNightmare, nightmareInput, advanceNightmare } from '../src/engine.js';

const quiet = () => .99;
function playing(extra = {}) { return Object.assign(createGame(quiet),{screen:'GAME'},extra); }
const near = (actual, expected) => assert.ok(Math.abs(actual - expected) < 1e-8,`${actual} ≠ ${expected}`);

test('new game matches Java defaults, and menu does not tick', () => {
  const s = createGame(quiet);
  assert.equal(s.screen,'MENU'); assert.equal(s.coins,6); assert.equal(s.momTimerSeconds,300);
  assert.equal(s.temperature,25); assert.equal(s.roachesPresent,true);
  tick(s,quiet); assert.equal(s.momTimerSeconds,300);
});
test('one 500ms tick preserves actual Java timer and decay rates', () => {
  const s = playing(); tick(s,quiet);
  assert.equal(s.momTimerSeconds,299);
  near(s.temperature,25.25); near(s.hunger,99.65); near(s.thirst,99.55); near(s.catHunger,99.75);
  near(s.phoneBattery,79);
});
test('blast cooling, windows, stress, illness and final heat ramp combine', () => {
  const s = playing({temperature:40,acSetting:3,windowOpen:true,bedroomWindowOpen:true,kitchenWindowOpen:true,stress:60,hasCold:true,momTimerSeconds:61});
  tick(s,quiet);
  near(s.temperature,40 + .25 * 1.25 - .45 - .2 - .1 - .1 + .2 + .65 * .5);
});
test('maxed stress loses before the nightmare check', () => {
  const s = playing({stress:100,temperature:92,smokeAlarmActive:true,squirrelPresent:true}); tick(s,quiet);
  assert.equal(s.screen,'GAMEOVER'); assert.match(s.gameOverReason,/stressed/);
});
test('dehydration and starvation lose when heat is below nightmare threshold', () => {
  for (const stat of ['hunger','thirst']) {
    const s = playing({[stat]:0}); tick(s,quiet);
    assert.equal(s.screen,'GAMEOVER'); assert.equal(s.won,false);
  }
});
test('arrival has Java priority over death, pays score and allowance once', () => {
  const s = playing({momTimerSeconds:1,hunger:0,thirst:0,stress:100});
  tick(s,quiet);
  assert.equal(s.screen,'MOM_ARRIVAL'); assert.equal(s.day,2); assert.equal(s.won,true);
  const paid = s.coins; tick(s,quiet); assert.equal(s.coins,paid);
  assert.equal(s.pendingAllowance,0); assert.equal(finishArrival(s),true); assert.equal(s.screen,'SHOP');
});
test('all six chores grant full allowance and four coins per chore', () => {
  const s = playing({choreDishes:true,choreTrashOut:true,choreTrashBagIn:true,choreHouseClean:true,choreLawnMowed:true,choreHomework:true,laundryState:5});
  assert.equal(choreCount(s),6); assert.equal(dayBonus(s),37);
  s.momTimerSeconds=1; tick(s,quiet);
  assert.equal(s.pendingAllowance,10); assert.equal(s.lastDayBonus,37); assert.equal(s.coins,53);
});
test('partial trash does not count until replacement bag is installed', () => {
  const s = playing({choreTrashOut:true}); assert.equal(choreCount(s),0);
  s.choreTrashBagIn=true; assert.equal(choreCount(s),1);
  s.momTimerSeconds=1; tick(s,quiet); assert.equal(s.pendingAllowance,2);
});
test('reset preserves upgrades, money, cleared roaches and AC setting; resets daily inventory', () => {
  const s = playing({day:2,coins:42,hasFan:true,hasSnackPack:true,hasCatTreats:true,hasChillPills:true,hasKitchenStool:true,hasLaundrysoap:true,roachesClearedEver:true,hasWrench:true,acSetting:3,choreDishes:true,phoneBattery:0});
  resetForDay(s,quiet);
  assert.equal(s.coins,42); assert.equal(s.momTimerSeconds,270); assert.equal(s.acBroken,true); assert.equal(s.powerOut,false);
  assert.equal(s.acSetting,3); assert.equal(s.hasWrench,false); assert.equal(s.hasStepStool,true); assert.equal(s.stepStoolInBedroom,false);
  assert.equal(s.roachesPresent,false); assert.equal(s.ratsPresent,true); assert.equal(s.phoneBattery,80); assert.equal(s.choreDishes,false);
  assert.equal(s.laundryDetergent,4); assert.equal(s.tempRiseRate,.7); assert.equal(s.hungerFallRate,.75); assert.equal(s.catHungerFallRate,.7); assert.equal(s.stressRiseRate,.7);
});
test('day three starts with broken AC and power out; completing it reaches winning ending', () => {
  const s = playing({day:3}); resetForDay(s,quiet);
  assert.equal(s.momTimerSeconds,240); assert.equal(s.powerOut,true); assert.equal(s.acBroken,true);
  s.momTimerSeconds=1; tick(s,quiet); assert.equal(s.day,3); assert.equal(s.lastDayDone,true);
  finishArrival(s); assert.equal(s.screen,'GAMEOVER'); assert.equal(s.won,true); assert.match(s.gameOverReason,/all 3 days/);
});
test('shop follows day slots, rejects duplicate, unavailable and unaffordable purchases', () => {
  const s = playing({screen:'SHOP',day:2,coins:10});
  assert.deepEqual(availableShopItems(s).map(i=>i.id),['fan','snack','bottle','roach','gen','ice','soap','mousetrap']);
  assert.equal(purchase(s,'lamp'),false); assert.equal(purchase(s,'fan'),true); assert.equal(s.coins,2); assert.equal(s.hasFan,true);
  assert.equal(purchase(s,'fan'),false); assert.equal(purchase(s,'roach'),false);
  resetForDay(s,quiet); assert.equal(s.tempRiseRate,.7);
});
test('homework happens in bedroom for 20 cumulative ticks, or 12 with lamp', () => {
  for (const [lamp,needed] of [[false,20],[true,12]]) {
    const s = playing({currentRoom:'BEDROOM',hasStudyLamp:lamp});
    for (let i=0;i<needed-1;i++) tick(s,quiet);
    assert.equal(s.choreHomework,false); tick(s,quiet); assert.equal(s.choreHomework,true);
  }
});
test('laundry progresses across rooms and outages, using upgrade durations', () => {
  for (const [soap,duration] of [[false,10],[true,7]]) {
    const s = playing({laundryState:1,powerOut:true,hasLaundrysoap:soap});
    for (let i=0;i<duration;i++) tick(s,quiet);
    assert.equal(s.laundryState,2);
    s.laundryState=3;
    for (let i=0;i<duration;i++) tick(s,quiet);
    assert.equal(s.laundryState,4);
  }
});
test('charge completion and independent delivery countdowns run on game ticks', () => {
  const s=playing({phoneCharging:true,phoneBattery:99,deliveryOrdered:true,deliveryTicks:1,deliveryItemType:'Drinks',thirst:20,detergentOrdered:true,detergentTicks:1,coldMedOrdered:true,coldMedTicks:1,powerOut:true});
  tick(s,quiet);
  assert.equal(s.phoneBattery,100); assert.equal(s.phoneCharging,false); assert.equal(s.phoneChargeCount,1);
  assert.equal(s.deliveryArrived,true); assert.equal(s.drinkArrived,true); near(s.thirst,69.55);
  assert.equal(s.detergentArrived,true); assert.equal(s.coldMedArrived,true);
});
test('storm waters plants once; enemy cooldowns and squirrel exit match source', () => {
  const s=playing({acBreakCooldown:0,powerOutCooldown:0}); tick(s,()=>0);
  assert.equal(s.acBroken,true); assert.equal(s.hasWrench,false); assert.equal(s.wrenchInGarage,true);
  assert.equal(s.powerOut,true); assert.equal(s.thunderstorm,true); assert.equal(s.choreWaterPlants,true); assert.equal(s.coins,7);
  s.squirrelPresent=true;s.squirrelTicks=30;tick(s,quiet); assert.equal(s.squirrelPresent,false);
});
test('Mittens escapes through open windows and angry chaos undoes a chore', () => {
  const s=playing({catHunger:1,windowOpen:true}); tick(s,()=>0);
  assert.equal(s.mittensEscaped,true); assert.equal(s.windowOpen,false);
  const t=playing({catHunger:15,mittensChaosTimer:1,choreCleanLR:true,choreHouseClean:true}); tick(t,()=>0);
  assert.equal(t.mittensMessRoom,'LIVING_ROOM'); assert.equal(t.choreCleanLR,false); assert.equal(t.choreHouseClean,false);
});
test('nightmare freezes main simulation; complete 4/5/6-node circuits to escape', () => {
  const s=playing({temperature:90}); tick(s,quiet);
  assert.equal(s.screen,'NIGHTMARE'); const remaining=s.momTimerSeconds;
  for (let round=1;round<=3;round++) {
    assert.equal(s.nightmare.round,round); assert.equal(s.nightmare.numNodes,round+3);
    assert.equal(new Set(s.nightmare.sequence).size,round+3);
    advanceNightmare(s,10,quiet); assert.equal(s.nightmare.phase,'input');
    for (const node of s.nightmare.sequence) assert.equal(nightmareInput(s,node),true);
    advanceNightmare(s,2,quiet);
  }
  assert.equal(s.screen,'GAME'); assert.equal(s.momTimerSeconds,remaining); assert.equal(s.temperature,70); assert.equal(s.stress,80); assert.equal(s.coins,11);
});
test('wrong circuit node or 20-second timeout reaches the demon-cat ending', () => {
  for (const failure of ['wrong','timeout']) {
    const s=playing(); startNightmare(s,quiet); advanceNightmare(s,10,quiet);
    if (failure==='wrong') nightmareInput(s,(s.nightmare.sequence[0]+1)%4);
    else advanceNightmare(s,20,quiet);
    assert.equal(s.nightmare.phase,'failed'); advanceNightmare(s,2,quiet);
    assert.equal(s.screen,'GAMEOVER'); assert.equal(s.won,false); assert.match(s.gameOverReason,/Demon Cat/);
  }
});

test('real room actions can complete all chores and survive the entire three-day progression', async () => {
  const {executeAction,navigate} = await import('../src/actions.js');
  const s=playing();
  const action=(room,id,payload={})=>{
    assert.equal(navigate(s,room),true);
    const result=executeAction(s,id,payload,quiet);
    assert.equal(result.ok,true,`${room}/${id}: ${result.message}`);
    return result;
  };
  for (let day=1;day<=3;day++) {
    assert.equal(s.day,day);
    if (s.roachesPresent) {
      action('MOMS_ROOM','spray'); action('GARAGE','roaches');
      action('GARAGE','roaches-result',{success:true});
    }
    if (s.powerOut) { action('GARAGE','circuit'); action('GARAGE','circuit-result',{success:true}); }
    if (s.acBroken) { action('GARAGE','wrench'); action('LIVING_ROOM','fix-ac'); }
    if (s.acSetting!==3) action('LIVING_ROOM','ac-3');
    action('BEDROOM','stool');
    action('BATHROOM','shower'); action('BEDROOM','dry-off'); action('BEDROOM','change-clothes');
    action('BASEMENT','washer');
    action('LIVING_ROOM','tidy-living'); action('KITCHEN','tidy-kitchen'); action('BEDROOM','tidy-bedroom');
    action('OUTSIDE','trash'); action('KITCHEN','trash-bag');
    action('OUTSIDE','start-mowing');
    for(let i=0;i<8;i++) action('OUTSIDE','mow');
    action('KITCHEN','dishes');
    for(let i=0;i<4;i++) action('KITCHEN','dishes-hit',{needle:.5});
    let ticks=0;
    while(s.screen==='GAME') {
      if(s.laundryState===2) action('BASEMENT','dryer');
      if(s.laundryState===4) action('BASEMENT','fold');
      if(ticks%15===0) {
        action('BEDROOM','rest-bedroom');
        if(s.catHunger<70) action('BEDROOM','feed-bedroom');
        action('KITCHEN','drink-kitchen');
        action('KITCHEN','microwave'); action('KITCHEN','microwave');
      }
      navigate(s,'BEDROOM'); tick(s,quiet); ticks++;
      assert.ok(ticks<=300,'day countdown should complete');
    }
    assert.equal(s.screen,'MOM_ARRIVAL',s.gameOverReason);
    assert.equal(choreCount(s),6);
    assert.equal(s.pendingAllowance,8+day*2);
    assert.equal(ticks,300-(day-1)*30);
    finishArrival(s);
    if(day<3) {
      assert.equal(s.screen,'SHOP');
      if(day===1) assert.equal(purchase(s,'mousetrap'),true);
      resetForDay(s,quiet);
    }
  }
  assert.equal(s.screen,'GAMEOVER'); assert.equal(s.won,true);
});
