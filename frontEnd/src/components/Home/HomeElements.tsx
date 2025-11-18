// src/components/home/HomeButton.tsx

import React from 'react';
import { Link } from 'react-router-dom';

interface HomeButtonProps {
    to: string;
    label: string;
    icon: string; // Para usar um emoji ou ícone real
    className?: string;
}

export const HomeButton: React.FC<HomeButtonProps> = ({ to, label, icon, className }) => (
    <Link to={to} className={`btn btn-home ${className || ''}`}>
        <span className="btn-icon">{icon}</span>
        <span className="btn-label">{label}</span>
    </Link>
);