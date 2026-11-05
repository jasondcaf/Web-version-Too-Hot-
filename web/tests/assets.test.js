import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,existsSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {syncRuntimeAssets} from '../scripts/asset-files.mjs';

function fixture(fn){const root=mkdtempSync(path.join(tmpdir(),'too-hot-assets-'));try{fn(root,path.join(root,'web/public/assets'));}finally{rmSync(root,{recursive:true,force:true});}}
const put=(root,file,content)=>{mkdirSync(path.dirname(path.join(root,file)),{recursive:true});writeFileSync(path.join(root,file),content);};
test('actual runtime art wins over both flat uploads and generic collection aliases',()=>fixture((root,out)=>{
  put(root,'assets/rooms/living_room.png','current game art');put(root,'living_room.jpg','flat art');put(root,'lounge.jpg','source collection');
  const {manifest,provenance}=syncRuntimeAssets(root,out);
  assert.equal(manifest.living_room,'assets/living_room.png');assert.equal(readFileSync(path.join(out,'living_room.png'),'utf8'),'current game art');
  assert.equal(provenance.living_room,'assets/rooms/living_room.png');assert.equal(readFileSync(path.join(root,'lounge.jpg'),'utf8'),'source collection');
}));
test('Windows-style filename casing and flat garage uploads work on a Linux build host',()=>fixture((root,out)=>{
  put(root,'Assets/Rooms/CAT.PNG','cat sprite');put(root,'GARAGE.JPG','garage');
  put(root,'Assets/Rooms/rat.png.png','original rat sprite');
  const {manifest,provenance}=syncRuntimeAssets(root,out);
  assert.equal(manifest.garage,'assets/garage.jpg');assert.equal(manifest.cat,'assets/cat.png');assert.equal(readFileSync(path.join(out,'garage.jpg'),'utf8'),'garage');
  assert.equal(manifest.rat,'assets/rat.png');assert.equal(readFileSync(path.join(out,'rat.png'),'utf8'),'original rat sprite');
  assert.equal(provenance.rat,'Assets/Rooms/rat.png.png');assert.equal(readFileSync(path.join(root,'Assets/Rooms/rat.png.png'),'utf8'),'original rat sprite');
}));
test('source aliases remain a fallback, credits survive, nested preview art stays out',()=>fixture((root,out)=>{
  put(root,'lounge.jpg','fallback');put(root,'assets/rooms/CREDITS.txt','artist');put(root,'assets/rooms/vn_house/garage.jpg','unselected source collection');
  put(root,'license.txt','license');put(root,'web/public/assets/old.png','stale');
  const {manifest}=syncRuntimeAssets(root,out);
  assert.equal(manifest.living_room,'assets/living_room.jpg');assert.equal(manifest.garage,undefined);
  assert.equal(existsSync(path.join(out,'old.png')),false);assert.equal(readFileSync(path.join(out,'CREDITS.txt'),'utf8'),'artist');
  assert.equal(readFileSync(path.join(out,'license.txt'),'utf8'),'license');assert.equal(existsSync(path.join(root,'assets/rooms/vn_house/garage.jpg')),true);
}));
