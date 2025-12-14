import { Player, RoundInfoResponse } from "../../services/game/responsesType";

export interface Die {
    value: string;
    held: boolean;
}

export interface RoundState {
    current: number;
    total: number;
    turnPlayerId: number;
    isMyTurn: boolean;
    winner: any | null;
}

export interface GameState {
    players: Player[];
    hand: Die[];
    rollNumber: number;
    round: RoundState;
    status: "ACTIVE" | "FINISHED";
    winner: { username: string; points: number } | null;
    loading: boolean;
    error: string | null;
}

export const initialState: GameState = {
    players: [],
    hand: [],
    rollNumber: 0,
    round: {
        current: 0,
        total: 7,
        turnPlayerId: 0,
        isMyTurn: false,
        winner: null
    },
    status: "ACTIVE",
    winner: null,
    loading: false,
    error: null
};

export type GameAction =
    | { type: 'SET_LOADING'; payload: boolean }
    | { type: 'SET_ERROR'; payload: string }
    | { type: 'PLAYERS_LOADED'; payload: Player[] }
    | { type: 'HAND_LOADED'; payload: string[] }
    | { type: 'HAND_UPDATED'; payload: { hand: string[]; rollNumber: number } }
    | { type: 'TOGGLE_HOLD'; payload: number }
    | { type: 'ROUND_INFO_LOADED'; payload: { info: RoundInfoResponse; userId: number } }
    | { type: 'ROUND_STARTED'; payload: { info: RoundInfoResponse; userId: number } }
    | { type: 'ROUND_ENDED'; payload: any }
    | { type: 'GAME_ENDED'; payload: { winnerName: string; points: number } }
    | { type: 'RESET_HAND' };

export function gameReducer(state: GameState, action: GameAction): GameState {
    switch (action.type) {
        case 'SET_LOADING':
            return { ...state, loading: action.payload };

        case 'SET_ERROR':
            return { ...state, error: action.payload, loading: false };

        case 'PLAYERS_LOADED':
            return { ...state, players: action.payload, loading: false };

        case 'HAND_LOADED':
            return {
                ...state,
                hand: action.payload.map(val => ({ value: val, held: false })),
                loading: false
            };

        case 'HAND_UPDATED':
            return {
                ...state,
                hand: action.payload.hand.map(val => ({ value: val, held: false })),
                rollNumber: action.payload.rollNumber,
                loading: false
            };

        case 'TOGGLE_HOLD':
            return {
                ...state,
                hand: state.hand.map((die, idx) =>
                    idx === action.payload ? { ...die, held: !die.held } : die
                )
            };

        case 'ROUND_INFO_LOADED':
            return {
                ...state,
                round: {
                    ...state.round,
                    current: action.payload.info.round,
                    total: action.payload.info.maxRoundNumber,
                    turnPlayerId: action.payload.info.turn,
                    isMyTurn: action.payload.info.turn === action.payload.userId
                },
                loading: false
            };

        case 'ROUND_STARTED':
            return {
                ...state,
                round: {
                    ...state.round,
                    current: action.payload.info.round,
                    turnPlayerId: action.payload.info.turn,
                    isMyTurn: action.payload.info.turn === action.payload.userId,
                    winner: null
                },
                hand: [],
                rollNumber: 0,
                loading: false
            };

        case 'ROUND_ENDED':
            return {
                ...state,
                round: {
                    ...state.round,
                    winner: {
                        playerId: action.payload.playerId,
                        username: action.payload.username,
                        points: action.payload.points,
                        handValue: action.payload.handValue
                    }
                }
            };

        case 'GAME_ENDED':
            return {
                ...state,
                status: "FINISHED",
                winner: {
                    username: action.payload.winnerName,
                    points: action.payload.points
                },
                loading: false
            };

        case 'RESET_HAND':
            return { ...state, hand: [], rollNumber: 0 };

        default:
            return state;
    }
}