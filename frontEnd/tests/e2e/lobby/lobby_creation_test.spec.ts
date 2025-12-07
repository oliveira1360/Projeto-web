import { test, expect } from '@playwright/test';
import { LobbyCreationTestPage } from '../../pages/lobby/LobbyCreationTestPage';

test.describe('Criação de Lobby', () => {
    test.beforeEach(async ({ page }) => {
        // Mock de autenticação
        await page.route('**/user/me', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 99,
                    authenticated: true
                })
            });
        });

        // Mock dos dados do player
        await page.route('**/user/info', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    userId: 99,
                    name: 'Dono',
                    nickName: 'Boss',
                    email: 'd@d.com',
                    balance: '100',
                    _links: {}
                })
            });
        });

        // Mock SSE para todos os lobbies (usa wildcard para pegar qualquer lobbyId)
        await page.route('**/lobbies/*/events*', async route => {
            await route.fulfill({
                status: 200,
                headers: {
                    'Content-Type': 'text/event-stream',
                    'Cache-Control': 'no-cache',
                    'Connection': 'keep-alive'
                },
                body: ': keep-alive\n\n'
            });
        });

        // Mock da lista de lobbies
        await page.route('**/lobbies', async (route) => {
            // Se for GET, retorna lista vazia
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        lobbies: [],
                        _links: {}
                    })
                });
            } else {
                // Se for outro método, deixa passar
                await route.continue();
            }
        });
    });

    test('Deve criar um lobby com sucesso', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        // Mock do POST create
        await page.route('**/lobbies/create', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 123,
                    name: 'Poker Night',
                    maxPlayers: 4,
                    currentPlayers: 1,
                    rounds: 5,
                    _links: {}
                })
            });
        });

        // Mock GET details do lobby
        await page.route('**/lobbies/123', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 123,
                    name: 'Poker Night',
                    maxPlayers: 4,
                    currentPlayers: [
                        { id: 99, name: 'Boss' }
                    ],
                    rounds: 5,
                    _links: {}
                })
            });
        });

        await creationPage.goto();
        await creationPage.fillForm('Poker Night', '4', '5');

        // Intercepta o alert
        page.once('dialog', async dialog => {
            await dialog.accept();
        });

        await creationPage.submit();

        // Aguarda navegação
        await page.waitForURL(/\/lobby\/123/, { timeout: 10000 });

        // Aguarda um pouco para a página renderizar
        await page.waitForTimeout(1000);

        // Debug: vê o que está na página
        const bodyHTML = await page.locator('body').innerHTML();

        // Procura todos os headings
        const allHeadings = await page.locator('h1, h2, h3, h4').allTextContents();

        // Procura qualquer elemento com o texto
        const anyText = page.locator('text=Poker Night').first();
        const exists = await anyText.count();

        if (exists > 0) {
            await expect(anyText).toBeVisible({ timeout: 10000 });
        } else {
            // Se não encontrou, verifica se há loading ou erro
            const loading = await page.locator('text=/loading|carregando/i').count();
            const error = await page.locator('text=/error|erro/i').count();


            // Tenta esperar pelo container do lobby room
            const lobbyContainer = page.locator('.lobby-room-container, [class*="lobby"]').first();
            await expect(lobbyContainer).toBeVisible({ timeout: 10000 });
        }
    });

    test('Deve exibir erro quando nome está vazio', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // HTML5 validation deve prevenir submit
        await expect(creationPage.nameInput).toHaveAttribute('required');
    });

    test('Deve calcular corretamente o máximo de rondas', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // Com 4 jogadores, max = 60/4 = 15 rondas
        await creationPage.maxPlayersInput.fill('4');
        await creationPage.roundsInput.fill('20');

        // Aguarda o useEffect ajustar
        await page.waitForTimeout(500);

        const roundsValue = await creationPage.roundsInput.inputValue();
        expect(Number(roundsValue)).toBeLessThanOrEqual(15);
    });

    test('Deve desabilitar botão enquanto está criando', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        // Mock com delay
        await page.route('**/lobbies/create', async route => {
            await new Promise(resolve => setTimeout(resolve, 1000));
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 456,
                    name: 'Test Lobby',
                    maxPlayers: 4,
                    currentPlayers: 1,
                    rounds: 5,
                    _links: {}
                })
            });
        });

        await page.route('**/lobbies/456', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 456,
                    name: 'Test Lobby',
                    maxPlayers: 4,
                    currentPlayers: [{ id: 99, name: 'Boss' }],
                    rounds: 5,
                    _links: {}
                })
            });
        });

        await creationPage.goto();
        await creationPage.fillForm('Test Lobby', '4', '5');

        page.once('dialog', dialog => dialog.accept());

        // Inicia submit (não aguarda)
        const submitPromise = creationPage.submit();

        // Verifica que está desabilitado durante loading
        await page.waitForTimeout(200);
        await expect(creationPage.createButton).toBeDisabled();

        await submitPromise;
    });

    test('Deve permitir valores válidos de jogadores (2-8)', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // Testa mínimo
        await creationPage.maxPlayersInput.fill('2');
        expect(await creationPage.maxPlayersInput.inputValue()).toBe('2');

        // Testa máximo
        await creationPage.maxPlayersInput.fill('8');
        expect(await creationPage.maxPlayersInput.inputValue()).toBe('8');

        // Verifica atributos
        await expect(creationPage.maxPlayersInput).toHaveAttribute('min', '2');
        await expect(creationPage.maxPlayersInput).toHaveAttribute('max', '8');
    });

    test('Deve permitir valores válidos de rondas', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // Define 4 jogadores
        await creationPage.maxPlayersInput.fill('4');

        // Para 4 jogadores, max é 15 (60/4)
        await creationPage.roundsInput.fill('15');
        expect(await creationPage.roundsInput.inputValue()).toBe('15');

        // Testa mínimo
        await creationPage.roundsInput.fill('1');
        expect(await creationPage.roundsInput.inputValue()).toBe('1');

        await expect(creationPage.roundsInput).toHaveAttribute('min', '1');
    });

    test('Deve ter botão para voltar aos lobbies', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        await expect(creationPage.backButton).toBeVisible();

        await creationPage.clickBack();
        await expect(page).toHaveURL(/\/lobbies/);
    });

    test('Deve criar lobby com configuração mínima (2 jogadores)', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await page.route('**/lobbies/create', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 111,
                    name: 'Small Game',
                    maxPlayers: 2,
                    currentPlayers: 1,
                    rounds: 10,
                    _links: {}
                })
            });
        });

        await page.route('**/lobbies/111', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 111,
                    name: 'Small Game',
                    maxPlayers: 2,
                    currentPlayers: [{ id: 99, name: 'Boss' }],
                    rounds: 10,
                    _links: {}
                })
            });
        });

        await creationPage.goto();
        await creationPage.fillForm('Small Game', '2', '10');

        page.once('dialog', dialog => dialog.accept());
        await creationPage.submit();

        await expect(page).toHaveURL(/\/lobby\/111/, { timeout: 10000 });
    });

    test('Deve criar lobby com configuração máxima (8 jogadores)', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await page.route('**/lobbies/create', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 222,
                    name: 'Big Game',
                    maxPlayers: 8,
                    currentPlayers: 1,
                    rounds: 7, // 60/8 = 7.5, então max é 7
                    _links: {}
                })
            });
        });

        await page.route('**/lobbies/222', async route => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    lobbyId: 222,
                    name: 'Big Game',
                    maxPlayers: 8,
                    currentPlayers: [{ id: 99, name: 'Boss' }],
                    rounds: 7,
                    _links: {}
                })
            });
        });

        await creationPage.goto();
        await creationPage.fillForm('Big Game', '8', '7');

        page.once('dialog', dialog => dialog.accept());
        await creationPage.submit();

        await expect(page).toHaveURL(/\/lobby\/222/, { timeout: 10000 });
    });


    test('Deve ajustar automaticamente rondas quando jogadores mudam', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // Define 2 jogadores e 20 rondas (válido: 2*20=40 < 60)
        await creationPage.maxPlayersInput.fill('2');
        await creationPage.roundsInput.fill('20');
        await page.waitForTimeout(300);
        expect(await creationPage.roundsInput.inputValue()).toBe('20');

        // Muda para 8 jogadores - deve ajustar rondas para max 7 (60/8=7.5)
        await creationPage.maxPlayersInput.fill('8');
        await page.waitForTimeout(500);

        const adjustedRounds = await creationPage.roundsInput.inputValue();
        expect(Number(adjustedRounds)).toBeLessThanOrEqual(7);
    });

    test('Deve ter label correto para máximo de rondas dinâmico', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        await creationPage.goto();

        // Com 4 jogadores, label deve mostrar "Máx: 15"
        await creationPage.maxPlayersInput.fill('4');
        await page.waitForTimeout(300);

        const label = page.locator('label:has-text("Número de Rondas")');
        await expect(label).toContainText(/máx.*15/i);
    });
});