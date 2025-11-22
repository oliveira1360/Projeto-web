// src/pages/HomePage.tsx

import React from 'react';
import { HomeButton } from '../components/Home/HomeElements';

const HomePage: React.FC = () => {
    return (
        <div className="home-container lobby-page">

            <h1>Bem-vindo ao Jogo!</h1>
            <p>Escolha a sua próxima ação:</p>

            <div className="home-button-grid">

                <HomeButton
                    to="/lobbies"
                    label="LOBBIES"
                    icon="🎲"
                    className="btn-main-action"
                />

                <HomeButton
                    to="/playerProfile"
                    label="PERFIL DO JOGADOR"
                    icon="👤"
                    className="btn-info-action"
                />

                <HomeButton
                    to="/about"
                    label="SOBRE"
                    icon="ℹ️"
                    className="btn-secondary-action"
                />
            </div>
        </div>
    );
};

export default HomePage;