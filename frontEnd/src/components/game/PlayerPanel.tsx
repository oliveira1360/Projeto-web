// @ts-ignore
import React from "react";
import {Player} from "../../services/game/responsesType";

interface PlayerSlotProps {
    player?: Player;
    isCurrentPlayer?: boolean;
    isActiveTurn?: boolean;
    positionClass: string;
}


export const PlayerSlot: React.FC<PlayerSlotProps> = ({ player, isCurrentPlayer = false, isActiveTurn = false, positionClass }: PlayerSlotProps) => {
    const initial = player?.username?.[0]?.toUpperCase() || "E";
    const name = player?.username || "empty";

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

    const imageUrl = player?.url;

    return (
        <div className={`player-slot ${positionClass}`}>
            <div className={avatarClasses}>
                {imageUrl ? (
                    <img
                        src={imageUrl}
                        alt={name}
                        style={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'cover',
                            borderRadius: '50%'
                        }}
                        onError={(e) => {
                            const img = e.currentTarget;
                            img.style.display = 'none';
                            const parent = img.parentElement;
                            if (parent && !parent.querySelector('.player-initial')) {
                                const initialDiv = document.createElement('div');
                                initialDiv.className = 'player-initial';
                                initialDiv.textContent = initial;
                                initialDiv.style.cssText = `
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    width: 100%;
                                    height: 100%;
                                    display: flex;
                                    align-items: center;
                                    justify-content: center;
                                    font-size: 2em;
                                    color: white;
                                    font-weight: bold;
                                `;
                                parent.appendChild(initialDiv);
                            }
                        }}
                    />
                ) : (
                    initial
                )}
            </div>
            <span className={usernameClasses}>
                {name}
            </span>
        </div>
    );
};

