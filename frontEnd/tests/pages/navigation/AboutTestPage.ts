import { type Page, type Locator, expect } from '@playwright/test';

export class AboutTestPage {
    readonly page: Page;
    readonly title: Locator;
    readonly homeLink: Locator;
    readonly aboutContainer: Locator;
    readonly teamSection: Locator;

    constructor(page: Page) {
        this.page = page;
        this.aboutContainer = page.locator('.about-container');
        this.title = page.locator('.about-header h1');
        this.homeLink = page.locator('.about-header-home');
        this.teamSection = page.locator('.about-team');
    }

    async goto() {
        await this.page.goto('/about');
    }

    async clickHome() {
        await this.homeLink.click();
    }

    async expectPageVisible() {
        await expect(this.aboutContainer).toBeVisible();
        await expect(this.title).toBeVisible();
    }

    async expectTeamMembers() {
        await expect(this.teamSection).toBeVisible();
        await expect(this.page.locator('text=Diogo Oliveira')).toBeVisible();
        await expect(this.page.locator('text=Paulo Nascimento')).toBeVisible();
        await expect(this.page.locator('text=Jessé Alencar')).toBeVisible();
    }
}