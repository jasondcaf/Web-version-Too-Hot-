/** Original room-art adapter. All coordinates use Java's 800 × 455 room space.
 * Raster art is copied unchanged by sync-assets; fallback colors/shapes below
 * translate the Swing paint helpers. Gameplay controls live outside this layer.
 */
const ROOM_ASSETS = {
  MENU: 'menu', LIVING_ROOM: 'living_room', KITCHEN: 'kitchen', GARAGE: 'garage',
  BEDROOM: 'bedroom', OUTSIDE: 'outdoor', MOMS_ROOM: 'moms_room',
  BATHROOM: 'bathroom', BASEMENT: 'laundry_room',
};
const esc = (value) => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const image = (url, x, y, width, height) => `<image href="${esc(url)}" x="${x}" y="${y}" width="${width}" height="${height}" preserveAspectRatio="none"/>`;
const rect = (x,y,w,h,fill,rx=0,stroke='none') => `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" rx="${rx}" stroke="${stroke}"/>`;
const ellipse = (cx,cy,rx,ry,fill,stroke='none') => `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fill}" stroke="${stroke}"/>`;
const text = (label,x,y,fill='#dedbea',size=13) => `<text x="${x}" y="${y}" fill="${fill}" font-family="Arial,sans-serif" font-size="${size}">${esc(label)}</text>`;
const defs = `<defs>
 <linearGradient id="th-room-wall" x2="0" y2="1"><stop stop-color="#262434"/><stop offset="1" stop-color="#161420"/></linearGradient>
 <linearGradient id="th-basement-wall" x2="0" y2="1"><stop stop-color="#3e3a37"/><stop offset="1" stop-color="#302c28"/></linearGradient>
 <linearGradient id="th-appliance" x2="1" y2="1"><stop stop-color="#d2d2d7"/><stop offset="1" stop-color="#afafb6"/></linearGradient>
 <radialGradient id="th-flashlight"><stop stop-color="#ffebb4" stop-opacity=".43"/><stop offset=".6" stop-color="#ffd282" stop-opacity=".12"/><stop offset="1" stop-opacity="0"/></radialGradient>
 </defs>`;

