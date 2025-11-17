import {BASE_URL, getToken} from "../utils/comman";

export async function request(url: string, options: RequestInit = {}): Promise<Response> {
    const token = getToken();

    return await fetch(`${BASE_URL}${url}`, {
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...(token ? {Authorization: `Bearer ${token}`} : {}),
            ...(options.headers ?? {}),
        },
        credentials: "include",
        ...options,
    });
}