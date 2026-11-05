import './style.css';
import { createGame, resetForDay, tick, choreCount, availableShopItems, purchase, finishArrival, nightmareInput } from './engine.js';
import { rooms, getActions, executeAction } from './actions.js';
import { startMinigame } from './minigames.js';
import { mountFlappy } from './flappy.js';
import { renderRoomArt } from './room-art.js';
import { layoutActions } from './room-layout.js';
import { renderPhoneContent, parentPhoneScreen } from './phone-view.js';
import './phone-view.css';
import { shopFrameHTML, arrivalSceneHTML, shopItemIconHTML } from './interlude-art.js';
import './interlude-art.css';
import './source-layout.css';

const app = document.querySelector('#app');
let state = createGame();
let manifest = {};
let screenKey = '', actionKey = '', artKey = '', phoneTab = 'HOME';
let navFloor='GROUND', floorKey='', selectedDelivery='order-Food';
let paused = false, modalKind = '', miniCleanup = null, flappyCleanup = null;
let notice = '', noticeUntil = 0;
let modalReturnFocus = null;
const base = import.meta.env.BASE_URL;
try {
  const response = await fetch(`${base}assets/manifest.json`);
  if (response.ok) manifest = Object.fromEntries(Object.entries(await response.json()).map(([k,v]) => [k,`${base}${v}`]));
} catch { /* Missing assets are visibly identified by the room renderer. */ }

const escape = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const button = (label, attrs = '') => `<button type="button" ${attrs}>${label}</button>`;
const time = n => `${String(Math.floor(Math.max(0,n)/60)).padStart(2,'0')}:${String(Math.max(0,n)%60).padStart(2,'0')}`;
const roomTitle = id => rooms.find(r => r.id === id)?.title || id;
const renderedHTML = new WeakMap();
// Keep keyboard focus and DOM nodes when a clock tick has not changed controls.
function setContent(element, html) {
  if(renderedHTML.get(element)===html)return;
  const buttons=[...element.querySelectorAll('button')];
  const focused=buttons.indexOf(document.activeElement);
  element.innerHTML=html;renderedHTML.set(element,html);
  if(focused>=0)element.querySelectorAll('button')[focused]?.focus({preventScroll:true});
}
const chores = () => [
  ['Dishes',state.choreDishes, `${state.dishScrubs}/4`, 'Get the step stool in the bedroom. Time four scrubs in the green zone at the kitchen sink.'],
  ['Trash',state.choreTrashOut && state.choreTrashBagIn, state.choreTrashOut?'½':'', 'Take the trash out in the backyard, then put a new bag in at the kitchen.'],
  ['Clean Rooms',state.choreHouseClean, `${[state.choreCleanLR,state.choreCleanKitchen,state.choreCleanBedroom].filter(Boolean).length}/3`, 'Tidy the living room, kitchen, and bedroom. Mittens can make a new mess!'],
  ['Lawn',state.choreLawnMowed, `${Math.round(state.choreLawnPct)}%`, 'Mow in the backyard. Each pass adds 4° heat. Take breaks before 85°.'],
  ['Homework',state.choreHomework, `${Math.min(100,Math.floor(state.choreHomeworkTicks/(state.hasStudyLamp?12:20)*100))}%`, 'Stay in the bedroom to finish automatically. A study lamp makes it faster.'],
  ['Laundry',state.laundryState>=5, ['','wash','wet','dry','ready','done'][state.laundryState], 'In the basement: wash, move to dryer, dry, then fold. Each wash uses detergent.']
];

