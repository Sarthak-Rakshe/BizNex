# BizNex Frontend

## Configure API Base URL

- Create `.env` (for local) or set environment variable at build time:
  - `VITE_API_BASE=https://api.example.com`
- Runtime override (optional): set `window.__BIZNEX_API_BASE__` before loading the app if you need to point to a different API without rebuilding.

## Build and Preview

```bash
npm run build
npm run preview
```

## Deploy behind a subpath

- If serving under `/biznex/`, set Vite base in `vite.config.js`:
  ```js
  export default defineConfig({ base: "/biznex/" /* ... */ });
  ```

## Icons and antivirus note

- We alias `lucide-react` to a local wrapper to avoid antivirus false positives and only import needed icons.
