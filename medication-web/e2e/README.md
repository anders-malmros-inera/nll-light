Lightweight Playwright E2E test for medication-web

Quick start (PowerShell):

cd e2e
npm install
npx playwright install chromium
npm test

Environment variables (optional):
- APP_URL (default: http://localhost:8080)
- KEYCLOAK_USER (default: user666)
- KEYCLOAK_PASS (default: secret)

Notes:
- The test uses Playwright in headless Chromium mode.
- If the Keycloak login page is customized heavily, adjust selectors in `tests/login.spec.js`.
- Running tests inside CI requires Docker services up (Keycloak, medication-web).
