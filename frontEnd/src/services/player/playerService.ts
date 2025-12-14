import {request} from "../request";
import {PlayerInfoResponse, PlayerStatsResponse} from "./playerResponseTypes";




export const playerService = {
    /**
     * GET /user/info
     * Obtém informações do jogador autenticado.
     */
    async playerInfo(): Promise<PlayerInfoResponse> {
        const response = await request(`/user/info`, {
            method: 'GET',
        });

        const data = await response.json();
        return {
            userId: data.userId,
            name: data.name,
            nickName: data.nickName,
            email: data.email,
            balance: data.balance,
            imageUrl: data.imageUrl,
            _links: data._links,
        };
    },

    async playerStats(): Promise<PlayerStatsResponse> {
        const response = await request(`/user/stats`, {
            method: 'GET',
        });

        const data = await response.json();
        return {
            userId: data.userId,
            totalGamesPlayed: data.totalGamesPlayed,
            totalWins: data.totalWins,
            totalLosses: data.totalLosses,
            totalPoints: data.totalPoints,
            longestStreak: data.longestStreak,
            currentStreak: data.currentStreak,
            _links: data._links,
        };
    },

    async updatePlayerProfile(profileData: { name?: string; nickName?: string; imageUrl?: string; password?: string; }): Promise<PlayerInfoResponse> {
        if (profileData.password != undefined && profileData.password.trim().length > 0) {
            // Using SHA-256 to hash the password before sending it to the server
            // Code obtained and adapted from https://www.geeksforgeeks.org/javascript/how-to-create-hash-from-string-in-javascript/
            const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(profileData.password)); 
            const hashArray = Array.from(new Uint8Array(hashBuffer)); 
            const passwordHashHex = hashArray.map(byte => byte.toString(16).padStart(2, '0')).join('');
            profileData.password = passwordHashHex;
        }

        const response = await request(`/user/update`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                name: profileData.name,
                nickName: profileData.nickName,
                imageUrl: profileData.imageUrl,
                password: (profileData.password == undefined || profileData.password.trim().length === 0) ? null : profileData.password,
            }),
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Failed to update profile');
        }

        return {
            userId: data.userId,
            name: data.name, 
            nickName: data.nickName,
            email: data.email,
            balance: data.balance,
            imageUrl: data.imageUrl,
            _links: data._links,
        };
    }
};