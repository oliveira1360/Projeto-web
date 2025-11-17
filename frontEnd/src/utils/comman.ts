
export const BASE_URL = 'http://localhost:8080';

const TOKEN_KEY = 'token';

export function setToken(token: string) { localStorage.setItem(TOKEN_KEY, token); }

export function getToken() { return localStorage.getItem(TOKEN_KEY); }

export function removeToken() {
    localStorage.removeItem(TOKEN_KEY);
}