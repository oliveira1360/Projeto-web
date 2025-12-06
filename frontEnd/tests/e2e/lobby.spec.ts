import { test, expect } from '@playwright/test';
import { LobbyTestPage } from '../pages/LobbyTestPage';

test.describe('Lobbies UI & Funcionalidades', () => {

    // Dados de teste reutilizáveis
    const mockLobbiesData = [
        {
            lobbyId: 1,
            name: 'Sala Normal',
            maxPlayers: 4,
            currentPlayers: 1,
            rounds: 5
        },
        {
            lobbyId: 2,
            name: 'Sala Cheia',
            maxPlayers: 2,
            currentPlayers: 2,
            rounds: 3
        },
        {
            lobbyId: 3,
            name: 'Sala Vazia',
            maxPlayers: 6,
            currentPlayers: 0,
            rounds: 10
        }
    ];

    test.beforeEach(async ({ page }) => {
        // 1. Autenticação
        await page.route('**/api/user/me', async route => route.fulfill({ status: 200 }));

        // 2. Mock Padrão de Lobbies (pode ser sobrescrito em testes específicos)
        await page.route('**/api/lobbies', async route => {
            await route.fulfill({
                status: 200,
                json: { lobbies: mockLobbiesData, _links: {} }
            });
        });
    });

    test.describe('Renderização e Estados', () => {

        test('Deve mostrar estado de Loading enquanto carrega', async ({ page }) => {
            // Forçar o pedido a demorar para vermos o loading
            await page.route('**/api/lobbies', async route => {
                // Não chamamos fulfill imediatamente
                setTimeout(() => route.fulfill({ json: { lobbies: [], _links: {} } }), 2000);
            });

            await page.goto('/lobbies');
            await expect(page.getByText('A carregar Lobbies...')).toBeVisible();
        });

        test('Deve listar corretamente os cards dos lobbies', async ({ page }) => {
            const lobbyPage = new LobbyTestPage(page);
            await lobbyPage.goto();

            // Verificar quantidade
            await expect(page.locator('.lobby-card')).toHaveCount(3);

            // Verificar conteúdo de um card específico
            const cardNormal = page.locator('.lobby-card').filter({ hasText: 'Sala Normal' });
            await expect(cardNormal).toBeVisible();
            // Texto: "Jogadores: 1/4" (O componente usa strong, o playwight apanha o texto corrido)
            await expect(cardNormal).toContainText('Jogadores: 1/4');
        });
    });


    test.describe('Interação: Entrar (Join)', () => {

        test('Botão ENTRAR deve estar habilitado em sala com vagas', async ({ page }) => {
            const lobbyPage = new LobbyTestPage(page);
            await lobbyPage.goto();

            const card = page.locator('.lobby-card').filter({ hasText: 'Sala Normal' }); // 1/4
            await expect(card.getByRole('button', { name: 'ENTRAR' })).toBeEnabled();
        });

        test('Botão ENTRAR deve estar DESABILITADO em sala cheia', async ({ page }) => {
            const lobbyPage = new LobbyTestPage(page);
            await lobbyPage.goto();

            const card = page.locator('.lobby-card').filter({ hasText: 'Sala Cheia' }); // 2/2

            // Verifica estado disabled
            await expect(card.getByRole('button', { name: 'ENTRAR' })).toBeDisabled();

            // Garante que clicar não faz nada (opcional, o disabled já previne)
            await card.getByRole('button', { name: 'ENTRAR' }).click({ force: true });
            await expect(page).toHaveURL('/lobbies'); // Mantém-se na mesma página
        });

        test.skip('Clicar em ENTRAR deve navegar para a sala de jogo (Join bem sucedido)', async ({ page }) => {
            const lobbyPage = new LobbyTestPage(page);

            // Mock do pedido de Join
            // O componente espera o evento SSE ou timeout.
            // Aqui vamos simular o Timeout que redireciona para /lobby/:id,
            // ou podemos simular que o joinLobbyWithGameStart devolve logo se mockarmos o serviço.
            // Como estamos a testar UI e2e, vamos deixar o timeout acontecer ou o mock responder.

            // Nota: O código do lobbies.tsx faz `lobbyService.joinLobbyWithGameStart`.
            // Se não houver SSE, ele dá timeout e faz `navigate('/lobby/' + lobbyId)`.

            await page.route('**/api/lobbies/join/1', async route => {
                await route.fulfill({ json: { message: 'Joined' } });
            });

            // Mock dos detalhes para onde vamos ser redirecionados
            await page.route('**/api/lobbies/1', async route => route.fulfill({ json: mockLobbiesData[0] }));
            // Mock SSE para a sala de destino (para não dar erro lá)
            await page.route('**/api/lobbies/1/events**', async route => route.fulfill({ status: 200, body: '\n\n' }));


            await lobbyPage.goto();
            const card = page.locator('.lobby-card').filter({ hasText: 'Sala Normal' });

            await card.getByRole('button', { name: 'ENTRAR' }).click();

            // Como não simulamos o evento GAME_STARTED via SSE nesta lista,
            // o código vai esperar o timeout (2s) e navegar para a sala de espera.
            // Podemos ajustar o timeout do expect para esperar por isso.
            await expect(page).toHaveURL(/\/lobby\/1/, { timeout: 4000 });
        });
    });

    test.describe('Navegação e Header', () => {

        test('Botão "CRIAR NOVO LOBBY" deve navegar corretamente', async ({ page }) => {
            const lobbyPage = new LobbyTestPage(page);
            await lobbyPage.goto();

            await lobbyPage.createLobbyButton.click();
            await expect(page).toHaveURL('/lobbyCreation');
        });

        test('Botão "VOLTAR PARA HOME" deve navegar corretamente', async ({ page }) => {
            await page.goto('/lobbies');

            await page.getByRole('link', { name: 'VOLTAR PARA HOME' }).click();
            await expect(page).toHaveURL('/home');
        });
    });

});