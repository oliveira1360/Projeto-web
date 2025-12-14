import { useEffect, useState, useCallback } from "react";
import { lobbyService,} from "../services/lobby/lobbyService";
import {Lobby, LobbyDetailsResponse} from "../services/lobby/lobbyResponseTypes";

interface LobbyHookResult {
    lobbies: Lobby[];
    currentLobbyDetails: LobbyDetailsResponse | null;
    loading: boolean;
    error: string | null;
    listAllLobbies: () => Promise<void>;
    getDetails: (lobbyId: number) => Promise<void>;
    joinLobby: (lobbyId: number) => Promise<void>;
    leaveLobby: (lobbyId: number) => Promise<void>;
}

export function useLobby(): LobbyHookResult {
    const [lobbies, setLobbies] = useState<Lobby[]>([]);
    const [currentLobbyDetails, setCurrentLobbyDetails] = useState<LobbyDetailsResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // --- Funções de Serviço ---

    const listAllLobbies = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await lobbyService.listLobbies();
            setLobbies(data.lobbies);
        } catch (e: any) {
            setError(`Failed to list lobbies: ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, []);

    const getDetails = useCallback(async (lobbyId: number) => {
        setLoading(true);
        setError(null);
        try {
            const details = await lobbyService.getLobbyDetails(lobbyId);
            setCurrentLobbyDetails(details);
        } catch (e: any) {
            setError(`Failed to get lobby details: ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, []);

    const joinLobby = useCallback(async (lobbyId: number) => {
        setLoading(true);
        setError(null);
        try {
            await lobbyService.joinLobby(lobbyId);
            await getDetails(lobbyId); // Atualiza os detalhes para o usuário ver
        } catch (e: any) {
            setError(`Failed to join lobby: ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, [getDetails]);

    const leaveLobby = useCallback(async (lobbyId: number) => {
        setLoading(true);
        setError(null);
        try {
            await lobbyService.leaveLobby(lobbyId);
            setCurrentLobbyDetails(null); // Limpa os detalhes
            await listAllLobbies(); // Atualiza a lista
        } catch (e: any) {
            setError(`Failed to leave lobby: ${e.message}`);
        } finally {
            setLoading(false);
        }
    }, [listAllLobbies]);

    useEffect(() => {
        const DASHBOARD_LOBBY_ID = 0;

        const connection = lobbyService.subscribeToLobbyEvents(
            DASHBOARD_LOBBY_ID,
            (event) => {
                if (event.type === "LOBBY_LIST_UPDATED" || event.type === "LOBBY_CLOSED") {
                    console.log("Evento SSE recebido. A aguardar commit DB...");

                    // Pequeno delay (300ms) para garantir que o Backend acabou de gravar os dados novos
                    setTimeout(() => {
                        console.log("A atualizar lista de lobbies...");
                        listAllLobbies();
                    }, 300);
                }
            },
            (err) => {
                //Tratar erros silenciosamente ou reconectar
                console.warn("SSE Dashboard connection warning:", err);
            }
        );

        return () => {
            connection.close();
        };
    }, [listAllLobbies]);

    return {
        lobbies,
        currentLobbyDetails,
        loading,
        error,
        listAllLobbies,
        getDetails,
        joinLobby,
        leaveLobby,
    };
}