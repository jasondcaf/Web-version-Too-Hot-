import { existsSync, mkdirSync, readdirSync, copyFileSync, writeFileSync, rmSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const web = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = path.resolve(web, '../assets/rooms');
const root = path.resolve(web, '..');
const target = path.join(web, 'public/assets');
const names = ['menu','living_room','kitchen','garage','bedroom','outdoor','moms_room','bathroom','laundry_room','basement','cat','cat_bowl','squirrel','closet_interior','roach_ai','rat','sprinkler_on','sprinkler_off','sprinkler'];
// This is generated output only. Remove stale copies when the source art changes.
rmSync(target, {recursive:true,force:true});
mkdirSync(target, { recursive: true });
const manifest = {};
const suppliedCollection = {
  menu:'outside2.jpg', living_room:'lounge.jpg', bedroom:'single bedroom.jpg',
  moms_room:'master bedroom.jpg', kitchen:'kitchen.jpg', bathroom:'bathroom.jpg', outdoor:'outside1.jpg'
};
for (const name of names) {
  for (const ext of ['.jpg','.png','.jpeg']) {
    const file = path.join(source, name + ext);
    if (!existsSync(file)) continue;
    copyFileSync(file, path.join(target, name + ext));
    manifest[name] = 'assets/' + name + ext;
    break;
  }
  if (!manifest[name] && suppliedCollection[name] && existsSync(path.join(root,suppliedCollection[name]))) {
    copyFileSync(path.join(root,suppliedCollection[name]),path.join(target,name+'.jpg'));
    manifest[name]='assets/'+name+'.jpg';
  }
}
// Preserve supplied credits alongside the selected runtime assets; no preview downloads.
if (existsSync(source)) for (const file of readdirSync(source)) {
  if (/credit|licen[cs]e|attribution|readme/i.test(file) && /\.(txt|md|html)$/i.test(file)) copyFileSync(path.join(source,file), path.join(target,file));
}
for(const file of ['Credits and Information.txt','license.txt']) if(existsSync(path.join(root,file))) copyFileSync(path.join(root,file),path.join(target,file));
writeFileSync(path.join(target,'manifest.json'), JSON.stringify(manifest,null,2) + '\n');
console.log(`Prepared ${Object.keys(manifest).length} original game images.`);
const missing = names.slice(0,9).filter(name => !manifest[name]);
if (missing.length) console.warn(`Missing original backgrounds: ${missing.join(', ')}. Java-style fallbacks will be shown; see PARITY.md.`);
