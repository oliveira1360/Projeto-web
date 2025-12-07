import { test, expect } from '@playwright/test';
import { LoginTestPage } from '../../pages/login/LoginTestPage';

test.describe('Autenticação', () => {
    test('Deve fazer login com sucesso e redirecionar para a home', async ({ page }) => {
        const loginPage = new LoginTestPage(page);

        // 1. Mock da resposta da API de Login (Para não depender do backend real Java)
        await page.route('**/api/user/login', async route => {
            const json = { token: 'fake-jwt-token' };
            await route.fulfill({ json });
        });

        // 2. Mock da verificação de sessão
        await page.route('**/api/user/me', async route => {
            await route.fulfill({ status: 200 }); // Retorna 200 OK para dizer que está logado
        });

        // 3. Ação
        await loginPage.goto();
        await loginPage.login('test@isel.pt', 'Password123');

        // 4. Asserção
        // Verifica se fomos redirecionados para a home
        await expect(page).toHaveURL(/\/home/);
        await expect(page.getByText('Bem-vindo ao Jogo!')).toBeVisible();
    });
});