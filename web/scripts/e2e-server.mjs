// Dedicated development server: test hooks stay available, while filesystem
// activity and the user's interactive dev server cannot reset an active game.
import { createServer } from 'vite';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

process.env.E2E = '1';
const root = fileURLToPath(new URL('../', import.meta.url));
const synced = spawnSync(process.execPath, ['scripts/sync-assets.mjs'], { cwd: root, stdio: 'inherit' });
if (synced.status !== 0) process.exit(synced.status || 1);
const server = await createServer({
  root,
  server: { host: '127.0.0.1', port: 5174, strictPort: true, hmr: false, watch: null },
});
await server.listen();
server.printUrls();
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.once(signal, async () => { await server.close(); process.exit(0); });
}