function say(text) { if (!text) return; notice = text; noticeUntil = Date.now()+5000; }
function stopActivities() { miniCleanup?.(); miniCleanup=null; flappyCleanup?.(); flappyCleanup=null; }
function closeModal() { miniCleanup?.(); miniCleanup=null; document.querySelector('#modal')?.remove(); modalKind=''; if(modalReturnFocus?.isConnected)modalReturnFocus.focus({preventScroll:true});modalReturnFocus=null; }
function modal(title, body, kind='info') {
  closeModal(); modalKind=kind; modalReturnFocus=document.activeElement;
  const overlay=document.createElement('div'); overlay.id='modal'; overlay.className='modal-scrim';
  overlay.innerHTML=`<section class="modal-card" role="dialog" aria-modal="true" aria-label="${escape(title)}"><div class="modal-heading"><h2>${escape(title)}</h2>${button('Close', 'data-command="close"')}</div><div id="modal-body">${body}</div></section>`;
  app.append(overlay);
  overlay.querySelector('button')?.focus();
}
function newGame() {
  stopActivities(); closeModal(); state=createGame(); state.screen='GAME'; paused=false;
  navFloor='GROUND'; selectedDelivery='order-Food'; phoneTab='HOME'; notice='Mom left a note. Check your chores, grab the stool, and keep cool.'; noticeUntil=Date.now()+7000; screenKey=''; render();
}
function navigate(id) {
  if (state.screen!=='GAME' || modalKind==='minigame') return;
  flappyCleanup?.(); flappyCleanup=null;
  if (id==='PHONE' && state.currentRoom!=='PHONE') state.roomBeforePhone=state.currentRoom;
  state.currentRoom=id; navFloor=roomFloor(id); phoneTab='HOME'; actionKey=''; artKey=''; render();
}
function doAction(id,payload={}) {
  if (paused || state.screen!=='GAME') return;
  const result=executeAction(state,id,payload);
  if (result?.message) say(result.message);
  if (result?.minigame) openMini(result.minigame);
  actionKey=''; render();
  return result;
}
function openMini(type) {
  const titles={dishes:'Wash the dishes',roaches:'Clear the roaches',circuit:'Repair the fuse box'};
  modal(titles[type],'<div id="minigame"></div>','minigame');
  // The room simulation keeps ticking, as in Swing. Close cancels without a reward.
  miniCleanup=startMinigame(type,document.querySelector('#minigame'),{
    isActive:()=>!paused&&!document.hidden,
    getState:()=>state, assets:manifest,
    onAction:(id,payload)=>doAction(id,payload),
    onCancel:()=>{closeModal();render();},
    onComplete:({success}={success:true})=>{
      closeModal();
      if(type!=='dishes') doAction(`${type==='roaches'?'roaches':'circuit'}-result`,{success});
      render();
    }
  });
}
function journal() {
  modal('Mom’s note',`<p class="note-intro">“Be good while I’m out. And don’t forget <strong>${escape(state.momPriorityChores.join(' and '))}</strong>!”</p><div class="journal-list">${chores().map(([name,done,progress,tip])=>`<article><h3>${done?'✓':'○'} ${name} <small>${escape(progress)}</small></h3><p>${tip}</p></article>`).join('')}</div><p>Every completed main chore earns 4 coins at day end. Bonus jobs earn coins immediately. Keep yourself and Mittens fed until Mom gets home.</p>`);
}
function help() {
  modal('How to play',`<p>You’re home alone. Survive three days without overheating, starving, dehydrating, or losing control of your stress.</p><ol><li>Take the step stool from the bedroom to reach the kitchen sink.</li><li>Pick up roach spray in Mom’s dresser, clear six roaches in the garage, then take the wrench to fix the living-room AC.</li><li>Feed Mittens, drink, eat, and do chores. Open windows cool you but can let trouble in.</li><li>When Mom arrives, collect your allowance and buy upgrades for the next day.</li></ol><p>Tap or click the room buttons and actions. Keyboard: <kbd>1</kbd>–<kbd>9</kbd> rooms, <kbd>P</kbd> phone, <kbd>Esc</kbd> pause or close, <kbd>Space</kbd> dishes/flap; <kbd>1</kbd>–<kbd>7</kbd> fuse nodes, <kbd>A</kbd>–<kbd>F</kbd> nightmare nodes. Use <kbd>Tab</kbd> and <kbd>Enter</kbd> for all buttons.</p><p>Landscape gives the most room on iPad. Leaving this tab pauses the simulation; no time is fast-forwarded on return. The original 500 ms Java tick and countdown are retained. No auto-save, just like the desktop version.</p><p><a href="${base}credits.html" target="_blank" rel="noopener noreferrer">Artwork credits &amp; licenses</a></p>`);
}

