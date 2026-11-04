// Rules ported from GameState.java, GameLoop.java and the screen controllers.
// One tick is 500 ms. The Java countdown consumes one displayed second per tick.
const defaults = {
  temperature: 25,
  hunger: 100,
  thirst: 100,
  catHunger: 100,
  stress: 0,
  momTimerSeconds: 300,
  day: 1,
  MAX_DAYS: 3,
  coins: 6,
  acBroken: false,
  powerOut: false,
  acSetting: 2,
  windowOpen: false,
  windowOpenTicks: 0,
  windowBroken: false,
  windowBrokenCooldown: 0,
  bedroomWindowOpen: false,
  kitchenWindowOpen: false,
  squirrelPresent: false,
  squirrelCount: 1,
  squirrelTicks: 0,
  roachesPresent: true,
  roachesCleared: false,
  roachesClearedEver: false,
  ratsPresent: false,
  ratsClearedToday: false,
  hasStepStool: false,
  hasWrench: false,
  hasRoachSpray: false,
  catFoodScoops: 3,
  hidingInCloset: false,
  stepStoolInBedroom: true,
  wrenchInGarage: true,
  roachSprayInDrawer: true,
  tempRiseRate: 1.0,
  hungerFallRate: 1.0,
  thirstFallRate: 1.0,
  catHungerFallRate: 1.0,
  stressRiseRate: 1.0,
  hasCold: false,
  coldMedOrdered: false,
  coldMedTicks: 0,
  coldMedArrived: false,
  hasFan: false,
  hasSnackPack: false,
  hasBigBottle: false,
  hasCatTreats: false,
  hasRoachTrap: false,
  hasGenerator: false,
  hasKitchenStool: false,
  hasChillPills: false,
  hasIcePack: false,
  hasLaundrysoap: false,
  hasStudyLamp: false,
  hasSquirrelSpray: false,
  hasMouseTrap: false,
  hasMedicineKit: false,
  hasMomFlashlight: false,
  medicineUsed: false,
  looseChangeFound_LivingRoom: false,
  looseChangeFound_Kitchen: false,
  looseChangeFound_Bedroom: false,
  looseChangeFound_Garage: false,
  looseChangeFound_Outside: false,
  looseChangeFound_MomsRoom: false,
  looseChangeFound_Bathroom: false,
  sprinklerUsed: false,
  choreDishes: false,
  dishScrubs: 0,
  choreTrashOut: false,
  choreTrashBagIn: false,
  choreCleanLR: false,
  choreCleanKitchen: false,
  choreCleanBedroom: false,
  choreHouseClean: false,
  choreLawnPct: 0,
  choreLawnMowed: false,
  choreHomework: false,
  choreHomeworkTicks: 0,
  dishMisses: 0,
  laundryState: 0,
  laundryWashTicks: 0,
  laundryDryTicks: 0,
  showeredToday: false,
  needsTowelDry: false,
  needsClothesChange: false,
  dirtyClothesAdded: false,
  foodMicrowaved: false,
  TOTAL_CHORES: 6,
  pendingAllowance: 0,
  lastDayDone: false,
  currentRoom: "LIVING_ROOM",
  gameOver: false,
  won: false,
  gameOverReason: "",
  inNightmare: false,
  phoneBattery: 80,
  phoneCharging: false,
  phoneChargeTicks: 0,
  phoneChargeCount: 0,
  deliveryOrdered: false,
  deliveryTicks: 0,
  deliveryArrived: false,
  deliveryItemType: "Food",
  drinkArrived: false,
  hasSpeaker: false,
  musicPlaying: false,
  musicGenre: "",
  roomBeforePhone: "LIVING_ROOM",
  laundryDetergent: 2,
  detergentOrdered: false,
  detergentTicks: 0,
  detergentArrived: false,
  thunderstorm: false,
  thunderstormTicks: 0,
  smokeAlarmActive: false,
  smokeAlarmFans: 0,
  friendTextPending: false,
  friendTextIgnores: 0,
  nextFriendTextIn: 90,
  tvWatching: false,
  napping: false,
  napTicksLeft: 0,
  mittensChaosTimer: 50,
  mittensEscaped: false,
  mittensMessRoom: "",
  choreVacuum: false,
  choreWaterPlants: false,
  choreMailIn: false,
  choreSweepKitchen: false,
  choreMopBathroom: false,
  iceCreamFound: false,
  iceCreamEaten: false,
  acBreakCooldown: 0,
  powerOutCooldown: 0,
  momPriorityChores: ['Dishes', 'Homework'],
  mowingActive: false,
  mowStreak: 0,
};

