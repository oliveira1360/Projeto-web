import {BASE_URL, getToken, removeToken, setToken} from "../../utils/comman";

export type TokenExternalInfo = {
    token: string;
}

export type TokenCreationError = {
    message: string;
}

// @ts-ignore
export async function login(email: string, password: string): Promise<TokenExternalInfo> {
    
    // Using SHA-256 to hash the password before sending it to the server
    // Code obtained and adapted from https://www.geeksforgeeks.org/javascript/how-to-create-hash-from-string-in-javascript/
    const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(password)); 
    const hashArray = Array.from(new Uint8Array(hashBuffer)); 
    const passwordHashHex = hashArray.map(byte => byte.toString(16).padStart(2, '0')).join(''); 


    const response = await fetch(`${BASE_URL}/user/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password: passwordHashHex }),
    });

    if (!response.ok) {
        const errorData: TokenCreationError = await response.json();

        throw new Error(errorData.message || 'Failed to login');
    }
    return response.json();
}

// @ts-ignore
export async function register(name: string, nickName: string, email: string, password: string, inviteCode: string): Promise<TokenExternalInfo> {
    
    // Using SHA-256 to hash the password before sending it to the server
    // Code obtained and adapted from https://www.geeksforgeeks.org/javascript/how-to-create-hash-from-string-in-javascript/
    const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(password)); 
    const hashArray = Array.from(new Uint8Array(hashBuffer)); 
    const passwordHashHex = hashArray.map(byte => byte.toString(16).padStart(2, '0')).join(''); 

    const response = await fetch(`${BASE_URL}/user/create`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'invite ' + inviteCode,
        },
        body: JSON.stringify({ name, nickName, email, password: passwordHashHex }),
    })

    if (!response.ok) {
        const errorData: TokenCreationError = await response.json();
        throw new Error(errorData.message || 'Failed to sign up');
    }
    return login(email, password);
}

export async function logout(): Promise<void> {
    const response = await fetch(`${BASE_URL}/user/logout`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getToken()}`,
        },
    });
    if (!response.ok) {
        const errorData: TokenCreationError = await response.json();
        throw new Error(errorData.message || 'Failed to log out');
    }
    removeToken();
}

export async function getUserStats() {
    const response = await fetch(`${BASE_URL}/user/stats`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    });
    if (!response.ok) {
        const errorData: TokenCreationError = await response.json();
        throw new Error(errorData.message || 'Failed to get user stats');
    }
    return response.json();
}

export function isSafePassword(password: string): boolean {
    let hasUpperCase = /[A-Z]/.test(password);
    let hasNumbers = /[0-9]/.test(password);
    let isLongEnough = password.length >= 8;
    return hasUpperCase && hasNumbers && isLongEnough;
}