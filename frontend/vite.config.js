import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El proxy envía /api al backend Spring Boot (puerto configurado en application.properties).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
