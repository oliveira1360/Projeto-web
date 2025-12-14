import { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../services/lobby/lobbyService";
import { playerService } from "../services/player/playerService";
import {PlayerInfo} from "../services/lobby/lobbyResponseTypes";

interface LobbyRoomState {
    lobbyId: number;
    name: string;
    currentPlayers: PlayerInfo[];
    maxPlayers: number;
    rounds: number;
    players: PlayerInfo[];
    loading: boolean;
    error: string | null;
    timeRemaining: number | null;
    timerStatus: 'waiting' | 'starting' | 'closing' | null;
    minPlayersToStart: number;
    leaveLobby: () => Promise<void>;
}

export function useLobbyRoom(
    lobbyId: number | undefined,
): LobbyRoomState {
    const navigate = useNavigate();

    const [name, setName] = useState("");
    const [currentPlayers, setCurrentPlayers] = useState<PlayerInfo[]>([]);
    const [maxPlayers, setMaxPlayers] = useState(0);
    const [rounds, setRounds] = useState(0);
    const [players, setPlayers] = useState<PlayerInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [timeRemaining, setTimeRemaining] = useState<number | null>(null);
    const [timerStatus, setTimerStatus] =
        useState<'waiting' | 'starting' | 'closing' | null>(null);
    const [minPlayersToStart, setMinPlayersToStart] = useState(2);

    const timerIntervalRef = useRef<NodeJS.Timeout | null>(null);
    const initialTimeRef = useRef<number | null>(null);
    const timerStartRef = useRef<number | null>(null);

    useEffect(() => {
        return () => {
            if (timerIntervalRef.current) {
                clearInterval(timerIntervalRef.current);
            }
        };
    }, []);

    const startCountdown = useCallback(
        (remainingSeconds: number, status: 'waiting' | 'starting' | 'closing' = 'waiting') => {
            if (timerIntervalRef.current) {
                clearInterval(timerIntervalRef.current);
            }

            initialTimeRef.current = remainingSeconds;
            timerStartRef.current = Date.now();

            setTimerStatus(status);
            setTimeRemaining(remainingSeconds);

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

                setTimeRemaining(remaining);

                if (remaining === 0) {
                    if (timerIntervalRef.current) {
                        clearInterval(timerIntervalRef.current);
                        timerIntervalRef.current = null;
                    }
                    setTimerStatus(null);
                }
            };

            updateTimer();
            timerIntervalRef.current = setInterval(updateTimer, 1000);
        },
        []
    );

    const stopTimer = useCallback(() => {
        if (timerIntervalRef.current) {
            clearInterval(timerIntervalRef.current);
            timerIntervalRef.current = null;
        }

        setTimeRemaining(null);
        setTimerStatus(null);
        initialTimeRef.current = null;
        timerStartRef.current = null;
    }, []);

    const loadLobbyDetails = useCallback(async () => {
        if (!lobbyId) {
            setError("Lobby ID não fornecido");
            setLoading(false);
            return;
        }

        try {
            const details = await lobbyService.getLobbyDetails(lobbyId);

            setName(details.name);
            setMaxPlayers(details.maxPlayers);
            setRounds(details.rounds);
            setMinPlayersToStart(details.minPlayersToStart);

            const playersList: PlayerInfo[] = details.currentPlayers.map(
                (player) => ({
                    id: player.id,
                    username: player.username,
                    imageUrl: player.imageUrl,
                })
            );

            setPlayers(playersList);
            setCurrentPlayers(details.currentPlayers);

            if (details.timeRemaining !== undefined && details.timeRemaining > 0) {
                startCountdown(details.timeRemaining, 'waiting');
            }

            setLoading(false);
        } catch (e: any) {
            setError(`Erro ao carregar lobby: ${e.message}`);
            setLoading(false);
        }
    }, [lobbyId, startCountdown]);

    useEffect(() => {
        if (!lobbyId) return;

        loadLobbyDetails();

        const subscription = lobbyService.subscribeToLobbyEvents(
            lobbyId,
            (eventData) => {
                const { type, data } = eventData;

                switch (type) {
                    case "PLAYER_JOINED": {
                        const newPlayer: PlayerInfo = {
                            id: data.data.userId,
                            username: data.data.userName,
                            imageUrl: data.data.imageUrl,
                        };

                        setPlayers((current) =>
                            current.some((p) => p.id === newPlayer.id)
                                ? current
                                : [...current, newPlayer]
                        );

                        setCurrentPlayers((current) =>
                            current.some((p) => p.id === newPlayer.id)
                                ? current
                                : [...current, newPlayer]
                        );
                        break;
                    }

                    case "PLAYER_LEFT": {
                        const leftUserId = data.data.userId;

                        setPlayers((current) =>
                            current.filter((p) => p.id !== leftUserId)
                        );
                        setCurrentPlayers((current) =>
                            current.filter((p) => p.id !== leftUserId)
                        );
                        break;
                    }

                    case "LOBBY_STARTING":
                        stopTimer();
                        startCountdown(3, 'starting');
                        break;

                    case "GAME_STARTED": {
                        stopTimer();

                        let gameId = data.data.gameId;

                            gameId = gameId.value.gameId;


                        if (gameId) {
                            navigate(`/game/${Number(gameId)}`);
                        }
                        break;
                    }

                    case "LOBBY_CLOSED":
                        stopTimer();

                        if (data.message) {
                            setError(data.message);
                        }


                            navigate("/lobbies");

                        break;
                }
            },
            () => {
                setError("Conexão perdida com o servidor");
                stopTimer();
            }
        );

        return () => {
            subscription.close();
            stopTimer();
        };
    }, [lobbyId, loadLobbyDetails, navigate, stopTimer, startCountdown]);

    const leaveLobby = useCallback(async () => {
        if (!lobbyId) return;

        try {
            stopTimer();
            await lobbyService.leaveLobby(lobbyId);
            navigate("/lobbies");
        } catch (e: any) {
            setError(`Erro ao sair do lobby: ${e.message}`);
        }
    }, [lobbyId, navigate, stopTimer]);

    return {
        lobbyId: lobbyId ?? 0,
        name,
        currentPlayers,
        maxPlayers,
        rounds,
        players,
        loading,
        error,
        timeRemaining,
        timerStatus,
        minPlayersToStart,
        leaveLobby,
    };
}
