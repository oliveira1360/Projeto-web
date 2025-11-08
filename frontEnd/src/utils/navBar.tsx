import * as React from "react";
import {Link} from "react-router-dom";

export function NavBar() {
    return (
        <nav>
            <Link to="/">Home</Link>
            <span> | </span>
            <Link to="/auth">Login / Register</Link>
        </nav>
    );
}