import * as React from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { isSafePassword } from "../services/user/userApi";

type FormState = {
    name: string;
    nickName: string;
    email: string;
    password?: string;
    imageUrl: string;
};

type Status = "idle" | "saving" | "success" | "error";

type State = {
    userInfo: PlayerInfoResponse | null;
    formData: FormState;
    status: Status;
    errorMessage: string;
};

type Action =
    | { type: "change_field"; payload: { name: keyof FormState; value: string } }
    | { type: "save_start" }
    | { type: "save_success"; payload: PlayerInfoResponse }
    | { type: "save_error"; payload: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "change_field":
            return {
                ...state,
                formData: { ...state.formData, [action.payload.name]: action.payload.value },
            };
        case "save_start":
            return { ...state, status: "saving", errorMessage: "" };
        case "save_success":
            return {
                ...state,
                status: "success",
                userInfo: action.payload,
                formData: {
                    name: action.payload.name || "",
                    nickName: action.payload.nickName || "",
                    email: action.payload.email || "",
                    imageUrl: action.payload.imageUrl || "",
                },
            };
        case "save_error":
            return { ...state, status: "error", errorMessage: action.payload };
        default:
            return state;
    }
}

function UpdatePlayerProfilePage() {
    const location = useLocation();
    const userInfo = (location.state as { user?: PlayerInfoResponse } | null)?.user;

    const initialState: State = {
        userInfo: userInfo ?? null,
        formData: {
            name: userInfo?.name || "",
            nickName: userInfo?.nickName || "",
            email: userInfo?.email || "",
            password: "",
            imageUrl: userInfo?.imageUrl || "",
        },
        status: "idle",
        errorMessage: "",
    };

    const [state, dispatch] = React.useReducer(reducer, initialState);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.target;
        dispatch({ type: "change_field", payload: { name: name as keyof FormState, value } });
    };

    const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        dispatch({ type: "save_start" });
        try {
            const payload: {
                name: string;
                nickName: string;
                imageUrl: string;
                password?: string;
            } = {
                name: state.formData.name,
                nickName: state.formData.nickName,
                imageUrl: state.formData.imageUrl,
            };

            if (state.formData.password && state.formData.password.trim().length > 0) {
                payload.password = state.formData.password.trim();
            }

            const updated = await playerService.updatePlayerProfile(payload);
            dispatch({ type: "save_success", payload: updated });
        } catch (error) {
            console.error("Error updating user info:", error);
            dispatch({ type: "save_error", payload: "Unable to update your profile. Please try again." });
        }
    };

    const isSaving = state.status === "saving";

    const sucess = state.status === "success";
    const navigate = useNavigate();
    if (sucess) {
        setTimeout(() => {
            navigate("/playerProfile");
        }, 1500);
    }

    return (
        <div className="home-container lobby-page">
            <h1>Update Player Profile</h1>

            {state.errorMessage && (
                <p style={{ color: "red" }}>{state.errorMessage}</p>
            )}
            {state.status === "success" && (
                <p style={{ color: "green" }}>Profile updated successfully.</p>
            )}

            {!sucess && <div className="profile-update">
                <form className="profile-form" onSubmit={handleSubmit} style={{ display: "grid", gap: "12px", maxWidth: "480px", alignItems: "center" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <p style={{ margin: 0 }}>Name:</p>
                        <label style={{ margin: 0 }}>
                            Name
                            <input
                                name="name"
                                type="text"
                                value={state.formData.name}
                                onChange={handleChange}
                                placeholder="Your name"
                                required
                            />
                        </label>
                    </div>
                    
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <p>Nickname:</p>
                        <label style={{ margin: 0 }}>
                            Nickname
                            <input
                                name="nickName"
                                type="text"
                                value={state.formData.nickName}
                                onChange={handleChange}
                                placeholder="Your nickname"
                                required
                            />
                        </label>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <p>Email:</p>
                        <label style={{ margin: 0 }}>
                            Email
                            <input
                                name="email"
                                type="email"
                                value={state.formData.email}
                                disabled
                                readOnly
                                aria-disabled="true"
                            />
                            <small style={{ color: "#666", display: "block" }}>Email cannot be changed.</small>
                        </label>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <p>Password:</p>
                        <label style={{ margin: 0 }}>
                            New Password
                            <input
                                name="password"
                                type="password"
                                value={state.formData.password || ""}
                                onChange={handleChange}
                                placeholder="Enter new password"
                            />
                            <small style={{ color: "#666", display: "block" }}>Leave blank to keep your current password.</small>
                        </label>
                    </div>

                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <p>Avatar URL:</p>
                        <label style={{ margin: 0 }}>
                            Avatar URL
                            <input
                                name="imageUrl"
                                type="url"
                                value={state.formData.imageUrl}
                                onChange={handleChange}
                                placeholder="https://..."
                            />
                        </label>
                    </div>
                
                    <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
                        <button type="submit" disabled={isSaving || !isSafePassword(state.formData.password || "")}>
                            {isSaving ? "Saving..." : "Save changes"}
                        </button>
                        <Link to="/playerProfile">
                            Cancel
                        </Link>
                    </div>
                </form>
            </div>
            }
        </div>
        
    );
}

export default UpdatePlayerProfilePage