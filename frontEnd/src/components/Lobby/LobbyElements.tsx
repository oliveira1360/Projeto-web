// components/lobby/LobbyElements.tsx

import React from 'react';
import { Lobby, LobbyDetailsResponse } from '../../services/lobby/lobbyService';
import { Link } from "react-router-dom";

// Propriedades para o Card de um Lobby
interface LobbyCardProps {
    lobby: Lobby;
    isSelected: boolean;
    onSelect: (lobbyId: number) => void;
    onJoin: (lobbyId: number) => void;
}

export const LobbyCard: React.FC<LobbyCardProps> = ({
                                                        lobby,
                                                        isSelected,
                                                        onSelect,
                                                        onJoin
                                                    }) => {
    const { lobbyId, name, currentPlayers, maxPlayers } = lobby;

    return (
        <div
            key={lobbyId}
            className={`lobby-card ${isSelected ? 'selected' : ''}`}
            onClick={() => onSelect(lobbyId)}
        >
            <h3>{name}</h3>
            <p>Jogadores: **{currentPlayers}/{maxPlayers}**</p>
            {/* REMOVIDO: Linha das Rondas */}

            <div className="card-actions">
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        onSelect(lobbyId);
                    }}
                    className="btn btn-secondary btn-small"
                >
                    {isSelected ? 'Detalhes' : 'Ver Detalhes'}
                </button>
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        onJoin(lobbyId);
                    }}
                    className="btn btn-success btn-small"
                    disabled={currentPlayers === maxPlayers}
                >
                    ENTRAR
                </button>
            </div>
        </div>
    );
};

export const LobbyHeaderControls: React.FC = () => (
    <div className="lobby-controls">
        <Link to="/" className="btn btn-secondary">
            VOLTAR PARA HOME
        </Link>
        <Link to="/lobbyCreation" className="btn btn-primary">
            CRIAR NOVO LOBBY
        </Link>
    </div>
);

interface LobbyDetailsProps {
    details: LobbyDetailsResponse;
    onLeave: (lobbyId: number) => void;
    // Removido: latestEvent, isSubscribed
}

export const LobbyDetails: React.FC<LobbyDetailsProps> = ({
                                                              details,
                                                              onLeave,
                                                              // Removido: latestEvent, isSubscribed
                                                          }) => (
    <div className="details-panel">
        <h3>Detalhes do Lobby (ID: {details.lobbyId})</h3>
        <p>Nome: **{details.name}**</p>
        <p>Rondas: **{details.rounds}**</p>
        <p>Jogadores Atuais: **{details.currentPlayers}/{details.maxPlayers}**</p>

        <button
            onClick={() => onLeave(details.lobbyId)}
            className="btn btn-danger"
        >
            SAIR
        </button>

        {/* REMOVIDO: Div do event-log SSE */}
    </div>
);