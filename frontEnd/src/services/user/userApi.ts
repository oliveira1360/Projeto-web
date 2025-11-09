import {BASE_URL} from "../../utils/comman";

export type TokenExternalInfo = {
    token: string;
}

export type TokenCreationError = {
    message: string;
}

// @ts-ignore
export async function login(email: string, password: string): Promise<TokenExternalInfo> {
    const response = await fetch(`${BASE_URL}/user/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
        const errorData: TokenCreationError = await response.json();

        throw new Error(errorData.message || 'Failed to login');
    }
    return response.json();
}

// @ts-ignore
export async function register(name: string, email: string, password: string): Promise<TokenCreationError> {
    const response = await fetch(`${BASE_URL}/user/register`, {})

}
