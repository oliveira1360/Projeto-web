import * as React from "react";
import { logoutUser } from "../services/user/userApi";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { Link, useNavigate } from "react-router";
import "../../style/playerProfile.css";

function LogoutPage() {
    const navigate = useNavigate();
    React.useEffect(() => {
        async function logoutUserEffect() {
            try {   
                const result = await logoutUser();
                if (result) {
                    navigate("/");
                } else {
                    console.error("Logout failed");
                }
            } catch (error) {
                console.error("Error fetching user info:", error);
            }
        }
        logoutUserEffect();
    }, [true]);
    return (
        <div className="player-profile-container">
            <p>Logging out...</p>
        </div>
    )
}

export default LogoutPage