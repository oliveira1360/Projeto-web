import { type Page, type Locator, expect } from '@playwright/test';

export class LoginTestPage {
    readonly page: Page;
    readonly emailInput: Locator;
    readonly passwordInput: Locator;
    readonly signInButton: Locator;
    readonly container: Locator;

    constructor(page: Page) {
        this.page = page;
        // Seletores baseados no teu authentication.tsx
        this.container = page.locator('.sign-in-container');
        this.emailInput = this.container.locator('input[placeholder="Email"]');
        this.passwordInput = this.container.locator('input[placeholder="Password"]');
        this.signInButton = this.container.locator('button', { hasText: 'Sign In' });
    }

    async goto() {
        await this.page.goto('/');
    }

    async login(email: string, pass: string) {
        await this.emailInput.fill(email);
        await this.passwordInput.fill(pass);
        await this.signInButton.click();
    }
}