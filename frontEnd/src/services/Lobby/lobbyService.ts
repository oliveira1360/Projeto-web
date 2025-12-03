// src/services/lobby/lobbyService.ts


import {
    CreateLobbyResponse,
    ListLobbiesResponse,
    LobbyDetailsResponse,
    JoinLeaveLobbyResponse, LobbyEventResponse,
} from "./lobbyResponseTypes";
// Assumimos que 'request' agora retorna Promise<Response>
import { request } from "../request";
import { Lobby } from "./lobbyResponseTypes";
import {BASE_URL} from "../../utils/comman";


export const lobbyService = {

    async createLobby(name: string, maxPlayers: number, rounds: number): Promise<CreateLobbyResponse> {
        const response = await request(`/lobbies/create`, {
            method: "POST",
            body: JSON.stringify({ name, maxPlayers, rounds }),
        });
        const data = await response.json();
        return {
            lobbyId: data.lobbyId,
            name: data.name,
            maxPlayers: data.maxPlayers,
            currentPlayers: data.currentPlayers,
            rounds: data.rounds,
            _links: data._links,
        };
    },

    //SEGUE O PADRÃO: Response -> json() -> Mapeamento
    async listLobbies(): Promise<ListLobbiesResponse> {
        const response = await request(`/lobbies`);
        const data = await response.json();
        return {
            lobbies: data.lobbies as Lobby[],
            _links: data._links,
        };
    },

    async getLobbyDetails(lobbyId: number): Promise<LobbyDetailsResponse> {
        const response = await request(`/lobbies/${lobbyId}`);
        const data = await response.json();
        return {
            lobbyId: data.lobbyId,
            name: data.name,
            maxPlayers: data.maxPlayers,
            currentPlayers: data.currentPlayers,
            rounds: data.rounds,
            _links: data._links,
        };
    },

    async joinLobby(lobbyId: number): Promise<JoinLeaveLobbyResponse> {
        const response = await request(`/lobbies/join/${lobbyId}`, {
            method: "POST",
        });
        const data = await response.json();
        return {
            lobbyId: data.lobbyId,
            message: data.message,
            _links: data._links,
        };
    },

    async joinLobbyWithGameStart(lobbyId: number, timeoutMs: number = 2000): Promise<number> {
        return new Promise(async (resolve, reject) => {
            let subscription: { close: () => void } | null = null;
            const timeoutId = setTimeout(() => {
                subscription?.close();
                reject(new Error("Timeout esperando início do jogo"));
            }, timeoutMs);

            try {
                subscription = this.subscribeToLobbyEvents(
                    lobbyId,
                    (eventData) => {
                        const { type, data } = eventData;
                        if (type === "GAME_STARTED") {
                            clearTimeout(timeoutId);
                            const gameId = data.data.gameId;
                            if (gameId) {
                                subscription?.close();
                                resolve(Number(gameId));
                            }
                        }
                    },
                    (err) => {
                        clearTimeout(timeoutId);
                        subscription?.close();
                        reject(err);
                    }
                );

                // Faz o join após estabelecer a conexão SSE
                await this.joinLobby(lobbyId);

            } catch (error) {
                clearTimeout(timeoutId);
                subscription?.close();
                reject(error);
            }
        });
    },





async leaveLobby(lobbyId: number): Promise<JoinLeaveLobbyResponse> {
        const response = await request(`/lobbies/leave/${lobbyId}`, {
            method: "POST",
        });
        const data = await response.json();
        return {
            lobbyId: data.lobbyId,
            message: data.message,
            _links: data._links,
        };
    },



    /**
     * GET /lobbies/{lobbyId}/events
     * Estabelece uma conexão SSE (Server-Sent Events) para receber eventos do lobby.
     */


    subscribeToLobbyEvents(
        lobbyId: number,
        onEvent: (data: LobbyEventResponse) => void,
        onError?: (err: any) => void,
        onClose?: () => void
    ) {
        const eventSource = new EventSource(
            `${BASE_URL}/lobbies/${lobbyId}/events?`,
                { withCredentials: true }
        );

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            onEvent(data);
        };

        const events = ["PLAYER_JOINED", "PLAYER_LEFT", "GAME_STARTED", "LOBBY_CLOSED", "CONNECTED", ];
        events.forEach((e) => {
            eventSource.addEventListener(e, (event: MessageEvent) => {
                const data = JSON.parse(event.data);
                onEvent({ type: e, data: data });
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