import * as React from "react";
import {Link} from "react-router-dom";

export function NavBar() {
    return (
        <nav>
            <Link to="/home">Home</Link>
            <span> | </span>
            <Link to="/">Login / Register</Link>
        </nav>
    );
}