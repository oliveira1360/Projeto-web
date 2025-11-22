// src/services/lobby/lobbyService.ts

import { BASE_URL, getToken } from "../../utils/comman";
import {
    CreateLobbyResponse,
    ListLobbiesResponse,
    LobbyDetailsResponse,
    JoinLeaveLobbyResponse,
} from "./lobbyResponseTypes";
// Assumimos que 'request' agora retorna Promise<Response>
import { request } from "../request";
import { Lobby } from "./lobbyResponseTypes";


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

    // 💡 SEGUE O PADRÃO: Response -> json() -> Mapeamento
    async listLobbies(): Promise<ListLobbiesResponse> {
        const response = await request(`/lobbies`);
        const data = await response.json();
        return {
            lobbies: data.lobbies as Lobby[], // Assumimos que data.lobbies é o array de lobbies
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

    /*
    subscribeToLobbyEvents(
        lobbyId: number,
        onEvent: (data: LobbyEventResponse) => void,
        onError?: (err: any) => void,
        onClose?: () => void
    ) {
        const token = getToken();
        // Nota: Assumindo que BASE_URL está acessível
        const eventSource = new EventSource(
            `${BASE_URL}/lobbies/${lobbyId}/events?token=${token}`
        );

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            onEvent(data);
        };

        const events = ["player_joined", "player_left", "game_starting", "lobby_closed", "connected"];
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


     */
};