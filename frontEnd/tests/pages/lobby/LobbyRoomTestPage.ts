import { type Page, type Locator, expect } from '@playwright/test';

export class LobbyRoomTestPage {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async expectLobbyTitle(name: string) {
        await expect(this.page.getByRole('heading', { name: name })).toBeVisible();
    }

    async expectPlayerInList(username: string) {
        await expect(this.page.locator('.player-name', { hasText: username })).toBeVisible();
    }

    async clickLeave() {
        await this.page.getByRole('button', { name: 'Sair do Lobby' }).click();
    }
}