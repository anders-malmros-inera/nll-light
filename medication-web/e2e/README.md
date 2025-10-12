Lightweight Playwright E2E test for NLL Light Web

Quick start (PowerShell):

cd e2e
npm install
npx playwright install chromium
npm test

Environment variables (optional):
- APP_URL (default: http://localhost:8080)
- KEYCLOAK_USER (default: patient001)
- KEYCLOAK_PASS (default: patient001)

Notes:
- The test uses Playwright in headless Chromium mode.
- If the Keycloak login page is customized heavily, adjust selectors in `tests/login.spec.js`.
- Running tests inside CI requires Docker services up (Keycloak, NLL Light web app).
