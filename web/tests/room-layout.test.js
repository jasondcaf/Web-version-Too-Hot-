import test from 'node:test';
import assert from 'node:assert/strict';
import { actionRect, layoutActions, ROOM_WIDTH, ROOM_HEIGHT } from '../src/room-layout.js';
import { createGame } from '../src/engine.js';
import { getActions, rooms } from '../src/actions.js';

const game = patch => Object.assign(createGame(() => .5), { screen: 'GAME' }, patch);
const intersects = (a, b) => a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;
const rect = (room, id, values, state) => assert.deepEqual(actionRect(room, id, state), values);

// These expected coordinates are source landmarks, not values calculated from
// the module under test: they catch the old generic grid and column swapping.
test('source room columns preserve object associations and deliberately empty space', () => {
  rect('BEDROOM', 'feed-bedroom', { x: 22, y: 300, w: 195, h: 36 });
  rect('BEDROOM', 'rest-bedroom', { x: 305, y: 300, w: 190, h: 36 });
  rect('BEDROOM', 'homework', { x: 583, y: 300, w: 195, h: 36 });
  rect('KITCHEN', 'drink-kitchen', { x: 18, y: 300, w: 185, h: 34 });
  rect('KITCHEN', 'feed-kitchen', { x: 215, y: 300, w: 180, h: 34 });
  rect('KITCHEN', 'microwave', { x: 603, y: 317, w: 185, h: 34 });
  rect('GARAGE', 'change-Garage', { x: 22, y: 300, w: 190, h: 36 });
  rect('GARAGE', 'roaches', { x: 230, y: 300, w: 280, h: 38 });
  rect('GARAGE', 'circuit', { x: 560, y: 300, w: 220, h: 38 });
  rect('MOMS_ROOM', 'closet', { x: 560, y: 300, w: 220, h: 36 });
  rect('BATHROOM', 'medicine-cabinet', { x: 558, y: 300, w: 220, h: 36 });
  rect('OUTSIDE', 'sprinkler', { x: 448, y: 300, w: 210, h: 36 });
});

test('living room keeps four thermostat controls and squirrel alert in the source slots', () => {
  for (let i = 0; i < 4; i++) rect('LIVING_ROOM', `ac-${i}`, { x: 20 + i * 188, y: 384, w: 180, h: 30 });
  rect('LIVING_ROOM', 'shoo', { x: 200, y: 300, w: 400, h: 38 }, { squirrelPresent: true });
  rect('LIVING_ROOM', 'window', { x: 18, y: 342, w: 170, h: 36 }, { windowBroken: true });
  assert.equal(actionRect('LIVING_ROOM', 'tv', { squirrelPresent: true }), null);
  assert.equal(actionRect('LIVING_ROOM', 'tidy-living', { choreCleanLR: true }), null);
  assert.equal(actionRect('LIVING_ROOM', 'ac-3', { powerOut: true }), null);
});

test('shared kitchen priority slots never put a drawer behind the new bag or ice cream behind delivery', () => {
  const s = game({ currentRoom: 'KITCHEN', choreTrashOut: true, deliveryArrived: true, hunger: 50, iceCreamFound: true });
  const ids = ['trash-bag', 'change-Kitchen', 'pickup-food', 'freezer', 'ice-cream'];
  const laid = layoutActions('KITCHEN', ids.map(id => ({ id })), s);
  assert.deepEqual(laid.map(a => a.id), ['trash-bag', 'pickup-food']);
  rect('KITCHEN', 'trash-bag', { x: 215, y: 380, w: 180, h: 32 }, s);
  rect('KITCHEN', 'pickup-food', { x: 603, y: 283, w: 185, h: 30 }, s);
  s.choreTrashBagIn = true;
  assert.equal(actionRect('KITCHEN', 'trash-bag', s), null);
  rect('KITCHEN', 'change-Kitchen', { x: 215, y: 380, w: 180, h: 32 }, s);
});

test('bedroom towel, clothes, and nap share the bed slot without coin-search flags hiding clothes', () => {
  for (const [patch, id] of [
    [{ needsTowelDry: true }, 'dry-off'],
    [{ needsClothesChange: true, looseChangeFound_Bedroom: true }, 'change-clothes'],
    [{}, 'nap'],
  ]) {
    const s = game({ currentRoom: 'BEDROOM', ...patch });
    const laid = layoutActions('BEDROOM', getActions(s), s);
    const bed = laid.filter(a => ['dry-off', 'change-clothes', 'nap'].includes(a.id));
    assert.deepEqual(bed.map(a => a.id), [id]);
    assert.deepEqual(bed[0].rect, { x: 305, y: 382, w: 190, h: 30 });
  }
});

