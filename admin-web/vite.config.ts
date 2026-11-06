import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
export default defineConfig({ plugins: [react()], server: { port: 5173, strictPort: true },
    resolve: { alias: [{ find: /^query-string$/, replacement: fileURLToPath(new URL('./src/queryStringCompat.mjs', import.meta.url)) }] }
});
