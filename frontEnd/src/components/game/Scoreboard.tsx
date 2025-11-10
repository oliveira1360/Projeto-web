// @ts-ignore
import React from "react";
import { Player } from "../../services/game/gameService";

interface ScoreboardProps {
    players: Player[];
}

const Scoreboard: React.FC<ScoreboardProps> = ({ players }) => {
    return (
        <div className="mt-6 w-full max-w-md">
            <h2 className="text-xl font-semibold mb-2">Scoreboard</h2>
            <ul className="bg-white rounded-xl border shadow">
                {players.map((p) => (
                    <li
                        key={p.playerId}
                        className="flex justify-between px-4 py-2 border-b last:border-none"
                    >
                        <span>{p.username ?? `Player ${p.playerId}`}</span>
                        <span className="font-bold">{p.points ?? 0}</span>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default Scoreboard;
