
import React from 'react';
import { HomeButton } from '../components/Home/HomeElements';
import { Link } from 'react-router';
import '../../style/HomePage.css';

const HomePage: React.FC = () => {
    return (
        <div className="home-container">
            <div className="home-header">
                <div className="home-header-center">
                    <h1>Welcome to the Game! - Poker Dice</h1>
                </div>
                
                <div className="home-header-right">
                    <Link to="/playerProfile" className="home-header-profile">
                        👤 PROFILE
                    </Link>
                    <Link to="/logout" className="home-header-logout">
                        ❌ LOGOUT
                    </Link>
                </div>
            </div>

            <div className="home-button-grid">
                <p>Choose your next action:</p>
                <HomeButton
                    to="/lobbies"
                    label="LOBBIES"
                    icon="🎲"
                    className="btn-main-action"
                />

                <HomeButton
                    to="/stats"
                    label="ESTATÍSTICAS"
                    icon="📊"
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