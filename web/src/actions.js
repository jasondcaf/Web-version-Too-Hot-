/** Room interactions transcribed from the Swing panel event handlers.
 * State names intentionally match GameState.java for parity review.
 */
export const rooms = [
  { id: 'LIVING_ROOM', title: 'Living Room', art: 'living_room' },
  { id: 'KITCHEN', title: 'Kitchen', art: 'kitchen' },
  { id: 'GARAGE', title: 'Garage', art: 'garage' },
  { id: 'BEDROOM', title: 'Bedroom', art: 'bedroom' },
  { id: 'OUTSIDE', title: 'Backyard', art: 'outdoor' },
  { id: 'MOMS_ROOM', title: "Mom’s Room", art: 'moms_room' },
  { id: 'BATHROOM', title: 'Bathroom', art: 'bathroom' },
  { id: 'BASEMENT', title: 'Laundry Room', art: 'laundry_room' },
  { id: 'PHONE', title: 'Phone', art: null },
];

export const deliveryItems = [
  { id: 'Food', cost: 3, ticks: 20 },
  { id: 'Drinks', cost: 2, ticks: 16 },
  { id: 'Cold Medicine', cost: 2, ticks: 15 },
  { id: 'Bluetooth Speaker', cost: 4, ticks: 0 },
  { id: 'Detergent', cost: 2, ticks: 20 },
];

const clamp = v => Math.max(0, Math.min(100, v));
const add = (s, key, amount) => { s[key] = clamp(s[key] + amount); };
const entry = (id, label, enabled = true, category = 'action') => ({ id, label, enabled, category });
const toggle = (s, key, open, close) => s[key] ? close : open;
const changeKeys = {
  LIVING_ROOM: ['LivingRoom', 'Check Cushions'], KITCHEN: ['Kitchen', 'Junk Drawer'],
  BEDROOM: ['Bedroom', 'Check Under Bed'], GARAGE: ['Garage', 'Search Workbench'],
  OUTSIDE: ['Outside', 'Search the Yard'], MOMS_ROOM: ['MomsRoom', 'Check Nightstand'],
  BATHROOM: ['Bathroom', 'Check Cabinet Shelf'],
};

