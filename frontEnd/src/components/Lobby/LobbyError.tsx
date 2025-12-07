import React from "react";
import {useNavigate} from "react-router-dom";

interface LobbyErrorProps {
    error: string;
}

export const LobbyError: React.FC<LobbyErrorProps> = ({ error }) => {
    const navigate = useNavigate();

    return (
        <div>

            <div className="lobby-error-container">
                <h2>Erro ao carregar lobby</h2>
                <p>{error}</p>
                <button
                    onClick={() => navigate('/lobbies')}
                    className="btn btn-primary"
                >
                    Voltar para Lobbies
                </button>
            </div>
        </div>
    );
};
