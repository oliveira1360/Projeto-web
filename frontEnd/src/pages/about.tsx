import * as React from "react";
import { Link } from "react-router";
import "../../style/about.css";


function AboutPage() {
    return (
        <div className="about-container">
            <div className="about-header">
                <h1>About</h1>
            </div>

            <nav className="about-nav">
                <Link to="/home">Home</Link>
            </nav>

            <div className="about-card">
                <div className="about-content">
                    <p>
                        This game was developed during the
                        DAW (Desenvolvimento de Aplicações Web) course at ISEL (Instituto Superior de Engenharia de Lisboa).
                    </p>
                    
                    <p>
                        The project was created by a team of dedicated students aiming to apply their knowledge in web development.
                    </p>
                    
                    <div className="about-team">
                        <strong>Team Members:</strong>
                        <ul>     
                            <li>Diogo Oliveira (A51662)</li>
                            <li>Paulo Nascimento (A51665)</li>
                            <li>Jessé Alencar (A51745)</li>
                        </ul>
                    </div>
                </div>
                
                <div className="about-footer">
                    <p>&copy; 2025 DAW Poker Dice Project - ISEL</p>
                </div>
            </div>
        </div>
    );
}

export default AboutPage;