function appliance(x, y, washer, s) {
 const active = s.laundryState === (washer ? 1 : 3);
 const done = s.laundryState >= (washer ? 2 : 4);
 return `<g transform="translate(${x},${y})">${rect(0,0,150,170,'url(#th-appliance)',8,'#8c8c94')}${rect(5,5,140,30,'#b9b9c0',4)}
 ${ellipse(133,17,5,5,active?'#00dc50':done?'#ffc800':'#3c3c41')}${ellipse(75,100,53,50,'#282832')}${ellipse(75,100,47,44,active?'#407fbe':'#161620')}
 ${active?'<g stroke="#b4d2f0" opacity=".65" stroke-width="2"><path d="M75 100L109 81M75 100L75 138M75 100L41 81"/></g>':''}
 ${done&&!active?text('✓',60,112,'#50dc64',28):''}${rect(115,92,14,22,'#a0a0a8',4)}${ellipse(21,19,9,9,'#9b9ba2')}${ellipse(43,19,9,9,'#9b9ba2')}
 ${text(washer?'WASH':'DRY',56,23,'#50505a',9)}</g>`;
}
function basement(s) {
 let blocks = '';
 for(let y=20;y<375;y+=50) {
  blocks += `<path d="M0 ${y}H800"/>`;
  for(let x=Math.floor(y/50)%2===0?0:70;x<800;x+=140) blocks+=`<path d="M${x} ${y}v50"/>`;
 }
 return `${rect(0,0,800,375,'url(#th-basement-wall)')}${rect(0,375,800,80,'#2b2825')}
 <g stroke="#282421" opacity=".5">${blocks}<path d="M120 385L210 415M420 380L500 420M650 390L720 405"/></g>
 ${rect(0,12,800,14,'#5a5046')}${rect(0,12,800,4,'#463c37')}${[80,260,440,620].map(x=>rect(x-10,8,20,22,'#4b413c',4)).join('')}${rect(0,32,800,8,'#37506e')}
 ${ellipse(400,85,60,50,'#fff0a0', 'none').replace('fill="#fff0a0"','fill="#fff0a0" opacity=".12"')}${ellipse(400,48,6,8,s.powerOut?'#807c65':'#fff5c8')}${rect(399,26,3,16,'#50463c')}
 ${appliance(140,195,true,s)}${appliance(480,195,false,s)}${text('WASHER',168,360,'#c8c3b9',11)}${text('DRYER',510,360,'#c8c3b9',11)}`;
}
function cat(s, assets) {
 const angry=s.catHunger<=20, body=angry?'#b4503c':'#c8aa82';
 let out=assets.cat?image(assets.cat,55,165,105,105):`<g transform="translate(55,240)">${ellipse(30,25,30,25,body)}${ellipse(54,-1,19,18,body)}<path d="M38 -18L43 -36L50 -18M55 -18L60 -36L68 -18" fill="${body}"/>${ellipse(30,25,15,15,angry?'#dc7864':'#e6c8aa')}${ellipse(54,-3,2,3,'#1b1514')}${ellipse(65,-3,2,3,'#1b1514')}</g>`;
 out+=text(angry?'FEED ME >:(':'purrr...',59,157,angry?'#dc5050':'#64c864',11);
 if(angry&&assets.cat) out+=rect(55,165,105,105,'#b400004f',14);
 out+=assets.cat_bowl?image(assets.cat_bowl,152,270,44,32):`${ellipse(169,283,18,9,'#c83c3c')}${s.catFoodScoops>0?ellipse(169,283,12,5,'#a0825a'):''}`;
 return out;
}
function squirrel(x,y,assets) {
 if(assets.squirrel) return image(assets.squirrel,x,y-95,95,95);
 return `<g transform="translate(${x},${y})">${ellipse(20,15,20,15,'#91693e')}${ellipse(39,0,11,10,'#91693e')}<path d="M4 20C-26 26 -14 -22 5 -3" fill="none" stroke="#a5804b" stroke-width="8" stroke-linecap="round"/>${ellipse(40,-2,2.5,2.5,'#000')}${rect(36,7,5,6,'#fff')}</g>`;
}
function rat(x,y,assets) {
 if(assets.rat) return image(assets.rat,x,y,52,34);
 return `<g transform="translate(${x},${y})">${ellipse(14,7,14,7,'#5a4e44')}${ellipse(29,2,7,5.5,'#5a4e44')}${ellipse(31,-4,3.5,3.5,'#a06464')}${ellipse(33,-.5,1.5,1.5,'#dc1e1e')}<path d="M3 10C-22 2 -17 24 -2 20" fill="none" stroke="#826450" stroke-width="2"/><path d="M6 13L4 20M14 13L16 20" stroke="#463c32" stroke-width="1.5"/></g>`;
}
function sprinkler(s,assets) {
 const url=s.sprinklerUsed?assets.sprinkler_off:(assets.sprinkler_on||assets.sprinkler);
 if(url) return image(url,30,235,s.sprinklerUsed?72:92,s.sprinklerUsed?56:70);
 return `<g transform="translate(30,235)"><path d="M30 50V78" stroke="#3c643c" stroke-width="4"/>${rect(16,42,28,14,'#4b734b',5)}${rect(25,30,10,16,'#4b734b',4)}${ellipse(16,49,4,3,'#325a32')}${ellipse(44,49,4,3,'#325a32')}</g>`;
}
function closet() {
 // Translate the warm door gap and wooden panels from GenerateCloset2.java.
 return `${rect(0,0,800,455,'#180906')}${rect(340,0,120,455,'#fff8f0')}
 ${[40,478].map(x=>`${rect(x,28,282,178,'#100603',8,'#35180c')}${rect(x,224,282,200,'#100603',8,'#35180c')}`).join('')}
 ${rect(332,0,8,455,'#060302')}${rect(460,0,8,455,'#060302')}${ellipse(312,215,6,6,'#d8b860')}${ellipse(488,215,6,6,'#d8b860')}`;
}

