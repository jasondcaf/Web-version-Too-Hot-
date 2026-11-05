// Browser equivalents of KitchenPanel's dish meter and GaragePanel's
// roach/fuse-box games. The main simulation continues while these are open.

export function advanceDishNeedle(needle, direction, speed) {
  const next = needle + direction * speed;
  if (next >= 1) return { needle: 1, direction: -1 };
  if (next <= 0) return { needle: 0, direction: 1 };
  return { needle: next, direction };
}

export function isDishHit(needle) { return needle >= 0.35 && needle <= 0.65; }

export function createCircuitRound(round, random = Math.random) {
  if (!Number.isInteger(round) || round < 1 || round > 3) throw new RangeError('Circuit round must be 1–3');
  const nodeCount = round + 4;
  return {
    round, nodeCount, sequence: Array.from({ length: round + 2 }, () => Math.floor(random() * nodeCount)),
    progress: 0, phase: 'show', secondsLeft: 20,
  };
}

export function acceptCircuitNode(model, index) {
  if (model.phase !== 'input') return 'ignored';
  if (index !== model.sequence[model.progress]) {
    model.phase = 'retry';
    return 'wrong';
  }
  model.progress++;
  if (model.progress === model.sequence.length) {
    model.phase = 'complete';
    return model.round === 3 ? 'won' : 'round';
  }
  return 'correct';
}

// Timer deadlines use active play time, so backgrounding Safari neither spends
// a roach/fuse countdown nor skips the memory sequence. Kept independent of DOM
// and wall clocks so suspension/cancellation can be tested deterministically.
export function createActiveTimerQueue() {
  const tasks = new Set();
  return {
    schedule(fn, ms, repeating = false) {
      const task = { fn, remaining: ms, period: repeating ? ms : 0 };
      tasks.add(task);
      return task;
    },
    cancel(task) { tasks.delete(task); },
    clear() { tasks.clear(); },
    advance(ms, active = true) {
      if (!active || ms <= 0) return;
      for (const task of [...tasks]) {
        if (!tasks.has(task)) continue;
        task.remaining -= ms;
        while (task.remaining <= 0 && tasks.has(task)) {
          if (task.period) task.remaining += task.period;
          else tasks.delete(task);
          task.fn();
        }
      }
    },
  };
}

const POSITIONS = [
  [[200,155],[400,110],[595,175],[530,315],[280,330]],
  [[175,145],[365,95],[590,145],[645,280],[430,355],[210,295]],
  [[215,125],[415,85],[610,130],[670,255],[570,360],[340,378],[155,268]],
];

const CSS = `
.th-minigame{color:#e4eafa;font:16px system-ui,sans-serif;width:100%;box-sizing:border-box;touch-action:manipulation}
.th-minigame *{box-sizing:border-box}.th-minigame h2{margin:0 0 8px;color:#a3f8cd;font-size:24px}
.th-minigame p{margin:8px 0 14px;line-height:1.45}.th-mini-status{min-height:26px;color:#ffd68a}
.th-mini-head{display:flex;gap:12px;justify-content:space-between;align-items:center;flex-wrap:wrap}
.th-minigame button{min-width:44px;min-height:44px;touch-action:manipulation;border:1px solid #7187b8;border-radius:9px;background:linear-gradient(#25304b,#11182c);color:#e8edff;font:600 16px system-ui;cursor:pointer;padding:10px 18px}
.th-minigame button:focus-visible{outline:3px solid #ffd36e;outline-offset:3px}.th-minigame button:active{background:#40527a}
.th-mini-cancel{margin-top:14px}.th-mini-board{position:relative;width:100%;aspect-ratio:800/455;min-height:220px;max-height:52vh;overflow:hidden;border:1px solid #427856;border-radius:10px;background:repeating-linear-gradient(0deg,transparent 0,transparent 19px,#22573433 20px),repeating-linear-gradient(90deg,#0b2115 0,#0b2115 19px,#22573466 20px)}
.th-mini-node{position:absolute;transform:translate(-50%,-50%);width:clamp(44px,7vw,62px);height:clamp(44px,7vw,62px);padding:0!important;border-radius:50%!important;background:linear-gradient(#234e37,#10271b)!important;border:2px solid #408c5c!important;color:#9beeb9!important;z-index:2}
.th-mini-node.lit{background:#9affc9!important;color:#082718!important;box-shadow:0 0 22px #60ffb4}.th-mini-node.wrong{background:#df453c!important;color:white!important}.th-mini-node.done{border-color:#89e8ac!important}
.th-mini-wires{position:absolute;width:100%;height:100%;inset:0;pointer-events:none}.th-mini-wires line{stroke:#57e291;stroke-width:3;vector-effect:non-scaling-stroke}
.th-mini-meter{height:48px;position:relative;background:#4d253b;border:1px solid #8691b3;border-radius:8px;margin:38px 12px 28px}
.th-mini-target{position:absolute;left:35%;width:30%;height:100%;background:#28764d;border-inline:2px solid #7aefac}
.th-mini-needle{position:absolute;top:-10px;bottom:-10px;width:5px;transform:translateX(-50%);background:#fff2ad;border-radius:3px;box-shadow:0 0 9px #ffdf72}
.th-mini-scrub{display:block;margin:12px auto;min-width:180px!important;min-height:54px!important;background:#244e3a!important;font-size:20px!important}
.th-mini-score{text-align:center;color:#b6d9f0;font-variant-numeric:tabular-nums}.th-mini-timer{font-variant-numeric:tabular-nums;color:#ffa96d;font-weight:700}
.th-mini-roach{position:absolute;width:56px;min-width:56px!important;height:56px;padding:3px!important;border:0!important;background:transparent!important;transform:translate(-50%,-50%);filter:drop-shadow(0 2px 3px #000);z-index:2}
.th-mini-roach svg,.th-mini-roach img{width:100%;height:100%;object-fit:contain;position:absolute;inset:0}
.th-mini-roach img{z-index:1}.th-mini-bugs{background:radial-gradient(ellipse at top,#48404a,#191c25);border-color:#766559}
@media(max-width:500px){.th-minigame h2{font-size:20px}.th-minigame p{font-size:14px}.th-mini-board{min-height:235px}.th-mini-node{width:44px;height:44px}}
`;

