
export type PlayerInfo = {
    id: number;
    username: string;
}


export interface Lobby {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: PlayerInfo[];
    rounds?: number;
}

export type CreateLobbyResponse = {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: number;
    rounds: number;
    _links?: any;
};

export type ListLobbiesResponse = {
    lobbies: Lobby[];
    _links?: any;
};

// Detalhes geralmente têm as mesmas propriedades do Lobby, mas garantindo 'rounds'
export type LobbyDetailsResponse = {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: PlayerInfo[];
    rounds: number;
    _links?: any;
};

// Resposta para Juntar/Sair do Lobby
export type JoinLeaveLobbyResponse = {
    lobbyId?: number; // Presente no Join
    message: string;
    _links?: any;
};

// SSE
export type LobbyEventResponse = {
    type: string;
    data: any;
};