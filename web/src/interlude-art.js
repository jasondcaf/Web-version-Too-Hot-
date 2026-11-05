/** SVG translations of ShopPanel / MomArrivalPanel's Java2D drawings.
 * Illustration only: callers retain state, purchases, scoring and navigation.
 */
const R=(x,y,w,h,fill,rx=0,extra='')=>`<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" ${extra}/>`;
const E=(x,y,w,h,fill,extra='')=>`<ellipse cx="${x+w/2}" cy="${y+h/2}" rx="${w/2}" ry="${h/2}" fill="${fill}" ${extra}/>`;
const P=(d,fill,extra='')=>`<path d="${d}" fill="${fill}" ${extra}/>`;
const S=(content,viewBox,cls='')=>`<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}" class="${cls}" aria-hidden="true" focusable="false">${content}</svg>`;

/** Pass existing header, #shop-items, #shop-message and buttons as innerHTML. */
export function shopFrameHTML(innerHTML) {
 const grilles=[...Array(44)].map((_,i)=>`M${60+i*5} 9v44M${400+i*5} 9v44`).join('');
 const bottom=S(`${R(0,0,680,68,'#23232d',18)}<path d="${grilles}" stroke="#32323c" stroke-width="1"/>${E(322,7,16,16,'#00ff3c', 'opacity=".24"')}${E(326,11,8,8,'#00be00')}`, '0 0 680 68','source-tv-speakers');
 const stand=S(`<defs><linearGradient id="source-tv-pole"><stop stop-color="#32323c"/><stop offset="1" stop-color="#23232d"/></linearGradient><linearGradient id="source-tv-base"><stop stop-color="#373741"/><stop offset="1" stop-color="#1e1e28"/></linearGradient></defs>${R(295,0,100,20,'url(#source-tv-pole)',6,'stroke="#464650"')}${R(205,18,280,18,'url(#source-tv-base)',8,'stroke="#464650"')}`, '0 0 690 46','source-tv-stand');
 return `<div class="source-tv-set"><div class="source-tv-body"><div class="source-tv-screen">${innerHTML}</div>${bottom}</div>${stand}</div>`;
}

/** The same tiny shop-card pictures drawn by ShopPanel.drawItemIcon. */
export function shopItemIconHTML(key) {
 let shapes='';
 switch(key) {
 case 'fan': shapes=`<path d="M24 22L36 26M24 22L20 34M24 22L12 18M24 22L28 10" stroke="#64b4dc" stroke-width="3" stroke-linecap="round"/>${E(20,18,8,8,'#3c8cb4')}`;break;
 case 'snack': shapes=R(16,10,16,22,'#dcb428',4)+R(17,12,14,6,'#c83232',3)+R(17,20,14,10,'#ffdc3c',3);break;
 case 'bottle': shapes=R(19,10,10,22,'#50b4e6',5)+R(20,17,8,12,'#3c96c878',4)+R(21,8,6,5,'#a0c8dc',2);break;
 case 'treats': shapes=E(17,18,14,12,'#e6b482')+E(15,12,7,7,'#e6b482')+E(23,11,7,7,'#e6b482')+E(26,18,7,7,'#e6b482')+E(19,20,10,8,'#d2966e');break;
 case 'roach': shapes=R(15,18,18,12,'#8c7850',3)+`<path d="M18 18v12M22 18v12M26 18v12M30 18v12" stroke="#645537"/>`+P('M19 6l9 9M28 6l-9 9','none','stroke="#dc3c3c" stroke-width="2"');break;
 case 'gen': shapes=R(14,13,20,16,'#468250',3)+R(20,10,8,4,'#5a5a5a')+P('M25 15l-6 8h5l-2 6 8-10h-6l1-4','#ffd73c');break;
 case 'stool': shapes=R(14,14,20,5,'#b48c50')+R(14,27,20,5,'#b48c50')+P('M18 19v8M30 19v8','none','stroke="#9b763e" stroke-width="2.5"');break;
 case 'chill': shapes=E(14,16,12,12,'#50a0dc')+E(22,16,12,12,'#dc5050')+E(14,16,12,6,'#ffffff50');break;
 case 'ice': shapes=E(16,14,16,16,'#b4e6ff')+P('M14 22h20M19 13l10 18M29 13L19 31','none','stroke="#64bef0" stroke-width="1.5"');break;
 case 'soap': shapes=R(17,11,14,20,'#dcf0ff',4)+R(18,14,12,6,'#82c8f0',3)+E(21,12,5,4,'#ffffffb4');break;
 case 'lamp': shapes=E(12,6,24,20,'#ffffc864')+E(16,10,16,14,'#ffdc50')+R(22,24,4,10,'#c8a032')+R(16,32,16,3,'#c8a032');break;
 case 'squirt': shapes=R(15,18,18,10,'#b4dc82',3)+P('M28 15q8-9 14-1M31 19q6-7 10-1','none','stroke="#64aaf0b4" stroke-width="1.5"');break;
 case 'mousetrap': shapes=R(13,24,22,5,'#a08c64')+P('M15 19a9 9 0 0 1 18 0','none','stroke="#b4b4b4" stroke-width="2"')+E(20,20,8,5,'#c8aa50')+E(22,21,4,3,'#f0d23c');break;
 // Java's medicine-kit card has no drawItemIcon case; retain its blank icon area.
 }
 return S(shapes,'0 0 48 44','source-shop-icon');
}

