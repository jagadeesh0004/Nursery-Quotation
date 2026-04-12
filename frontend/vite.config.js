import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

const port = parseInt(process.env.VITE_PORT || '5173', 10)

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    port,
    strictPort: false,
    allowedHosts: ['moneywise-uniformly-donor.ngrok-free.dev'],
    hmr: {
      host: 'localhost',
      port,
      protocol: 'ws',
    },
  },
})