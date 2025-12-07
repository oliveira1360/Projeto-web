import { type Page, type Locator, expect } from '@playwright/test';

export class HomeTestPage {
    readonly page: Page;
    readonly title: Locator;
    readonly lobbiesButton: Locator;
    readonly profileButton: Locator;
    readonly aboutButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.title = page.getByRole('heading', { name: 'Bem-vindo ao Jogo!' });
        this.lobbiesButton = page.getByRole('link', { name: 'LOBBIES' });
        this.profileButton = page.getByRole('link', { name: 'PERFIL DO JOGADOR' });
        this.aboutButton = page.getByRole('link', { name: 'SOBRE' });
    }

    async goto() {
        await this.page.goto('/home');
    }

    async clickLobbies() {
        await this.lobbiesButton.click();
    }

    async clickProfile() {
        await this.profileButton.click();
    }
}