test('laundry geometry changes for idle, no detergent, running, ready to fold, and completed loads', () => {
  rect('BASEMENT', 'washer', { x: 210, y: 290, w: 380, h: 38 }, { laundryState: 0, laundryDetergent: 2 });
  rect('BASEMENT', 'washer', { x: 170, y: 290, w: 460, h: 38 }, { laundryState: 0, laundryDetergent: 0 });
  rect('BASEMENT', 'washer', { x: 260, y: 290, w: 280, h: 38 }, { laundryState: 1, laundryDetergent: 0 });
  rect('BASEMENT', 'fold', { x: 220, y: 290, w: 360, h: 38 }, { laundryState: 4 });
  rect('BASEMENT', 'fold', { x: 260, y: 290, w: 280, h: 38 }, { laundryState: 5 });
  rect('BASEMENT', 'pickup-detergent', { x: 50, y: 390, w: 240, h: 36 });
});

test('source removes consumed wrench and completed trash, while retaining disabled completed chores', () => {
  assert.equal(actionRect('GARAGE', 'wrench', { wrenchInGarage: false, hasWrench: false }), null);
  rect('GARAGE', 'roaches-cleared', { x: 230, y: 300, w: 280, h: 38 }, { roachesCleared: true });
  assert.equal(actionRect('OUTSIDE', 'trash', { choreTrashOut: true, choreTrashBagIn: true }), null);
  for (const [room, id] of [['BEDROOM', 'homework'], ['KITCHEN', 'dishes'], ['BATHROOM', 'mop'], ['OUTSIDE', 'mail']]) {
    assert.ok(actionRect(room, id, game({ choreHomework: true, choreDishes: true, choreMopBathroom: true, choreMailIn: true })));
  }
});

test('closet interior has only its exit and source rectangles do not leak mutable shared storage', () => {
  const s = game({ currentRoom: 'MOMS_ROOM', hidingInCloset: true });
  const laid = layoutActions('MOMS_ROOM', getActions(s), s);
  assert.deepEqual(laid.map(a => a.id), ['leave-closet']);
  assert.deepEqual(laid[0].rect, { x: 300, y: 300, w: 200, h: 40 });
  const first = actionRect('BEDROOM', 'charge'); first.x = 999;
  assert.equal(actionRect('BEDROOM', 'charge').x, 22);
  assert.equal(actionRect('PHONE', 'charge'), null);
  assert.equal(actionRect('GARAGE', 'typo'), null);
  assert.equal(actionRect('GARAGE', 'toString'), null);
  assert.equal(actionRect('constructor', 'name'), null);
});

test('every normal room action maps inside source room bounds and visible controls do not overlap', () => {
  const patches = [
    {},
    { squirrelPresent: true, squirrelCount: 3, windowBroken: true, powerOut: true },
    { mittensMessRoom: 'LIVING_ROOM', acBroken: true, hasWrench: true, hasRoachSpray: true },
    { hasStepStool: true, catHunger: 40, needsTowelDry: true, phoneCharging: true },
    { needsClothesChange: true, hasMomFlashlight: true, deliveryArrived: true, hunger: 60, choreTrashOut: true },
    { roachesPresent: false, roachesCleared: true, ratsPresent: true, hasMouseTrap: true, mittensEscaped: true, mowingActive: true },
    { choreTrashOut: true, choreTrashBagIn: true, choreLawnMowed: true, choreCleanLR: true, choreCleanKitchen: true, choreCleanBedroom: true, choreDishes: true, choreHomework: true, choreVacuum: true, choreSweepKitchen: true, choreMopBathroom: true, choreWaterPlants: true, choreMailIn: true },
    { iceCreamFound: true, laundryState: 1 }, { iceCreamFound: true, iceCreamEaten: true, laundryState: 2 },
    { laundryState: 3, powerOut: true }, { laundryState: 4, detergentArrived: true },
    { laundryState: 5, dirtyClothesAdded: true, detergentArrived: true },
    { laundryDetergent: 0 }, { thunderstorm: true, mowingActive: true },
    { hidingInCloset: true },
  ];
  const seen = new Set();
  for (const room of rooms.filter(r => r.id !== 'PHONE')) for (const patch of patches) {
    const s = game({ currentRoom: room.id, ...patch });
    const actions = getActions(s);
    const laid = layoutActions(room.id, actions, s);
    for (const action of actions) {
      // Unknown identifiers must not disappear silently as if intentionally hidden.
      assert.ok(actionRect(room.id, action.id) !== null, `Missing geometry: ${room.id}/${action.id}`);
      seen.add(`${room.id}/${action.id}`);
    }
    for (const { id, rect: r } of laid) {
      assert.ok(r.x >= 0 && r.y >= 0 && r.w > 0 && r.h > 0 && r.x + r.w <= ROOM_WIDTH && r.y + r.h <= ROOM_HEIGHT, `${room.id}/${id} is outside room`);
    }
    for (let i = 0; i < laid.length; i++) for (let j = i + 1; j < laid.length; j++) {
      assert.equal(intersects(laid[i].rect, laid[j].rect), false, `${room.id}: ${laid[i].id} overlaps ${laid[j].id}`);
    }
  }
  for (const id of ['roaches', 'rats', 'circuit', 'dishes', 'dry-off', 'change-clothes', 'mow', 'new-load', 'pickup-food', 'pickup-detergent']) {
    assert.ok([...seen].some(key => key.endsWith(`/${id}`)), `Scenario missed ${id}`);
  }
});
