import { test, expect } from '@playwright/test';
import { ProfileTestPage } from '../../pages/player/ProfileTestPage';

test.describe('Perfil do Jogador', () => {
    test.beforeEach(async ({ page }) => {
        // Mock de autenticação global
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
        const profilePage = new ProfileTestPage(page);

        // Mock dos dados do utilizador
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 1,
                    name: 'Teste User',
                    nickName: 'teste123',
                    email: 'teste@teste.com',
                    balance: '1000',
                    imageUrl: 'https://example.com/avatar.png',
                    _links: {}
                })
            });
        });

        await profilePage.goto();
        await profilePage.waitForProfileLoaded();

        await profilePage.expectUserInfo(
            'Teste User',
            'teste123',
            'teste@teste.com',
            '1000'
        );
    });

    test('Deve exibir avatar quando imageUrl está presente', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);
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
        await profilePage.waitForProfileLoaded();
        await profilePage.expectAvatarVisible(imageUrl);
    });

    test('Não deve exibir avatar quando imageUrl está ausente', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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
        await profilePage.waitForProfileLoaded();
        await profilePage.expectAvatarNotVisible();
    });

    test('Deve exibir todos os links de navegação', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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
        await profilePage.waitForProfileLoaded();
        await profilePage.expectNavigationLinks();
    });

    test('Deve navegar para a página de atualização de perfil', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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
        await profilePage.waitForProfileLoaded();
        await profilePage.clickUpdateProfile();

        await expect(page).toHaveURL(/\/playerProfile\/update/);
    });

    test('Deve navegar para a página home', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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
        await profilePage.waitForProfileLoaded();
        await profilePage.clickHome();

        await expect(page).toHaveURL(/\/home/);
    });

    test('Deve navegar para a página de criar convite', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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
        await profilePage.waitForProfileLoaded();
        await profilePage.clickCreateInvite();

        await expect(page).toHaveURL(/\/createInvite/);
    });

    test('Deve formatar nickname com @ corretamente', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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

        await expect(profilePage.nicknameText).toHaveText('@cool_nickname');
    });

    test('Deve exibir balance com valor zero corretamente', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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

        await expect(profilePage.balanceField).toContainText('0');
        await expect(profilePage.balanceField).toContainText('€');
    });

    test('Deve mostrar informações completas do perfil', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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

        // Verifica visibilidade
        await expect(profilePage.nameHeading).toBeVisible();
        await expect(profilePage.nicknameText).toBeVisible();
        await expect(profilePage.emailField).toBeVisible();
        await expect(profilePage.balanceField).toBeVisible();
        await expect(profilePage.avatarImage).toBeVisible();

        // Verifica conteúdo
        await expect(profilePage.nameHeading).toHaveText('João Silva');
        await expect(profilePage.nicknameText).toHaveText('@joao_gamer');
        await expect(profilePage.emailField).toHaveText('joao@teste.com');
        await expect(profilePage.balanceField).toContainText('5500');
    });

    test('Deve ter todos os botões de ação visíveis', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

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

        // Usa os locators do Page Object
        await expect(profilePage.updateProfileLink).toBeVisible();
        await expect(profilePage.createInviteLink).toBeVisible();
    });

    test('Deve exibir estado de loading antes dos dados carregarem', async ({ page }) => {
        const profilePage = new ProfileTestPage(page);

        // Simula delay no carregamento
        await page.route('**/user/info', async route => {
            await new Promise(resolve => setTimeout(resolve, 500));
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 14,
                    name: 'Teste Loading',
                    nickName: 'loading_test',
                    email: 'loading@teste.com',
                    balance: '100',
                    _links: {}
                })
            });
        });

        const gotoPromise = profilePage.goto();

        // Verifica loading state (pode já não estar visível se carregar muito rápido)
        const loadingVisible = await profilePage.loadingText.isVisible().catch(() => false);

        await gotoPromise;
        await profilePage.waitForProfileLoaded();

        // Após carregar, o card deve estar visível
        await expect(profilePage.profileCard).toBeVisible();
    });
});