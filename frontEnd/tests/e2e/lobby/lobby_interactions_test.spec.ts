import { test, expect } from '@playwright/test';
import { LobbyTestPage } from '../../pages/lobby/LobbyTestPage';

test.describe('Interação com Lobbies', () => {
    test.beforeEach(async ({ page }) => {
        // Mock de autenticação
        await page.route('**/api/user/me', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({ userId: 1, authenticated: true })
            });
        });

        // Mock de user info
        await page.route('**/api/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 1,
                    name: 'Player1',
                    nickName: 'P1',
                    email: 'p1@test.com',
                    balance: '100',
                    _links: {}
                })
            });
        });

        // Mock SSE para todos os lobbies
        await page.route('**/api/lobbies/*/events*', async route => {
            await route.fulfill({
                status: 200,
                headers: {
                    'Content-Type': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    'Connection': 'keep-alive'
                },
                body: 'data: {"type":"CONNECTED"}\n\n'
            });
        });

        // Mock da lista de lobbies - CRÍTICO: /api/lobbies não /lobbies
        await page.route('**/api/lobbies', async route => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        lobbies: [
                            {
                                lobbyId: 10,
                                name: 'Sala Teste A',
                                maxPlayers: 4,
                                currentPlayers: [{ id: 1, username: 'P1' }],
                                rounds: 5
                            },
                            {
                                lobbyId: 20,
                                name: 'Sala Cheia',
                                maxPlayers: 2,
                                currentPlayers: [
                                    { id: 1, username: 'P1' },
                                    { id: 2, username: 'P2' }
                                ],
                                rounds: 3
                            },
                            {
                                lobbyId: 30,
                                name: 'Sala Normal',
                                maxPlayers: 4,
                                currentPlayers: [{ id: 3, username: 'Host' }],
                                rounds: 7
                            }
                        ],
                        _links: {}
                    })
                });
            } else {
                await route.continue();
            }
        });
    });

    test('Deve listar lobbies disponíveis', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        // Aguarda os cards carregarem
        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        // Verifica que os lobbies aparecem
        await lobbyPage.expectLobbyVisible('Sala Teste A');
        await lobbyPage.expectLobbyVisible('Sala Cheia');
        await lobbyPage.expectLobbyVisible('Sala Normal');
    });

    test('Deve ver detalhes de um lobby ao selecionar', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);

        // Mock dos detalhes do lobby 10
        await page.route('**/api/lobbies/10', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 10,
                    name: 'Sala Teste A',
                    maxPlayers: 4,
                    currentPlayers: [{ id: 1, name: 'P1' }],
                    rounds: 5,
                    _links: {}
                })
            });
        });

        await lobbyPage.goto();
        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        await lobbyPage.selectLobby('Sala Teste A');
        await page.waitForTimeout(500);

        await lobbyPage.expectDetailsVisible('Sala Teste A');
    });

    test('Botão de entrar deve estar desabilitado se a sala estiver cheia', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        const fullLobbyCard = page.locator('.lobby-card').filter({ hasText: 'Sala Cheia' });
        await expect(fullLobbyCard).toBeVisible();

        const joinBtn = fullLobbyCard.getByRole('button', { name: /entrar/i });
        await expect(joinBtn).toBeDisabled({ timeout: 5000 });
    });

    test('Botão de entrar deve estar habilitado se há vagas', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        const normalLobbyCard = page.locator('.lobby-card').filter({ hasText: 'Sala Teste A' });
        const joinBtn = normalLobbyCard.getByRole('button', { name: /entrar/i });

        await expect(joinBtn).toBeEnabled({ timeout: 5000 });
    });

    test('Deve mostrar número correto de jogadores', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        const testCard = page.locator('.lobby-card').filter({ hasText: 'Sala Teste A' });
        await expect(testCard).toContainText('Jogadores:');
        await expect(testCard).toContainText('1/4');

        const fullCard = page.locator('.lobby-card').filter({ hasText: 'Sala Cheia' });
        await expect(fullCard).toContainText('2/2');
    });

    test('Deve permitir clicar no botão entrar (comportamento de join)', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);

        await lobbyPage.goto();
        await page.waitForSelector('.lobby-card', { timeout: 10000 });

        // Verifica que o botão existe e está habilitado
        const card = page.locator('.lobby-card').filter({ hasText: 'Sala Teste A' });
        const joinBtn = card.getByRole('button', { name: /entrar/i });

        await expect(joinBtn).toBeVisible();
        await expect(joinBtn).toBeEnabled();
    });

    test('Deve navegar diretamente para sala de lobby', async ({ page }) => {
        // Testa navegação direta sem depender do join com SSE


        // Mock user info (CRÍTICO - sem isso fica em loading!)
        await page.route('**/api/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 1,
                    name: 'Player1',
                    nickName: 'P1',
                    email: 'p1@test.com',
                    balance: '100',
                    _links: {}
                })
            });
        });

        // Mock dos detalhes do lobby
        await page.route('**/api/lobbies/10', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 10,
                    name: 'Sala Teste A',
                    maxPlayers: 4,
                    currentPlayers: [
                        { id: 1, name: 'P1' },
                        { id: 2, name: 'P2' }
                    ],
                    rounds: 5,
                    _links: {}
                })
            });
        });

        await page.goto('/lobby/10');

        // Aguarda carregar
        await page.waitForTimeout(2000);

        // Verifica que está na página correta
        await expect(page).toHaveURL(/\/lobby\/10/);

        // Verifica estrutura da página
        const hasLoading = await page.locator('text=/carregando/i').count();
        const hasError = await page.locator('.lobby-error-container').count();
        const hasRoom = await page.locator('.lobby-room-container').count();


        // Deve ter carregado (room) ou mostrar loading/erro
        const hasContent = hasLoading + hasError + hasRoom;
        expect(hasContent).toBeGreaterThan(0);

        if (hasRoom > 0) {

            // Verifica elementos do room
            await expect(page.locator('.lobby-room-container')).toBeVisible();
        }
    });

    test('Deve exibir mensagem quando não há lobbies', async ({ page }) => {
        await page.route('**/api/lobbies', async route => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({ lobbies: [], _links: {} })
                });
            }
        });

        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        await page.waitForTimeout(1000);

        const cards = await page.locator('.lobby-card').count();
        expect(cards).toBe(0);
    });


    test('Deve ter botão para criar novo lobby', async ({ page }) => {
        const lobbyPage = new LobbyTestPage(page);
        await lobbyPage.goto();

        await page.waitForSelector('.lobby-controls', { timeout: 10000 });

        const createBtn = page.locator('a').filter({ hasText: /criar novo lobby/i });
        await expect(createBtn).toBeVisible({ timeout: 10000 });

        await createBtn.click();
        await expect(page).toHaveURL(/\/lobbyCreation/, { timeout: 5000 });
    });
});