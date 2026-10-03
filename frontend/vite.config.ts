/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// O build vai direto para META-INF/resources do jar do módulo, de onde o
// Spring Boot serve os arquivos estáticos.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'target/classes/META-INF/resources',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