/** A fresh descriptor list keeps controls in sync with timed changes. */
export function getActions(s, room = s.currentRoom) {
  const a = [];
  const push = (...args) => a.push(entry(...args));
  if (s.hidingInCloset && room === 'MOMS_ROOM') return [entry('leave-closet', 'Come Out of Closet')];
  switch (room) {
    case 'LIVING_ROOM':
      if (s.squirrelPresent) push('shoo', `SHOO ${s.squirrelCount} SQUIRREL(S)!`, true, 'danger');
      else {
        push('tv', toggle(s, 'tvWatching', 'Watch TV / Chill', 'Stop Watching TV'));
        push('vacuum', s.choreVacuum ? 'Vacuumed ✓' : 'Vacuum', !s.choreVacuum, 'chore');
        if (s.mittensMessRoom === room) push('clean-mess', 'Clean Mittens’ Mess');
      }
      push('window', s.windowBroken ? `Window Broken (${Math.ceil(s.windowBrokenCooldown / 2)}s)` : toggle(s, 'windowOpen', 'Open Window', 'Close Window'), !s.windowBroken);
      push('tidy-living', 'Tidy Living Room' + (s.choreCleanLR ? ' ✓' : ''), !s.choreCleanLR, 'chore');
      push('fix-ac', s.acBroken ? (s.hasWrench ? 'Fix AC (wrench)' : 'AC Broken — need wrench') : 'AC Running ✓', s.acBroken && s.hasWrench, 'repair');
      if (!s.acBroken && !s.powerOut) ['OFF', 'Eco', 'Normal', 'BLAST'].forEach((n, i) => push(`ac-${i}`, `AC: ${n}${s.acSetting === i ? ' ✓' : ''}`, s.acSetting !== i, 'repair'));
      break;
    case 'KITCHEN':
      push('drink-kitchen', 'Drink Water' + (!(s.hasStepStool || s.hasKitchenStool) ? ' — need stool' : ''), s.hasStepStool || s.hasKitchenStool, 'care');
      push('dishes', s.choreDishes ? 'Dishes ✓' : 'Wash Dishes', !s.choreDishes && (s.hasStepStool || s.hasKitchenStool), 'chore');
      push('sweep', s.choreSweepKitchen ? 'Swept ✓' : 'Sweep Kitchen', !s.choreSweepKitchen, 'chore');
      push('feed-kitchen', s.catFoodScoops>0?`Cat Food (${s.catFoodScoops})`:'Cat Food Empty', s.catFoodScoops > 0, 'care');
      push('window-kitchen', toggle(s, 'kitchenWindowOpen', 'Open Kitchen Window', 'Close Kitchen Window'));
      if (s.choreTrashOut && !s.choreTrashBagIn) push('trash-bag', 'Put Trash Bag In', true, 'chore');
      if (s.deliveryArrived && s.hunger < 95) push('pickup-food', 'Pick Up Delivery!', true, 'care');
      else if (!s.iceCreamFound) push('freezer', 'Search Freezer');
      else push('ice-cream', s.iceCreamEaten ? 'Ice Cream Gone ✓' : 'Eat Ice Cream!', !s.iceCreamEaten, 'care');
      push('microwave', s.powerOut ? 'Food Spoiled — power out' : s.foodMicrowaved ? 'Eat Heated Food' : 'Microwave Food', !s.powerOut, 'care');
      if (!s.powerOut) push('eat-cold', 'Eat Cold Leftovers (less)', true, 'care');
      push('tidy-kitchen', 'Tidy Kitchen' + (s.choreCleanKitchen ? ' ✓' : ''), !s.choreCleanKitchen, 'chore');
      break;
    case 'BEDROOM':
      push('feed-bedroom', s.catFoodScoops<=0?'No Cat Food Left':s.catHunger >= 90 ? 'Mittens Is Full' : `Feed Mittens (${s.catFoodScoops})`, s.catFoodScoops > 0 && s.catHunger < 90, 'care');
      push('window-bedroom', toggle(s, 'bedroomWindowOpen', 'Open Bedroom Window', 'Close Bedroom Window'));
      push('charge', s.phoneCharging ? `Unplug Phone (${Math.floor(s.phoneBattery)}%)` : s.phoneBattery >= 100 ? 'Phone Fully Charged ✓' : 'Charge Phone' + (s.phoneChargeCount >= 3 ? ' — high power risk!' : ''), s.phoneCharging || s.phoneBattery < 100);
      push('rest-bedroom', 'Rest on Bed', true, 'care');
      if (s.needsTowelDry) push('dry-off', 'Dry Off (towel)', true, 'care');
      else if (s.needsClothesChange) push('change-clothes', 'Change Clothes', true, 'care');
      else push('nap', s.napping ? `Napping (${Math.ceil(s.napTicksLeft / 2)}s)` : 'Take a Nap', !s.napping, 'care');
      push('homework', s.choreHomework ? 'Homework ✓' : `Do Homework (${Math.min(100, Math.floor(s.choreHomeworkTicks / (s.hasStudyLamp ? 12 : 20) * 100))}%)`, !s.choreHomework, 'chore');
      push('stool', s.stepStoolInBedroom ? 'Grab Step Stool' : 'Stool: Carrying ✓', s.stepStoolInBedroom);
      push('tidy-bedroom', 'Tidy Bedroom' + (s.choreCleanBedroom ? ' ✓' : ''), !s.choreCleanBedroom, 'chore');
      break;
    case 'GARAGE':
      push('wrench', s.hasWrench ? 'Wrench: Carrying ✓' : s.roachesCleared ? 'Pick Up Wrench' : 'Wrench — clear roaches first', s.wrenchInGarage && s.roachesCleared);
      if (s.roachesPresent && !s.roachesCleared) push('roaches', s.hasRoachSpray ? 'Use Roach Spray!' : 'Roaches — need spray from Mom’s room', s.hasRoachSpray, 'repair');
      else if (s.ratsPresent && !s.ratsClearedToday) push('rats', s.hasMouseTrap ? 'Place Mouse Trap!' : 'RATS — buy a Mouse Trap in shop', s.hasMouseTrap, 'repair');
      else if(s.roachesCleared)push('roaches-cleared','Roaches ✓ Cleared',false,'repair');
      if (s.powerOut) push('circuit', 'Open Fuse Box', true, 'repair');
      break;
    case 'MOMS_ROOM':
      push('spray', s.roachSprayInDrawer ? 'Search Dresser Drawer' : 'Drawer Checked ✓', s.roachSprayInDrawer);
      if (s.powerOut || s.hasMomFlashlight) push('flashlight', s.hasMomFlashlight ? 'Flashlight: Carrying ✓' : 'Search Bottom Drawer (flashlight)', !s.hasMomFlashlight);
      else push('rest-mom', 'Rest on Mom’s Bed', true, 'care');
      push('closet', 'Hide in Mom’s Closet', true, 'care');
      break;
    case 'BATHROOM':
      push('shower', s.showeredToday ? 'Showered ✓ — dry off in bedroom' : 'Take a Cold Shower', !s.showeredToday, 'care');
      push('drink-bathroom', 'Drink From Sink', true, 'care');
      push('medicine-cabinet', s.medicineUsed ? 'Cabinet Used Today ✓' : 'Medicine Cabinet', !s.medicineUsed, 'care');
      push('mop', s.choreMopBathroom ? 'Mopped ✓' : 'Mop Bathroom', !s.choreMopBathroom, 'chore');
      break;
    case 'BASEMENT':
      switch (s.laundryState) {
        case 0: push('washer', s.laundryDetergent>0?`Put Laundry in Washer (soap: ${s.laundryDetergent})`:'Out of Detergent! Order via phone (2 coins)', s.laundryDetergent > 0, 'chore'); break;
        case 1: push('washer', `Washer Running (${Math.min(100, Math.floor(s.laundryWashTicks / (s.hasLaundrysoap ? 7 : 10) * 100))}%)`, false, 'chore'); break;
        case 2: push('dryer', 'Move to Dryer!', true, 'chore'); break;
        case 3: push('dryer', `Dryer Running (${Math.min(100, Math.floor(s.laundryDryTicks / (s.hasLaundrysoap ? 7 : 10) * 100))}%)`, false, 'chore'); break;
        case 4: push('fold', 'Fold & Put Away Clothes!', true, 'chore'); break;
        case 5: push('fold', 'Laundry Done ✓', false, 'chore'); if (s.dirtyClothesAdded) push('new-load', 'Wash New Load', true, 'chore'); break;
      }
      if (s.detergentArrived) push('pickup-detergent', 'Pick Up Detergent (+2 uses)');
      break;
    case 'OUTSIDE':
      push('trash', s.choreTrashOut ? 'Trash Out ✓ — new bag in kitchen' : 'Take Out Trash', !s.choreTrashOut, 'chore');
      push('mail', s.choreMailIn ? 'Mail In ✓' : 'Bring In Mail', !s.choreMailIn, 'chore');
      if (s.mittensEscaped) push('find-mittens', 'Find Mittens!', true, 'care');
      if (s.choreLawnMowed) push('start-mowing', 'Lawn Mowed ✓', false, 'chore');
      else if(s.thunderstorm)push('start-mowing','Can’t Mow — Thunderstorm!',false,'chore');
      else if (!s.mowingActive) push('start-mowing', s.thunderstorm ? 'Can’t Mow — Thunderstorm!' : `Start Mowing (${s.choreLawnPct}%)`, !s.thunderstorm, 'chore');
      else { push('mow', `MOW! (${s.choreLawnPct}%)`, !s.thunderstorm, 'chore'); push('stop-mowing', 'Stop Mowing'); }
      push('sprinkler', s.sprinklerUsed ? 'Sprinkler Used ✓' : 'Run Through Sprinkler', !s.sprinklerUsed, 'care');
      push('plants', s.choreWaterPlants ? 'Outdoor Plants ✓' : 'Water Outdoor Plants', !s.choreWaterPlants, 'chore');
      break;
    case 'PHONE': return phoneActions(s);
  }
  const change = changeKeys[room];
  if (change && !s[`looseChangeFound_${change[0]}`] && !(room === 'KITCHEN' && s.choreTrashOut && !s.choreTrashBagIn)) push(`change-${change[0]}`, change[1]);
  return a;
}

