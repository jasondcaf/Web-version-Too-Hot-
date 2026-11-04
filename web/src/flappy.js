// PhonePanel.FlappyBird port. Simulation uses the Java 33 ms update interval,
// independent of display density or the browser's animation-frame rate.
export const FLAPPY = Object.freeze({
  // drawGameContent sets GH = SH(330) - status bar(22) - hint(20) = 288.
  width: 208, height: 288, gravity: 0.42, flapVelocity: -7.2,
  birdX: 48, birdRadius: 9, pipeWidth: 28, pipeGap: 76, pipeSpeed: 2,
});

export function createFlappy(bestScore = 0) {
  return {birdY: 130, birdVY: 0, alive: true, started: false,
    pipes: [], score: 0, bestScore, frame: 0};
}

export function flapFlappy(game, state) {
  if (state.phoneBattery <= 5) return false;
  if (!game.alive) {
    Object.assign(game, createFlappy(game.bestScore), {birdY: FLAPPY.height / 2 - 20});
    return true;
  }
  game.started = true;
  game.birdVY = FLAPPY.flapVelocity;
  state.phoneBattery = Math.max(0, state.phoneBattery - 0.5);
  return true;
}

export function stepFlappy(game, rng = Math.random) {
  if (!game.started || !game.alive) return;
  const f = FLAPPY;
  game.birdVY += f.gravity;
  game.birdY += game.birdVY;
  game.frame++;
  if (game.frame % 90 === 10) {
    game.pipes.push({x: f.width, top: 24 + Math.floor(rng() * (f.height - f.pipeGap - 48)), scored: false});
  }
  for (const pipe of game.pipes) {
    pipe.x -= f.pipeSpeed;
    if (!pipe.scored && pipe.x + f.pipeWidth < f.birdX) {
      pipe.scored = true;
      game.score++;
      game.bestScore = Math.max(game.bestScore, game.score);
    }
  }
  game.pipes = game.pipes.filter(p => p.x + f.pipeWidth >= 0);
  const outside = game.birdY - f.birdRadius < 0 || game.birdY + f.birdRadius > f.height - 14;
  const pipeHit = game.pipes.some(p => f.birdX + f.birdRadius > p.x && f.birdX - f.birdRadius < p.x + f.pipeWidth &&
    (game.birdY - f.birdRadius < p.top || game.birdY + f.birdRadius > p.top + f.pipeGap));
  if (outside || pipeHit) { game.alive = false; game.started = false; }
}

