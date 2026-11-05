import { existsSync, mkdirSync, readdirSync, copyFileSync, writeFileSync, rmSync } from 'node:fs';
import path from 'node:path';

export const runtimeNames = ['menu','living_room','kitchen','garage','bedroom','outdoor','moms_room','bathroom','laundry_room','basement','cat','cat_bowl','squirrel','closet_interior','roach_ai','rat','sprinkler_on','sprinkler_off','sprinkler'];
const collection = { menu:'outside2', living_room:'lounge', bedroom:'single bedroom', moms_room:'master bedroom', kitchen:'kitchen', bathroom:'bathroom', outdoor:'outside1' };
const entries = folder => existsSync(folder) ? readdirSync(folder,{withFileTypes:true}) : [];
const directory = (folder, name) => {
  const entry=entries(folder).find(e=>e.isDirectory()&&e.name.toLowerCase()===name.toLowerCase());
  return path.join(folder,entry?.name||name);
};
function image(folder, name) {
  const files=entries(folder).filter(e=>e.isFile());
  for(const ext of ['.jpg','.png','.jpeg']) {
    const file=files.find(e=>e.name.toLowerCase()===(name+ext).toLowerCase());
    if(file)return {source:path.join(folder,file.name),ext};
  }
  return null;
}

/** Prefer actual desktop runtime art, including flat GitHub image uploads.
 * Case-insensitive lookup reproduces Windows filenames on Linux hosts.
 * Large nested source-art/preview collections are never copied recursively.
 */
export function syncRuntimeAssets(root,target) {
  const rooms=directory(directory(root,'assets'),'rooms');
  rmSync(target,{recursive:true,force:true});mkdirSync(target,{recursive:true});
  const manifest={},provenance={};
  for(const name of runtimeNames) {
    const selected=image(rooms,name)||image(root,name)||(collection[name]?image(root,collection[name]):null);
    if(!selected)continue;
    const file=name+selected.ext;copyFileSync(selected.source,path.join(target,file));
    manifest[name]='assets/'+file;provenance[name]=path.relative(root,selected.source).replaceAll('\\','/');
  }
  for(const entry of entries(rooms)) if(entry.isFile()&&/credit|licen[cs]e|attribution|readme/i.test(entry.name)&&/\.(txt|md|html)$/i.test(entry.name)) copyFileSync(path.join(rooms,entry.name),path.join(target,entry.name));
  for(const file of ['Credits and Information.txt','license.txt'])if(existsSync(path.join(root,file)))copyFileSync(path.join(root,file),path.join(target,file));
  writeFileSync(path.join(target,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
  writeFileSync(path.join(target,'sources.json'),JSON.stringify(provenance,null,2)+'\n');
  return {manifest,provenance};
}
