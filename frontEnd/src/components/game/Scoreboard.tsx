// @ts-ignore
import React from "react";
import {Player} from "../../services/game/responsesType";

interface ScoreboardProps {
    players: Player[];
}

const Scoreboard: React.FC<ScoreboardProps> = ({ players }) => {
    return (
        <div className="mt-6 w-full max-w-md">
        </div>
    );
};

export default Scoreboard;
