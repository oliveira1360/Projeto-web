import { test, expect } from '@playwright/test';
import { ProfileTestPage } from '../pages/ProfileTestPage';

test.describe('Perfil do Jogador', () => {
    test.beforeEach(async ({ page }) => {
        await page.route('**/api/user/me', async route => route.fulfill({ status: 200 }));
    });

    test('Deve exibir os dados do jogador corretamente', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

        // Mock dos dados do utilizador
        await page.route('**/api/user/info', async route => {
            await route.fulfill({
                status: 200,
                json: {
                    userId: 1,
                    name: 'Diogo Oliveira',
                    nickName: 'TheAce',
                    email: 'diogo@isel.pt',
                    balance: '5000',
                    imageUrl: 'http://fake.url/img.png',
                    _links: {}
                }
            });
        });

        await profilePage.goto();
        await profilePage.expectUserInfo('Diogo Oliveira', 'diogo@isel.pt', '5000');
    });
});