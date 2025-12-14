import {useEffect, useReducer} from "react";
import { gameReducer, initialState } from "./gameReducer";
import { useGameActions } from "./useGameActions";
import { useGameSubscription } from "./useGameSubscription";
import {gameService} from "../services/game/gameService";

export function useGame(gameId?: number, userId?: number) {
    const [state, dispatch] = useReducer(gameReducer, initialState);

    const actions = useGameActions(gameId, userId, state, dispatch);

    useGameSubscription(gameId, userId, actions, dispatch);

    return {
        players: state.players,
        hand: state.hand,
        rollNumber: state.rollNumber,

        currentRound: state.round.current,
        totalRounds: state.round.total,
        isMyTurn: state.round.isMyTurn,
        roundWinner: state.round.winner,

        gameStatus: state.status,
        winner: state.winner,
        loading: state.loading,
        error: state.error,

        rollDice: actions.rollDice,
        toggleHold: actions.toggleHold,
        startRound: actions.startRound,
        finishTurn: actions.finishTurn,
        leaveGame: actions.leaveGame
    };
}

export function useCloseWindow(gameId?: number) {
    useEffect(() => {
        if (!gameId) return;

        const handleBeforeUnload = () => {
            gameService.leaveGameBeacon(gameId);
        };

        window.addEventListener('beforeunload', handleBeforeUnload);

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [gameId]);
}

export default useGame;