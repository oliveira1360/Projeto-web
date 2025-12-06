import { type Page, type Locator, expect } from '@playwright/test';

export class GameTestPage {
    readonly page: Page;
    readonly loadingIndicator: Locator;
    readonly errorContainer: Locator;
    readonly startButton: Locator;
    readonly rollButton: Locator;
    readonly finishButton: Locator;
    readonly leaveButton: Locator;
    readonly roundLabel: Locator;
    readonly diceContainer: Locator;
    readonly winnerModal: Locator;
    readonly finishedScreen: Locator;

    constructor(page: Page) {
        this.page = page;
        this.loadingIndicator = page.locator('.loading-text');
        this.errorContainer = page.locator('.error-container');

        // Botões
        this.startButton = page.getByRole('button', { name: 'INICIAR JOGO' });
        this.rollButton = page.getByRole('button', { name: 'Rolar' });
        this.finishButton = page.getByRole('button', { name: 'Finalizar Turno' });
        this.leaveButton = page.getByRole('button', { name: 'SAIR DO JOGO' });

        // Elementos de Info
        this.roundLabel = page.locator('.round-label');
        this.diceContainer = page.locator('.dice-area');

        // Modais
        this.winnerModal = page.locator('.round-winner-modal');
        this.finishedScreen = page.locator('.game-container.finished');
    }

    async goto(gameId: number) {
        await this.page.goto(`/game/${gameId}`);
    }

    // Helpers para Players
    playerSlot(username: string) {
        return this.page.locator('.player-slot', { hasText: username });
    }

    async expectPlayerActive(username: string) {
        const avatar = this.playerSlot(username).locator('.avatar');
        await expect(avatar).toHaveClass(/active-turn/);
    }

    async expectPlayerIsMe(username: string) {
        const avatar = this.playerSlot(username).locator('.avatar');
        await expect(avatar).toHaveClass(/current-player/);
    }

    // Helpers para Dados
    async getDiceCount() {
        return await this.diceContainer.locator('.die').count();
    }

    async clickDie(index: number) {
        await this.diceContainer.locator('.die').nth(index).click();
    }

    async expectDieHeld(index: number) {
        await expect(this.diceContainer.locator('.die').nth(index)).toHaveClass(/held/);
    }
}