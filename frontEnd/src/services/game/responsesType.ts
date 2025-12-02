export interface Player {
    playerId: number;
    username?: string;
    points?: number;
}


export type CreateGameResponse = {
    gameId: number;
    status: string;
    _links: any;
};

export type ListPlayersResponse = {
    players: Player[];
    _links: any;
};

export type PlayerHandResponse = {
    hand: string[];
    _links: any;
};

export type ShuffleResponse = {
    hand: string[];
    rollNumber: number;
    _links: any;
};

export type FinishTurnResponse = {
    points: number;
    finished: boolean;
    _links: any;
};

export type StartRoundResponse = {
    roundNumber: number;
    message: string;
    _links: any;
};

export type RoundWinner = {
    playerId: number;
    points: number;
    handValue: string;
    roundNumber: number;
};

export type RoundWinnerResponse = {
    winner: RoundWinner;
    _links: any;
};

export type ScoreboardResponse = {
    players: Player[];
    _links: any;
};

export type RoundInfoResponse = {
    round: number;
    maxRoundNumber: number;
    players: number;
    order: { idPlayer: number }[];
    pointsQueue: { playerId: number }[];
    turn: number;
    _links: any;
};

export type RemainingTimeResponse = {
    remainingSeconds: number;
    _links: any;
};

export type GameWinner = {
    playerId: number;
    totalPoints: number;
    roundsWon: number;
};

export type GameWinnerResponse = {
    winner: GameWinner;
    _links: any;
};