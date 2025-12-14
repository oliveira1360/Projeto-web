
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLobbies } from "../hooks/lobby/useLobbies";
import {lobbyService} from "../services/lobby/lobbyService";
import {LobbyCard} from "../components/Lobby/LobbyCard";
import {LobbyDetails} from "../components/Lobby/LobbyDetails";
import {LobbyHeaderControls} from "../components/Lobby/LobbyHeaderControls";


const LobbyPage: React.FC = () => {
    const navigate = useNavigate();
    const {
        lobbies,
        currentLobbyDetails,
        loading,
        error,
        listAllLobbies,
        getDetails,
        leaveLobby,
    } = useLobbies();

    const [selectedLobbyId, setSelectedLobbyId] = useState<number | null>(null);


    useEffect(() => {
        listAllLobbies();
    }, [listAllLobbies]);

    useEffect(() => {
        if (selectedLobbyId) {
            getDetails(selectedLobbyId);
        }
    }, [selectedLobbyId, getDetails]);

    const handleSelectLobby = (lobbyId: number) => {
        setSelectedLobbyId(lobbyId === selectedLobbyId ? null : lobbyId);
    };

    const handleJoinLobby = async (lobbyId: number) => {
        try {
            // Tenta fazer o join e espera pelo gameId se o jogo começar
            const gameId = await lobbyService.joinLobbyWithGameStart(lobbyId);
            // Se chegou aqui, recebemos um gameId do evento GAME_STARTED
            navigate(`/game/${gameId}`);
        } catch (error) {
            // @ts-ignore
            if (error.message === "Timeout esperando início do jogo") {
                // Se der timeout, significa que o jogo não começou imediatamente
                navigate(`/lobby/${lobbyId}`);
            } else {
                console.error("Erro ao entrar no lobby:", error);
            }
        }


    };

    const handleLeaveLobby = (lobbyId: number) => {
        leaveLobby(lobbyId);
        setSelectedLobbyId(null);
    };

    return (
        <div className="lobby-container lobby-page">


            <h2>Lobbies Disponíveis</h2>

            {loading && lobbies.length === 0 && <p className="loading-message">A carregar Lobbies...</p>}
            {error && <p className="error-message">Erro: {error}</p>}

            <div className="lobby-grid-container">
                <div className="lobby-grid">
                    {lobbies.map((lobby) => (
                        <LobbyCard
                            key={lobby.lobbyId}
                            lobby={lobby}
                            isSelected={selectedLobbyId === lobby.lobbyId}
                            onSelect={handleSelectLobby}
                            onJoin={handleJoinLobby}
                        />
                    ))}
                </div>
            </div>

            {selectedLobbyId && currentLobbyDetails && (
                <>
                    <hr />
                    <LobbyDetails
                        details={currentLobbyDetails}
                        onLeave={handleLeaveLobby}
                    />
                </>
            )}
            <LobbyHeaderControls/>
        </div>
    );
};

export default LobbyPage;