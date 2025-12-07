import {BASE_URL} from "../utils/comman";

export async function request(url: string, options: RequestInit = {}): Promise<Response> {
    const defaultHeaders = {
        "Content-Type": "application/json",
        Accept: "application/json",
    };

    return await fetch(`${BASE_URL}${url}`, {
        headers: {
            ...defaultHeaders,
            ...(options.headers ?? {}),
        },
        credentials: "include",
        ...options,
    });
}

export async function checkAuth(): Promise<boolean> {
    try {
        const res = await fetch(`${BASE_URL}/user/me`, { credentials: "include" });
        return res.ok;
    } catch {
        return false;
    }
}