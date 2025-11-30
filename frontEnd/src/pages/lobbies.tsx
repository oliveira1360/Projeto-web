// pages/LobbyPage.tsx

import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLobby } from "../hooks/useLobby";
import {LobbyCard, LobbyDetails, LobbyHeaderControls} from "../components/lobby/LobbyElements";


const LobbyPage: React.FC = () => {
    const navigate = useNavigate();
    const {
        lobbies,
        currentLobbyDetails,
        loading,
        error,
        listAllLobbies,
        getDetails,
        joinLobby,
        leaveLobby,
    } = useLobby();

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
            await joinLobby(lobbyId);
            navigate(`/lobby/${lobbyId}`);
        } catch (error) {
            console.error("Erro ao entrar no lobby:", error);
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