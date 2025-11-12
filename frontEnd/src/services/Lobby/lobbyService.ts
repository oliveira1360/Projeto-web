import { BASE_URL, getToken } from "../../utils/comman";


// --- Interfaces de Dados ---

export interface Lobby {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: number;
    rounds?: number; // Opcional, pois não está sempre presente na listagem
}

export interface LobbyDetailsResponse extends Lobby {
    rounds: number; // Presente nos detalhes
}

export interface ListLobbiesResponse {
    lobbies: Lobby[];
}

export interface CreateLobbyResponse {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: number;
    rounds: number;
}

export interface JoinLeaveLobbyResponse {
    lobbyId?: number; // Presente no Join, ausente no Leave
    message: string;
}

// --- Função de Requisição Genérica (Assumida) ---


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

    // Retorna vazio se a resposta for 204 No Content
    if (response.status === 204) {
        return {} as T;
    }

    return response.json() as Promise<T>;
}


// --- Lobby Service ---

export const lobbyService = {
    /**
     * POST /lobbies/create
     * Cria um novo lobby.
     * @param name Nome do lobby.
     * @param maxPlayers Número máximo de jogadores.
     * @param rounds Número de rondas.
     */
    async createLobby(name: string, maxPlayers: number, rounds: number) {
        const data = await request<CreateLobbyResponse>(
            `${BASE_URL}/lobbies/create`,
            {
                method: "POST",
                body: JSON.stringify({ name, maxPlayers, rounds }),
            }
        );
        return data;
    },

    /**
     * GET /lobbies
     * Lista todos os lobbies disponíveis.
     */
    async listLobbies() {
        const data = await request<ListLobbiesResponse>(
            `${BASE_URL}/lobbies`
        );
        return data.lobbies;
    },

    /**
     * GET /lobbies/{lobbyId}
     * Obtém os detalhes de um lobby específico.
     * @param lobbyId ID do lobby.
     */
    async getLobbyDetails(lobbyId: number) {
        const data = await request<LobbyDetailsResponse>(
            `${BASE_URL}/lobbies/${lobbyId}`
        );
        // O controller devolve um objeto plano (LobbyDetailsResponse)
        return data;
    },

    /**
     * POST /lobbies/join/{lobbyId}
     * O utilizador autenticado junta-se a um lobby.
     * @param lobbyId ID do lobby a juntar.
     */
    async joinLobby(lobbyId: number) {
        const data = await request<JoinLeaveLobbyResponse>(
            `${BASE_URL}/lobbies/join/${lobbyId}`,
            {
                method: "POST",
            }
        );
        return data;
    },

    /**
     * POST /lobbies/leave/{lobbyId}
     * O utilizador autenticado sai de um lobby.
     * @param lobbyId ID do lobby a sair.
     */
    async leaveLobby(lobbyId: number) {
        const data = await request<JoinLeaveLobbyResponse>(
            `${BASE_URL}/lobbies/leave/${lobbyId}`,
            {
                method: "POST",
            }
        );
        return data;
    },



    // TODO Não sei se esta rota também é preciso
    /**
     * GET /lobbies/{lobbyId}/events
     * Estabelece uma conexão SSE (Server-Sent Events) para receber eventos do lobby.
     * NOTA: Esta função não usa a abstração 'request' e requer o tratamento especial de SSE no lado do cliente.
     * @param lobbyId ID do lobby.
     */
    /*
    subscribeToLobbyEvents(lobbyId: number): EventSource {
        const url = `${BASE_URL}/lobbies/${lobbyId}/events`;
        // Para incluir a autenticação, pode ser necessário passar o token
        // ou usar um mod de conexão que suporte headers (ex: fetch com ReadableStream, mas EventSource é mais simples para SSE).
        // Se a autenticação estiver apenas em cookies/credentials, EventSource funciona:
        const token = getToken();
        let finalUrl = url;

        // NOTA: Se o token for exigido na URL (e não em cookie/header), descomente o seguinte:
        // if (token) {
        //     finalUrl += `?token=${token}`;
        // }

        return new EventSource(finalUrl, { withCredentials: true });
    }

     */
};