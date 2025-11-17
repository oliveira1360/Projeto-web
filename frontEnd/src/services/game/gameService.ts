import {BASE_URL, getToken} from "../../utils/comman";
import {
    CreateGameResponse,
    FinishTurnResponse, GameWinnerResponse,
    ListPlayersResponse,
    PlayerHandResponse, RemainingTimeResponse, RoundInfoResponse, RoundWinnerResponse, ScoreboardResponse,
    ShuffleResponse, StartRoundResponse
} from "./responsesType";
import {request} from "../request";

export const gameService = {

    async createGame(lobbyId: number): Promise<CreateGameResponse> {
        const response = await request(`/game/create`, {
            method: "POST",
            body: JSON.stringify({ lobbyId }),
        });
        const data = await response.json();
        return {
            gameId: data.gameId,
            status: data.status,
            _links: data._links,
        };
    },


    async listPlayers(gameId: number): Promise<ListPlayersResponse> {
        const response = await request(`/game/${gameId}/players`);
        const data = await response.json();
        return {
            players: data.players,
            _links: data._links,
        };
    },


    async getPlayerHand(gameId: number): Promise<PlayerHandResponse> {
        const response = await request(`/game/${gameId}/player/hand`);
        const data = await response.json();
        return {
            hand: data.hand,
            _links: data._links,
        };
    },


    async shuffle(gameId: number, lockedDice: number[]): Promise<ShuffleResponse> {
        const response = await request(`/game/${gameId}/player/shuffle`, {
            method: "POST",
            body: JSON.stringify({ lockedDice }),
        });
        const data = await response.json();
        return {
            hand: data.hand,
            rollNumber: data.rollNumber,
            _links: data._links,
        };
    },


    async finishTurn(gameId: number): Promise<FinishTurnResponse> {
        const response = await request(`/game/${gameId}/player/finish`, {
            method: "PUT",
        });
        const data = await response.json();
        return {
            points: data.points,
            finished: data.finished,
            _links: data._links,
        };
    },

    async startRound(gameId: number): Promise<StartRoundResponse> {
        const response = await request(`/game/${gameId}/round/start`, {
            method: "POST",
        });
        const data = await response.json();
        return {
            roundNumber: data.roundNumber,
            message: data.message,
            _links: data._links,
        };
    },

    async getRoundWinner(gameId: number): Promise<RoundWinnerResponse> {
        const response = await request(`/game/${gameId}/round/winner`);
        const data = await response.json();
        return {
            winner: data.winner,
            _links: data._links,
        };
    },

    async getScoreboard(gameId: number): Promise<ScoreboardResponse> {
        const response = await request(`/game/${gameId}/scores`);
        const data = await response.json();
        return {
            players: data.players,
            _links: data._links,
        };
    },

    async getRoundInfo(gameId: number): Promise<RoundInfoResponse> {
        const response = await request(`/game/${gameId}/round`);
        const data = await response.json();
        return {
            round: data.round,
            players: data.players,
            pointsQueue: data.order,
            _links: data._links,
        };
    },

    async getRemainingTime(gameId: number): Promise<RemainingTimeResponse> {
        const response = await request(`/game/${gameId}/remaining-time`);
        const data = await response.json();
        return {
            remainingSeconds: data.remainingSeconds,
            _links: data._links,
        };
    },

    async getGameWinner(gameId: number): Promise<GameWinnerResponse> {
        const response = await request(`/game/${gameId}/winner`, {
            method: "PATCH",
        });
        const data = await response.json();
        return {
            winner: data.winner,
            _links: data._links,
        };
    },

    async closeGame(gameId: number): Promise<void> {
        await request(`/game/${gameId}`, {
            method: "POST",
        });
    },

    subscribeToGameEvents(
        gameId: number,
        onEvent: (eventType: string, data: any) => void,
        onError?: (error: Error) => void,
        onClose?: () => void
    ): { close: () => void } {
        const baseUrl = BASE_URL;
        const token = getToken();
        let abortController = new AbortController();

        const connectToStream = async () => {
            try {
                const response = await fetch(`${baseUrl}/game/${gameId}/events`, {
                    method: "GET",
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                    signal: abortController.signal,
                });

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status} - ${response.statusText}`);
                }

                const reader = response.body?.getReader();
                if (!reader) {
                    throw new Error("Unable to read response body");
                }

                const decoder = new TextDecoder();
                let buffer = "";

                while (true) {
                    const { done, value } = await reader.read();

                    if (done) {
                        onClose?.();
                        break;
                    }

                    buffer += decoder.decode(value, { stream: true });
                    const lines = buffer.split("\n");

                    buffer = lines.pop() || "";

                    let currentEvent: string | null = null;

                    lines.forEach((line) => {
                        if (line.startsWith("event:")) {
                            currentEvent = line.replace("event:", "").trim();
                        } else if (line.startsWith("data:") && currentEvent) {
                            const jsonData = line.replace("data:", "").trim();
                            try {
                                const data = JSON.parse(jsonData);
                                onEvent(currentEvent, data);
                            } catch (e) {
                                console.error("Failed to parse event data:", e, jsonData);
                            }
                            currentEvent = null;
                        }
                    });
                }
            } catch (error) {
                if ((error as Error).name !== "AbortError") {
                    onError?.(error as Error);
                }
            }
        };

        connectToStream();

        return {
            close: () => {
                abortController.abort();
                onClose?.();
            },
        };
    },
};