const stars=[[38,22],[82,54],[158,18],[232,44],[318,28],[405,52],[488,18],[565,42],[632,26],[714,55],[768,35],[62,88],[128,72],[205,105],[288,85],[372,108],[455,88],[528,62],[602,95],[678,72],[748,98],[18,138],[95,118],[175,148],[260,122],[345,155],[428,135],[510,112],[588,145],[662,128],[740,118]];
function houseWindow(x,y,w,h) {
 return `${R(x-6,y-6,w+12,h+12,'#ffd27830')}${R(x,y,w,h,'url(#source-home-glass)')}${R(x,y,11,h,'#daaf625a')}${R(x+w-11,y,11,h,'#daaf625a')}${R(x,y,w,h,'none',0,'stroke="#ded2b9" stroke-width="3.5"')}${P(`M${x+w/2} ${y}v${h}M${x} ${y+h/2}h${w}`,'none','stroke="#ded2b9" stroke-width="2"')}${R(x-4,y+h,w+8,7,'#c8bca5')}${R(x+2,y+2,w/3,h/3,'#ffffff3c')}`;
}
function house() {
 let blocks='';
 for(let row=0;row<7;row++)for(let col=(row%2===0?0:35)-35;col<365;col+=68)blocks+=R(85+col,262+row*30,60,24,'none',3,'stroke="#bcaf984b"');
 const garagePanels=[0,1,2,3].map(p=>R(423,330+p*32,102,32,'none',0,'stroke="#807869"')+P(`M474 ${330+p*32}v32`,'none','stroke="#807869"')).join('');
 return `${R(97,476,344,14,'#00000041')}${R(415,302,118,180,'#bcb29e')}${R(423,330,102,128,'#948c7d')}${garagePanels}${E(466,314,16,14,'#ffd76455')}
 ${R(85,252,330,230,'#d4c8b2')}<g clip-path="url(#source-brick-clip)">${blocks}</g>
 ${P('M61 252H439L250 132Z','#3e2818','stroke="#523a24" stroke-width="2.5"')}${P('M250 132L439 252L268 142Z','#28180c')}${R(61,244,378,12,'#624830')}${R(240,127,20,12,'#694e32')}
 ${R(343,157,36,112,'#875844')}${R(339,148,44,14,'#986952')}<g class="source-chimney-smoke">${E(341,117,30,28,'#a5a2af46')}${E(345,92,24,22,'#918e9e23')}</g>
 ${houseWindow(103,294,70,62)}${houseWindow(327,294,70,62)}${houseWindow(224,200,52,44)}
 ${R(202,327,96,158,'#64462a',7)}${R(207,332,86,153,'#341c0e',5)}${R(214,340,34,52,'#261208',4,'stroke="#442a16"')}${R(252,340,34,52,'#261208',4,'stroke="#442a16"')}${R(214,402,72,52,'#261208',4,'stroke="#442a16"')}${E(276,420,13,13,'#d7ac3a','stroke="#af8726"')}
 ${R(207,304,86,24,'#ffe1946e',0,'stroke="#64462a" stroke-width="2"')}${P('M235 304v28M264 304v28','none','stroke="#64462a" stroke-width="2"')}${E(169,270,90,68,'#ffcd5055')}${E(242,276,12,12,'#ffcd50')}${R(85,252,330,230,'none',0,'stroke="#948773" stroke-width="1.5"')}${E(93,282,30,22,'#ffd27846')}${E(377,282,30,22,'#ffd27846')}`;
}
function wheel(x,y) {
 return `<g transform="translate(${x} ${y})">${E(-22,-22,44,44,'#121212')}${E(-16,-16,32,32,'url(#source-wheel-rim)')}${E(-8,-8,16,16,'#878276')}<g class="source-wheel-spokes">${[0,1,2,3,4].map(i=>P(`M0 0L${Math.cos(i*Math.PI*2/5)*12} ${Math.sin(i*Math.PI*2/5)*12}`,'none','stroke="#d7d2c6" stroke-width="2.2" stroke-linecap="round"')).join('')}</g></g>`;
}
function car() {
 return `<g class="source-car-motion"><g transform="translate(495 498)">${E(0,52,215,20,'#00000037')}${R(0,24,212,40,'#af2323',9)}${P('M20 24H192L178-8H34Z','#af2323')}${R(0,30,212,8,'#d74444')}${P('M37 24H175L170-5H42Z','#94cce4af')}${R(6,-6,26,28,'#80b9d29b',4,'stroke="#2a201c" stroke-width="1.5"')}${R(180,-6,26,28,'#80b9d29b',4,'stroke="#2a201c" stroke-width="1.5"')}${wheel(42,62)}${wheel(165,62)}${R(-5,30,8,24,'#ff4848af',3)}${E(-18,22,22,40,'#ff373746')}${R(209,30,8,18,'#fffccdd2',3)}${E(195,20,24,32,'#fff8b95a')}${P('M105 24V64M34 40H176','none','stroke="#8a1616" stroke-width="1.2"')}${R(120,36,18,5,'#d7ac41',2)}${R(68,36,18,5,'#d7ac41',2)}${R(-6,56,14,8,'#372d28',3)}${R(204,56,14,8,'#372d28',3)}</g></g>`;
}
function mom() {
 return `<g class="source-mom-motion"><g transform="translate(305 388)">${E(-24,132,64,15,'#0000002d')}${R(-5,98,14,44,'#28160e',5)}${R(-14,138,26,9,'#28160e',4)}${P('M-22 60H30L44 108H-38Z','#943766')}${R(-10,60,10,48,'#a84876')}${R(10,60,10,48,'#7a2a55')}<g class="source-mom-leg">${R(2,96,14,44,'#321e12',5)}${R(-4,137,26,10,'#321e12',4)}</g>${R(-20,26,50,40,'#d2cddc',9)}${R(6,26,14,40,'#bcb8ca')}${E(-4,22,18,14,'#e6e4ee')}${R(-22,58,52,7,'#7d2d55')}${E(3,59,9,6,'#cda234')}
 <g class="source-mom-bag">${R(22,28,13,40,'#dab494',5)}${R(26,50,32,36,'#cdbe98',5)}${P('M30 49a9 7 0 0 1 18 0','none','stroke="#a0916c" stroke-width="2.2"')}${R(26,62,32,8,'#589158')}${E(28,46,14,11,'#418c3c')}${E(38,44,12,9,'#418c3c')}${R(50,38,8,24,'#d2a85a',3)}</g>
 <g class="source-mom-arm">${R(-24,28,13,38,'#dab494',5)}${E(-24,62,13,12,'#dab494')}</g>${R(1,14,10,15,'#dab494')}${E(-17,-38,46,54,'#e1bc9b')}${E(10,-30,16,40,'#c8a584')}${P('M-18-23A24 17 0 0 1 30-23Z','#341c0a')}${R(-18,-32,9,34,'#341c0a',4)}${R(21,-32,9,30,'#341c0a',4)}${E(17,-52,20,18,'#341c0a')}${P('M-12-28Q0-47 12-32','#503012')}${E(-8,-18,8,8,'#1c120a')}${E(8,-18,8,8,'#1c120a')}${E(-7,-16,3,3,'#fff')}${E(9,-16,3,3,'#fff')}${P('M-10-24L-3-26M8-26L15-24','none','stroke="#2a1608" stroke-width="2" stroke-linecap="round"')}${E(2,-8,7,6,'#bc9678')}${P('M-3 2Q5 10 13 2','none','stroke="#a85050" stroke-width="2" stroke-linecap="round"')}${E(-13,-7,13,9,'#e68a8255')}${E(12,-7,13,9,'#e68a8255')}${E(-20,-16,7,7,'#d7ac37','stroke="#af8723"')}</g></g>`;
}

