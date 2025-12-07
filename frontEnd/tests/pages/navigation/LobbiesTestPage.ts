import { type Page, type Locator, expect } from '@playwright/test';

export class LobbiesTestPage {
    readonly page: Page;
    readonly title: Locator;
    readonly lobbiesContainer: Locator;
    readonly loadingMessage: Locator;
    readonly errorMessage: Locator;
    readonly createLobbyButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.title = page.locator('h2', { hasText: 'Lobbies Disponíveis' });
        this.lobbiesContainer = page.locator('.lobby-container');
        this.loadingMessage = page.locator('.loading-message');
        this.errorMessage = page.locator('.error-message');
        this.createLobbyButton = page.locator('button, a').filter({ hasText: /criar|create|novo/i }).first();
    }

    async goto() {
        await this.page.goto('/lobbies');
    }

    async expectPageVisible() {
        await expect(this.title).toBeVisible({ timeout: 10000 });
    }

    async expectLoading() {
        await expect(this.loadingMessage).toBeVisible();
    }

    async expectError(errorText?: string) {
        await expect(this.errorMessage).toBeVisible();
        if (errorText) {
            await expect(this.errorMessage).toContainText(errorText);
        }
    }

    async clickCreateLobby() {
        if (await this.createLobbyButton.count() > 0) {
            await this.createLobbyButton.click();
        }
    }

    lobbyCard(lobbyName: string): Locator {
        return this.page.locator('.lobby-card').filter({ hasText: lobbyName });
    }

    async expectLobbyVisible(lobbyName: string) {
        await expect(this.lobbyCard(lobbyName)).toBeVisible();
    }
}