// @ts-ignore
import React from "react";
import {Player} from "../../services/game/gameService";

interface PlayerSlotProps {
    player?: Player;
    isCurrentPlayer?: boolean;
    isActiveTurn?: boolean;
    positionClass: string;
}


export const PlayerSlot: React.FC<PlayerSlotProps> = ({ player, isCurrentPlayer = false, isActiveTurn = false, positionClass }) => {
    const defaultPlayer: Partial<Player> = { username: "empty" };
    const p = (player && player.username) ? player : (defaultPlayer as Player);
    const initial = p.username?.[0]?.toUpperCase() || "E";
    const name = p.username || "empty";

    const isPlaceholder = name === "empty";

    const avatarClasses = `
        avatar
        ${isPlaceholder ? 'placeholder' : ''}
        ${isCurrentPlayer ? 'current-player' : ''}
        ${isActiveTurn ? 'active-turn' : ''}
    `;

    const usernameClasses = `
        username
        ${isPlaceholder ? 'placeholder' : ''}
    `;

    return (
        <div className={`player-slot ${positionClass}`}>
            <div className={avatarClasses}>
                {initial}
            </div>
            <span className={usernameClasses}>
                {name}
            </span>
        </div>
    );
};