const BUG = '<svg viewBox="0 0 64 64" aria-hidden="true"><g stroke="#c68a4c" stroke-width="2.5" fill="none"><path d="M24 25 10 14M24 32 8 32M24 40 10 52M40 25 54 14M40 32 56 32M40 40 54 52M28 16 21 3M36 16 43 3"/></g><ellipse cx="32" cy="36" rx="13" ry="20" fill="#472410" stroke="#d19964" stroke-width="2"/><ellipse cx="32" cy="17" rx="9" ry="8" fill="#684020"/><path d="M32 20v33" stroke="#b48250" stroke-width="2"/></svg>';

/**
 * opts.onAction receives authoritative state actions (dish timing hits).
 * opts.onComplete({success}) signals that the parent should resolve the result
 * and close its overlay. cleanup stops all timers/listeners without a result.
 * opts.isActive optionally adds a pause condition; hidden documents always pause.
 */
export function startMinigame(type, container, opts = {}) {
  if (!['dishes','roaches','circuit'].includes(type)) throw new Error(`Unknown minigame: ${type}`);
  const doc = container.ownerDocument;
  let alive = true;
  const timers = createActiveTimerQueue();
  const listeners = [];
  const isActive = () => !doc.hidden && (opts.isActive ? opts.isActive() : true);
  const now = () => doc.defaultView?.performance?.now() ?? Date.now();
  let lastTime = now();
  let wasActive = isActive();
  const heartbeat = setInterval(() => {
    const time = now();
    const active = isActive();
    timers.advance(time - lastTime, active && wasActive);
    lastTime = time;
    wasActive = active;
  }, 16);
  const root = doc.createElement('section');
  root.className = 'th-minigame';
  root.setAttribute('aria-label', `${type} minigame`);
  const style = doc.createElement('style');
  style.textContent = CSS;
  root.append(style);
  container.append(root);

  function later(fn, ms) {
    return timers.schedule(() => { if (alive) fn(); }, ms);
  }
  function repeat(fn, ms) {
    return timers.schedule(() => { if (alive) fn(); }, ms, true);
  }
  function stopTimer(id) { timers.cancel(id); }
  function listen(el, event, fn) { el.addEventListener(event, fn); listeners.push(() => el.removeEventListener(event, fn)); }
  listen(doc, 'visibilitychange', () => { lastTime = now(); wasActive = isActive(); });
  function cleanup() {
    if (!alive) return;
    alive = false;
    clearInterval(heartbeat);
    timers.clear();
    for (const remove of listeners) remove();
    root.remove();
  }
  function complete(success) { cleanup(); opts.onComplete?.({ success }); }
  function cancel() { cleanup(); opts.onCancel?.(); }
  function el(tag, cls, text) { const e = doc.createElement(tag); if (cls) e.className = cls; if (text) e.textContent = text; return e; }
  function button(text, cls, fn) {
    const b = el('button', cls, text); b.type = 'button';
    listen(b, 'click', () => { if (alive && isActive()) fn(); });
    return b;
  }
  function cancelButton() { root.append(button(type === 'dishes' ? 'Give up' : 'Cancel', 'th-mini-cancel', cancel)); }

  if (type === 'dishes') {
    root.append(el('h2', '', 'Wash the dishes'));
    root.append(el('p', '', 'Tap SCRUB when the needle is inside the green zone. Four good scrubs clean the dishes. More misses than hits starts the scrubs over.'));
    const meter = el('div', 'th-mini-meter');
    meter.setAttribute('aria-label', 'Moving needle with green target from 35 to 65 percent');
    const needle = el('div', 'th-mini-needle');
    meter.append(el('div', 'th-mini-target'), needle);
    root.append(meter);
    const score = el('p', 'th-mini-score');
    const feedback = el('p', 'th-mini-status');
    feedback.setAttribute('aria-live', 'polite');
    let pos = { needle: 0, direction: 1 };
    let hits = opts.getState?.().dishScrubs || 0;
    let misses = opts.getState?.().dishMisses || 0;
    function updateScore() { score.textContent = `Scrubs ${hits}/4 · Misses ${misses}`; }
    function scrub() {
      const hit = isDishHit(pos.needle);
      opts.onAction?.('dishes-hit', { needle: pos.needle });
      const state = opts.getState?.();
      if (state) { hits = state.dishScrubs; misses = state.dishMisses; }
      else {
        if (hit) hits++; else misses++;
        if (misses > hits) { hits = 0; misses = 0; }
      }
      feedback.textContent = hit ? 'Clean hit!' : (hits === 0 && misses === 0 ? 'Too many misses — start over!' : 'Missed the green zone.');
      updateScore();
      if (state?.choreDishes || hits >= 4) complete(true);
    }
    const scrubButton = button('SCRUB!', 'th-mini-scrub', scrub);
    root.append(scrubButton, score, feedback);
    updateScore();
    repeat(() => {
      pos = advanceDishNeedle(pos.needle, pos.direction, Math.min(0.025 + hits * 0.007, 0.09));
      needle.style.left = `${pos.needle * 100}%`;
    }, 28);
    cancelButton();
    scrubButton.focus({ preventScroll: true });
  }

  if (type === 'roaches') {
    const head = el('div', 'th-mini-head');
    const timer = el('span', 'th-mini-timer', '12s');
    head.append(el('h2', '', 'Clear the roaches'), timer);
    root.append(head, el('p', '', 'Tap all six roaches before time runs out. The spray is used only when you clear them all.'));
    const board = el('div', 'th-mini-board th-mini-bugs');
    root.append(board);
    let left = 6;
    let seconds = 12;
    const score = el('p', 'th-mini-score', '6 roaches left');
    root.append(score);
    const random = opts.random || Math.random;
    // Stratify positions to prevent one touch target obscuring another, while
    // retaining Java's random placement and its six stationary targets.
    for (let i = 0; i < 6; i++) {
      const bug = button('', 'th-mini-roach', () => {
        if (!bug.isConnected) return;
        bug.remove();
        left--;
        score.textContent = `${left} roach${left === 1 ? '' : 'es'} left`;
        if (!left) complete(true);
      });
      bug.setAttribute('aria-label', `Spray roach ${i + 1}`);
      bug.innerHTML = BUG;
      const asset = opts.roachAsset || opts.assets?.roach_ai;
      if (asset) {
        const img = el('img'); img.alt = '';
        const fallback = bug.querySelector('svg');
        listen(img, 'load', () => { fallback.style.display = 'none'; });
        listen(img, 'error', () => { fallback.style.display = ''; img.remove(); });
        img.src = asset;
        bug.append(img);
      }
      bug.style.left = `${18 + (i % 3) * 31 + random() * 8}%`;
      bug.style.top = `${26 + Math.floor(i / 3) * 41 + random() * 12}%`;
      bug.style.rotate = `${Math.floor(random() * 360)}deg`;
      board.append(bug);
    }
    repeat(() => {
      seconds--;
      timer.textContent = `${seconds}s`;
      if (!seconds) complete(false);
    }, 1000);
    cancelButton();
    board.querySelector('button')?.focus({ preventScroll: true });
  }

  if (type === 'circuit') {
    const head = el('div', 'th-mini-head');
    const title = el('h2', '', 'Fuse box · Round 1/3');
    const timer = el('span', 'th-mini-timer');
    head.append(title, timer);
    const status = el('p', 'th-mini-status', 'Watch the sequence.');
    status.setAttribute('aria-live', 'polite');
    root.append(head, el('p', '', 'Watch the flashing nodes, then tap them in the same order. A wrong answer or timeout replays this round. Keyboard: 1–7.'), status);
    const board = el('div', 'th-mini-board');
    root.append(board);
    cancelButton();
    let model;
    let countdown;
    let generation = 0;
    let nodes = [];
    let wires;
    function drawWires() {
      wires.replaceChildren();
      const coords = POSITIONS[model.round - 1];
      for (let i = 0; i < model.progress - 1; i++) {
        const a = coords[model.sequence[i]], b = coords[model.sequence[i + 1]];
        const line = doc.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', a[0]); line.setAttribute('y1', a[1]);
        line.setAttribute('x2', b[0]); line.setAttribute('y2', b[1]);
        wires.append(line);
      }
    }
    function clearLights() { nodes.forEach(n => n.classList.remove('lit', 'wrong', 'done')); }
    function demo() {
      stopTimer(countdown);
      const gen = ++generation;
      model.phase = 'show'; model.progress = 0; model.secondsLeft = 20;
      clearLights(); drawWires();
      status.textContent = `Round ${model.round}/3 — watch ${model.sequence.length} flashes.`;
      timer.textContent = '';
      function flash(index) {
        if (gen !== generation) return;
        nodes[model.sequence[index]].classList.add('lit');
        later(() => {
          if (gen !== generation) return;
          nodes[model.sequence[index]].classList.remove('lit');
          if (index + 1 < model.sequence.length) later(() => flash(index + 1), 350);
          else beginInput();
        }, 500);
      }
      later(() => flash(0), 700);
    }
    function beginInput() {
      model.phase = 'input';
      status.textContent = `Your turn — ${model.sequence.length} nodes in order. (${model.progress}/${model.sequence.length})`;
      timer.textContent = '20s';
      countdown = repeat(() => {
        model.secondsLeft--;
        timer.textContent = `${model.secondsLeft}s`;
        if (!model.secondsLeft) {
          stopTimer(countdown); model.phase = 'retry';
          status.textContent = 'Time is up! Watch the same sequence again.';
          later(demo, 900);
        }
      }, 1000);
    }
    function selectNode(index) {
      const result = acceptCircuitNode(model, index);
      if (result === 'ignored') return;
      if (result === 'wrong') {
        stopTimer(countdown);
        nodes[index].classList.add('wrong');
        status.textContent = 'Wrong node! Watch the same sequence again.';
        timer.textContent = '';
        later(demo, 950);
        return;
      }
      const gen = generation;
      nodes[index].classList.add('lit', 'done');
      later(() => { if (gen === generation) nodes[index].classList.remove('lit'); }, 180);
      drawWires();
      status.textContent = `Your turn — ${model.progress}/${model.sequence.length} correct.`;
      if (result === 'correct') return;
      stopTimer(countdown); timer.textContent = '';
      if (result === 'won') {
        status.textContent = 'POWER RESTORED!';
        complete(true);
      } else {
        status.textContent = `Round ${model.round} complete! Get ready…`;
        later(() => startRound(model.round + 1), 950);
      }
    }
    function startRound(round) {
      model = createCircuitRound(round, opts.random || Math.random);
      title.textContent = `Fuse box · Round ${round}/3`;
      board.replaceChildren();
      wires = doc.createElementNS('http://www.w3.org/2000/svg', 'svg');
      wires.classList.add('th-mini-wires');
      wires.setAttribute('viewBox', '0 0 800 455');
      wires.setAttribute('preserveAspectRatio', 'none');
      wires.setAttribute('aria-hidden', 'true');
      board.append(wires);
      nodes = POSITIONS[round - 1].map(([x,y], i) => {
        const node = button(String(i + 1), 'th-mini-node', () => selectNode(i));
        node.setAttribute('aria-label', `Circuit node ${i + 1}`);
        node.style.left = `${x / 8}%`; node.style.top = `${y / 4.55}%`;
        board.append(node); return node;
      });
      demo();
    }
    listen(doc, 'keydown', e => {
      if (!isActive() || e.altKey || e.ctrlKey || e.metaKey || e.repeat) return;
      const index = Number(e.key) - 1;
      if (Number.isInteger(index) && index >= 0 && index < model.nodeCount) { e.preventDefault(); selectNode(index); }
    });
    startRound(1);
    nodes[0].focus({ preventScroll: true });
  }
  return cleanup;
}