export const TICK_MS = 500;
export const clamp = (v, min = 0, max = 100) => Math.max(min, Math.min(max, v));
export function notify(s, text) {
  s.messages ??= [];
  s.messages.push(text);
  if (s.messages.length > 40) s.messages.shift();
  s.message = text;
}
const upgrades = ['hasFan','hasSnackPack','hasBigBottle','hasCatTreats','hasRoachTrap','hasGenerator','hasKitchenStool','hasChillPills','hasIcePack','hasLaundrysoap','hasStudyLamp','hasSquirrelSpray','hasMouseTrap','hasMedicineKit'];
const shuffle = (list, rng) => {
  const result = [...list];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(rng() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
};
export function createGame(rng = Math.random) {
  const s = {...defaults, messages: []};
  resetForDay(s, rng);
  s.screen = 'MENU';
  return s;
}
export function resetForDay(s, rng = Math.random) {
  const retained = {};
  for (const key of [...upgrades, 'coins','day','lastDayDone','pendingAllowance','roachesClearedEver','acSetting','roomBeforePhone']) {
    retained[key] = s[key] ?? defaults[key];
  }
  Object.assign(s, defaults, retained, {messages: [], message: '', nightmare: null, screen: 'GAME'});
  s.momTimerSeconds = Math.max(180, 300 - (s.day - 1) * 30);
  s.acBroken = s.day >= 2;
  s.powerOut = s.day >= 3;
  s.roachesPresent = !s.hasRoachTrap && !s.roachesClearedEver;
  s.roachesCleared = s.hasRoachTrap || s.roachesClearedEver;
  s.ratsPresent = s.day >= 2 && s.roachesClearedEver && !s.hasMouseTrap;
  s.hasStepStool = s.hasKitchenStool;
  s.stepStoolInBedroom = !s.hasKitchenStool;
  s.laundryDetergent = s.hasLaundrysoap ? 4 : 2;
  s.nextFriendTextIn = 80 + Math.floor(rng() * 40);
  s.mittensChaosTimer = 40 + Math.floor(rng() * 30);
  s.momPriorityChores = shuffle(['Dishes','Trash','Clean Rooms','Lawn','Homework','Laundry'],rng).slice(0,2);
  s.acBreakCooldown = 60;
  s.powerOutCooldown = 120;
  s.tempRiseRate = s.hasFan ? .7 : 1;
  s.hungerFallRate = s.hasSnackPack ? .75 : 1;
  s.catHungerFallRate = s.hasCatTreats ? .7 : 1;
  s.stressRiseRate = s.hasChillPills ? .7 : 1;
  notify(s, 'Mom is coming home. Keep cool, feed Mittens, and finish your chores!');
  return s;
}
export function choreCount(s) {
  return [s.choreDishes, s.choreTrashOut && s.choreTrashBagIn, s.choreHouseClean, s.choreLawnMowed, s.choreHomework, s.laundryState >= 5].filter(Boolean).length;
}
export function lose(s, reason) {
  Object.assign(s,{gameOver: true, won: false, inNightmare: false, gameOverReason: reason, screen:'GAMEOVER'});
}

function tickStats(s, rng) {
  const coldMult = s.hasCold ? 1.25 : 1;
  let rise = .25 * s.tempRiseRate * coldMult;
  if (s.acBroken || s.powerOut) rise += .4;
  else rise += [.35,-.1,0,-.45][s.acSetting] ?? 0;
  if (s.windowOpen) rise -= .2;
  if (s.bedroomWindowOpen) rise -= .1;
  if (s.kitchenWindowOpen) rise -= .1;
  if (s.stress > 50) rise += .2;
  rise += (s.day - 1) * .1;
  if (s.momTimerSeconds > 0 && s.momTimerSeconds < 120) rise += .65 * (1 - s.momTimerSeconds / 120);
  s.temperature = clamp(s.temperature + rise);
  s.hunger = clamp(s.hunger - .35 * s.hungerFallRate);
  s.thirst = clamp(s.thirst - .45 * s.thirstFallRate);
  s.catHunger = clamp(s.catHunger - .25 * s.catHungerFallRate);
  if (s.musicPlaying && s.phoneBattery > 0) {
    s.stress = clamp(s.stress - (s.hasSpeaker ? .7 : .42));
    s.phoneBattery = clamp(s.phoneBattery - .2);
    if (s.phoneBattery <= 0) s.musicPlaying = false;
  }
  if (s.tvWatching && !s.hidingInCloset) s.stress = clamp(s.stress - 1.5);
  if (s.napping) {
    s.stress = clamp(s.stress - 2);
    s.temperature = clamp(s.temperature - .4);
    if (s.napTicksLeft > 0 && --s.napTicksLeft === 0) {
      s.napping = false;
      s.currentRoom = 'BEDROOM';
      notify(s, 'You woke up from your nap. Feeling better!');
    }
  }
  if (s.smokeAlarmActive) s.stress = clamp(s.stress + 3);
  if (s.thunderstorm && anyWindow(s)) s.stress = clamp(s.stress + .6);
  if (s.friendTextIgnores >= 3 && !s.friendTextPending) s.stress = clamp(s.stress + .3);
  if (s.hasCold) s.stress = clamp(s.stress + .5 * coldMult);
  if (s.thunderstorm && s.currentRoom === 'OUTSIDE') {
    s.stress = clamp(s.stress + 2.5);
    if (!s.hasCold && rng() < .04) {
      s.hasCold = true;
      notify(s, "You got caught in the storm! You've come down with a cold...");
    }
  }
  if (s.needsTowelDry) { s.stress = clamp(s.stress + .4); s.temperature = clamp(s.temperature - .15); }
  if (s.needsClothesChange) s.stress = clamp(s.stress + .3);
  if (s.hidingInCloset) s.stress = clamp(s.stress - 1.2);
  else if (s.squirrelPresent) s.stress = clamp(s.stress + 1.2 * s.squirrelCount * s.stressRiseRate * (s.hasSquirrelSpray ? .5 : 1));
  else if (s.ratsPresent) s.stress = clamp(s.stress + .8 * s.stressRiseRate);
  else s.stress = clamp(s.stress - .3);
  if (s.catHunger <= 20 && !s.hidingInCloset) s.stress = clamp(s.stress + .5);
}
const anyWindow = s => s.windowOpen || s.bedroomWindowOpen || s.kitchenWindowOpen;
function tickEnvironment(s,rng) {
  if (s.windowBrokenCooldown > 0 && --s.windowBrokenCooldown === 0) s.windowBroken = false;
  if (anyWindow(s)) {
    s.windowOpenTicks++;
    if (s.windowOpenTicks > 8 && !s.squirrelPresent && rng() < Math.min(.10,.015 + s.windowOpenTicks * .004)) {
      s.squirrelPresent = true;
      s.squirrelCount = 1 + Math.floor(rng() * 3);
      s.squirrelTicks = 0;
      if (s.windowOpen && rng() < .25) {
        s.windowBroken = true;
        s.windowBrokenCooldown = 24;
        notify(s, `${s.squirrelCount} squirrel(s) in the house! The window slider broke!`);
      } else notify(s, `${s.squirrelCount} squirrel(s) climbed through a window!`);
    }
  } else s.windowOpenTicks = 0;
  if (s.squirrelPresent && ++s.squirrelTicks > 30) {
    Object.assign(s,{squirrelPresent:false,squirrelCount:1,squirrelTicks:0});
    notify(s,'The squirrels left on their own... eventually.');
  }
  if (s.acBreakCooldown > 0) s.acBreakCooldown--;
  if (s.powerOutCooldown > 0) s.powerOutCooldown--;
}
function tickRandomEvents(s,rng) {
  const diff = 1 + (s.day - 1) * .3;
  if (!s.acBroken && s.acBreakCooldown === 0 && rng() < .008 * diff) {
    Object.assign(s,{acBroken:true, acBreakCooldown:80,wrenchInGarage:true,hasWrench:false});
    notify(s,'The AC just stopped working! A wrench appeared in the garage.');
  }
  let chance = .004 * diff;
  if (s.acSetting === 3 && !s.acBroken) chance *= 3;
  if (s.phoneCharging && s.phoneChargeCount >= 3) chance *= 2;
  if (s.thunderstorm) chance *= 3;
  if (!s.powerOut && s.powerOutCooldown === 0 && rng() < chance) {
    s.powerOut = true; s.powerOutCooldown = 160;
    s.acBreakCooldown = Math.max(s.acBreakCooldown,s.hasGenerator ? 10 : 30);
    notify(s,'POWER OUT! Head to the garage fuse box.');
  }
  if (!s.ratsPresent && !s.ratsClearedToday && s.roachesClearedEver && s.day >= 2 && !s.hasMouseTrap && rng() < .005 * diff) {
    s.ratsPresent = true;
    notify(s,'Rats! They moved in after the roaches left. Get a mouse trap!');
  }
  if (!s.thunderstorm) {
    if (rng() < .003 * diff) {
      s.thunderstorm = true;
      s.thunderstormTicks = 80 + Math.floor(rng() * 80);
      const watered = !s.choreWaterPlants;
      s.choreWaterPlants = true;
      if (watered) s.coins++;
      notify(s, `Thunderstorm!${watered ? ' Rain watered the outdoor plants! +1 coin.' : ''} Power may go out!`);
    }
  } else if (--s.thunderstormTicks <= 0) {
    s.thunderstorm = false;
    notify(s,'The storm passed. You can go outside again.');
  }
}
function tickChores(s) {
  if (s.currentRoom === 'BEDROOM' && !s.choreHomework) {
    if (++s.choreHomeworkTicks >= (s.hasStudyLamp ? 12 : 20)) {
      s.choreHomework = true;
      notify(s,'Homework done!');
    }
  }
  if (s.choreCleanLR && s.choreCleanKitchen && s.choreCleanBedroom) s.choreHouseClean = true;
  const duration = s.hasLaundrysoap ? 7 : 10;
  if (s.laundryState === 1 && ++s.laundryWashTicks >= duration) {
    s.laundryState = 2;
    notify(s,'Laundry is done washing! Move it to the dryer in the basement.');
  }
  if (s.laundryState === 3 && ++s.laundryDryTicks >= duration) {
    s.laundryState = 4;
    notify(s,'Laundry is dry! Fold and put it away in the basement.');
  }
}
function tickPhone(s) {
  s.phoneBattery = clamp(s.phoneBattery - (s.tvWatching ? 1.4 : 1));
  if (s.phoneCharging && s.phoneBattery < 100) {
    s.phoneBattery = clamp(s.phoneBattery + 4);
    s.phoneChargeTicks++;
    if (s.phoneBattery >= 100) {
      s.phoneCharging = false; s.phoneChargeTicks = 0; s.phoneChargeCount++;
      notify(s,`Phone fully charged! (${s.phoneChargeCount} charges today)`);
    }
  }
  if (s.deliveryOrdered && !s.deliveryArrived && --s.deliveryTicks <= 0) {
    s.deliveryArrived = true;
    if (s.deliveryItemType === 'Drinks') {
      s.thirst = clamp(s.thirst + 50); s.drinkArrived = true;
      notify(s,'Drinks delivered! You grabbed them at the door. Thirst way up!');
    } else notify(s,'Delivery arrived! Pick up your food in the kitchen.');
  }
  if (s.detergentOrdered && !s.detergentArrived && --s.detergentTicks <= 0) {
    s.detergentArrived = true; notify(s,'Detergent delivered! Pick it up in the basement.');
  }
  if (s.coldMedOrdered && !s.coldMedArrived && --s.coldMedTicks <= 0) {
    s.coldMedArrived = true; notify(s,'Cold medicine arrived! Open your phone to take it.');
  }
}
function tickEvents(s,rng) {
  if (!s.friendTextPending && --s.nextFriendTextIn <= 0) {
    s.friendTextPending = true; s.nextFriendTextIn = 80 + Math.floor(rng() * 60);
    notify(s,'Friend texted you! Check your phone.');
  }
}
function tickMittens(s,rng) {
  if (!s.mittensEscaped && s.catHunger <= 5 && anyWindow(s) && rng() < .08) {
    s.mittensEscaped = true; s.catHunger = 5; s.windowOpen = false;
    notify(s,'Mittens escaped through an open window! Find her outside!');
  }
  if (s.catHunger <= 20 && !s.mittensEscaped && --s.mittensChaosTimer <= 0) {
    s.mittensChaosTimer = 40 + Math.floor(rng() * 40);
    const rooms = ['LIVING_ROOM','KITCHEN','BEDROOM'];
    const target = rooms[Math.floor(rng() * 3)];
    s.mittensMessRoom = target;
    s[{LIVING_ROOM:'choreCleanLR',KITCHEN:'choreCleanKitchen',BEDROOM:'choreCleanBedroom'}[target]] = false;
    s.choreHouseClean = false;
    notify(s,`Mittens knocked things over in the ${target.replaceAll('_',' ').toLowerCase()}! Re-tidy it.`);
  }
}
export function dayBonus(s) {
  return 5 + (s.temperature < 40 ? 3 : 0) + (s.hunger > 60 ? 2 : 0) + (s.thirst > 60 ? 2 : 0) + (!s.squirrelPresent ? 1 : 0) + choreCount(s) * 4;
}
export function tick(s, rng = Math.random) {
  if (s.screen === 'NIGHTMARE') { advanceNightmare(s,.5,rng); return; }
  if (s.screen !== 'GAME' || s.gameOver || s.inNightmare) return;
  s.momTimerSeconds = Math.max(0,s.momTimerSeconds - 1);
  tickStats(s,rng); tickEnvironment(s,rng); tickRandomEvents(s,rng);
  tickChores(s); tickPhone(s); tickEvents(s,rng); tickMittens(s,rng);
  if (s.momTimerSeconds <= 0) {
    s.won = true;
    s.completedDay = s.day;
    s.lastDayBonus = dayBonus(s);
    s.coins += s.lastDayBonus;
    const count = choreCount(s);
    s.pendingAllowance = count >= s.TOTAL_CHORES ? 8 + s.day * 2 : count * 2;
    // MomArrivalPanel.onShow pays the allowance immediately, once.
    s.coins += s.pendingAllowance;
    if (s.day < s.MAX_DAYS) { s.lastDayDone = false; s.day++; }
    else s.lastDayDone = true;
    s.screen = 'MOM_ARRIVAL';
  } else if (s.stress >= 100) lose(s,'You stressed out completely before mom got home.');
  else if (s.temperature >= 90) startNightmare(s,rng);
  else if (s.hunger <= 0) lose(s,'You starved before mom got home.');
  else if (s.thirst <= 0) lose(s,'You were too dehydrated to go on.');
}
export function finishArrival(s) {
  if (s.screen !== 'MOM_ARRIVAL') return false;
  if (s.lastDayDone) {
    s.gameOverReason = 'You survived all 3 days! Mom never knew.';
    s.screen = 'GAMEOVER';
  } else s.screen = 'SHOP';
  return true;
}

export const shopItems = [
  ['fan','Portable Fan',8,'Temp rises 30% slower','hasFan'],
  ['snack','Snack Pack',6,'Hunger drops 25% slower','hasSnackPack'],
  ['bottle','Big Water Bottle',6,'Each drink restores more','hasBigBottle'],
  ['treats','Cat Treats',5,'Cat hunger 30% slower','hasCatTreats'],
  ['chill','Chill Pills',7,'Stress rises 30% slower','hasChillPills'],
  ['stool','Kitchen Step Stool',5,'Stool always in kitchen','hasKitchenStool'],
  ['roach','Roach Trap',10,'Roaches never appear','hasRoachTrap'],
  ['gen','Backup Generator',9,'Extends AC safety cooldown during outages','hasGenerator'],
  ['ice','Ice Pack',8,'One-time -15 temperature drop','hasIcePack'],
  ['soap','Laundry Detergent',5,'Laundry takes less time','hasLaundrysoap'],
  ['lamp','Study Lamp',6,'Homework in 12 ticks','hasStudyLamp'],
  ['squirt','Squirrel Spray',7,'Squirrel stress cut in half','hasSquirrelSpray'],
  ['mousetrap','Mouse Trap',6,'Clears rats from garage','hasMouseTrap'],
  ['medkit','Medicine Kit',6,'Original keepsake upgrade; use phone medicine to cure colds','hasMedicineKit'],
].map(([id,name,cost,description,flag])=>({id,name,cost,description,flag}));
const daySlots = [[0,1,2,3,4,5,6,13],[0,1,2,6,7,8,9,12],[2,3,4,5,7,10,11,13]];
export function availableShopItems(s) {
  return daySlots[clamp(s.day - 1,0,2)].map(i=>shopItems[i]);
}
export function purchase(s,id) {
  const item = availableShopItems(s).find(i=>i.id === id);
  if (s.screen !== 'SHOP' || !item || s[item.flag] || s.coins < item.cost) return false;
  s.coins -= item.cost;
  s[item.flag] = true;
  if (id === 'ice') s.temperature = clamp(s.temperature - 15);
  notify(s,`${item.name} purchased.`);
  return true;
}

function nightmareRound(s,round,rng) {
  s.nightmare = {round, numNodes:3 + round, sequence:shuffle(Array.from({length:3 + round},(_,i)=>i),rng), inputIndex:0, phase:'show', elapsed:0, litNode:-1, secondsLeft:20, wires:[]};
}
export function startNightmare(s,rng = Math.random) {
  s.inNightmare = true; s.screen = 'NIGHTMARE';
  nightmareRound(s,1,rng);
}
export function resumeAfterNightmare(s) {
  s.inNightmare = false; s.temperature = 70; s.stress = 80; s.coins += 5;
  s.screen = 'GAME'; s.currentRoom = 'LIVING_ROOM'; s.nightmare = null;
  notify(s,'You escaped the nightmare... for now. +5 coins!');
}
export function advanceNightmare(s,seconds = .5,rng = Math.random) {
  if (s.screen !== 'NIGHTMARE' || !s.nightmare) return;
  const n = s.nightmare;
  n.elapsed += seconds;
  if (n.phase === 'show') {
    const index = Math.floor((n.elapsed - .9) / .7);
    n.litNode = index >= 0 && index < n.numNodes ? n.sequence[index] : -1;
    if (n.elapsed >= .9 + n.numNodes * .7 + .6) {
      n.phase = 'input'; n.elapsed = 0; n.litNode = -1;
    }
  } else if (n.phase === 'input') {
    n.secondsLeft = Math.max(0,20 - n.elapsed);
    if (n.secondsLeft <= 0) {
      n.phase = 'failed'; n.elapsed = 0;
      n.reason = "Time's up! The demon cat strikes...";
    }
  } else if (n.phase === 'complete' && n.elapsed >= (n.round >= 3 ? 1.6 : 1.2)) {
    if (n.round >= 3) resumeAfterNightmare(s);
    else nightmareRound(s,n.round + 1,rng);
  } else if (n.phase === 'failed' && n.elapsed >= 1.6) {
    lose(s,"The Fire Demon Cat consumed your soul.\nShould've kept it cooler.");
  }
}
export function nightmareInput(s,index) {
  const n = s.nightmare;
  if (s.screen !== 'NIGHTMARE' || n?.phase !== 'input') return false;
  if (index !== n.sequence[n.inputIndex]) {
    n.phase = 'failed'; n.elapsed = 0;
    n.reason = 'Wrong node! The demon cat claims you...';
    return false;
  }
  if (n.inputIndex > 0) n.wires.push([n.sequence[n.inputIndex - 1],index]);
  n.inputIndex++;
  n.litNode = index;
  if (n.inputIndex === n.numNodes) {
    n.wires.push([n.sequence[n.numNodes - 1],n.sequence[0]]);
    n.phase = 'complete'; n.elapsed = 0;
  }
  return true;
}
