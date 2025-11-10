import { BASE_URL, getToken } from "../../utils/comman";

export interface Player {
    playerId: number;
    username?: string;
    points?: number;
}

export interface HandResponse {
    hand: string[];
}

export interface ScoreboardResponse {
    players: Player[];
}

export interface RoundWinnerResponse {
    winner: {
        playerId: number;
        points: number;
        handValue: string;
        roundNumber: number;
    };
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
    const token = getToken();

    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...(options.headers ?? {}),
        },
        credentials: "include",
        ...options,
    });

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`HTTP ${response.status} - ${response.statusText}\n${errorBody}`);
    }

    return response.json() as Promise<T>;
}

export const gameService = {
    async createGame(lobbyId: number) {
        return request(`${BASE_URL}/game/create`, {
            method: "POST",
            body: JSON.stringify({ lobbyId }),
        });
    },

    async listPlayers(gameId: number) {
        const data = await request<{ players: Player[] }>(
            `${BASE_URL}/game/${gameId}/players`
        );
        return data.players;
    },

    async getPlayerHand(gameId: number) {
        const data = await request<HandResponse>(
            `${BASE_URL}/game/${gameId}/player/hand`
        );
        return data.hand;
    },

    async shuffle(gameId: number, lockedDice: number[]) {
        const data = await request<HandResponse>(
            `${BASE_URL}/game/${gameId}/player/shuffle`,
            {
                method: "POST",
                body: JSON.stringify({ lockedDice }),
            }
        );
        return data.hand;
    },

    async finishTurn(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/player/finish`, {
            method: "PUT",
        });
    },

    async getRoundWinner(gameId: number) {
        const data = await request<RoundWinnerResponse>(
            `${BASE_URL}/game/${gameId}/round/winner`
        );
        return data.winner;
    },

    async getScoreboard(gameId: number) {
        const data = await request<ScoreboardResponse>(
            `${BASE_URL}/game/${gameId}/scores`
        );
        return data.players;
    },

    async startRound(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/round/start`, { method: "POST" });
    },

    async getRoundInfo(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/round`);
    },

    async getRemainingTime(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/remaining-time`);
    },

    async getGameWinner(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/winner`);
    },

    async closeGame(gameId: number) {
        return request(`${BASE_URL}/game/${gameId}/close`, { method: "POST" });
    },
};