export function phoneActions(s) {
  const power = s.phoneBattery > 5;
  const a = [entry('charge', s.phoneCharging ? 'Unplug Phone' : `Charge Phone${s.phoneChargeCount >= 3 ? ' — overload risk!' : ''}`, s.phoneCharging || s.phoneBattery < 100)];
  [['call-mom', 'Call Mom (6% battery)'], ['text-mom', 'Text Mom (2%)'], ['call-friend', 'Call Friend (8%)']].forEach(([id, label]) => a.push(entry(id, label, power, 'messages')));
  if (s.friendTextPending) {
    a.push(entry('reply-friend', 'Reply to Text (3%)', power, 'messages'), entry('ignore-friend', 'Ignore Text', true, 'messages'));
  } else a.push(entry('text-friend', 'Text Friend (2%)', power, 'messages'));
  // The original Delivery app uses a single-order gate for all products.
  const canOrder = power && !s.deliveryOrdered && !s.coldMedOrdered && !s.hasSpeaker && !s.detergentOrdered;
  deliveryItems.forEach(item => a.push(entry(`order-${item.id}`, `Order ${item.id} (${item.cost} coins)`, canOrder && s.coins >= item.cost, 'delivery')));
  if (s.coldMedArrived) a.push(entry('take-cold-medicine', 'Take Cold Medicine', true, 'delivery'));
  ['Pop', 'Hip-Hop', 'Classical', 'Lo-fi'].forEach(genre => a.push(entry(`music-${genre}`, `${s.musicPlaying && s.musicGenre === genre ? 'Stop' : 'Play'} ${genre}`, power, 'music')));
  a.push(entry('flappy', 'Play Flappy Bird', power, 'games'));
  return a;
}