function render() {
  if (screenKey !== state.screen) {
    stopActivities(); closeModal(); screenKey=state.screen; actionKey=''; artKey='';
    if(state.screen==='GAME') buildGame();
    else if(state.screen==='MENU') buildMenu();
    else if(state.screen==='NIGHTMARE') buildNightmare();
    else if(state.screen==='MOM_ARRIVAL') buildArrival();
    else if(state.screen==='SHOP') buildShop();
    else buildGameOver();
  }
  if(state.screen==='GAME') updateGame();
  if(state.screen==='NIGHTMARE') updateNightmare();
  if(state.screen==='SHOP') updateShop();
  if(state.screen==='GAMEOVER') updateGameOver();
}
function buildMenu() {
  app.innerHTML=`<main class="title-screen"><div id="menu-art" class="title-art"></div><div class="title-content"><p class="eyebrow">THREE DAYS. ONE VERY HOT HOUSE.</p><h1>TOO HOT</h1><p class="subtitle">Don’t let her find out.</p><p>Keep cool. Feed the cat. Finish your chores.<br>If you overheat… Mittens won’t be Mittens anymore.</p><div class="menu-buttons">${button('NEW GAME','class="primary" data-command="new"')}${button('How to play','data-command="help"')}${button('Credits','data-command="credits"')}</div><small>A browser port of the original Java Swing game<br>Touch · Mouse · Keyboard</small></div></main>`;
  renderRoomArt(document.querySelector('#menu-art'),'MENU',state,manifest);
}
function buildGame() {
  navFloor=roomFloor(state.currentRoom);floorKey='';
  app.innerHTML=`<main class="game-shell source-game"><header class="topbar"><a class="brand" href="#" data-command="help">TOO HOT</a><div class="day-label" id="day-label"></div><div class="top-tools">${button('Chores','data-command="journal"')}${button('Help','data-command="help"')}${button('Pause','data-command="pause" id="pause-button"')}${button('⛶','data-command="fullscreen" aria-label="Toggle fullscreen"')}</div></header><section class="hud" aria-label="Survival status"><div class="stats" id="stats"></div><div class="environment" id="environment"></div><div class="countdown"><small id="hud-room"></small><span id="timer"></span><small>until Mom arrives</small><strong id="coins"></strong></div></section><div class="chores-strip" id="chores"></div><div class="bonus-strip" id="bonus"></div><section class="room-scene" aria-label="Current room"><div id="room-art" class="room-art"></div><div class="room-heading"><h1 id="room-title"></h1><span id="room-hint"></span></div><div class="room-progress" id="room-progress"></div><div id="room-actions" class="room-actions source-room-actions"></div><div id="phone" hidden></div></section><div class="status-line" id="status" role="status" aria-live="polite"></div><nav class="room-nav" aria-label="Rooms"></nav></main>`;
}
function roomFloor(id) {
  return ['MOMS_ROOM','BATHROOM'].includes(id)?'UPSTAIRS':id==='BASEMENT'?'BASEMENT':'GROUND';
}
function updateFloorNav() {
  const entries=navFloor==='UPSTAIRS'?[['MOMS_ROOM',"Mom's Room"],['BATHROOM','Bathroom'],['PHONE','Phone'],['@GROUND','⬇ Downstairs']]:navFloor==='BASEMENT'?[['BASEMENT','Laundry Room'],['@GROUND','⬆ Ground Floor']]:[['LIVING_ROOM','Living Room'],['KITCHEN','Kitchen'],['GARAGE','Garage'],['BEDROOM','Bedroom'],['OUTSIDE','Backyard'],['@BASEMENT','⬇ Basement'],['PHONE','Phone'],['@UPSTAIRS','⬆ Upstairs']];
  const next=JSON.stringify([navFloor,state.currentRoom]);
  if(next===floorKey)return;floorKey=next;
  setContent(document.querySelector('.room-nav'),entries.map(([id,label])=>button(escape(label),id.startsWith('@')?`data-floor="${id.slice(1)}"`:`data-room="${id}" class="${id===state.currentRoom?'active':''}" aria-current="${id===state.currentRoom}"`)).join(''));
}
function actionHTML(a) {
  const r=a.rect;
  return button(escape(a.label),`data-action="${a.id}" class="action ${a.category||''}" data-source-x="${r.x}" data-source-y="${r.y}" style="--room-x:${r.x/8}%;--room-y:${r.y/4.55}%;--room-w:${r.w/8}%;--room-h:${r.h/4.55}%" ${a.enabled===false?'disabled':''}`);
}
function updateGame() {
  document.querySelector('#day-label').textContent=`DAY ${state.day} / 3`;
  document.querySelector('#timer').textContent=time(state.momTimerSeconds);
  document.querySelector('#timer').classList.toggle('danger',state.momTimerSeconds<60);
  document.querySelector('#coins').textContent=`★ ${state.coins} coins`;
  const stats=[['temperature','Temp','°','#dc5145',true],['hunger','Hunger','%','#b77c49'],['thirst','Thirst','%','#438bd3'],['catHunger','Mittens','%','#d5a140'],['stress','Stress','%','#b27acc',true],['phoneBattery','Phone','%','#60bf97']];
  document.querySelector('#stats').innerHTML=stats.map(([key,name,unit,color,invert])=>`<div class="stat ${invert?state[key]>=75?'critical':'':state[key]<=20?'critical':''}" style="--fill:${color}"><div><span>${name}</span><strong>${Math.round(state[key])}${unit}${key==='phoneBattery'&&state.phoneCharging?' ⚡':''}</strong></div><meter min="0" max="100" value="${state[key]}" aria-label="${name}"></meter><div class="meter-track" aria-hidden="true"><i style="width:${Math.max(0,Math.min(100,state[key]))}%"></i></div></div>`).join('');
  const statuses=[ [state.acBroken?'AC broken':`AC ${['off','eco','normal','blast'][state.acSetting]}`,state.acBroken], [state.powerOut?'Power out':'Power on',state.powerOut], [state.thunderstorm?'Thunderstorm':'Clear sky',state.thunderstorm], [state.squirrelPresent?`${state.squirrelCount} squirrel(s)`:'No squirrels',state.squirrelPresent], [state.hasCold?'You have a cold':'Feeling well',state.hasCold], [state.windowOpen||state.bedroomWindowOpen||state.kitchenWindowOpen?'Windows open':'Windows closed', false], [state.mittensEscaped?'Mittens escaped!':state.catHunger<=20?'Mittens angry!':'Mittens home',state.mittensEscaped||state.catHunger<=20] ];
  const inventory=[['hasStepStool','Stool'],['hasWrench','Wrench'],['hasRoachSpray','Spray'],['hasMomFlashlight','Flashlight']].filter(([key])=>state[key]).map(([,name])=>name);
  document.querySelector('#environment').innerHTML=`<div class="badges">${statuses.map(([label,bad])=>`<span class="badge ${bad?'bad':''}">${label}</span>`).join('')}</div><small>Inventory: ${inventory.join(' · ')||'empty'} · Food ${state.catFoodScoops}</small>`;
  setContent(document.querySelector('#chores'),`${chores().map(([name,done,progress])=>button(`${done?'✓':'○'} ${name} <small>${done?'':escape(progress)}</small>`,`data-command="journal" class="${done?'done':''}"`)).join('')}`);
  document.querySelector('#bonus').textContent='BONUS  '+[['Vacuum','choreVacuum'],['Plants','choreWaterPlants'],['Mail','choreMailIn'],['Sweep','choreSweepKitchen'],['Mop','choreMopBathroom']].map(([name,key])=>(state[key]?'✓ ':'○ ')+name).join('   ·   ');
  const room=state.currentRoom;
  document.querySelector('.room-scene').classList.toggle('phone-open',room==='PHONE');
  document.querySelector('#hud-room').textContent=roomTitle(room);
  updateFloorNav();
  document.querySelector('#room-title').textContent=roomTitle(room);
  const hints={LIVING_ROOM:'Keep the AC running. Watch the windows.',KITCHEN:'Drink, eat, and leave the sink spotless.',GARAGE:'Clear the workbench. Keep the power on.',BEDROOM:'Mittens, homework, and a little rest.',OUTSIDE:'Mow in short bursts. The heat adds up.',MOMS_ROOM:'The dresser might have what you need.',BATHROOM:'Shower, dry, change. Take care of yourself.',BASEMENT:'Wash → dry → fold. Come back when it’s ready.',PHONE:'Keep in touch. Keep an eye on your battery.'};
  document.querySelector('#room-hint').textContent=hints[room]||'';
  document.querySelectorAll('[data-room]').forEach(b=>{b.classList.toggle('active',b.dataset.room===room);b.setAttribute('aria-current',String(b.dataset.room===room));});
  const newArtKey=JSON.stringify([room,state.roomBeforePhone,state.hidingInCloset,state.squirrelPresent,state.squirrelCount,state.roachesPresent,state.ratsPresent,state.mittensEscaped,state.catHunger<=20,state.catFoodScoops,state.laundryState,state.temperature>=75,state.windowOpen,state.bedroomWindowOpen,state.kitchenWindowOpen,state.powerOut,state.hasMomFlashlight,state.thunderstorm,state.sprinklerUsed]);
  if(newArtKey!==artKey){artKey=newArtKey;renderRoomArt(document.querySelector('#room-art'),room,state,manifest);}
  const actions=getActions(state,room);
  if(room==='PHONE') { updatePhone(actions); document.querySelector('#room-actions').hidden=true; }
  else {
    flappyCleanup?.(); flappyCleanup=null; document.querySelector('#phone').hidden=true;document.querySelector('#room-actions').hidden=false;
    const placed=layoutActions(room,actions,state);const next=JSON.stringify(placed);
    if(next!==actionKey){ actionKey=next;setContent(document.querySelector('#room-actions'),placed.map(actionHTML).join('')); }
  }
  const progress=room==='BEDROOM'&&!state.choreHomework?`Homework ${Math.min(100,Math.floor(state.choreHomeworkTicks/(state.hasStudyLamp?12:20)*100))}% — stay here to finish`:room==='BASEMENT'?`Laundry: ${['not started',`washing (${state.laundryWashTicks} ticks)`, 'ready for dryer',`drying (${state.laundryDryTicks} ticks)`,'ready to fold','folded & put away'][state.laundryState]} · Detergent ${state.laundryDetergent}`:room==='OUTSIDE'?`Lawn ${Math.round(state.choreLawnPct)}% · ${state.sprinklerUsed?'Sprinkler already used':'Sprinkler available once today'}`:'';
  document.querySelector('#room-progress').textContent=progress;
  const message=state.messages?.at(-1); const messageText=typeof message==='string'?message:message?.text;
  document.querySelector('#status').textContent=Date.now()<noticeUntil?notice:messageText||'Keep cool. Mom will be home soon.';
  document.querySelector('#pause-button').textContent=paused?'Resume':'Pause';
  app.classList.toggle('heat-danger',state.temperature>=75);
}

