import test from 'node:test';
import assert from 'node:assert/strict';
import {createFlappy, flapFlappy, stepFlappy, FLAPPY} from '../src/flappy.js';

test('flaps preserve Java velocity and cost half a phone-battery point', () => {
  const game = createFlappy(), state = {phoneBattery: 80};
  assert.equal(flapFlappy(game, state), true);
  assert.equal(game.birdVY, -7.2);
  assert.equal(state.phoneBattery, 79.5);
  stepFlappy(game);
  assert.equal(game.birdVY, -7.2 + .42);
  assert.equal(game.birdY, 130 - 7.2 + .42);
});

test('idle games do not simulate; empty phone cannot start', () => {
  const game = createFlappy();
  stepFlappy(game);
  assert.equal(game.frame, 0);
  assert.equal(flapFlappy(game, {phoneBattery: 5}), false);
  assert.equal(game.started, false);
});

test('pipe spawn uses Java cadence, dimensions, and speed', () => {
  const game = createFlappy();
  Object.assign(game, {started: true, frame: 9, birdVY: 0});
  stepFlappy(game, () => .5);
  assert.deepEqual(game.pipes, [{x: 206, top: 106, scored: false}]);
});

test('passing pipes scores once and does not invent economy rewards', () => {
  const game = createFlappy();
  Object.assign(game, {started: true, pipes: [{x: 18, top: 100, scored: false}]});
  stepFlappy(game);
  assert.equal(game.score, 1);
  assert.equal(game.bestScore, 1);
  stepFlappy(game);
  assert.equal(game.score, 1);
  assert.equal(game.coins, undefined);
});

test('pipe and floor collision end session; restart preserves best without immediate flap', () => {
  const game = createFlappy(7), state = {phoneBattery: 80};
  Object.assign(game, {started: true, birdY: FLAPPY.height - 20, birdVY: 2});
  stepFlappy(game);
  assert.equal(game.alive, false);
  flapFlappy(game, state);
  assert.equal(game.bestScore, 7);
  assert.equal(game.started, false);
  assert.equal(game.pipes.length, 0);
  assert.equal(state.phoneBattery, 80);
  Object.assign(game, {started: true, birdY: 90, pipes: [{x: 48, top: 100, scored: false}]});
  stepFlappy(game);
  assert.equal(game.alive, false);
});
