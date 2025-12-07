import React from "react";

export const LobbyLoading: React.FC = () => {
    return (
        <div>

            <div style={{ textAlign: 'center', marginTop: '50px' }}>
                <h2>Carregando lobby...</h2>
                <div className="spinner"></div>
            </div>
        </div>
    );
};