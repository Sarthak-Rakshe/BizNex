import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: [
      {
        // Only alias bare "lucide-react" imports; allow subpath imports to hit node_modules
        find: /^lucide-react$/,
        replacement: path.resolve(__dirname, "src/icons"),
      },
    ],
  },
});
