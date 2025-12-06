import { type Page, type Locator, expect } from '@playwright/test';

export class LobbyTestPage {
    readonly page: Page;
    readonly createLobbyButton: Locator;
    readonly lobbyCards: Locator;
    readonly detailsPanel: Locator;

    constructor(page: Page) {
        this.page = page;
        this.createLobbyButton = page.getByRole('link', { name: 'CRIAR NOVO LOBBY' });
        this.lobbyCards = page.locator('.lobby-card');
        this.detailsPanel = page.locator('.details-panel');
    }

    async goto() {
        await this.page.goto('/lobbies');
    }

    async expectLobbyVisible(lobbyName: string) {
        await expect(this.lobbyCards.filter({ hasText: lobbyName })).toBeVisible();
    }

    async selectLobby(lobbyName: string) {
        // Clica no card específico
        await this.lobbyCards.filter({ hasText: lobbyName }).click();
    }

    async clickJoin(lobbyName: string) {
        // Clica no botão ENTRAR dentro do card específico
        await this.lobbyCards
            .filter({ hasText: lobbyName })
            .getByRole('button', { name: 'ENTRAR' })
            .click();
    }

    async expectDetailsVisible(lobbyName: string) {
        await expect(this.detailsPanel).toBeVisible();
        await expect(this.detailsPanel).toContainText(lobbyName);
    }
}