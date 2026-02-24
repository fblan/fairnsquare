import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vite.dev/config/
export default defineConfig({
  plugins: [svelte()],
  server: {
    allowedHosts: ['honest-camels-happen.loca.lt'],
  },
  resolve: {
    alias: {
      $lib: path.resolve(__dirname, './src/lib'),
    },
    // Ensure browser conditions are used (important for Svelte 5)
    conditions: ['browser', 'development'],
  },
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{js,ts}'],
    setupFiles: ['src/test/setup.ts'],
    alias: {
      $lib: path.resolve(__dirname, './src/lib'),
    },
    // Ensure browser conditions for Svelte 5 in tests
    server: {
      deps: {
        inline: [/svelte/],
      },
    },
    // Force browser export conditions
    resolve: {
      conditions: ['browser', 'development'],
    },
  },
})
