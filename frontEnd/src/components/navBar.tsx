import * as React from "react";
import {Link, useNavigate} from "react-router-dom";
import { getToken } from "../utils/comman";
import { logout } from "../services/user/userApi";

export function NavBar() {
    const token = getToken();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await logout();
            // After logout, redirect to the authentication/sign-in page
            navigate('/');
        } catch (error) {
            console.error('Logout failed', error);
        }
    }


    return (
        <nav>
            <button onClick={() => navigate('/home')}>Home</button>
            <button onClick={() => navigate('/playerProfile')}>Profile</button>
            {token ? (
                <button onClick={handleLogout}>
                    Logout
                </button>
            ) : (
                <Link to="/">Sign In</Link>
            )}
        </nav>
    );
}