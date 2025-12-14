import { useCallback, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../../services/lobby/lobbyService";
import { LobbyAction } from "./lobbyReducer";
import { PlayerInfo } from "../../services/lobby/lobbyResponseTypes";

export function useLobbyActions(
    lobbyId: number | undefined,
    dispatch: React.Dispatch<LobbyAction>
) {
    const navigate = useNavigate();
    const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);
    const initialTimeRef = useRef<number | null>(null);
        const timerStartRef = useRef<number | null>(null);

    // Cleanup timer on unmount
    useEffect(() => {
        return () => {
            if (timerIntervalRef.current) {
                clearInterval(timerIntervalRef.current);
            }
        };
    }, []);

    const loadLobbyDetails = useCallback(async () => {
        if (!lobbyId) {
            dispatch({ type: 'SET_ERROR', payload: "Lobby ID não fornecido" });
            return;
        }

        try {
            dispatch({ type: 'SET_LOADING', payload: true });
            const details = await lobbyService.getLobbyDetails(lobbyId);

            const playersList: PlayerInfo[] = details.currentPlayers.map(
                (player) => ({
                    id: player.id,
                    username: player.username,
                    imageUrl: player.imageUrl,
                })
            );

            dispatch({
                type: 'LOBBY_DETAILS_LOADED',
                payload: {
                    name: details.name,
                    maxPlayers: details.maxPlayers,
                    rounds: details.rounds,
                    minPlayersToStart: details.minPlayersToStart,
                    players: playersList,
                    timeRemaining: details.timeRemaining
                }
            });

            if (details.timeRemaining !== undefined && details.timeRemaining > 0) {
                startCountdown(details.timeRemaining, 'waiting');
            }
        } catch (e: any) {
            dispatch({ type: 'SET_ERROR', payload: `Erro ao carregar lobby: ${e.message}` });
        }
    }, [lobbyId, dispatch]);

    const startCountdown = useCallback(
        (remainingSeconds: number, status: 'waiting' | 'starting' | 'closing' = 'waiting') => {
            if (timerIntervalRef.current) {
                clearInterval(timerIntervalRef.current);
            }

            initialTimeRef.current = remainingSeconds;
            timerStartRef.current = Date.now();

            dispatch({
                type: 'START_TIMER',
                payload: { timeRemaining: remainingSeconds, status }
            });

            const updateTimer = () => {
                if (
                    timerStartRef.current === null ||
                    initialTimeRef.current === null
                ) {
                    return;
                }

                const elapsed = Math.floor(
                    (Date.now() - timerStartRef.current) / 1000
                );
                const remaining = Math.max(
                    0,
                    initialTimeRef.current - elapsed
                );

                dispatch({ type: 'UPDATE_TIMER', payload: remaining });

                if (remaining === 0) {
                    if (timerIntervalRef.current) {
                        clearInterval(timerIntervalRef.current);
                        timerIntervalRef.current = null;
                    }
                }
            };

            updateTimer();
            timerIntervalRef.current = setInterval(updateTimer, 1000);
        },
        [dispatch]
    );

    const stopTimer = useCallback(() => {
        if (timerIntervalRef.current) {
            clearInterval(timerIntervalRef.current);
            timerIntervalRef.current = null;
        }

        dispatch({ type: 'STOP_TIMER' });
        initialTimeRef.current = null;
        timerStartRef.current = null;
    }, [dispatch]);

    const leaveLobby = useCallback(async () => {
        if (!lobbyId) return;

        try {
            stopTimer();
            await lobbyService.leaveLobby(lobbyId);
            navigate("/lobbies");
        } catch (e: any) {
            dispatch({ type: 'SET_ERROR', payload: `Erro ao sair do lobby: ${e.message}` });
        }
    }, [lobbyId, navigate, stopTimer, dispatch]);

    return {
        loadLobbyDetails,
        startCountdown,
        stopTimer,
        leaveLobby
    };
}