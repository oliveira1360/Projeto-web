import { type Page, type Locator, expect } from '@playwright/test';

export class HomeTestPage {
    readonly page: Page;
    readonly title: Locator;
    readonly lobbiesButton: Locator;
    readonly profileButton: Locator;
    readonly aboutButton: Locator;

    constructor(page: Page) {
        this.page = page;
        this.title = page.getByRole('heading', { name: /Welcome to the Game!|Bem-vindo ao Jogo!/i });
        this.lobbiesButton = page.getByRole('link', { name: 'LOBBIES' });
        this.profileButton = page.getByRole('link', { name: /PERFIL DO JOGADOR|PROFILE/i });
        this.aboutButton = page.getByRole('link', { name: /SOBRE|ABOUT/i });
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

    async clickAbout() {
        await this.aboutButton.click();
    }
}