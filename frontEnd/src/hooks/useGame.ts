import { useGamePlayers } from "./useGamePlayers";
import { useGameHand } from "./useGameHand";
import { useGameRound } from "./useGameRound";
import { useGameStatus } from "./useGameStatus";
import { useGameEvents } from "./useGameEvents";
import { gameService } from "../services/game/gameService";
import {useEffect} from "react";

export interface Die {
    value: string;
    held: boolean;
}

export function useGame(gameId?: number, userId?: number) {
    const {
        players,
        loading: playersLoading,
        error: playersError,
        loadPlayers,
        setPlayers
    } = useGamePlayers(gameId);

    const {
        hand,
        rollNumber,
        loading: handLoading,
        error: handError,
        toggleHold,
        rollDice,
        loadHand,
        resetHand
    } = useGameHand(gameId);

    const {
        currentRound,
        totalRounds,
        isMyTurn,
        roundWinner,
        loading: roundLoading,
        error: roundError,
        loadRoundInfo,
        startRound,
        finishTurn,
        setCurrentRound,
        setIsMyTurn,
        setRoundWinner
    } = useGameRound(gameId, userId);

    const {
        gameStatus,
        winner,
        loading: statusLoading,
        error: statusError,
        loadGameWinner,
        leaveGame,
        setGameStatus
    } = useGameStatus(gameId);

    useGameEvents(gameId, userId, {
        onPlayerFinishedTurn: async (data) => {
            if (!gameId || !userId) return;
            const roundInfo = await gameService.getRoundInfo(gameId);
            setCurrentRound(roundInfo.round);
            setIsMyTurn(roundInfo.turn === userId);
        },
        onRoundStarted: () => {
            loadPlayers(false);
            loadHand();
            loadRoundInfo();
            resetHand();
        },
        onRoundEnded: (data) => {
            setRoundWinner({
                playerId: data.data.winner.playerId,
                username: data.data.winner.username,
                points: data.data.winner.points,
                handValue: data.data.winner.handValue,
                roundNumber: data.data.roundNumber,
            });
        },
        onPlayerLeave: async () => {
            await loadPlayers(false);
        },
        onGameEnded: async () => {
            setGameStatus("FINISHED");
            await loadGameWinner();
        },
        onConnected: () => {
            loadPlayers();
            loadHand();
            loadRoundInfo();
        },
        onError: (error) => {
            console.error("Game event error:", error);
        }
    });

    const loading = playersLoading || handLoading || roundLoading || statusLoading;
    const error = playersError || handError || roundError || statusError;

    return {
        // Players
        players,

        // Hand
        hand,
        rollNumber,
        toggleHold,
        rollDice,

        // Round
        currentRound,
        totalRounds,
        isMyTurn,
        roundWinner,
        startRound,
        finishTurn,

        // Status
        gameStatus,
        winner,
        leaveGame,

        // General
        loading,
        error
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