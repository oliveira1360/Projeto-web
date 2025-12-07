import { type Page, type Locator, expect } from '@playwright/test';

export class InviteTestPage {
    readonly page: Page;
    readonly title: Locator;
    readonly homeLink: Locator;
    readonly createButton: Locator;
    readonly inviteContainer: Locator;
    readonly successMessage: Locator;
    readonly errorMessage: Locator;
    readonly tokenDisplay: Locator;

    constructor(page: Page) {
        this.page = page;
        this.inviteContainer = page.locator('.invite-container');
        this.title = page.locator('.invite-header h1');
        this.homeLink = page.locator('.invite-nav a', { hasText: 'Back to Home' });
        this.createButton = page.locator('button', { hasText: /Create New Invite|Creating/ });
        this.successMessage = page.locator('.invite-success');
        this.errorMessage = page.locator('.invite-error');
        this.tokenDisplay = page.locator('.invite-token-code');
    }

    async goto() {
        await this.page.goto('/createInvite');
    }

    async clickHome() {
        await this.homeLink.click();
    }

    async clickCreateInvite() {
        await this.createButton.click();
    }

    async expectPageVisible() {
        await expect(this.inviteContainer).toBeVisible();
        await expect(this.title).toBeVisible();
    }

    async expectInviteCreated() {
        await expect(this.successMessage).toBeVisible({ timeout: 10000 });
        await expect(this.tokenDisplay).toBeVisible();
    }

    async expectError() {
        await expect(this.errorMessage).toBeVisible({ timeout: 10000 });
    }
}