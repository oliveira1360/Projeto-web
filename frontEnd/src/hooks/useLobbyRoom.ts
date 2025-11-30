import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../services/lobby/lobbyService";
import {gameService} from "../services/game/gameService";

interface Player {
    userId: number;
    username: string;
}

interface LobbyRoomState {
    lobbyId: number;
    name: string;
    currentPlayers: number;
    maxPlayers: number;
    rounds: number;
    players: Player[];
    loading: boolean;
    error: string | null;
    leaveLobby: () => Promise<void>;
}

export function useLobbyRoom(lobbyId: number | undefined, userId: number | undefined): LobbyRoomState {
    const navigate = useNavigate();
    const [name, setName] = useState<string>("");
    const [currentPlayers, setCurrentPlayers] = useState<number>(0);
    const [maxPlayers, setMaxPlayers] = useState<number>(0);
    const [rounds, setRounds] = useState<number>(0);
    const [players, setPlayers] = useState<Player[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Carregar detalhes iniciais do lobby
    const loadLobbyDetails = useCallback(async () => {
        if (!lobbyId) {
            setError("Lobby ID não fornecido");
            setLoading(false);
            return;
        }

        try {
            const details = await lobbyService.getLobbyDetails(lobbyId);
            setName(details.name);
            setCurrentPlayers(details.currentPlayers);
            setMaxPlayers(details.maxPlayers);
            setRounds(details.rounds);


            // Por enquanto, criamos uma lista básica
            const playersList: Player[] = [];           //TODO fazer um endpoint para isto?
            for (let i = 0; i < details.currentPlayers; i++) {
                playersList.push({
                    userId: i,
                    username: `Jogador ${i + 1}`
                });
            }
            setPlayers(playersList);

            setLoading(false);
        } catch (e: any) {
            setError(`Erro ao carregar lobby: ${e.message}`);
            setLoading(false);
        }
    }, [lobbyId]);

    // Subscrever aos eventos SSE do lobby
    useEffect(() => {
        if (!lobbyId) return;

        loadLobbyDetails();

        const subscription = lobbyService.subscribeToLobbyEvents(
            lobbyId,
            (eventData) => {
                const { type, data } = eventData;

                switch (type) {
                    case "CONNECTED":
                        console.log("Conectado ao lobby SSE");
                        break;

                    case "PLAYER_JOINED":
                        console.log("Jogador entrou:", data);
                        setCurrentPlayers(prev => prev + 1);
                        setPlayers(prev => [...prev, {
                            userId: data.userId || prev.length,
                            username: data.username || `Jogador ${prev.length + 1}`
                        }]);
                        break;

                    case "PLAYER_LEFT":
                        console.log("Jogador saiu:", data);
                        setCurrentPlayers(prev => Math.max(0, prev - 1));
                        setPlayers(prev => prev.filter(p => p.userId !== data.userId));
                        break;

                    case "GAME_STARTED":
                        console.log("Jogo iniciando:", data);

                        // Navegar para a página do jogo
                        const gameId = data.data.gameId.value.gameId;
                        console.log(gameId);
                        if (gameId) {
                            navigate(`/game/${Number(gameId)}`);
                        }
                        break;

                    case "LOBBY_CLOSED":
                        console.log("Lobby fechado:", data);
                        // Navegar de volta para a lista de lobbies
                        navigate("/lobbies");
                        break;

                    default:
                        console.log("Evento desconhecido:", type, data);
                }
            },
            (err) => {
                console.error("Erro no SSE:", err);
                setError("Conexão perdida com o servidor");
            },
        );

        return () => {
            subscription.close();
        };
    }, [lobbyId]);

    const leaveLobby = useCallback(async () => {
        if (!lobbyId) return;

        try {
            await lobbyService.leaveLobby(lobbyId);
            navigate("/lobbies");
        } catch (e: any) {
            setError(`Erro ao sair do lobby: ${e.message}`);
        }
    }, [lobbyId, navigate]);

    return {
        lobbyId: lobbyId ?? 0,
        name,
        currentPlayers,
        maxPlayers,
        rounds,
        players,
        loading,
        error,
        leaveLobby,
    };
}