/** Mount once per app entry; dispose when leaving the phone game screen. */
export function mountFlappy(container, {getState, onMessage = () => {}, isActive = () => !document.hidden}) {
  const game = createFlappy();
  const canvas = document.createElement('canvas');
  canvas.className = 'flappy-canvas';
  canvas.tabIndex = 0;
  canvas.setAttribute('role', 'button');
  canvas.setAttribute('aria-label', 'Flappy Bird. Tap, click, or press Space to flap. Tap after game over to restart.');
  Object.assign(canvas.style, {display: 'block', width: '100%', maxWidth: '310px',
    aspectRatio: '208 / 288', margin: '0 auto', touchAction: 'none', borderRadius: '10px'});
  const hint = document.createElement('p');
  hint.className = 'flappy-hint';
  hint.textContent = 'Tap or Space to flap · Uses phone battery';
  container.replaceChildren(canvas, hint);
  const ctx = canvas.getContext('2d');
  const act = () => {
    if (!isActive()) return;
    if (!flapFlappy(game, getState())) onMessage('No battery — charge the phone!');
    draw();
  };
  const pointer = event => { event.preventDefault(); canvas.focus({preventScroll: true}); act(); };
  const key = event => {
    if ((event.code === 'Space' || event.code === 'ArrowUp') && !event.repeat) { event.preventDefault(); act(); }
  };
  canvas.addEventListener('pointerdown', pointer);
  canvas.addEventListener('keydown', key);

  function draw() {
    const {width: w, height: h} = FLAPPY;
    const ratio = Math.min(3, window.devicePixelRatio || 1);
    const displayedWidth = canvas.clientWidth || w;
    const pxWidth = Math.round(displayedWidth * ratio);
    const pxHeight = Math.round(displayedWidth * h / w * ratio);
    if (canvas.width !== pxWidth || canvas.height !== pxHeight) { canvas.width = pxWidth; canvas.height = pxHeight; }
    ctx.setTransform(canvas.width / w, 0, 0, canvas.height / h, 0, 0);
    const sky = ctx.createLinearGradient(0, 0, 0, h);
    sky.addColorStop(0, '#64b4ff'); sky.addColorStop(1, '#b4dcff');
    ctx.fillStyle = sky; ctx.fillRect(0, 0, w, h);
    ctx.fillStyle = '#ffffffbb';
    ctx.fillRect(20, 18, 36, 16); ctx.fillRect(110, 30, 28, 12);
    ctx.fillStyle = '#a06428'; ctx.fillRect(0, h - 14, w, 14);
    ctx.fillStyle = '#50af37'; ctx.fillRect(0, h - 14, w, 7);
    for (const p of game.pipes) {
      ctx.fillStyle = '#32a032';
      ctx.fillRect(p.x, 0, FLAPPY.pipeWidth, p.top);
      ctx.fillRect(p.x, p.top + FLAPPY.pipeGap, FLAPPY.pipeWidth, h - 14 - p.top - FLAPPY.pipeGap);
      ctx.fillStyle = '#1e781e';
      ctx.fillRect(p.x - 3, p.top - 14, FLAPPY.pipeWidth + 6, 14);
      ctx.fillRect(p.x - 3, p.top + FLAPPY.pipeGap, FLAPPY.pipeWidth + 6, 14);
      ctx.strokeStyle = '#50c850';
      ctx.strokeRect(p.x, 0, FLAPPY.pipeWidth, p.top);
      ctx.strokeRect(p.x, p.top + FLAPPY.pipeGap, FLAPPY.pipeWidth, h - 14 - p.top - FLAPPY.pipeGap);
    }
    ctx.save(); ctx.translate(FLAPPY.birdX, game.birdY);
    ctx.rotate(Math.max(-25, Math.min(35, game.birdVY * 3)) * Math.PI / 180);
    ctx.fillStyle = '#ffd232'; ctx.beginPath(); ctx.arc(0, 0, 9, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#dca01e'; ctx.beginPath(); ctx.ellipse(-5, 3, 4, 2.5, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = 'white'; ctx.beginPath(); ctx.arc(5, -2, 3, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = 'black'; ctx.beginPath(); ctx.arc(5, -2, 1.8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#ff8200'; ctx.beginPath(); ctx.moveTo(8, -3); ctx.lineTo(16, 0); ctx.lineTo(8, 3); ctx.fill();
    ctx.restore();
    ctx.textAlign = 'center'; ctx.font = 'bold 16px sans-serif'; ctx.fillStyle = 'white';
    ctx.fillText(String(game.score), w / 2, 25);
    const deadBattery = getState().phoneBattery <= 5;
    if (!game.started || !game.alive || deadBattery) {
      ctx.fillStyle = '#000000bb'; ctx.fillRect(16, h / 2 - 43, w - 32, 86);
      ctx.font = 'bold 14px sans-serif'; ctx.fillStyle = game.alive ? '#c8f0c8' : '#ff5050';
      ctx.fillText(deadBattery ? 'Charge your phone!' : game.alive ? 'Flappy Bird' : 'Game Over!', w / 2, h / 2 - 18);
      ctx.font = '11px sans-serif'; ctx.fillStyle = 'white';
      ctx.fillText(`Score: ${game.score}   Best: ${game.bestScore}`, w / 2, h / 2 + 2);
      ctx.fillStyle = '#c8f0c8';
      ctx.fillText(deadBattery ? 'Return home to charge' : game.alive ? 'Tap / Space to start!' : 'Tap to play again!', w / 2, h / 2 + 23);
    }
  }
  const timer = setInterval(() => {
    if (isActive() && getState().phoneBattery > 5 && getState().screen === 'GAME') stepFlappy(game);
    draw();
  }, 33);
  draw();
  return () => {
    clearInterval(timer);
    canvas.removeEventListener('pointerdown', pointer);
    canvas.removeEventListener('keydown', key);
    container.replaceChildren();
  };
}
