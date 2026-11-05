import test from 'node:test';
import assert from 'node:assert/strict';
import {createGame} from '../src/engine.js';
import {phoneActions} from '../src/actions.js';
import {parentPhoneScreen,renderPhoneContent} from '../src/phone-view.js';
test('phone Back follows Java contact → messages → home → previous room flow',()=>{
  for(const contact of ['MSG_MOM','MSG_FRIEND'])assert.equal(parentPhoneScreen(contact),'MESSAGES');
  for(const app of ['MESSAGES','DELIVERY','MUSIC','GAMES'])assert.equal(parentPhoneScreen(app),'HOME');
  assert.equal(parentPhoneScreen('HOME'),'PUT_AWAY');
});
test('Mom and Friend conversations expose only their own call and text actions',()=>{
  const s=createGame(),actions=phoneActions(s);
  const mom=renderPhoneContent(s,actions,'MSG_MOM'),friend=renderPhoneContent(s,actions,'MSG_FRIEND');
  assert.match(mom,/data-action="call-mom"/);assert.doesNotMatch(mom,/data-action="call-friend"/);
  assert.match(friend,/data-action="call-friend"/);assert.doesNotMatch(friend,/data-action="call-mom"/);
});
test('delivery selection retains selected product and disables ordering without coins',()=>{
  const s=createGame();s.coins=0;
  const view=renderPhoneContent(s,phoneActions(s),'DELIVERY','order-Drinks');
  assert.match(view,/<option[^>]*value="order-Drinks"[^>]*selected/);
  assert.match(view,/<button[^>]*id="phone-order"[^>]*disabled/);
});
