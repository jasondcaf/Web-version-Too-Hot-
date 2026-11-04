import test from 'node:test';
import assert from 'node:assert/strict';
import { advanceDishNeedle, isDishHit, createCircuitRound, acceptCircuitNode, createActiveTimerQueue } from '../src/minigames.js';

test('dish needle bounces at the Java endpoints without overshooting', () => {
  assert.deepEqual(advanceDishNeedle(.99, 1, .025), { needle: 1, direction: -1 });
  assert.deepEqual(advanceDishNeedle(.01, -1, .025), { needle: 0, direction: 1 });
  assert.deepEqual(advanceDishNeedle(.5, -1, .04), { needle: .46, direction: -1 });
});

test('dish timing requires inclusive green-zone bounds from Java', () => {
  assert.equal(isDishHit(.35), true);
  assert.equal(isDishHit(.65), true);
  assert.equal(isDishHit(.349), false);
  assert.equal(isDishHit(.651), false);
});

test('fuse rounds use 5/6/7 nodes and 3/4/5 random entries with repeats allowed', () => {
  for (let round = 1; round <= 3; round++) {
    const model = createCircuitRound(round, () => .999);
    assert.equal(model.nodeCount, round + 4);
    assert.equal(model.sequence.length, round + 2);
    assert.ok(model.sequence.every(i => i === model.nodeCount - 1));
    assert.equal(model.secondsLeft, 20);
    assert.equal(acceptCircuitNode(model, 0), 'ignored');
    assert.equal(model.progress, 0);
  }
});

test('wrong fuse input requires replay; correct three-round completion wins', () => {
  const wrong = createCircuitRound(1, () => 0);
  wrong.phase = 'input';
  assert.equal(acceptCircuitNode(wrong, 1), 'wrong');
  assert.equal(wrong.phase, 'retry');
  assert.equal(acceptCircuitNode(wrong, 0), 'ignored');
  assert.equal(wrong.progress, 0);
  for (let round = 1; round <= 3; round++) {
    const model = createCircuitRound(round, () => .3);
    model.phase = 'input';
    for (let i = 0; i < model.sequence.length - 1; i++) assert.equal(acceptCircuitNode(model, model.sequence[i]), 'correct');
    assert.equal(acceptCircuitNode(model, model.sequence.at(-1)), round === 3 ? 'won' : 'round');
    assert.equal(model.phase, 'complete');
  }
});

test('hidden time preserves both countdown and memory-flash timer remaining time', () => {
  const timers = createActiveTimerQueue();
  const events = [];
  timers.schedule(() => events.push('flash'), 700);
  timers.schedule(() => events.push('second'), 1000, true);
  timers.advance(600);
  timers.advance(60000, false);
  assert.deepEqual(events, []);
  timers.advance(99);
  assert.deepEqual(events, []);
  timers.advance(1);
  assert.deepEqual(events, ['flash']);
  timers.advance(300);
  assert.deepEqual(events, ['flash', 'second']);
  timers.advance(10000, false);
  timers.advance(1000);
  assert.deepEqual(events, ['flash', 'second', 'second']);
});

test('minigame timer cancellation and cleanup prevent later callbacks', () => {
  const timers = createActiveTimerQueue();
  const events = [];
  const canceled = timers.schedule(() => events.push('cancelled'), 20);
  timers.cancel(canceled);
  timers.schedule(() => { events.push('complete'); timers.clear(); }, 10);
  timers.schedule(() => events.push('after cleanup'), 10, true);
  timers.advance(1000);
  assert.deepEqual(events, ['complete']);
});
