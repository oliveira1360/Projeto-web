import { useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { gameService } from "../../services/game/gameService";
import { GameAction, GameState } from "./gameReducer";

export function useGameActions(
    gameId: number | undefined,
    userId: number | undefined,
    state: GameState,
    dispatch: React.Dispatch<GameAction>
) {
    const navigate = useNavigate();

    const loadPlayers = useCallback(async () => {
        if (!gameId) return;
        try {
            const data = await gameService.listPlayers(gameId);
            dispatch({ type: 'PLAYERS_LOADED', payload: data.players });
        } catch (e: any) {
            dispatch({ type: 'SET_ERROR', payload: e.message });
        }
    }, [gameId, dispatch]);

    const loadHand = useCallback(async () => {
        if (!gameId) return;
        try {
            const data = await gameService.getPlayerHand(gameId);
            dispatch({ type: 'HAND_LOADED', payload: data.hand });
        } catch (e: any) {
            if (e.code === 'USER_NOT_IN_GAME') {
                navigate('/lobbies');
            }

            dispatch({ type: 'RESET_HAND' });

        }
    }, [gameId, dispatch, navigate]);

    const loadRoundInfo = useCallback(async () => {
        if (!gameId || !userId) return;
        try {
            const info = await gameService.getRoundInfo(gameId);
            dispatch({ type: 'ROUND_INFO_LOADED', payload: { info, userId } });
        } catch (e: any) {
            dispatch({ type: 'SET_ERROR', payload: e.message });
        }
    }, [gameId, userId, dispatch]);

    const rollDice = async () => {
        if (!gameId) return;
        try {
            const lockedIndices = state.hand
                .map((d, i) => (d.held ? i : -1))
                .filter((i) => i !== -1);

            const data = await gameService.shuffle(gameId, lockedIndices);
            dispatch({ type: 'HAND_UPDATED', payload: data });
        } catch (e: any) {
            dispatch({ type: 'SET_ERROR', payload: e.message });
        }
    };

    const toggleHold = (index: number) => {
        dispatch({ type: 'TOGGLE_HOLD', payload: index });
    };

    const startRound = async () => {
        if (gameId) await gameService.startRound(gameId);
    };

    const finishTurn = async () => {
        if (gameId) await gameService.finishTurn(gameId);
    };

    const leaveGame = async () => {
        if (gameId) {
            await gameService.leaveGame(gameId);
            navigate("/lobbies");
        }
    };

    return {
        loadPlayers,
        loadHand,
        loadRoundInfo,
        rollDice,
        toggleHold,
        startRound,
        finishTurn,
        leaveGame
    };
}