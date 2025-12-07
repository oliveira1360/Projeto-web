import { test, expect } from '@playwright/test';
import { ProfileTestPage } from '../../pages/player/ProfileTestPage';

test.describe('Perfil do Jogador', () => {
    let profilePage: ProfileTestPage;

    test.beforeEach(async ({ page }) => {
        profilePage = new ProfileTestPage(page);


        await page.route('**/user/me', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 1,
                    authenticated: true
                })
            });
        });
    });

    test('Deve exibir os dados do jogador corretamente', async ({ page }) => {

        // Mock dos dados do utilizador
        await page.route('**/user/info', async route => {
            const responseData = {
                userId: 1,
                name: 'Teste User',
                nickName: 'teste123',
                email: 'teste@teste.com',
                balance: '1000',
                imageUrl: 'https://example.com/avatar.png',
                _links: {}
            };

            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify(responseData)
            });
        });

        await profilePage.goto();

        // Aguarda um pouco
        await page.waitForTimeout(1000);

        // Verifica o HTML da página
        const bodyHTML = await page.locator('body').innerHTML();

        // Verifica se estamos na página correta (não no login)
        const isLoginPage = bodyHTML.includes('sign-in-container') || bodyHTML.includes('Create Account');

        if (isLoginPage) {
        }

        // Verifica se o container do perfil existe
        const containerExists = await page.locator('.player-profile-container').count();

        // Verifica se o card aparece
        const cardExists = await page.locator('.player-profile-card').count();

        // Tira screenshot
        await page.screenshot({ path: 'debug-profile-page.png', fullPage: true });

        await profilePage.expectUserInfo(
            'Teste User',
            'teste123',
            'teste@teste.com',
            '1000'
        );

    });

    test('Deve exibir avatar quando imageUrl está presente', async ({ page }) => {
        const imageUrl = 'https://example.com/avatar.png';

        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 2,
                    name: 'Teste Avatar',
                    nickName: 'avatar_test',
                    email: 'avatar@teste.com',
                    balance: '500',
                    imageUrl: imageUrl,
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.expectAvatarVisible(imageUrl);
    });

    test('Não deve exibir avatar quando imageUrl está ausente', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 3,
                    name: 'Teste Sem Avatar',
                    nickName: 'no_avatar',
                    email: 'noavatar@teste.com',
                    balance: '250',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.expectAvatarNotVisible();
    });

    test('Deve exibir todos os links de navegação', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 5,
                    name: 'Teste Links',
                    nickName: 'links_test',
                    email: 'links@teste.com',
                    balance: '100',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.expectNavigationLinks();
    });

    test('Deve navegar para a página de atualização de perfil', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 6,
                    name: 'Teste Update',
                    nickName: 'update_test',
                    email: 'update@teste.com',
                    balance: '750',
                    imageUrl: 'https://example.com/pic.jpg',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.clickUpdateProfile();

        await expect(page).toHaveURL(/.*playerProfile\/update/, { timeout: 10000 });
    });

    test('Deve navegar para a página home', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 7,
                    name: 'Teste Home',
                    nickName: 'home_test',
                    email: 'home@teste.com',
                    balance: '300',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.clickHome();

        await expect(page).toHaveURL(/.*\/home/, { timeout: 10000 });
    });

    test('Deve navegar para a página de criar convite', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 8,
                    name: 'Teste Invite',
                    nickName: 'invite_test',
                    email: 'invite@teste.com',
                    balance: '200',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.clickCreateInvite();

        await expect(page).toHaveURL(/.*createInvite/, { timeout: 10000 });
    });

    test('Deve formatar nickname com @ corretamente', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 10,
                    name: 'Teste Nickname',
                    nickName: 'cool_nickname',
                    email: 'nickname@teste.com',
                    balance: '888',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.waitForProfileLoaded();

        await expect(profilePage.nicknameText).toHaveText('@cool_nickname', { timeout: 10000 });
    });

    test('Deve exibir balance com valor zero corretamente', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 11,
                    name: 'Teste Zero',
                    nickName: 'zero_balance',
                    email: 'zero@teste.com',
                    balance: '0',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.waitForProfileLoaded();

        await expect(profilePage.balanceField).toContainText('0', { timeout: 10000 });
        await expect(profilePage.balanceField).toContainText('€', { timeout: 10000 });
    });

    test('Deve mostrar informações completas do perfil', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 12,
                    name: 'João Silva',
                    nickName: 'joao_gamer',
                    email: 'joao@teste.com',
                    balance: '5500',
                    imageUrl: 'https://example.com/joao.jpg',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.waitForProfileLoaded();

        await expect(profilePage.nameHeading).toBeVisible({ timeout: 10000 });
        await expect(profilePage.nicknameText).toBeVisible({ timeout: 10000 });
        await expect(profilePage.emailField).toBeVisible({ timeout: 10000 });
        await expect(profilePage.balanceField).toBeVisible({ timeout: 10000 });
        await expect(profilePage.avatarImage).toBeVisible({ timeout: 10000 });

        await expect(profilePage.nameHeading).toHaveText('João Silva', { timeout: 10000 });
        await expect(profilePage.nicknameText).toHaveText('@joao_gamer', { timeout: 10000 });
        await expect(profilePage.emailField).toHaveText('joao@teste.com', { timeout: 10000 });
        await expect(profilePage.balanceField).toContainText('5500', { timeout: 10000 });
    });

    test('Deve ter todos os botões de ação visíveis', async ({ page }) => {
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 13,
                    name: 'Teste Botões',
                    nickName: 'botoes_test',
                    email: 'botoes@teste.com',
                    balance: '300',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.waitForProfileLoaded();

        const updateLink = page.locator('a', { hasText: 'Update Profile' });
        const inviteLink = page.locator('a', { hasText: 'Create Invite' });

        await expect(updateLink).toBeVisible({ timeout: 10000 });
        await expect(inviteLink).toBeVisible({ timeout: 10000 });
    });
});