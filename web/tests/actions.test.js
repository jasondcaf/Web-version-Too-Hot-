import test from 'node:test';
import assert from 'node:assert/strict';
import { createGame, tick, resetForDay, choreCount } from '../src/engine.js';
import { rooms, getActions, executeAction, navigate } from '../src/actions.js';

const safeRandom = () => 0.99;
function game() { const s = createGame(safeRandom); s.screen = 'GAME'; return s; }
function act(s, room, id, payload) { navigate(s, room); return executeAction(s, id, payload, safeRandom); }
function ticks(s, n) { for (let i = 0; i < n; i++) tick(s, safeRandom); }

test('all eight rooms and phone expose reachable controls', () => {
  const s = game();
  assert.equal(rooms.length, 9);
  for (const room of rooms) { assert.ok(navigate(s, room.id)); assert.ok(getActions(s).length); }
  assert.equal(navigate(s, 'INVALID'), false);
});

test('original spray → roaches → wrench → AC chain and consumption', () => {
  const s = game(); s.acBroken = true;
  assert.equal(act(s, 'GARAGE', 'wrench').ok, false);
  assert.equal(act(s, 'GARAGE', 'roaches').ok, false);
  act(s, 'MOMS_ROOM', 'spray');
  assert.equal(s.hasRoachSpray, true);
  assert.equal(act(s, 'GARAGE', 'roaches').minigame, 'roaches');
  act(s, 'GARAGE', 'roaches-result', { success: false });
  assert.equal(s.hasRoachSpray, true);
  assert.equal(s.roachesCleared, false);
  act(s, 'GARAGE', 'roaches-result', { success: true });
  assert.equal(s.roachesClearedEver, true);
  assert.equal(s.hasRoachSpray, false);
  assert.equal(s.coins, 8);
  s.ratsPresent = true; // rats do not gate the wrench in the original
  act(s, 'GARAGE', 'wrench');
  act(s, 'LIVING_ROOM', 'fix-ac');
  assert.equal(s.acBroken, false);
  assert.equal(s.hasWrench, false);
  assert.equal(s.coins, 11);
  s.day = 2; resetForDay(s, safeRandom);
  assert.equal(s.roachesCleared, true);
  assert.equal(s.ratsPresent, true);
  assert.equal(s.wrenchInGarage, true);
});

test('step stool gates sink and dishes; four hits complete with miss reset', () => {
  const s = game(); s.thirst = 30; s.temperature = 50;
  assert.equal(act(s, 'KITCHEN', 'drink-kitchen').ok, false);
  assert.equal(act(s, 'KITCHEN', 'dishes').ok, false);
  act(s, 'BEDROOM', 'stool');
  act(s, 'KITCHEN', 'drink-kitchen');
  assert.equal(s.thirst, 52); assert.equal(s.temperature, 47);
  act(s, 'KITCHEN', 'dishes');
  act(s, 'KITCHEN', 'dishes-hit', { needle: 0.5 });
  act(s, 'KITCHEN', 'dishes-hit', { needle: 0 });
  act(s, 'KITCHEN', 'dishes-hit', { needle: 0 });
  assert.equal(s.dishScrubs, 0); assert.equal(s.dishMisses, 0);
  for (const needle of [0.35, 0.65, 0.5, 0.5]) act(s, 'KITCHEN', 'dishes-hit', { needle });
  assert.equal(s.choreDishes, true);
  assert.equal(act(s, 'KITCHEN', 'dishes-hit', { needle: 0.5 }).ok, false);
});

test('six chores run through timing, interactions, and full laundry lifecycle', () => {
  const s = game();
  act(s, 'BEDROOM', 'stool');
  ticks(s, 20); assert.equal(s.choreHomework, true);
  act(s, 'KITCHEN', 'dishes');
  for (let i = 0; i < 4; i++) act(s, 'KITCHEN', 'dishes-hit', { needle: 0.5 });
  act(s, 'OUTSIDE', 'trash');
  assert.equal(choreCount(s), 2); // trash isn't finished without a new bag
  act(s, 'KITCHEN', 'trash-bag');
  act(s, 'KITCHEN', 'tidy-kitchen'); act(s, 'BEDROOM', 'tidy-bedroom'); act(s, 'LIVING_ROOM', 'tidy-living');
  act(s, 'OUTSIDE', 'start-mowing');
  for (let i = 0; i < 8; i++) act(s, 'OUTSIDE', 'mow');
  assert.equal(s.choreLawnMowed, true);
  act(s, 'BASEMENT', 'washer'); assert.equal(s.laundryDetergent, 1);
  assert.equal(act(s, 'BASEMENT', 'fold').ok, false);
  ticks(s, 10); assert.equal(s.laundryState, 2);
  act(s, 'BASEMENT', 'dryer'); ticks(s, 10); assert.equal(s.laundryState, 4);
  act(s, 'BASEMENT', 'fold'); assert.equal(choreCount(s), 6);
});

