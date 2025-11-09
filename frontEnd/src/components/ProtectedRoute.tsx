// @ts-ignore
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import {getToken} from "../utils/comman";

export function ProtectedRoute() {
    const token = getToken();

    if (!token) {
        return <Navigate to="/" replace />;
    }

    return (
        <div>
            <Outlet />
        </div>
    );
}