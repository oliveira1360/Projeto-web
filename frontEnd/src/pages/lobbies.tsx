// pages/LobbyPage.tsx

import React, { useEffect, useState } from "react";
import { useLobby } from "../hooks/useLobby";
import {LobbyCard, LobbyDetails, LobbyHeaderControls} from "../components/lobby/LobbyElements";


const LobbyPage: React.FC = () => {
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

    const handleJoinLobby = (lobbyId: number) => {
        joinLobby(lobbyId);
        setSelectedLobbyId(lobbyId);
    };

    const handleLeaveLobby = (lobbyId: number) => {
        leaveLobby(lobbyId);
        setSelectedLobbyId(null);
    };

    return (
        <div className="lobby-container lobby-page">

            <LobbyHeaderControls />

            <h2>Lobbies Disponíveis</h2>

            {loading && lobbies.length === 0 && <h2>A carregar Lobbies...</h2>}
            {error && <p className="error-message">Erro: {error}</p>}

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

            <hr />

            {selectedLobbyId && currentLobbyDetails && (
                <LobbyDetails
                    details={currentLobbyDetails}
                    onLeave={handleLeaveLobby}
                />
            )}
        </div>
    );
};

export default LobbyPage;