function updatePhone(actions) {
  const el=document.querySelector('#phone');el.hidden=false;
  const battery=el.querySelector('#phone-battery');if(battery)battery.textContent=`${Math.round(state.phoneBattery)}% ${state.phoneCharging?'⚡':''}`;
  const next=JSON.stringify([phoneTab,actions,state.phoneBattery<=5,selectedDelivery,state.deliveryTicks,state.detergentTicks,state.coldMedTicks,state.deliveryArrived,state.detergentArrived,state.coldMedArrived]);
  if(next===actionKey)return;actionKey=next;
  const oldContent=el.querySelector('#phone-content');const scrollTop=oldContent?.scrollTop||0;
  const active=document.activeElement;const focusedAction=el.contains(active)?active.dataset.action:null;
  const focusedSelect=active?.id==='phone-delivery';
  const preserved=phoneTab==='GAMES'&&flappyCleanup?el.querySelector('#flappy'):null;
  if(preserved)preserved.remove();
  const charge=actions.find(a=>a.id==='charge');
  el.innerHTML=`<div class="phone-frame source-phone"><div class="phone-speaker" aria-hidden="true"></div><div class="phone-top"><strong>12:00</strong><span id="phone-battery">${Math.round(state.phoneBattery)}% ${state.phoneCharging?'⚡':''}</span></div><div class="phone-content" id="phone-content">${renderPhoneContent(state,actions,phoneTab,selectedDelivery)}</div><div class="phone-bottom">${button('◁ Back','data-phone="BACK"')}${button('○ Home','data-phone="HOME"')}${charge?button(state.phoneCharging?'✕ Unplug':'⚡ Charge',`data-action="charge" ${charge.enabled?'':'disabled'}`):''}</div><div class="phone-hardware">${button('Put away','data-command="put-phone"')}</div></div>`;
  const content=el.querySelector('#phone-content');
  if(phoneTab==='GAMES') {
    const slot=content.querySelector('#flappy');
    if(preserved&&slot)slot.replaceWith(preserved);
    else if(slot&&state.phoneBattery>5)flappyCleanup=mountFlappy(slot,{getState:()=>state,onMessage:say,isActive:()=>!paused&&!document.hidden&&state.screen==='GAME'&&state.currentRoom==='PHONE'&&phoneTab==='GAMES'&&!modalKind});
  }
  content.scrollTop=scrollTop;
  if(focusedSelect)el.querySelector('#phone-delivery')?.focus({preventScroll:true});
  else if(focusedAction)[...el.querySelectorAll('[data-action]')].find(b=>b.dataset.action===focusedAction)?.focus({preventScroll:true});
}

