import React from "react";
import {Link} from "react-router-dom";

export const LobbyHeaderControls: React.FC = () => (
    <div className="lobby-controls">
        <Link to="/home" className="btn btn-secondary">
            VOLTAR PARA HOME
        </Link>
        <Link to="/lobbyCreation" className="btn btn-primary">
            CRIAR NOVO LOBBY
        </Link>
    </div>
);
