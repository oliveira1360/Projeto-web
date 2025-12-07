import { type Page, type Locator, expect } from '@playwright/test';

export class LobbyCreationTestPage {
    readonly page: Page;
    readonly pageTitle: Locator;
    readonly nameInput: Locator;
    readonly maxPlayersInput: Locator;
    readonly roundsInput: Locator;
    readonly createButton: Locator;
    readonly backButton: Locator;
    readonly errorMessage: Locator;
    readonly container: Locator;

    constructor(page: Page) {
        this.page = page;
        this.container = page.locator('.lobby-creation-container');
        this.pageTitle = page.locator('h2', { hasText: 'Criar Novo Lobby' });

        // Input do nome - primeiro input de texto
        this.nameInput = page.locator('label:has-text("Nome do Lobby")').locator('input');

        // Inputs específicos por label
        this.maxPlayersInput = page.locator('label', { hasText: 'Máximo de Jogadores' }).locator('input');
        this.roundsInput = page.locator('label:has-text("Número de Rondas")').locator('input');

        // Botões
        this.createButton = page.locator('button[type="submit"]').filter({ hasText: /criar|creating/i });
        this.backButton = page.locator('button, a').filter({ hasText: /voltar|back/i });

        // Mensagem de erro
        this.errorMessage = page.locator('.error-message');
    }

    async goto() {
        await this.page.goto('/lobbyCreation');
    }

    async expectPageVisible() {
        await expect(this.container).toBeVisible({ timeout: 10000 });
        await expect(this.pageTitle).toBeVisible({ timeout: 10000 });
    }

    async fillForm(name: string, maxPlayers: string, rounds: string) {
        if (name) {
            await this.nameInput.fill(name);
        }
        if (maxPlayers) {
            await this.maxPlayersInput.fill(maxPlayers);
        }
        if (rounds) {
            await this.roundsInput.fill(rounds);
        }
    }

    async fillName(name: string) {
        await this.nameInput.fill(name);
    }

    async fillMaxPlayers(maxPlayers: string) {
        await this.maxPlayersInput.fill(maxPlayers);
    }

    async fillRounds(rounds: string) {
        await this.roundsInput.fill(rounds);
    }

    async submit() {
        await this.createButton.click();
    }

    async clickBack() {
        await this.backButton.click();
    }

    async expectErrorMessage(message: string) {
        await expect(this.errorMessage).toBeVisible({ timeout: 5000 });
        await expect(this.errorMessage).toContainText(message);
    }

    async expectNoError() {
        await expect(this.errorMessage).not.toBeVisible();
    }

    async expectButtonDisabled() {
        await expect(this.createButton).toBeDisabled();
    }

    async expectButtonEnabled() {
        await expect(this.createButton).toBeEnabled();
    }

    async expectButtonText(text: string | RegExp) {
        await expect(this.createButton).toContainText(text);
    }

    async expectMaxPlayersValue(value: string) {
        await expect(this.maxPlayersInput).toHaveValue(value);
    }

    async expectRoundsValue(value: string) {
        await expect(this.roundsInput).toHaveValue(value);
    }

    async expectRoundsMaxAttribute(max: string) {
        await expect(this.roundsInput).toHaveAttribute('max', max);
    }
}