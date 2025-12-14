import * as React from "react";
import { playerService } from "../services/player/playerService";
import { PlayerStatsResponse } from "../services/player/playerResponseTypes";
import { Link } from "react-router";
import "../../style/playerStats.css";

function PlayerStatsPage() {
    const [userStats, setUserStats] = React.useState<PlayerStatsResponse>();
    React.useEffect(() => {
        async function fetchUserStats() {
            try {
                const userStats = await playerService.playerStats();
                console.log("User Stats:", userStats);
                setUserStats(userStats);
            } catch (error) {
                console.error("Error fetching user stats:", error);
            }
        }
        fetchUserStats();
    }, []);
    
    const winRate = userStats && userStats.totalGamesPlayed > 0 
        ? ((userStats.totalWins / userStats.totalGamesPlayed) * 100).toFixed(1)
        : "0.0";
    
    return (
        <div className="player-stats-container">
            <div className="player-stats-header">
                <div className="player-stats-header-center">
                    <h1>Player Statistics</h1>
                </div>
                <div className="player-stats-header-left">
                    <Link to="/home" className="player-stats-header-home">
                        Home
                    </Link>
                </div>
            </div>

            {userStats ? (
                <div className="player-stats-card">
                    <div className="player-stats-overview">
                        <h2>Game Statistics</h2>
                        <p>Your performance overview</p>
                    </div>
                    
                    <div className="player-stats-info">
                        <p><strong>Total Games</strong> <span>{userStats.totalGamesPlayed}</span></p>
                        <p><strong>Wins</strong> <span>{userStats.totalWins}</span></p>
                        <p><strong>Losses</strong> <span>{userStats.totalLosses}</span></p>
                        <p><strong>Win Rate</strong> <span>{winRate}%</span></p>
                        <p><strong>Total Points</strong> <span>{userStats.totalPoints}</span></p>
                        <p><strong>Current Streak</strong> <span>{userStats.currentStreak}</span></p>
                        <p><strong>Longest Streak</strong> <span>{userStats.longestStreak}</span></p>
                    </div>
                    
                    <div className="player-stats-actions">
                        <Link to="/playerProfile">
                            View Profile
                        </Link>
                        <Link to="/lobbies">
                            Play Game
                        </Link>
                    </div>
                </div>
            ) : (
                <div className="player-stats-loading">Loading statistics...</div>
            )}
        </div>
    );
}

export default PlayerStatsPage