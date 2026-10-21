import { defineConfig } from "vite";
import { fileURLToPath, URL } from "node:url";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { VitePWA } from "vite-plugin-pwa";

// https://vite.dev/config/
export default defineConfig({
  server: {
    host: "0.0.0.0",
    port: 3000,
    allowedHosts: ["all"]
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.svg"],
      manifest: {
        name: "Climate Insights",
        short_name: "Climate Insights",
        description:
          "Istraži klimatske podatke gradova kroz poređenje sa istorijskim trendovima.",
        theme_color: "#0B0E11",
        background_color: "#0B0E11",
        display: "standalone",
        start_url: "/",
        icons: [
          {
            src: "favicon.svg",
            sizes: "any",
            type: "image/svg+xml",
          },
        ],
      },
      workbox: {
        // Historijski CSV podaci se mijenjaju rijetko (ručni update) —
        // CacheFirst osigurava da app radi i offline jednom kad su
        // podaci preuzeti makar jednom.
        runtimeCaching: [
          {
            urlPattern: /\/data\/csv\/.*\.csv$/,
            handler: "CacheFirst",
            options: {
              cacheName: "historical-csv-cache",
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 * 30, // 30 dana
              },
            },
          },
          {
            urlPattern: /\/data\/cities\.json$/,
            handler: "StaleWhileRevalidate",
            options: {
              cacheName: "cities-config-cache",
            },
          },
          {
            // Live API (Open-Meteo) — uvijek pokušaj mrežu prvo,
            // jer su to "trenutni" podaci; ne keširaj dugoročno.
            urlPattern: /^https:\/\/api\.open-meteo\.com\/.*/,
            handler: "NetworkFirst",
            options: {
              cacheName: "live-weather-cache",
              networkTimeoutSeconds: 5,
              expiration: {
                maxEntries: 20,
                maxAgeSeconds: 60 * 30, // 30 min
              },
            },
          },
        ],
      },
    }),
  ],
});
