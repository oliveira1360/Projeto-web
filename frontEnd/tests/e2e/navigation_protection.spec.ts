import { test, expect } from '@playwright/test';

test.describe('Proteção de Rotas', () => {

    test('Deve redirecionar para login se tentar aceder a /home sem autenticação', async ({ page }) => {
        // Mock a dizer que NÃO está logado
        await page.route('**/api/user/me', async route => {
            await route.fulfill({ status: 401 });
        });

        await page.goto('/home');

        // Deve ser "kickado" para a raiz (login)
        await expect(page).toHaveURL('http://localhost:5173/');
        // Verifica se vê o formulário de login
        await expect(page.locator('.sign-in-container')).toBeVisible();
    });

    test('Deve redirecionar para login se tentar aceder a /game/1 sem autenticação', async ({ page }) => {
        await page.route('**/api/user/me', async route => {
            await route.fulfill({ status: 401 });
        });

        await page.goto('/game/1');
        await expect(page).toHaveURL('http://localhost:5173/');
    });
});