function buildNightmare() {
  app.innerHTML=`<main class="nightmare-screen"><p class="eyebrow">MITTENS ISN’T MITTENS ANYMORE</p><h1>THE NIGHTMARE REALM</h1><div class="demon-cat" aria-hidden="true">▲ ◉ ◉ ▲</div><section class="circuit-board"><div class="circuit-info"><strong id="night-round"></strong><span id="night-time"></span></div><p id="night-instruction"></p><p id="night-path" class="circuit-path"></p><div id="night-nodes" class="night-nodes"></div><p id="night-progress"></p></section><p>Complete three circuits to escape. One wrong node is the end.</p></main>`;
}
function updateNightmare() {
  const n=state.nightmare;if(!n)return;
  document.querySelector('#night-round').textContent=`ROUND ${n.round} / 3`;
  document.querySelector('#night-time').textContent=n.phase==='input'?`${Math.ceil(n.secondsLeft)}s`:'';
  document.querySelector('#night-instruction').textContent=n.phase==='show'?'WATCH the circuit path…':n.phase==='input'?'YOUR TURN — tap the nodes in the shown order.':n.phase==='failed'?n.reason:'Circuit complete…';
  document.querySelector('#night-path').textContent=`PATH: ${n.sequence.map(i=>String.fromCharCode(65+i)).join(' → ')}`;
  const container=document.querySelector('#night-nodes');
  const positions=[[[230,180],[560,180],[560,330],[230,330]],[[400,110],[580,230],[510,360],[290,360],[220,230]],[[400,100],[560,170],[565,315],[400,385],[235,315],[235,170]]][n.round-1];
  if(container.querySelectorAll('button').length!==n.sequence.length) container.innerHTML='<svg class="night-wires" viewBox="0 0 800 350" aria-hidden="true"></svg>'+Array.from({length:n.sequence.length},(_,i)=>button(String.fromCharCode(65+i),`data-node="${i}" class="circuit-node"`)).join('');
  container.querySelector('svg').innerHTML=n.wires.map(([a,b])=>`<line x1="${positions[a][0]}" y1="${positions[a][1]-70}" x2="${positions[b][0]}" y2="${positions[b][1]-70}"/>`).join('');
  container.querySelectorAll('button').forEach((b,i)=>{b.style.left=`${positions[i][0]/8}%`;b.style.top=`${(positions[i][1]-70)/3.5}%`;b.disabled=n.phase!=='input';b.classList.toggle('lit',n.litNode===i);b.classList.toggle('connected',n.sequence.slice(0,n.inputIndex).includes(i));});
  document.querySelector('#night-progress').textContent=`${n.inputIndex||0} / ${n.sequence.length} nodes connected`;
}
function buildArrival() {
  app.innerHTML=`<main class="arrival-screen">${arrivalSceneHTML({lastDayDone:state.lastDayDone})}<div class="arrival-copy"><p class="eyebrow">YOU MADE IT THROUGH DAY ${state.completedDay||state.day}</p><h1>Mom’s home!</h1><p>“I’m home! How did everything go?”</p><div class="allowance"><strong>+${state.pendingAllowance} coins</strong><span>allowance · ${choreCount(state)} / 6 chores complete<br>+${state.lastDayBonus} survival & chore bonus</span></div><p>${state.lastDayDone?'Three days survived. She never knew.':'Spend your allowance, then get ready for tomorrow.'}</p>${button(state.lastDayDone?'Thanks Mom! Finish':'Go to your room →','class="primary" data-command="arrival"')}</div></main>`;
}
function buildShop() {
  app.innerHTML=`<main class="shop-screen source-shop">${shopFrameHTML(`<header><div><p class="eyebrow">BETWEEN DAYS</p><h1>Upgrade shop</h1><p>A little preparation goes a long way.</p></div><div class="shop-balance"><strong id="shop-coins"></strong>${button('Next day →','class="primary" data-command="next-day"')}</div></header><div class="shop-grid" id="shop-items"></div><p id="shop-message" role="status"></p>`)}</main>`;
}
function updateShop() {
  const shop=document.querySelector('.source-shop');if(manifest.bedroom)shop.style.backgroundImage=`url("${manifest.bedroom}")`;
  document.querySelector('#shop-coins').textContent=`★ ${state.coins} coins`;
  setContent(document.querySelector('#shop-items'),availableShopItems(state).map(item=>`<article class="shop-item">${shopItemIconHTML(item.id)}<h2>${escape(item.name)}</h2><p>${escape(item.description)}</p>${button(`${state[item.flag]?'Owned':`${item.cost} coins · Buy`}`,`data-buy="${item.id}" ${state[item.flag]||state.coins<item.cost?'disabled':''}`)}</article>`).join(''));
  document.querySelector('#shop-message').textContent=notice;
}
function buildGameOver() {
  app.innerHTML=`<main class="ending-screen"><p class="eyebrow">TOO HOT</p><h1 id="ending-title"></h1><p id="ending-reason"></p><p id="ending-stats"></p><div class="menu-buttons">${button('Play again','class="primary" data-command="new"')}${button('Main menu','data-command="menu"')}</div></main>`;
}
function updateGameOver() {
  document.querySelector('#ending-title').textContent=state.won?'MOM NEVER KNEW!':'GAME OVER';
  document.querySelector('#ending-title').className=state.won?'success':'danger';
  document.querySelector('#ending-reason').textContent=state.gameOverReason;
  document.querySelector('#ending-stats').textContent=`Day ${state.day} reached · Temperature ${Math.round(state.temperature)}° · ${state.coins} coins left`;
}

