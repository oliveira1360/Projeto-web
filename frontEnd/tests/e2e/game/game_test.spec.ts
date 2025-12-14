import { test, expect } from '@playwright/test';
import { GameTestPage } from '../../pages/game/GameTestPage';
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
            stream.push('event: connected\ndata: {"message":"Connected","gameId":100,"userId":1}\n\n');
            await route.fulfill({
                status: 200,
                contentType: 'text/event-stream',
                body: stream
            });
        });
    });

    test.describe('Inicialização e Estados', () => {
        test('Deve mostrar Loading e depois o Tabuleiro', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => {
                await route.fulfill({
                    json: {
                        players: [
                            { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                            { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                        ],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => {
                await route.fulfill({
                    json: {
                        hand: [],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                await route.fulfill({
                    json: {
                        round: 0,
                        maxRoundNumber: 7,
                        players: 2,
                        order: [],
                        pointsQueue: [],
                        turn: 1,
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => {
                await route.fulfill({
                    json: { players: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2000);

            await expect(gamePage.startButton).toBeVisible({ timeout: 10000 });
            await expect(gamePage.playerSlot('Hero')).toBeVisible();
            await expect(gamePage.playerSlot('Villain')).toBeVisible();

            await gamePage.expectPlayerIsMe('Hero');
        });

        test('Deve redirecionar se user não está no jogo', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                status: 409,
                json: { title: 'User Not In Game', detail: 'User is not part of this game' }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 0, maxRoundNumber: 7, turn: -1, _links: {} }
            }));

            await gamePage.goto(GAME_ID);

            await expect(page).toHaveURL(/\/lobbies/, { timeout: 10000 });
        });
    });

    test.describe('Início de Ronda', () => {
        test.beforeEach(async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                        { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: { hand: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 0, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });
        });

        test('Deve permitir iniciar ronda quando está na ronda 0', async ({ page }) => {
            let roundStarted = false;
            await page.route(`**/api/game/${GAME_ID}/round/start`, async route => {
                roundStarted = true;
                await route.fulfill({
                    json: { roundNumber: 1, message: 'Round 1 started', _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await expect(gamePage.startButton).toBeVisible();
            await gamePage.startButton.click();

            expect(roundStarted).toBeTruthy();
        });
    });

    test.describe('Jogabilidade (O meu turno)', () => {
        test.beforeEach(async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                        { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: {
                    hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });
        });

        test('Deve permitir rolar dados quando é o meu turno', async ({ page }) => {
            let rollCalled = false;
            await page.route(`**/api/game/${GAME_ID}/player/shuffle`, async route => {
                rollCalled = true;
                await route.fulfill({
                    json: {
                        hand: ['KING', 'KING', 'QUEEN', 'JACK', 'TEN'],
                        rollNumber: 1,
                        _links: {}
                    }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await expect(gamePage.rollButton).toBeEnabled();
            await gamePage.rollButton.click();

            expect(rollCalled).toBeTruthy();
        });

        test('Deve permitir manter dados clicando neles', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => {
                await route.fulfill({
                    json: {
                        players: [
                            { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                            { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                        ],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => {
                await route.fulfill({
                    json: {
                        hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                await route.fulfill({
                    json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => {
                await route.fulfill({
                    json: { players: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            const firstDie = gamePage.diceContainer.locator('.die').first();
            await firstDie.click({ force: true });
            await page.waitForTimeout(500);

            await gamePage.expectDieHeld(0);
        });

        test('Deve permitir finalizar turno', async ({ page }) => {
            let finishCalled = false;

            await page.route(`**/api/game/${GAME_ID}/player/finish`, async route => {
                finishCalled = true;
                await route.fulfill({
                    json: { points: 100, finished: true, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await expect(gamePage.finishButton).toBeEnabled();
            await gamePage.finishButton.click();

            expect(finishCalled).toBeTruthy();
        });

        test('Deve desabilitar ações quando NÃO é meu turno', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 2, _links: {} }
            }));

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await expect(gamePage.rollButton).toBeDisabled();
            await expect(gamePage.finishButton).toBeDisabled();
        });

        test('Deve bloquear rolar após 3 rolls', async ({ page }) => {
            let shuffleCount = 0;

            await page.route(`**/api/game/${GAME_ID}/players`, async route => {
                await route.fulfill({
                    json: {
                        players: [
                            { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                            { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                        ],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => {
                await route.fulfill({
                    json: {
                        hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                await route.fulfill({
                    json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => {
                await route.fulfill({
                    json: { players: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/shuffle`, async route => {
                shuffleCount++;
                await route.fulfill({
                    json: {
                        hand: ['KING', 'KING', 'QUEEN', 'JACK', 'TEN'],
                        rollNumber: shuffleCount,
                        _links: {}
                    }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await gamePage.rollButton.click();
            await page.waitForTimeout(500);

            await gamePage.rollButton.click();
            await page.waitForTimeout(500);

            await gamePage.rollButton.click();
            await page.waitForTimeout(500);

            await expect(gamePage.rollButton).toBeDisabled();
        });
    });

    test.describe('Eventos SSE', () => {
        test('Deve atualizar turno via PLAYER_FINISHED_TURN', async ({ page }) => {
            let roundInfoCalls = 0;

            await page.route(`**/api/game/${GAME_ID}/events**`, async route => {
                const stream = new Readable();
                stream._read = () => {};
                stream.push('event: connected\ndata: {"message":"Connected"}\n\n');

                setTimeout(() => {
                    stream.push('event: PLAYER_FINISHED_TURN\ndata: {"type":"PLAYER_FINISHED_TURN","gameId":100,"data":{"playerId":1,"points":100,"handValue":"PAIR"}}\n\n');
                }, 1000);

                await route.fulfill({
                    status: 200,
                    contentType: 'text/event-stream',
                    body: stream
                });
            });

            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                        { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: {
                    hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                roundInfoCalls++;
                await route.fulfill({
                    json: {
                        round: 1,
                        maxRoundNumber: 7,
                        turn: roundInfoCalls === 1 ? 1 : 2,
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2500);

            await gamePage.expectPlayerActive('Hero');
        });

        test('Deve mostrar modal de vencedor da ronda via ROUND_ENDED', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/events**`, async route => {
                const stream = new Readable();
                stream._read = () => {};
                stream.push('event: connected\ndata: {"message":"Connected"}\n\n');

                setTimeout(() => {
                    const eventData = {
                        type: "ROUND_ENDED",
                        gameId: 100,
                        message: "Round 1 ended! Winner: Hero",
                        data: {
                            roundNumber: 1,
                            winner: {
                                playerId: 1,
                                username: "Hero",
                                points: 100,
                                handValue: "FULL_HOUSE"
                            },
                            totalRounds: 7
                        }
                    };
                    stream.push(`event: ROUND_ENDED\ndata: ${JSON.stringify(eventData)}\n\n`);
                }, 1000);

                await route.fulfill({
                    status: 200,
                    contentType: 'text/event-stream',
                    body: stream
                });
            });

            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } },
                        { playerId: 2, name: 'Villain', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: {
                    hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2500);
        });


    });

    test.describe('Sair do Jogo', () => {
        test('Deve permitir sair do jogo', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: {
                    hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            let gameCloseCalled = false;
            await page.route(`**/api/game/${GAME_ID}/leave`, async route => {
                gameCloseCalled = true;
                await route.fulfill({
                    status: 200,
                    json: {
                        "you have leave the Game": USER_ID,
                        _links: {}
                    }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await gamePage.leaveButton.click();
            await page.waitForTimeout(500);

            expect(gameCloseCalled).toBeTruthy();
        });
    });

    test.describe('UI e Renderização', () => {
        test('Deve mostrar informação da ronda corretamente', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => {
                await route.fulfill({
                    json: {
                        players: [
                            { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                        ],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => {
                await route.fulfill({
                    json: { hand: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                await route.fulfill({
                    json: { round: 3, maxRoundNumber: 7, turn: 1, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => {
                await route.fulfill({
                    json: { players: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2000);

            await expect(gamePage.roundLabel).toContainText('3');
            await expect(page.locator('text=/7/')).toBeVisible();
        });

        test('Deve renderizar corretamente os dados na mão', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: {
                    hand: ['ACE', 'KING', 'QUEEN', 'JACK', 'TEN'],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            await expect(page.locator('text=ACE')).toBeVisible();
            await expect(page.locator('text=KING')).toBeVisible();
            await expect(page.locator('text=QUEEN')).toBeVisible();
            await expect(page.locator('text=JACK')).toBeVisible();
            await expect(page.locator('text=TEN')).toBeVisible();
        });
    });

    test.describe('Casos Edge', () => {
        test('Deve lidar com desconexão SSE', async ({ page }) => {
            let streamClosed = false;

            await page.route(`**/api/game/${GAME_ID}/events**`, async route => {
                const stream = new Readable();
                stream._read = () => {};
                stream.push('event: connected\ndata: {"message":"Connected"}\n\n');

                setTimeout(() => {
                    stream.destroy();
                    streamClosed = true;
                }, 1000);

                await route.fulfill({
                    status: 200,
                    contentType: 'text/event-stream',
                    body: stream
                });
            });

            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: { hand: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 0, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2000);

            expect(streamClosed).toBeTruthy();
        });

        test('Deve lidar com mão vazia', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => route.fulfill({
                json: {
                    players: [
                        { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                    ],
                    _links: {}
                }
            }));

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => route.fulfill({
                json: { hand: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/round`, async route => route.fulfill({
                json: { round: 0, maxRoundNumber: 7, turn: 1, _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => route.fulfill({
                json: { players: [], _links: {} }
            }));

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(1000);

            const diceCount = await gamePage.getDiceCount();
            expect(diceCount).toBe(0);
        });

        test('Deve mostrar contador de rolls', async ({ page }) => {
            await page.route(`**/api/game/${GAME_ID}/players`, async route => {
                await route.fulfill({
                    json: {
                        players: [
                            { playerId: 1, name: 'Hero', balance: 0, rolls: 0, hand: { value: [] }, url: { value: null } }
                        ],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/hand`, async route => {
                await route.fulfill({
                    json: {
                        hand: ['ACE', 'ACE', 'ACE', 'ACE', 'ACE'],
                        _links: {}
                    }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/round`, async route => {
                await route.fulfill({
                    json: { round: 1, maxRoundNumber: 7, turn: 1, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/scores`, async route => {
                await route.fulfill({
                    json: { players: [], _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/remaining-time`, async route => {
                await route.fulfill({
                    json: { remainingSeconds: 60, _links: {} }
                });
            });

            await page.route(`**/api/game/${GAME_ID}/player/shuffle`, async route => {
                await route.fulfill({
                    json: {
                        hand: ['KING', 'KING', 'KING', 'KING', 'KING'],
                        rollNumber: 2,
                        _links: {}
                    }
                });
            });

            await gamePage.goto(GAME_ID);
            await page.waitForTimeout(2000);

            await expect(page.locator('text=/Rolar.*\\/.*3/i')).toBeVisible({ timeout: 5000 });

            await gamePage.rollButton.click({ force: true });
            await page.waitForTimeout(1000);

            await expect(page.locator('text=/Rolar.*2.*\\/.*3/i')).toBeVisible({ timeout: 5000 });
        });
    });
});