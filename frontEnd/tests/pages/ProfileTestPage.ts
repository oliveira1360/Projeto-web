import { type Page, type Locator, expect } from '@playwright/test';

export class ProfileTestPage {
    readonly page: Page;

    constructor(page: Page) {
        this.page = page;
    }

    async goto() {
        await this.page.goto('/playerProfile');
    }

    async expectUserInfo(name: string, email: string, balance: string) {
        // Verifica se o texto está visível. O seletor usa o texto exato gerado pelo componente.
        await expect(this.page.locator('p', { hasText: `Name: ${name}` })).toBeVisible();
        await expect(this.page.locator('p', { hasText: `Email: ${email}` })).toBeVisible();
        await expect(this.page.locator('p', { hasText: `Balance: ${balance}` })).toBeVisible();
    }
}