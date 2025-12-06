import { test, expect } from '@playwright/test';
import { GameTestPage } from '../pages/GameTestPage';
import { Readable } from 'stream';

test.describe('Game UI & Mecânicas', () => {
    let gamePage: GameTestPage;
    const GAME_ID = 100;
    const USER_ID = 1;

    test.beforeEach(async ({ page }) => {
        gamePage = new GameTestPage(page);

        // 1. Mock Auth
        await page.route('**/api/user/me', async route => route.fulfill({ status: 200 }));
        await page.route('**/api/user/info', async route => route.fulfill({
            json: { userId: USER_ID, name: 'Hero', nickName: 'Hero', email: 'h@h.com', balance: '100' }
        }));

        // 2. Mock SSE (Ligação persistente)
        await page.route(`**/api/game/${GAME_ID}/events**`, async route => {
            const stream = new Readable();
            stream._read = () => {};
            stream.push(': connected\n\n');
            await route.fulfill({ status: 200, contentType: 'text/event-stream', body: stream });
        });
    });

    test.describe('Inicialização e Estados', () => {
        test('Deve mostrar Loading e depois o Tabuleiro', async ({ page }) => {
            // Mocks de sucesso
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: { players: [{ playerId: 1, username: 'Hero' }, { playerId: 2, username: 'Villain' }], _links: {} }
            }));
            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({ json: { hand: [], _links: {} } }));
            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 0, maxRoundNumber: 7, turn: 1, _links: {} }
            }));
            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({ json: { players: [], _links: {} } }));
            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => route.fulfill({ json: { remainingSeconds: 60, _links: {} } }));

            await gamePage.goto(GAME_ID);

            // Validar Tabuleiro carregado
            await expect(gamePage.startButton).toBeVisible();

            await expect(gamePage.playerSlot('Hero')).toBeVisible();
            await expect(gamePage.playerSlot('Villain')).toBeVisible();

            // Validar que sou eu
            await gamePage.expectPlayerIsMe('Hero');
        });
    });

    test.describe('Jogabilidade (O meu turno)', () => {
        test.beforeEach(async ({ page }) => {
            // Setup de um jogo em andamento (Ronda 1, Turno do Hero)
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: { players: [{ playerId: 1, username: 'Hero' }, { playerId: 2, username: 'Villain' }], _links: {} }
            }));

            // Mock Hand inicial (já rolou uma vez?) vamos assumir que tem dados
            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: { hand: ['ACE', 'KING', 'ACE', 'TEN', 'TEN'], _links: {} }
            }));

            // Mock Round Info (É a vez do ID 1 = Hero)
            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            // Mocks auxiliares para evitar erros 404 na consola
            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({ json: { players: [], _links: {} } }));
            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => route.fulfill({ json: { remainingSeconds: 60, _links: {} } }));
        });

        test('Deve mostrar controlos quando é a minha vez', async ({ page }) => {
            await gamePage.goto(GAME_ID);

            // Verifica se os botões de jogo estão visíveis
            await expect(gamePage.rollButton).toBeVisible();
            await expect(gamePage.finishButton).toBeVisible();

            // Verifica se a indicação visual de turno está ativa no meu avatar
            await gamePage.expectPlayerActive('Hero');

            // Verifica se os dados estão renderizados
            await expect(await gamePage.getDiceCount()).toBe(5);
        });

        test('Deve permitir selecionar/manter dados (Hold)', async ({ page }) => {
            await gamePage.goto(GAME_ID);

            await gamePage.diceContainer.locator('.die').nth(0).click({ force: true });

            // Verificar se ficou com a classe "held"
            await gamePage.expectDieHeld(0);
        });

        test('Deve rolar dados (Roll) e atualizar a mão', async ({ page }) => {
            await gamePage.goto(GAME_ID);

            // Mock do POST shuffle
            await page.route(`**/api/game/${GAME_ID}/player/shuffle`, async route => {
                await route.fulfill({
                    json: {
                        hand: ['QUEEN', 'QUEEN', 'QUEEN', 'QUEEN', 'QUEEN'],
                        rollNumber: 2,
                        _links: {}
                    }
                });
            });

            await gamePage.rollButton.click();

            // Verifica se a mão atualizou na UI
            await expect(page.getByText('QUEEN').first()).toBeVisible();
            // Verifica se os dados não estão "held" (reset após roll)
            const die = gamePage.diceContainer.locator('.die').first();
            await expect(die).not.toHaveClass(/held/);
        });

        test('Deve finalizar turno', async ({ page }) => {
            await gamePage.goto(GAME_ID);

            // Mock do PUT finish
            let finishCalled = false;
            await page.route(`**/api/game/${GAME_ID}/player/finish`, async route => {
                finishCalled = true;
                await route.fulfill({ json: { points: 50, finished: true, _links: {} } });
            });

            // Temos de simular que o mock de Round Info muda o turno após o finish
            // (Isto normalmente acontece via SSE event, mas aqui testamos o clique)

            await gamePage.finishButton.click();

            // Verifica se a API foi chamada
            expect(finishCalled).toBeTruthy();

            // UI: O botão deve ficar disabled ou loading temporariamente
            // Como o estado muda via SSE no código real, aqui só testamos a chamada.
        });
    });

    test.describe('Fim de Jogo', () => {
        test.skip('Deve mostrar ecrã de vitória quando o jogo acaba', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/events**`, async route => {
                const stream = new Readable();
                stream._read = () => {};
                stream.push(': connected\n\n');

                // Dá tempo ao frontend de montar, depois envia o evento
                setTimeout(() => {
                    stream.push(`event: GAME_ENDED\ndata: {}\n\n`);
                }, 1000);

                await route.fulfill({ status: 200, contentType: 'text/event-stream', body: stream });
            });

            // Mock do vencedor (chamado pelo loadGameWinner quando recebe GAME_ENDED)
            await page.route(`**/api/game/${GAME_ID}/winner`, async route => {
                console.log('MOCK: Winner endpoint called');
                await route.fulfill({
                    json: {
                        winner: { playerId: 1, totalPoints: 500, roundsWon: 3 },
                        _links: {}
                    }
                });
            });

            // Mocks base para carregamento inicial sem erros
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({ json: { players: [{playerId: 1, username: 'Hero'}], _links: {}} }));
            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({ json: { hand: [], _links: {}} }));
            // IMPORTANTE: Se turn for null ou inválido no inicio, pode dar erro. Definimos 1.
            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({ json: { round: 7, maxRoundNumber: 7, turn: 1, _links: {}} }));
            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({ json: { players: [], _links: {} } }));
            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => route.fulfill({ json: { remainingSeconds: 0, _links: {} } }));

            await gamePage.goto(GAME_ID);

            // Aumentar timeout porque temos o setTimeout de 1s no SSE
            await expect(gamePage.finishedScreen).toBeVisible({ timeout: 8000 });

            // Usar filtro para garantir que apanhamos o texto certo dentro do modal
            await expect(gamePage.winnerModal.getByText('Vencedor: Hero')).toBeVisible();
        });
    });
});