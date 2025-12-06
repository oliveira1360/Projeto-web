import { test, expect } from '@playwright/test';
import { LobbyCreationTestPage } from '../pages/LobbyCreationTestPage';
import { Readable } from 'stream'; // Importação necessária para o fix

test.describe('Criação de Lobby', () => {
    test.beforeEach(async ({ page }) => {
        // Mock da Autenticação
        await page.route('**/api/user/me', async route => route.fulfill({ status: 200 }));

        // Mock dos dados do Player
        await page.route('**/api/user/info', async route => {
            await route.fulfill({
                json: { userId: 99, name: 'Dono', nickName: 'Boss', email: 'd@d.com', balance: '100', _links: {} }
            });
        });

        // Usamos um stream que nunca fecha para simular uma conexão persistente
        await page.route('**/api/lobbies/*/events**', async route => {
            const stream = new Readable();
            stream._read = () => {}; // Implementação vazia necessária
            stream.push(': connected\n\n'); // Envia um comentário SSE para confirmar a conexão

            await route.fulfill({
                status: 200,
                contentType: 'text/event-stream',
                body: stream // O Playwright vai manter a conexão aberta
            });
        });
    });

    test('Deve criar um lobby com sucesso', async ({ page }) => {
        const creationPage = new LobbyCreationTestPage(page);

        //Mock do POST create
        await page.route('**/api/lobbies/create', async route => {
            await route.fulfill({
                status: 200,
                json: {
                    lobbyId: 123,
                    name: 'Poker Night',
                    maxPlayers: 4,
                    currentPlayers: 1,
                    rounds: 5,
                    _links: {}
                }
            });
        });

        //Mock GET details
        await page.route('**/api/lobbies/123', async route => {
            await route.fulfill({
                json: {
                    lobbyId: 123,
                    name: 'Poker Night',
                    maxPlayers: 4,
                    currentPlayers: 1,
                    rounds: 5,
                    _links: {}
                }
            });
        });

        await creationPage.goto();
        await creationPage.fillForm('Poker Night', '4', '5');

        page.once('dialog', dialog => dialog.accept());

        await creationPage.submit();

        // Validação
        await expect(page).toHaveURL(/\/lobby\/123/);


        await expect(page.getByRole('heading', { name: 'Poker Night' })).toBeVisible();
    });
});