test('shower → dry → change → extra laundry and daily use restrictions', () => {
  const s = game(); s.temperature = 70; s.stress = 70; s.thirst = 50;
  act(s, 'BATHROOM', 'shower');
  assert.equal(s.temperature, 48); assert.equal(s.stress, 60); assert.equal(s.thirst, 56);
  assert.equal(act(s, 'BATHROOM', 'shower').ok, false);
  assert.equal(act(s, 'BEDROOM', 'change-clothes').ok, false);
  act(s, 'BEDROOM', 'dry-off'); act(s, 'BEDROOM', 'change-clothes');
  assert.equal(s.dirtyClothesAdded, true);
  s.laundryState = 5;
  act(s, 'BASEMENT', 'new-load');
  assert.equal(s.laundryState, 0); assert.equal(s.dirtyClothesAdded, false);
  act(s, 'BATHROOM', 'medicine-cabinet'); assert.equal(s.stress, 32);
  assert.equal(act(s, 'BATHROOM', 'medicine-cabinet').ok, false);
});

test('heated and cold food and ice cream preserve resource values', () => {
  const s = game(); s.hunger = 10; s.stress = 30; s.temperature = 60; s.thirst = 20;
  act(s, 'KITCHEN', 'microwave'); assert.equal(s.hunger, 10);
  act(s, 'KITCHEN', 'microwave'); assert.equal(s.hunger, 42); assert.equal(s.foodMicrowaved, false);
  act(s, 'KITCHEN', 'eat-cold'); assert.equal(s.hunger, 60); assert.equal(s.stress, 35);
  act(s, 'KITCHEN', 'freezer'); act(s, 'KITCHEN', 'ice-cream');
  assert.equal(s.hunger, 85); assert.equal(s.temperature, 55); assert.equal(s.stress, 27); assert.equal(s.thirst, 30);
  s.powerOut = true; assert.equal(act(s, 'KITCHEN', 'microwave').ok, false);
});

test('seven room searches and five bonus chores reward once each', () => {
  const s = game();
  for (const [room, key] of [['LIVING_ROOM', 'LivingRoom'], ['KITCHEN', 'Kitchen'], ['GARAGE', 'Garage'], ['BEDROOM', 'Bedroom'], ['OUTSIDE', 'Outside'], ['MOMS_ROOM', 'MomsRoom'], ['BATHROOM', 'Bathroom']]) {
    navigate(s, room); executeAction(s, `change-${key}`, {}, () => 0.5);
    assert.equal(executeAction(s, `change-${key}`, {}, () => 0.5).ok, false);
  }
  assert.equal(s.coins, 27);
  for (const [room, id] of [['LIVING_ROOM', 'vacuum'], ['KITCHEN', 'sweep'], ['OUTSIDE', 'mail'], ['OUTSIDE', 'plants'], ['BATHROOM', 'mop']]) {
    act(s, room, id); assert.equal(act(s, room, id).ok, false);
  }
  assert.equal(s.coins, 32); assert.equal(choreCount(s), 0);
});

test('mowing enforces start and stop heat thresholds and storm restrictions', () => {
  const s = game(); s.temperature = 82;
  assert.equal(act(s, 'OUTSIDE', 'start-mowing').ok, false);
  s.temperature = 81; act(s, 'OUTSIDE', 'start-mowing'); act(s, 'OUTSIDE', 'mow');
  assert.equal(s.temperature, 85); assert.equal(s.choreLawnPct, 12.5);
  assert.equal(act(s, 'OUTSIDE', 'mow').ok, false); assert.equal(s.mowingActive, false);
  s.temperature = 25; s.thunderstorm = true;
  assert.equal(act(s, 'OUTSIDE', 'start-mowing').ok, false);
  s.thunderstorm = false; act(s, 'OUTSIDE', 'sprinkler');
  assert.equal(s.temperature, 11); assert.equal(act(s, 'OUTSIDE', 'sprinkler').ok, false);
});

