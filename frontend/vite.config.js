import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/auth/internal": {
        target: "http://localhost:8081",
        changeOrigin: true,
        secure: false,
        headers: {
          "X-Internal-Gateway-Token": "DigitalWalletInternalSecret2026",
        },
      },
      "/api": {
        target: "http://localhost:8090",
        changeOrigin: true,
        secure: false,
      },
    },
  },
  preview: {
    port: 4173,
  },
});
