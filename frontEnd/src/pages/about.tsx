import * as React from "react";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { userInfo } from "os";


function AboutPage() {
    return (
        <div className="home-container lobby-page">
            <h1>About Page</h1>
            <div>
                <p>
                    This game was developed during the
                    DAW (Desenvolvimento de Aplicações Web) course at ISEL (Instituto Superior de Engenharia de Lisboa).
                </p>
                
                <p>
                    The project was created by a team of dedicated students aiming to apply their knowledge in web development.
                </p>
                <strong>Team Members:</strong>
                <ul>     
                    <li>Diogo Oliveira (A51662)</li>
                    <li>Paulo Nascimento (A51665)</li>
                    <li>Jessé Alencar (A51745)</li>
                </ul>
            </div>
            <div>
                <p>&copy; 2025 DAW Poker Dice Project - ISEL</p>
            </div>
        </div>
    );
}

export default AboutPage;