/** Full illustrated scene; caller renders allowance labels/continue controls. */
export function arrivalSceneHTML({lastDayDone=false}={}) {
 const sky=`<defs><linearGradient id="source-night-sky" x2="0" y2="1"><stop stop-color="#080620"/><stop offset="1" stop-color="#1c103c"/></linearGradient><linearGradient id="source-horizon" x2="0" y2="1"><stop stop-color="#c88228" stop-opacity="0"/><stop offset="1" stop-color="#c88228" stop-opacity=".22"/></linearGradient><linearGradient id="source-lawn" x2="0" y2="1"><stop stop-color="#163016"/><stop offset="1" stop-color="#0c1e0c"/></linearGradient><linearGradient id="source-home-glass"><stop stop-color="#ffda87d2"/><stop offset="1" stop-color="#d7a23ac3"/></linearGradient><linearGradient id="source-wheel-rim"><stop stop-color="#c8c3b6"/><stop offset="1" stop-color="#9b968a"/></linearGradient><clipPath id="source-brick-clip">${R(85,252,330,230,'#fff')}</clipPath></defs>
 ${R(0,0,800,480,'url(#source-night-sky)')}${R(0,420,800,60,'url(#source-horizon)')}${stars.map(([x,y],i)=>E(x,y,x%5===0?3:2,x%5===0?3:2,'#fff',`class="source-star" style="animation-delay:-${i*.17}s"`)).join('')}${E(668,10,96,96,'#fff8c316')}${E(656,0,120,120,'#fff8c30e')}${E(692,24,60,60,'#eee8b9')}${E(702,24,56,56,'#100a2c')}${E(698,58,14,11,'#c3be9446')}${E(710,44,9,8,'#c3be9432')}${R(0,480,800,140,'url(#source-lawn)')}`;
 const lamp=`${E(610,458,140,50,'#ffc85020')}${R(676,305,8,175,'#37342e')}${R(646,305,38,7,'#37342e')}${R(640,288,28,20,'#2c2a26',5)}${E(643,291,22,14,'#ffe16ec8')}${E(628,275,52,44,'#ffeba050')}`;
 const ground=`${P('M388 480H570L800 544H800Z','#6c645a')}${P('M420 480L690 544M510 480L772 544','none','stroke="#58504864" stroke-width="1.2"')}${R(0,544,800,20,'#807a70')}${P('M0 544H800M0 564H800','none','stroke="#6c665c"')}${[0,145,290,435,580,725].map(x=>P(`M${x} 544v20`,'none','stroke="#625c528c"')).join('')}${R(0,564,800,56,'#202024')}${P('M0 592H800','none','stroke="#c8b43096" stroke-width="3.5" stroke-dasharray="32 22"')}`;
 const speech=lastDayDone?'“So proud of you!  ♥”':'“You better have cleaned up!”';
 const bubble=`<g class="source-arrival-speech">${R(178,273,260,40,'#00000030',15)}${R(175,270,260,40,'#fffffff5',15,'stroke="#9b98ac" stroke-width="1.5"')}${P('M299 310L306 346L313 310','#fffffff5','stroke="#9b98ac" stroke-width="1.5"')}<text x="305" y="296" fill="#28243c" text-anchor="middle" font-family="Arial,sans-serif" font-size="13" font-weight="bold">${speech}</text></g>`;
 return `<div class="source-arrival-scene">${S(sky+house()+lamp+ground+car()+mom()+bubble,'0 0 800 620','source-arrival-illustration')}</div>`;
}
