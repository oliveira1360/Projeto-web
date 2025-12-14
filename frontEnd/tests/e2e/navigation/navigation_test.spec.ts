import { test, expect } from '@playwright/test';
import { HomeTestPage } from '../../pages/navigation/HomeTestPage';
import { AboutTestPage } from '../../pages/navigation/AboutTestPage';
import { InviteTestPage } from '../../pages/navigation/InviteTestPage';
import { LobbiesTestPage } from '../../pages/navigation/LobbiesTestPage';
import { ProfileTestPage } from '../../pages/player/ProfileTestPage';

test.describe('Navegação', () => {
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

        // Mock padrão para user info (usado em várias páginas)
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

        // Mock para lobbies
        await page.route('**/lobbies', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbies: []
                })
            });
        });
    });

    test.describe('Navegação a partir da Home', () => {
        test('Deve exibir título e botões na página Home', async ({ page }) => {
            const home = new HomeTestPage(page);
            await home.goto();

            await expect(home.title).toBeVisible();
            await expect(home.lobbiesButton).toBeVisible();
            await expect(home.profileButton).toBeVisible();
            await expect(home.aboutButton).toBeVisible();
        });

        test('Deve navegar da Home para Lobbies', async ({ page }) => {
            const home = new HomeTestPage(page);
            const lobbies = new LobbiesTestPage(page);

            await home.goto();
            await expect(home.title).toBeVisible();

            await home.clickLobbies();
            await expect(page).toHaveURL(/\/lobbies/);

            await lobbies.expectPageVisible();
        });

        test('Deve navegar da Home para Perfil do Jogador', async ({ page }) => {
            const home = new HomeTestPage(page);
            const profile = new ProfileTestPage(page);

            await home.goto();
            await home.clickProfile();

            await expect(page).toHaveURL(/\/playerProfile/);
            await profile.waitForProfileLoaded();
        });

        test('Deve navegar da Home para Sobre', async ({ page }) => {
            const home = new HomeTestPage(page);
            const about = new AboutTestPage(page);

            await home.goto();
            await home.clickAbout();

            await expect(page).toHaveURL(/\/about/);
            await about.expectPageVisible();
        });
    });

    test.describe('Navegação a partir do Perfil', () => {
        test('Deve navegar do Perfil para Home', async ({ page }) => {
            const profile = new ProfileTestPage(page);
            const home = new HomeTestPage(page);

            await profile.goto();
            await profile.waitForProfileLoaded();

            await profile.clickHome();

            await expect(page).toHaveURL(/\/home/);
            await expect(home.title).toBeVisible({ timeout: 10000 });
        });

        test('Deve navegar do Perfil para Update Profile', async ({ page }) => {
            const profile = new ProfileTestPage(page);

            await profile.goto();
            await profile.waitForProfileLoaded();

            await profile.clickUpdateProfile();

            await expect(page).toHaveURL(/\/playerProfile\/update/);
            await expect(page.locator('.update-profile-container')).toBeVisible({ timeout: 10000 });
        });

        test('Deve navegar do Perfil para Create Invite', async ({ page }) => {
            const profile = new ProfileTestPage(page);
            const invite = new InviteTestPage(page);

            await profile.goto();
            await profile.waitForProfileLoaded();

            await profile.clickCreateInvite();

            await expect(page).toHaveURL(/\/createInvite/);
            await invite.expectPageVisible();
        });
    });

    test.describe('Navegação a partir de About', () => {
        test('Deve navegar de About para Home', async ({ page }) => {
            const about = new AboutTestPage(page);
            const home = new HomeTestPage(page);

            await about.goto();
            await about.expectPageVisible();

            await about.clickHome();

            await expect(page).toHaveURL(/\/home/);
            await expect(home.title).toBeVisible({ timeout: 10000 });
        });

        test('Deve exibir informações da equipa', async ({ page }) => {
            const about = new AboutTestPage(page);

            await about.goto();
            await about.expectPageVisible();
            await about.expectTeamMembers();
        });
    });

    test.describe('Navegação a partir de Create Invite', () => {
        test('Deve navegar de Create Invite para Home', async ({ page }) => {
            const invite = new InviteTestPage(page);
            const home = new HomeTestPage(page);

            await invite.goto();
            await invite.expectPageVisible();

            await invite.clickHome();

            await expect(page).toHaveURL(/\/home/);
            await expect(home.title).toBeVisible({ timeout: 10000 });
        });
    });


    test.describe('Navegação circular', () => {
        test('Deve conseguir fazer ciclo: Home → Perfil → Home → About → Home', async ({ page }) => {
            const home = new HomeTestPage(page);
            const profile = new ProfileTestPage(page);
            const about = new AboutTestPage(page);

            // Home → Perfil
            await home.goto();
            await expect(home.title).toBeVisible();
            await home.clickProfile();
            await expect(page).toHaveURL(/\/playerProfile/);
            await profile.waitForProfileLoaded();

            // Perfil → Home
            await profile.clickHome();
            await expect(page).toHaveURL(/\/home/);

            // Home → About
            await home.clickAbout();
            await expect(page).toHaveURL(/\/about/);
            await about.expectPageVisible();

            // About → Home
            await about.clickHome();
            await expect(page).toHaveURL(/\/home/);

            // Verifica que está de volta à home
            await expect(home.title).toBeVisible();
        });

        test('Deve conseguir fazer ciclo: Home → Lobbies → Back', async ({ page }) => {
            const home = new HomeTestPage(page);
            const lobbies = new LobbiesTestPage(page);

            // Home → Lobbies
            await home.goto();
            await home.clickLobbies();
            await expect(page).toHaveURL(/\/lobbies/);
            await lobbies.expectPageVisible();

            // Verifica navegação com botões do browser
            await page.goBack();
            await expect(page).toHaveURL(/\/home/);

            // Testa navegação para frente
            await page.goForward();
            await expect(page).toHaveURL(/\/lobbies/);
        });
    });

    test.describe('Breadcrumbs e navegação por URL direta', () => {
        test('Deve conseguir aceder diretamente à página de perfil via URL', async ({ page }) => {
            const profile = new ProfileTestPage(page);

            await profile.goto();
            await profile.waitForProfileLoaded();
            await expect(page).toHaveURL(/\/playerProfile/);
        });

        test('Deve conseguir aceder diretamente à página About via URL', async ({ page }) => {
            const about = new AboutTestPage(page);

            await about.goto();
            await about.expectPageVisible();
            await expect(page).toHaveURL(/\/about/);
        });


        test('Deve conseguir aceder diretamente à página Create Invite via URL', async ({ page }) => {
            const invite = new InviteTestPage(page);

            await invite.goto();
            await invite.expectPageVisible();
            await expect(page).toHaveURL(/\/createInvite/);
        });
    });

    test.describe('Links e botões', () => {
        test('Todos os links na Home devem ser clicáveis', async ({ page }) => {
            const home = new HomeTestPage(page);
            await home.goto();

            // Verifica que os botões têm href correto
            await expect(home.lobbiesButton).toHaveAttribute('href', /lobbies/);
            await expect(home.profileButton).toHaveAttribute('href', /playerProfile/);
            await expect(home.aboutButton).toHaveAttribute('href', /about/);
        });

        test('Link Home no perfil deve funcionar', async ({ page }) => {
            const profile = new ProfileTestPage(page);

            await profile.goto();
            await profile.waitForProfileLoaded();

            await expect(profile.homeLink).toBeVisible();
            await expect(profile.homeLink).toHaveAttribute('href', /home/);
        });

        test('Link Back to Home no Create Invite deve funcionar', async ({ page }) => {
            const invite = new InviteTestPage(page);

            await invite.goto();
            await invite.expectPageVisible();

            await expect(invite.homeLink).toBeVisible();
            await expect(invite.homeLink).toHaveAttribute('href', /home/);
        });

        test('Link Home no About deve funcionar', async ({ page }) => {
            const about = new AboutTestPage(page);

            await about.goto();
            await about.expectPageVisible();

            await expect(about.homeLink).toBeVisible();
            await expect(about.homeLink).toHaveAttribute('href', /home/);
        });
    });
});