app.addEventListener('click',async e=>{
  const b=e.target.closest('button, a[data-command]'); if(!b||b.disabled)return;
  const cmd=b.dataset.command;
  if(cmd)e.preventDefault();
  if(b.dataset.floor){if(!paused&&state.screen==='GAME'){navFloor=b.dataset.floor;updateFloorNav();}return;}
  if(b.dataset.room){navigate(b.dataset.room);return;}
  if(b.dataset.action){doAction(b.dataset.action);return;}
  if(b.dataset.node){nightmareInput(state,Number(b.dataset.node));render();return;}
  if(b.dataset.phone){const target=b.dataset.phone==='BACK'?parentPhoneScreen(phoneTab):b.dataset.phone;if(target==='PUT_AWAY'){navigate(state.roomBeforePhone||'LIVING_ROOM');return;}if(state.phoneBattery<=5&&!['HOME','BACK'].includes(b.dataset.phone))return;flappyCleanup?.();flappyCleanup=null;phoneTab=target;actionKey='';render();return;}
  if(b.dataset.buy){const result=purchase(state,b.dataset.buy);say(result?state.message:'Cannot buy that item.');render();return;}
  if(cmd==='new')newGame();
  if(cmd==='menu'){state=createGame();paused=false;render();}
  if(cmd==='help')help();
  if(cmd==='credits')modal('Artwork credits',`<p><strong>Spiral Atlas Visual Novel House Backgrounds © 2023 by Spiral Atlas</strong>.</p><p>The matching house-background collection is licensed under <a href="https://creativecommons.org/licenses/by/4.0/" target="_blank" rel="noopener noreferrer">CC BY 4.0</a>. Other supplied game images retain their embedded credits and watermarks. Images are unchanged and scaled at display time.</p><p><a href="https://spiralatlas.github.io/credits/" target="_blank" rel="noopener noreferrer">House-background acknowledgements</a> · <a href="${base}credits.html" target="_blank" rel="noopener noreferrer">All game artwork credits</a></p>`);
  if(cmd==='journal')journal();
  if(cmd==='close'){if(modalKind==='pause')paused=false;closeModal();render();}
  if(cmd==='put-phone')navigate(state.roomBeforePhone||'LIVING_ROOM');
  if(cmd==='pause')togglePause();
  if(cmd==='arrival'){finishArrival(state);render();}
  if(cmd==='next-day'){resetForDay(state);notice='';render();}
  if(cmd==='fullscreen'){
    try{if(document.fullscreenElement)await document.exitFullscreen();else if(document.documentElement.requestFullscreen)await document.documentElement.requestFullscreen();else say('Fullscreen is unavailable here. You can still play normally.');}catch{say('Fullscreen is unavailable here. You can still play normally.');}
  }
});
app.addEventListener('change',e=>{if(e.target.id==='phone-delivery'){selectedDelivery=e.target.value;actionKey='';render();}});
function togglePause() {
  if(state.screen!=='GAME'&&state.screen!=='NIGHTMARE')return;
  if(modalKind==='minigame')closeModal();
  paused=!paused;
  if(paused)modal('Paused','<p>Take a breather. The house will wait.</p><p>Press Close or Escape to resume.</p>','pause');
  else closeModal();
  render();
}
document.addEventListener('keydown',e=>{
  if(e.key==='Tab'&&modalKind){
    const targets=[...document.querySelector('#modal').querySelectorAll('button:not(:disabled),a[href],[tabindex="0"]')];
    const first=targets[0],last=targets.at(-1);
    if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus();}
    else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus();}
    return;
  }
  if(e.repeat||['INPUT','TEXTAREA','SELECT'].includes(e.target.tagName))return;
  if(e.key==='Escape'){e.preventDefault();if(modalKind&&modalKind!=='pause'){closeModal();render();}else togglePause();return;}
  if(paused||modalKind)return;
  if(state.screen==='NIGHTMARE'&&/^[a-g]$/i.test(e.key)){nightmareInput(state,e.key.toUpperCase().charCodeAt(0)-65);render();return;}
  if(state.screen==='GAME'){
    if(e.key.toLowerCase()==='p')navigate('PHONE');
    if(/^[1-9]$/.test(e.key)) {const room=rooms[Number(e.key)-1];if(room)navigate(room.id);}
  }
});
// One fixed clock for the simulation, independent of renders/navigation. No catch-up
// after tab suspension on iPad; this is an explicit browser usability enhancement.
setInterval(()=>{
  if(paused||document.hidden)return;
  tick(state);render();
},500);
// Test hooks are present only under Vite's development server, never in production.
if(import.meta.env.DEV) window.__tooHot={get state(){return state;},action:doAction,navigate,tick:(n=1)=>{for(let i=0;i<n;i++)tick(state);render();},set:(patch)=>{Object.assign(state,patch);render();},newGame};
render();
