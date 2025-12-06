import { type Page, type Locator, expect } from '@playwright/test';

export class LobbyCreationTestPage {
    readonly page: Page;
    readonly nameInput: Locator;
    readonly maxPlayersInput: Locator;
    readonly roundsInput: Locator;
    readonly createButton: Locator;
    readonly errorMessage: Locator;

    constructor(page: Page) {
        this.page = page;
        this.nameInput = page.locator('input[type="text"]');
        this.maxPlayersInput = page.locator('label', { hasText: 'Máximo de Jogadores' }).locator('input');
        this.roundsInput = page.locator('label', { hasText: 'Número de Rondas' }).locator('input');
        this.createButton = page.getByRole('button', { name: 'Criar Lobby' });
        this.errorMessage = page.locator('.error-message');
    }

    async goto() {
        await this.page.goto('/lobbyCreation');
    }

    async fillForm(name: string, maxPlayers: string, rounds: string) {
        await this.nameInput.fill(name);
        await this.maxPlayersInput.fill(maxPlayers);
        await this.roundsInput.fill(rounds);
    }

    async submit() {
        await this.createButton.click();
    }

    async expectErrorMessage(message: string) {
        await expect(this.errorMessage).toBeVisible();
        await expect(this.errorMessage).toContainText(message);
    }
}