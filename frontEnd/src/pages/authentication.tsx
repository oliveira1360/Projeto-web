import * as React from "react";
import {useState} from 'react';
import {login} from "../services/user/userApi";
import {useNavigate} from "react-router-dom";
import {setToken} from "../utils/comman";


function AuthenticationPage() {
    const [isPanelActive, setIsPanelActive] = useState(false);
    const navigate = useNavigate();

    const [signInEmail, setSignInEmail] = useState('');
    const [signInPassword, setSignInPassword] = useState('');

    const [signUpName, setSignUpName] = useState('');
    const [signUpEmail, setSignUpEmail] = useState('');
    const [signUpPassword, setSignUpPassword] = useState('');

    const handleSignUpClick = () => setIsPanelActive(true);
    const handleSignInClick = () => setIsPanelActive(false);

    // @ts-ignore
    const handleSignInSubmit = async (event: React.FormEvent) => {
        // This stops the page from reloading
        event.preventDefault();

        try {
            const userData = await login(signInEmail, signInPassword);
            const token = userData.token;
            setToken(token)
            console.log('Login successful!', userData);
            navigate('/home');

        } catch (error) {
            console.error(error);
        }
    };

    // @ts-ignore
    const handleSignUpSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
    };

    return (
        <div>
            <div
                className={`container ${isPanelActive ? "right-panel-active" : ""}`}
                id="container"
            >
                {/* --- Sign Up Form --- */}
                <div className="form-container sign-up-container">
                    <form action="#" onSubmit={handleSignUpSubmit}>
                        <h1>Create Account</h1>
                        <span>or use your email for registration</span>
                        <input
                            type="text"
                            placeholder="Name"
                            value={signUpName}
                            onChange={e => setSignUpName(e.target.value)}
                        />
                        <input
                            type="email"
                            placeholder="Email"
                            value={signUpEmail}
                            onChange={e => setSignUpEmail(e.target.value)}
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={signUpPassword}
                            onChange={e => setSignUpPassword(e.target.value)}
                        />
                        <button>Sign Up</button>
                    </form>
                </div>

                {/* --- Sign in Form --- */}
                <div className="form-container sign-in-container">
                    <form action="#" onSubmit={handleSignInSubmit}>
                        <h1>Sign in</h1>
                        <span>or use your account</span>
                        <input
                            type="email"
                            placeholder="Email"
                            value={signInEmail}
                            onChange={e => setSignInEmail(e.target.value)}
                        />
                        <input
                            type="password"
                            placeholder="Password"
                            value={signInPassword}
                            onChange={e => setSignInPassword(e.target.value)}
                        />
                        <a href="#">Forgot your password?</a>
                        <button>Sign In</button>
                    </form>
                </div>

                <div className="overlay-container">
                    <div className="overlay">
                        <div className="overlay-panel overlay-left">
                            <h1>Welcome Back!</h1>
                            <p>To keep connected with us please login with your personal info</p>

                            <button
                                className="ghost"
                                id="signIn"
                                onClick={handleSignInClick}
                            >
                                Sign In
                            </button>
                        </div>
                        <div className="overlay-panel overlay-right">
                            <h1>Hello, Friend!</h1>
                            <p>Enter your personal details and start journey with us</p>

                            <button
                                className="ghost"
                                id="signUp"
                                onClick={handleSignUpClick}
                            >
                                Sign Up
                            </button>

                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default AuthenticationPage;