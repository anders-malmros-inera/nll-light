const { test, expect } = require('@playwright/test');

// Lightweight test: navigate to web app, click login, perform Keycloak login, confirm redirect back

const APP_URL = process.env.APP_URL || 'http://localhost:8080';
const KEYCLOAK_USER = process.env.KEYCLOAK_USER || 'user666';
const KEYCLOAK_PASS = process.env.KEYCLOAK_PASS || 'secret';

test('login via Keycloak', async ({ page }) => {
  // Go to app and click login
  await page.goto(APP_URL + '/');
  await expect(page).toHaveURL(APP_URL + '/');

  // Click the login button/link - adjust selector if your template differs
  await page.click('text=Logga in med Keycloak');

  // Keycloak will redirect to the login page; wait for username input
  await page.waitForSelector('input[name="username"], input#username');

  // Fill credentials and submit
  await page.fill('input[name="username"], input#username', KEYCLOAK_USER);
  await page.fill('input[name="password"], input#password', KEYCLOAK_PASS);
  await page.click('button[type="submit"]');

  // After successful login, the app should redirect back to the root or a protected page
  await page.waitForURL(url => url.startsWith(APP_URL), { timeout: 10000 });

  // Basic assertion: expect a logout link or username displayed
  const loggedIn = await page.locator('text=Logga ut').count() || await page.locator(`text=${KEYCLOAK_USER}`).count();
  expect(loggedIn).toBeGreaterThan(0);
});
