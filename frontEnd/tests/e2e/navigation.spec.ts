import { test, expect } from '@playwright/test';
import { HomeTestPage } from '../pages/HomeTestPage';

test.describe('Navegação Home', () => {
    test.beforeEach(async ({ page }) => {
        // Autenticação Mock
        await page.route('**/api/user/me', async route => route.fulfill({ status: 200 }));
    });

    test('Deve navegar da Home para Lobbies', async ({ page }) => {
        const home = new HomeTestPage(page);
        await home.goto();

        await expect(home.title).toBeVisible();
        await home.clickLobbies();

        await expect(page).toHaveURL(/\/lobbies/);
    });

    test('Deve navegar da Home para Perfil', async ({ page }) => {
        // Mock específico para quando carregar o perfil
        await page.route('**/api/user/info', async route => {
            await route.fulfill({
                json: { name: 'User', nickName: 'Nick', email: 'a@a.com', balance: '0', _links: {} }
            });
        });

        const home = new HomeTestPage(page);
        await home.goto();
        await home.clickProfile();

        await expect(page).toHaveURL(/\/playerProfile/);
    });
});