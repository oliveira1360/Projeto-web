import React from "react";
import {Lobby} from "../../services/Lobby/lobbyResponseTypes";

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
            <p>
                Jogadores: <strong>{currentPlayers.length}/{maxPlayers}</strong>
            </p>


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
                    disabled={currentPlayers.length === maxPlayers}
                >
                    ENTRAR
                </button>
            </div>
        </div>
    );
};
