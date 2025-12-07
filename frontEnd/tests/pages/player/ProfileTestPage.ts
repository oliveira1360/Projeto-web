import { type Page, type Locator, expect } from '@playwright/test';

export class ProfileTestPage {
    readonly page: Page;
    readonly nameHeading: Locator;
    readonly nicknameText: Locator;
    readonly emailField: Locator;
    readonly balanceField: Locator;
    readonly avatarImage: Locator;
    readonly updateProfileLink: Locator;
    readonly createInviteLink: Locator;
    readonly homeLink: Locator;
    readonly loadingText: Locator;
    readonly profileCard: Locator;

    constructor(page: Page) {
        this.page = page;
        this.profileCard = page.locator('.player-profile-card');
        this.nameHeading = page.locator('.player-profile-avatar h2');
        this.nicknameText = page.locator('.player-profile-avatar p');
        this.emailField = page.locator('.player-profile-info p').filter({ hasText: 'Email' }).locator('span');
        this.balanceField = page.locator('.player-profile-info p').filter({ hasText: 'Balance' }).locator('span');
        this.avatarImage = page.locator('.player-profile-avatar img');
        this.updateProfileLink = page.locator('a', { hasText: 'Update Profile' });
        this.createInviteLink = page.locator('a', { hasText: 'Create Invite' });
        this.homeLink = page.locator('.player-profile-nav a', { hasText: 'Home' });
        this.loadingText = page.locator('.player-profile-loading');
    }

    async goto() {
        await this.page.goto('/playerProfile');
    }

    async waitForProfileLoaded() {
        // Espera o card do perfil aparecer (quando dados carregam)
        await this.profileCard.waitFor({ state: 'visible', timeout: 10000 });
    }

    async expectUserInfo(name: string, nickname: string, email: string, balance: string) {
        await this.waitForProfileLoaded();
        await expect(this.nameHeading).toHaveText(name, { timeout: 10000 });
        await expect(this.nicknameText).toHaveText(`@${nickname}`, { timeout: 10000 });
        await expect(this.emailField).toHaveText(email, { timeout: 10000 });
        await expect(this.balanceField).toContainText(balance, { timeout: 10000 });
    }

    async expectAvatarVisible(imageUrl: string) {
        await this.waitForProfileLoaded();
        await expect(this.avatarImage).toBeVisible({ timeout: 10000 });
        await expect(this.avatarImage).toHaveAttribute('src', imageUrl);
        await expect(this.avatarImage).toHaveAttribute('alt', 'User Avatar');
    }

    async expectAvatarNotVisible() {
        await this.waitForProfileLoaded();
        await expect(this.avatarImage).not.toBeVisible();
    }

    async expectLoadingState() {
        await expect(this.loadingText).toBeVisible({ timeout: 2000 });
    }

    async expectNavigationLinks() {
        await this.waitForProfileLoaded();
        await expect(this.homeLink).toBeVisible({ timeout: 10000 });
        await expect(this.updateProfileLink).toBeVisible({ timeout: 10000 });
        await expect(this.createInviteLink).toBeVisible({ timeout: 10000 });
    }

    async clickUpdateProfile() {
        await this.waitForProfileLoaded();
        await this.updateProfileLink.click({ timeout: 10000 });
    }

    async clickCreateInvite() {
        await this.waitForProfileLoaded();
        await this.createInviteLink.click({ timeout: 10000 });
    }

    async clickHome() {
        await this.waitForProfileLoaded();
        await this.homeLink.click({ timeout: 10000 });
    }
}