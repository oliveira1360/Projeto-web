export type PlayerInfo = {
    id: number;
    username: string;
    imageUrl?: string;
}


export interface Lobby {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: PlayerInfo[];
    rounds?: number;
    timeRemaining?: number;
}

export type CreateLobbyResponse = {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: number;
    rounds: number;
    timeRemaining: number;
    minPlayersToStart: number;
    _links?: any;
};

export type ListLobbiesResponse = {
    lobbies: Lobby[];
    minPlayersToStart: number;
    _links?: any;
};


export type LobbyDetailsResponse = {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    currentPlayers: PlayerInfo[];
    rounds: number;
    timeRemaining: number;
    minPlayersToStart: number;
    _links?: any;
};

export type JoinLeaveLobbyResponse = {
    lobbyId?: number;
    message: string;
    timeRemaining?: number;
    _links?: any;
};

export type LobbyEventResponse = {
    type: string;
    data: any;
};