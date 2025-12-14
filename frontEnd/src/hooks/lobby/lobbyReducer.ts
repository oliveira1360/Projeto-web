import { PlayerInfo } from "../../services/lobby/lobbyResponseTypes";

export interface LobbyState {
    lobbyId: number;
    name: string;
    maxPlayers: number;
    rounds: number;
    players: PlayerInfo[];
    loading: boolean;
    error: string | null;
    timeRemaining: number | null;
    timerStatus: 'waiting' | 'starting' | 'closing' | null;
    minPlayersToStart: number;
}

export const initialState: LobbyState = {
    lobbyId: 0,
    name: "",
    maxPlayers: 0,
    rounds: 0,
    players: [],
    loading: true,
    error: null,
    timeRemaining: null,
    timerStatus: null,
    minPlayersToStart: 2
};

export type LobbyAction =
    | { type: 'SET_LOADING'; payload: boolean }
    | { type: 'SET_ERROR'; payload: string }
    | { type: 'LOBBY_DETAILS_LOADED'; payload: { name: string; maxPlayers: number; rounds: number; minPlayersToStart: number; players: PlayerInfo[]; timeRemaining?: number } }
    | { type: 'PLAYER_JOINED'; payload: PlayerInfo }
    | { type: 'PLAYER_LEFT'; payload: number }
    | { type: 'START_TIMER'; payload: { timeRemaining: number; status: 'waiting' | 'starting' | 'closing' } }
    | { type: 'UPDATE_TIMER'; payload: number }
    | { type: 'STOP_TIMER' }
    | { type: 'LOBBY_STARTING' }
    | { type: 'LOBBY_CLOSED'; payload?: string };

export function lobbyReducer(state: LobbyState, action: LobbyAction): LobbyState {
    switch (action.type) {
        case 'SET_LOADING':
            return { ...state, loading: action.payload };

        case 'SET_ERROR':
            return { ...state, error: action.payload, loading: false };

        case 'LOBBY_DETAILS_LOADED':
            return {
                ...state,
                name: action.payload.name,
                maxPlayers: action.payload.maxPlayers,
                rounds: action.payload.rounds,
                minPlayersToStart: action.payload.minPlayersToStart,
                players: action.payload.players,
                timeRemaining: action.payload.timeRemaining ?? null,
                timerStatus: action.payload.timeRemaining ? 'waiting' : null,
                loading: false
            };

        case 'PLAYER_JOINED':
            return {
                ...state,
                players: state.players.some(p => p.id === action.payload.id)
                    ? state.players
                    : [...state.players, action.payload]
            };

        case 'PLAYER_LEFT':
            return {
                ...state,
                players: state.players.filter(p => p.id !== action.payload)
            };

        case 'START_TIMER':
            return {
                ...state,
                timeRemaining: action.payload.timeRemaining,
                timerStatus: action.payload.status
            };

        case 'UPDATE_TIMER':
            return {
                ...state,
                timeRemaining: action.payload,
                timerStatus: action.payload === 0 ? null : state.timerStatus
            };

        case 'STOP_TIMER':
            return {
                ...state,
                timeRemaining: null,
                timerStatus: null
            };

        case 'LOBBY_STARTING':
            return {
                ...state,
                timerStatus: 'starting'
            };

        case 'LOBBY_CLOSED':
            return {
                ...state,
                error: action.payload ?? "Lobby fechado",
                timeRemaining: null,
                timerStatus: null
            };

        default:
            return state;
    }
}