import {BASE_URL} from "../../utils/comman";
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
            order: data.order,
            pointsQueue: data.pointsQueue,
            turn: data.turn,
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
        onError?: (err: any) => void,
        onClose?: () => void
    ) {

        const eventSource = new EventSource(
            `${BASE_URL}/game/${gameId}/events?`,
                { withCredentials: true }
        );

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            onEvent("message", data);
        };

        const events = ["PLAYER_FINISHED_TURN", "ROUND_STARTED", "ROUND_ENDED", "connected", "GAME_ENDED"];
        events.forEach((e) => {
            eventSource.addEventListener(e, (event: MessageEvent) => {
                const data = JSON.parse(event.data);
                onEvent(e, data);
            });
        });

        eventSource.onerror = (err) => {
            console.error("SSE error:", err);
            onError?.(err);
        };

        return {
            close: () => {
                eventSource.close();
                onClose?.();
            }
        };
    }
};