export function navigate(s, room) {
  if (!rooms.some(r => r.id === room)) return false;
  if (room === 'PHONE' && s.currentRoom !== 'PHONE') s.roomBeforePhone = s.currentRoom;
  s.currentRoom = room;
  return true;
}

/** Returns effects for the view; gameplay state remains in the supplied object. */
export function executeAction(s, id, payload = {}, rng = Math.random) {
  if (s.gameOver || s.inNightmare || (s.screen && s.screen !== 'GAME')) return { ok: false, message: 'Finish the current screen first.' };
  const internal = ['dishes-hit', 'roaches-result', 'circuit-result', 'flappy-flap'];
  if (!internal.includes(id)) {
    const action = getActions(s).find(a => a.id === id);
    if (!action || !action.enabled) return { ok: false, message: 'That action is not available here yet.' };
  }
  let message = '', minigame, complete = false;
  if (Object.values(changeKeys).some(([key]) => id === `change-${key}`)) {
    const key = id.slice(7), flag = `looseChangeFound_${key}`;
    const found = Math.floor(rng() * 6); s[flag] = true; s.coins += found;
    message = found ? `Found ${found} coin(s)!` : 'Nothing here today.';
  } else if (/^ac-[0-3]$/.test(id)) {
    s.acSetting = Number(id.slice(3)); message = `AC set to ${['OFF', 'Eco', 'Normal', 'BLAST'][s.acSetting]}.${s.acSetting === 3 ? ' Warning: high power-out risk!' : ''}`;
  } else if (id.startsWith('order-')) {
    const item = deliveryItems.find(i => i.id === id.slice(6)); s.coins -= item.cost; add(s, 'phoneBattery', -4);
    if (item.id === 'Cold Medicine') { s.coldMedOrdered = true; s.coldMedTicks = item.ticks; }
    else if (item.id === 'Detergent') { s.detergentOrdered = true; s.detergentTicks = item.ticks; }
    else if (item.id === 'Bluetooth Speaker') s.hasSpeaker = true;
    else { s.deliveryOrdered = true; s.deliveryItemType = item.id; s.deliveryTicks = item.ticks; }
    message = `${item.id} ordered!${item.ticks ? ` Arrives in ~${Math.ceil(item.ticks / 2)}s.` : ' Music now has a speaker boost.'}`;
  } else if (id.startsWith('music-')) {
    const genre = id.slice(6);
    if (s.musicPlaying && s.musicGenre === genre) { s.musicPlaying = false; s.musicGenre = ''; message = 'Music stopped.'; }
    else { s.musicPlaying = true; s.musicGenre = genre; add(s, 'phoneBattery', -2); message = `Now playing ${genre}${s.hasSpeaker ? ' — speaker boost!' : '.'}`; }
  } else switch (id) {
    case 'window': s.windowOpen = !s.windowOpen; if (!s.windowOpen) s.windowOpenTicks = 0; message = s.windowOpen ? 'Window open — cool breeze, but watch for squirrels!' : 'Window closed.'; break;
    case 'window-kitchen': s.kitchenWindowOpen = !s.kitchenWindowOpen; message = `Kitchen window ${s.kitchenWindowOpen ? 'open' : 'closed'}.`; break;
    case 'window-bedroom': s.bedroomWindowOpen = !s.bedroomWindowOpen; message = `Bedroom window ${s.bedroomWindowOpen ? 'open' : 'closed'}.`; break;
    case 'tv': s.tvWatching = !s.tvWatching; message = s.tvWatching ? 'Chilling in front of the TV. Stress fading…' : 'Turned off the TV.'; break;
    case 'vacuum': s.choreVacuum = true; s.coins++; message = 'Vacuumed! +1 coin.'; break;
    case 'sweep': s.choreSweepKitchen = true; s.coins++; message = 'Kitchen swept! +1 coin.'; break;
    case 'mop': s.choreMopBathroom = true; s.coins++; message = 'Bathroom mopped! +1 coin.'; break;
    case 'mail': s.choreMailIn = true; s.coins++; message = 'Mail’s in! +1 coin.'; break;
    case 'plants': s.choreWaterPlants = true; s.coins++; message = 'Outdoor plants watered! +1 coin.'; break;
    case 'tidy-living': s.choreCleanLR = true; message = 'Living room is tidy!'; break;
    case 'tidy-kitchen': s.choreCleanKitchen = true; message = 'Kitchen is spotless!'; break;
    case 'tidy-bedroom': s.choreCleanBedroom = true; message = 'Bedroom is spotless!'; break;
    case 'clean-mess': s.mittensMessRoom = ''; message = 'Cleaned Mittens’ mess. Now re-tidy the room!'; break;
    case 'shoo': { const count = s.squirrelCount; s.squirrelPresent = false; s.squirrelCount = 1; s.squirrelTicks = 0; add(s, 'stress', 10 + count * 5); s.coins++; message = 'Squirrels shoo’d! +1 coin, but that was stressful.'; break; }
    case 'fix-ac': s.acBroken = false; s.hasWrench = false; s.coins += 3; message = 'AC fixed! +3 coins.'; break;
    case 'drink-kitchen': add(s, 'thirst', s.hasBigBottle ? 35 : 22); add(s, 'temperature', -3); message = 'Drank some water. Refreshing!'; break;
    case 'drink-bathroom': add(s, 'thirst', s.hasBigBottle ? 30 : 18); message = 'Drank from the bathroom sink.'; break;
    case 'feed-kitchen': s.catFoodScoops--; add(s, 'catHunger', 40); message = `Fed Mittens! ${s.catFoodScoops} scoops left.`; break;
    case 'feed-bedroom': s.catFoodScoops--; add(s, 'catHunger', 50); s.coins += 2; message = 'Mittens purrs. Good human. +2 coins!'; break;
    case 'trash': s.choreTrashOut = true; message = 'Trash is out! Put a new bag in the kitchen bin.'; break;
    case 'trash-bag': s.choreTrashBagIn = true; message = 'New trash bag in! Chore complete.'; break;
    case 'pickup-food': add(s, 'hunger', 40); s.deliveryArrived = false; message = 'Ate the delivery. Hunger restored!'; break;
    case 'freezer': s.iceCreamFound = true; message = 'Found ice cream hidden in the freezer!'; break;
    case 'ice-cream': s.iceCreamEaten = true; add(s, 'hunger', 25); add(s, 'thirst', 10); add(s, 'temperature', -5); add(s, 'stress', -8); message = 'Ice cream! Heat and stress down a bit.'; break;
    case 'microwave': if (!s.foodMicrowaved) { s.foodMicrowaved = true; message = 'Beep! Food’s hot. Now eat it.'; } else { add(s, 'hunger', 32); s.foodMicrowaved = false; message = 'Hot food! Hunger restored.'; } break;
    case 'eat-cold': add(s, 'hunger', 18); add(s, 'stress', 5); s.foodMicrowaved = false; message = 'Cold leftovers… less satisfying. Stress slightly up.'; break;
    case 'charge': s.phoneCharging = !s.phoneCharging; message = s.phoneCharging ? `Phone charging.${s.phoneChargeCount >= 3 ? ' WARNING: high power-out risk!' : ''}` : 'Phone unplugged.'; break;
    case 'rest-bedroom': add(s, 'temperature', -4); add(s, 'stress', -10); message = 'A quick rest. Heat and stress down a bit.'; break;
    case 'rest-mom': add(s, 'temperature', -3); add(s, 'stress', -18); message = 'Mom’s bed is SO comfy. Stress way down.'; break;
    case 'dry-off': s.needsTowelDry = false; s.needsClothesChange = true; message = 'Dried off! Change into clean clothes.'; break;
    case 'change-clothes': s.needsClothesChange = false; s.dirtyClothesAdded = true; message = 'Changed! Old clothes are in the basement basket.'; break;
    case 'nap': s.napping = true; s.napTicksLeft = 20; message = 'Napping… Auto-wake in 10s.'; break;
    case 'homework': message = 'Stay in the bedroom — homework fills automatically!'; break;
    case 'stool': s.hasStepStool = true; s.stepStoolInBedroom = false; message = 'Got the step stool! You can reach the kitchen sink.'; break;
    case 'wrench': s.hasWrench = true; s.wrenchInGarage = false; message = 'Got the wrench! Fix the AC in the living room.'; break;
    case 'spray': s.hasRoachSpray = true; s.roachSprayInDrawer = false; message = 'Found roach spray! Head to the garage.'; break;
    case 'flashlight': s.hasMomFlashlight = true; message = 'Found a flashlight! Much less scary.'; break;
    case 'closet': s.hidingInCloset = true; message = 'Hiding in Mom’s closet. Stress fading…'; break;
    case 'leave-closet': s.hidingInCloset = false; message = 'Came out of the closet.'; break;
    case 'shower': add(s, 'temperature', -22); add(s, 'stress', -10); add(s, 'thirst', 6); s.showeredToday = true; s.needsTowelDry = true; message = 'Cold shower! Dry off in the bedroom.'; break;
    case 'medicine-cabinet': add(s, 'stress', -28); s.medicineUsed = true; message = 'Feeling much calmer.'; break;
    case 'washer': s.laundryState = 1; s.laundryWashTicks = 0; s.laundryDetergent--; message = 'Washer running! Come back when it’s done.'; break;
    case 'dryer': s.laundryState = 3; s.laundryDryTicks = 0; message = 'Dryer running! Come back when it’s done.'; break;
    case 'fold': s.laundryState = 5; message = 'Laundry folded and put away! Chore complete.'; break;
    case 'new-load': s.laundryState = 0; s.laundryWashTicks = 0; s.laundryDryTicks = 0; s.dirtyClothesAdded = false; message = 'Ready to wash the new dirty clothes.'; break;
    case 'pickup-detergent': s.laundryDetergent += 2; s.detergentOrdered = false; s.detergentArrived = false; message = 'Detergent picked up! +2 washes.'; break;
    case 'take-cold-medicine': if (s.hasCold) { s.hasCold = false; s.coldMedArrived = false; } message = 'Took the cold medicine. Feeling better!'; break;
    case 'rats': s.ratsPresent = false; s.ratsClearedToday = true; s.hasMouseTrap = false; s.coins += 3; message = 'Rats gone! +3 coins.'; break;
    case 'find-mittens': s.mittensEscaped = false; add(s, 'catHunger', 10); message = 'Found Mittens under the porch!'; break;
    case 'sprinkler': add(s, 'temperature', -14); add(s, 'thirst', 10); s.sprinklerUsed = true; message = 'Ran through the sprinkler! Temperature way down.'; break;
    case 'start-mowing': if (s.temperature >= 82) return { ok: false, message: 'Too hot to mow! Cool down first.' }; s.mowingActive = true; s.mowStreak = 0; message = 'Started mowing. Each pass heats you up!'; break;
    case 'stop-mowing': s.mowingActive = false; s.mowStreak = 0; message = `Stopped mowing. Lawn at ${s.choreLawnPct}%.`; break;
    case 'mow': {
      if (s.temperature >= 85) { s.mowingActive = false; s.mowStreak = 0; return { ok: false, message: 'Too hot! You had to stop mowing. Cool down first.' }; }
      s.choreLawnPct = Math.min(100, s.choreLawnPct + 12.5); add(s, 'temperature', 4); s.mowStreak = (s.mowStreak || 0) + 1;
      message = `Mowing… ${s.choreLawnPct}% done. Temperature +4.`;
      if (s.mowStreak >= 5 && rng() < 0.35) {
        const event = Math.floor(rng() * 3);
        if (event === 0) { s.squirrelPresent = true; s.squirrelTicks = 0; message += ' A squirrel ran out of the grass!'; }
        else if (event === 1) { add(s, 'stress', 20); message += ' Mower hit a rock! Stress +20.'; }
        else { add(s, 'thirst', -15); message += ' Getting dehydrated! Thirst -15.'; }
        s.mowStreak = 0;
      }
      if (s.choreLawnPct >= 100) { s.choreLawnMowed = true; s.mowingActive = false; s.mowStreak = 0; message = 'Lawn’s done! +4 coins at day end.'; }
      break;
    }
    case 'call-mom': add(s, 'phoneBattery', -6); add(s, 'stress', -5); message = 'Called Mom. She says be good!'; break;
    case 'text-mom': add(s, 'phoneBattery', -2); add(s, 'stress', -2); message = 'Mom replied with a heart emoji.'; break;
    case 'call-friend': add(s, 'phoneBattery', -8); add(s, 'stress', -15); if (s.friendTextPending) { s.friendTextPending = false; s.friendTextIgnores = 0; } message = 'Called your friend! Big stress drop.'; break;
    case 'reply-friend': s.friendTextPending = false; s.friendTextIgnores = 0; add(s, 'phoneBattery', -3); add(s, 'stress', -8); message = 'Replied to your friend! Stress down.'; break;
    case 'text-friend': add(s, 'phoneBattery', -2); add(s, 'stress', -3); message = 'Your friend sent a meme. Stress down.'; break;
    case 'ignore-friend': s.friendTextPending = false; s.friendTextIgnores++; message = 'Ignored… guilt creeping in.'; break;
    case 'dishes': s.dishScrubs = 0; s.dishMisses = 0; minigame = 'dishes'; message = 'Hit SCRUB when the needle is in the green zone!'; break;
    case 'dishes-hit':
      if (s.currentRoom !== 'KITCHEN' || s.choreDishes || !(s.hasStepStool || s.hasKitchenStool)) return { ok: false, message: 'Dishes are unavailable.' };
      if (payload.needle >= 0.35 && payload.needle <= 0.65) { s.dishScrubs++; message = 'NICE!'; }
      else { s.dishMisses++; message = 'MISS!'; if (s.dishMisses > s.dishScrubs) { s.dishScrubs = 0; s.dishMisses = 0; message = 'Too many misses! Starting over…'; } }
      if (s.dishScrubs >= 4) { s.choreDishes = true; complete = true; message = 'Dishes clean! +4 coins at day end.'; }
      break;
    case 'roaches': minigame = 'roaches'; message = 'Tap all 6 roaches in 12 seconds!'; break;
    case 'roaches-result':
      if (s.currentRoom !== 'GARAGE' || !s.hasRoachSpray || s.roachesCleared) return { ok: false, message: 'Roaches are unavailable.' };
      if (payload.success) { s.roachesPresent = false; s.roachesCleared = true; s.roachesClearedEver = true; s.hasRoachSpray = false; s.coins += 2; message = 'Roaches cleared! +2 coins. Wrench accessible.'; }
      else message = 'Ran out of time! Roaches scattered… try again.';
      break;
    case 'circuit': minigame = 'circuit'; message = 'Watch the circuit, then repeat the sequence!'; break;
    case 'circuit-result': if (s.currentRoom !== 'GARAGE' || !s.powerOut) return { ok: false, message: 'Power is already on.' }; if (payload.success) { s.powerOut = false; s.coins += 3; message = 'Fuse box fixed! Power’s back. +3 coins.'; } else message = 'Fuse box closed.'; break;
    case 'flappy': minigame = 'flappy'; message = 'Tap the game to flap!'; break;
    case 'flappy-flap': if (s.currentRoom !== 'PHONE') return { ok: false, message: 'Open your phone first.' }; add(s, 'phoneBattery', -0.5); break;
    default: return { ok: false, message: 'Unknown action.' };
  }
  return { ok: true, message, ...(minigame ? { minigame } : {}), complete };
}
