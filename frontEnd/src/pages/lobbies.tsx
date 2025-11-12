// pages/LobbyPage.tsx

import React, { useEffect, useState } from "react";
import { useLobby } from "../hooks/useLobby";
import { LobbyCard, LobbyDetails } from "../components/lobby/LobbyElements";

const LobbyPage: React.FC = () => {
    const {
        lobbies,
        currentLobbyDetails, // Reintroduzido
        loading,
        error,
        listAllLobbies,
        getDetails, // Reintroduzido
        joinLobby,
        leaveLobby,
    } = useLobby();

    const [selectedLobbyId, setSelectedLobbyId] = useState<number | null>(null);

    // Efeito para carregar a lista de lobbies ao montar
    useEffect(() => {
        listAllLobbies();
    }, [listAllLobbies]);

    // Efeito para lidar com a seleção e carregar os detalhes
    useEffect(() => {
        if (selectedLobbyId) {
            getDetails(selectedLobbyId);
        }
    }, [selectedLobbyId, getDetails]);

    const handleSelectLobby = (lobbyId: number) => {
        // Se o lobby já estiver selecionado, desseleciona-o
        setSelectedLobbyId(lobbyId === selectedLobbyId ? null : lobbyId);
    };

    const handleJoinLobby = (lobbyId: number) => {
        joinLobby(lobbyId);
        setSelectedLobbyId(lobbyId); // Seleciona para ver os detalhes depois de entrar
    };

    const handleLeaveLobby = (lobbyId: number) => {
        leaveLobby(lobbyId);
        setSelectedLobbyId(null); // Limpa a seleção após sair
    };

    return (
        <div className="lobby-container">
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
                    // Removido: latestEvent, isSubscribed
                />
            )}
        </div>
    );
};

export default LobbyPage;