test('bedroom feeding grants 50 and two coins; kitchen grants 40 without reward', () => {
  const s = game();
  assert.equal(act(s, 'BEDROOM', 'feed-bedroom').ok, false);
  s.catHunger = 30; act(s, 'BEDROOM', 'feed-bedroom');
  assert.equal(s.catHunger, 80); assert.equal(s.coins, 8);
  act(s, 'KITCHEN', 'feed-kitchen'); assert.equal(s.catHunger, 100); assert.equal(s.coins, 8);
  act(s, 'KITCHEN', 'feed-kitchen'); assert.equal(s.catFoodScoops, 0);
  assert.equal(act(s, 'KITCHEN', 'feed-kitchen').ok, false);
});

test('phone battery gates calls and orders; original global delivery gate preserved', () => {
  const s = game(); s.stress = 60;
  act(s, 'PHONE', 'call-friend'); assert.equal(s.stress, 45); assert.equal(s.phoneBattery, 72);
  s.friendTextPending = true; s.friendTextIgnores = 3;
  act(s, 'PHONE', 'reply-friend'); assert.equal(s.friendTextIgnores, 0); assert.equal(s.friendTextPending, false);
  s.coins = 20; act(s, 'PHONE', 'order-Food'); assert.equal(s.coins, 17);
  assert.equal(s.deliveryTicks, 20); assert.equal(act(s, 'PHONE', 'order-Drinks').ok, false);
  ticks(s, 20); assert.equal(s.deliveryArrived, true);
  s.hunger = 40; act(s, 'KITCHEN', 'pickup-food'); assert.equal(s.hunger, 80);
  assert.equal(s.deliveryOrdered, true); assert.equal(s.deliveryArrived, false);
  tick(s, safeRandom); assert.equal(s.deliveryArrived, true); // inherited Java quirk
  s.phoneBattery = 5; assert.equal(act(s, 'PHONE', 'call-mom').ok, false);
  act(s, 'PHONE', 'charge'); ticks(s, 32); assert.ok(s.phoneBattery > 90);
});

test('detergent and cold medicine deliveries resolve and music toggles', () => {
  const s = game(); s.coins = 30;
  act(s, 'PHONE', 'order-Detergent'); ticks(s, 20);
  act(s, 'BASEMENT', 'pickup-detergent'); assert.equal(s.laundryDetergent, 4); assert.equal(s.detergentOrdered, false);
  act(s, 'PHONE', 'order-Cold Medicine'); ticks(s, 15); s.hasCold = true;
  act(s, 'PHONE', 'take-cold-medicine'); assert.equal(s.hasCold, false); assert.equal(s.coldMedArrived, false);
  assert.equal(s.coldMedOrdered, true); // original does not clear this gate
  s.phoneBattery = 80; act(s, 'PHONE', 'music-Lo-fi'); assert.equal(s.musicPlaying, true); assert.equal(s.musicGenre, 'Lo-fi');
  act(s, 'PHONE', 'music-Lo-fi'); assert.equal(s.musicPlaying, false);
});

test('fuse restoration, trap use and squirrel shoo use exact original rewards', () => {
  const s = game(); s.powerOut = true; s.ratsPresent = true; s.hasMouseTrap = true;
  assert.equal(act(s, 'GARAGE', 'circuit').minigame, 'circuit');
  act(s, 'GARAGE', 'circuit-result', { success: true }); assert.equal(s.powerOut, false); assert.equal(s.coins, 9);
  s.roachesPresent = false; s.roachesCleared = true;
  act(s, 'GARAGE', 'rats'); assert.equal(s.hasMouseTrap, false); assert.equal(s.ratsPresent, false); assert.equal(s.coins, 12);
  s.squirrelPresent = true; s.squirrelCount = 3;
  act(s, 'LIVING_ROOM', 'shoo'); assert.equal(s.stress, 25); assert.equal(s.coins, 13); assert.equal(s.squirrelCount, 1);
});
