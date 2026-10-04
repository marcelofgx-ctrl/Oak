import { defineConfig } from 'vite'
export default defineConfig({
 publicDir: false,
 build: { outDir: 'dist-android', emptyOutDir: true },
})
