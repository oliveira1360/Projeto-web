import * as React from "react";
import {useState} from 'react';
import {login, register, isSafePassword} from "../services/user/userApi";
import {useNavigate} from "react-router-dom";

const ERROR_MESSAGES: Record<string, string> = {
    INVALID_CREDENTIALS: "Email ou password incorretos.",
    USER_NOT_FOUND: "Utilizador não existe.",
    EMAIL_ALREADY_EXISTS: "Este email já está registado.",
    NICKNAME_TAKEN: "Este nickname já está em uso.",
    INVALID_INVITE_CODE: "Código de convite inválido.",
    WEAK_PASSWORD: "A password é demasiado fraca.",
    NETWORK_ERROR: "Erro de ligação ao servidor.",
    UNKNOWN_ERROR: "Ocorreu um erro inesperado."
};

const mapError = async (err: any): Promise<string> => {
    if (err?.detail) return err.detail;
    if (err?.code && ERROR_MESSAGES[err.code]) return ERROR_MESSAGES[err.code];

    return ERROR_MESSAGES.UNKNOWN_ERROR;
};

function AuthenticationPage() {

    const [isPanelActive, setIsPanelActive] = useState(false);
    const navigate = useNavigate();

    const [signInEmail, setSignInEmail] = useState('');
    const [signInPassword, setSignInPassword] = useState('');

    const [signUpName, setSignUpName] = useState('');
    const [signUpEmail, setSignUpEmail] = useState('');
    const [signUpPassword, setSignUpPassword] = useState('');
    const [signUpNickname, setSignUpNickname] = useState('');
    const [signUpInviteCode, setSignUpInviteCode] = useState('');

    const handleSignUpClick = () => setIsPanelActive(true);
    const handleSignInClick = () => setIsPanelActive(false);

    const [loading, setLoading] = useState(false);
    const [signInError, setSignInError] = useState<string | null>(null);
    const [signUpError, setSignUpError] = useState<string | null>(null);

    const [signInEmailError, setSignInEmailError] = useState<string | null>(null);
    const [signInPasswordError, setSignInPasswordError] = useState<string | null>(null);

    const [signUpEmailError, setSignUpEmailError] = useState<string | null>(null);
    const [signUpPasswordError, setSignUpPasswordError] = useState<string | null>(null);


    const isValidEmail = (email: string) =>
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

    const validateSignIn = () => {
        let valid = true;

        if (!isValidEmail(signInEmail)) {
            setSignInEmailError("Email inválido.");
            valid = false;
        } else {
            setSignInEmailError(null);
        }

        if (!isSafePassword(signInPassword)) {
            setSignInPasswordError("Password fraca ou inválida.");
            valid = false;
        } else {
            setSignInPasswordError(null);
        }

        return valid;
    };

    const validateSignUp = () => {
        let valid = true;

        if (!isValidEmail(signUpEmail)) {
            setSignUpEmailError("Email inválido.");
            valid = false;
        } else {
            setSignUpEmailError(null);
        }

        if (!isSafePassword(signUpPassword)) {
            setSignUpPasswordError("Password fraca ou inválida.");
            valid = false;
        } else {
            setSignUpPasswordError(null);
        }

        return valid;
    };

    // @ts-ignore
    const handleSignInSubmit = async (event: React.FormEvent) => {
        // This stops the page from reloading
        event.preventDefault();

        const isValid = validateSignIn();
        if (!isValid) return;

        setLoading(true);
        setSignInError(null);

        try {
            const userData = await login(signInEmail, signInPassword);
            console.log('Login successful!', userData);
            navigate('/home');

        } catch (error: any) {
            setSignInError(await mapError(error));
        } finally {
            setLoading(false);
        }
    };

    // @ts-ignore
    const handleSignUpSubmit = async (event: React.FormEvent) => {
        event.preventDefault();

        const isValid = validateSignUp();
        if (!isValid) return;

        setLoading(true);
        setSignUpError(null);

        try {
            const userData = await register(
                signUpName,
                signUpNickname,
                signUpEmail,
                signUpPassword,
                signUpInviteCode
            );

            console.log('Registration successful!', userData);
            setIsPanelActive(false);
            navigate('/home');

        } catch (error: any) {
            setSignUpError(await mapError(error));
        } finally {
            setLoading(false);
        }
    }

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
                            type="text"
                            placeholder="Nickname"
                            value={signUpNickname}
                            onChange={e => setSignUpNickname(e.target.value)}
                        />
                        <input
                            type="email"
                            placeholder="Email"
                            value={signUpEmail}
                            onChange={e => setSignUpEmail(e.target.value)}
                        />
                        {signUpEmailError && <p className="error-text">{signUpEmailError}</p>}
                        <input
                            type="password"
                            placeholder="Password"
                            value={signUpPassword}
                            onChange={e => setSignUpPassword(e.target.value)}
                        />
                        {signUpPasswordError && <p className="error-text">{signUpPasswordError}</p>}
                        <input
                            type="text"
                            placeholder="Invite Code"
                            value={signUpInviteCode}
                            onChange={e => setSignUpInviteCode(e.target.value)}
                        />
                        {signUpError && <p className="error-text">{signUpError}</p>}
                        <button disabled={loading}>
                            {loading ? "A criar conta..." : "Sign Up"}
                        </button>
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
                        {signInEmailError && <p className="error-text">{signInEmailError}</p>}
                        <input
                            type="password"
                            placeholder="Password"
                            value={signInPassword}
                            onChange={e => setSignInPassword(e.target.value)}
                        />
                        {signInPasswordError && <p className="error-text">{signInPasswordError}</p>}
                        {signInError && <p className="error-text">{signInError}</p>}
                        <button disabled={loading}>
                            {loading ? "A entrar..." : "Sign In"}
                        </button>
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