/** Render original supplied imagery and Java-style fallbacks. No hitboxes live here. */
export function renderRoomArt(container, roomId, state={}, assetManifest={}) {
 const assets=assetManifest.assets||assetManifest;
 const actualRoom=roomId==='PHONE'?(state.roomBeforePhone||'LIVING_ROOM'):roomId;
 const hiding=actualRoom==='MOMS_ROOM'&&state.hidingInCloset;
 const assetName=hiding?'closet_interior':ROOM_ASSETS[actualRoom];
 const url=assets[assetName];
 container.replaceChildren();
 Object.assign(container.style,{position:'absolute',inset:'0',overflow:'hidden',pointerEvents:'none',background:'#161420'});
 container.setAttribute('aria-hidden','true');
 let fallback=rect(0,0,800,455,'url(#th-room-wall)');
 if(actualRoom==='BASEMENT') fallback=basement(state);
 if(hiding) fallback=closet();
 const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');
 svg.setAttribute('viewBox','0 0 800 455');svg.setAttribute('preserveAspectRatio','none');
 Object.assign(svg.style,{position:'absolute',inset:'0',width:'100%',height:'100%'});
 let overlays='';
 if(!hiding) {
  if(actualRoom==='BEDROOM') overlays+=cat(state,assets);
  if(actualRoom==='LIVING_ROOM'&&state.squirrelPresent) overlays+=[490,400,560,350].slice(0,Math.min(state.squirrelCount||1,4)).map((x,i)=>squirrel(x,[220,255,240,265][i],assets)).join('');
  if(actualRoom==='GARAGE'&&state.ratsPresent&&!state.ratsClearedToday) overlays+=[80,200,340].map(x=>rat(x,307,assets)).join('');
  if(actualRoom==='OUTSIDE') overlays+=sprinkler(state,assets);
 }
 if(state.thunderstorm&&['OUTSIDE','GARAGE'].includes(actualRoom)) {
  overlays+=rect(0,0,800,455,'#14143c66');
  for(let x=0;x<800;x+=22) overlays+=`<path d="M${x+8} ${(x*7)%455-18}l-8 18M${x+8} ${(x*7+227)%455-18}l-8 18" stroke="#8caaDC" stroke-opacity=".4"/>`;
 }
 if(hiding) overlays+=rect(0,0,800,455,'#00000037');
 overlays+=rect(0,375,800,80,'#00000050');
 if(state.powerOut) {
  overlays+=rect(0,0,800,455,state.hasMomFlashlight?'#00001450':'#000014a0');
  if(state.hasMomFlashlight) overlays+=ellipse(400,250,336,336,'url(#th-flashlight)');
 }
 if((state.temperature||0)>=75) overlays+=rect(0,0,800,455,'#b428141a');
 svg.innerHTML=defs+`<g data-art-fallback>${fallback}</g>`+overlays;
 container.append(svg);
 const badge=document.createElement('span');
 badge.textContent=`Original ${assetName||'room'} art not supplied`;
 Object.assign(badge.style,{position:'absolute',top:'8px',left:'50%',transform:'translateX(-50%)',maxWidth:'90%',padding:'4px 9px',borderRadius:'5px',font:'10px Arial,sans-serif',color:'#c5bed3',background:'#161420c9',whiteSpace:'nowrap'});
 if(!url) container.append(badge);
 if(url) {
  const img=document.createElement('img');img.alt='';img.decoding='async';img.src=url;
  Object.assign(img.style,{position:'absolute',inset:'0',width:'100%',height:'100%',objectFit:'fill'});
  img.addEventListener('error',()=>{img.remove();container.append(badge);});
  container.insertBefore(img,svg);
  // Background fallback is hidden only after image has decoded successfully.
  img.addEventListener('load',()=>svg.querySelector('[data-art-fallback]')?.setAttribute('display','none'));
  if(img.complete&&img.naturalWidth) svg.querySelector('[data-art-fallback]').setAttribute('display','none');
 }
}
