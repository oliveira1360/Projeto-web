import * as React from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { playerService } from "../services/player/playerService";
import { PlayerInfoResponse } from "../services/player/playerResponseTypes";
import { isSafeUpdatePassword } from "../services/user/userApi";
import "../../style/updatePlayerProfile.css";

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
        <div className="update-profile-container">
            <div className="update-profile-header">
                <h1>Update Player Profile</h1>
            </div>

            {state.errorMessage && (
                <p className="update-profile-error">{state.errorMessage}</p>
            )}
            {state.status === "success" && (
                <p className="update-profile-success">Profile updated successfully.</p>
            )}

            {!sucess && <div className="profile-update">
                <form className="profile-form" onSubmit={handleSubmit}>
                    <div className="profile-form-field">
                        <p>Name:</p>
                        <label>
                            <span>Name</span>
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
                    
                    <div className="profile-form-field">
                        <p>Nickname:</p>
                        <label>
                            <span>Nickname</span>
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

                    <div className="profile-form-field">
                        <p>Email:</p>
                        <label>
                            <span>Email</span>
                            <input
                                name="email"
                                type="email"
                                value={state.formData.email}
                                disabled
                                readOnly
                                aria-disabled="true"
                            />
                            <small>Email cannot be changed.</small>
                        </label>
                    </div>

                    <div className="profile-form-field">
                        <p>Password:</p>
                        <label>
                            <span>New Password</span>
                            <input
                                name="password"
                                type="password"
                                value={state.formData.password || ""}
                                onChange={handleChange}
                                placeholder="Enter new password"
                            />
                            <small>Leave blank to keep your current password.</small>
                        </label>
                    </div>

                    <div className="profile-form-field">
                        <p>Avatar URL:</p>
                        <label>
                            <span>Avatar URL</span>
                            <input
                                name="imageUrl"
                                type="url"
                                value={state.formData.imageUrl}
                                onChange={handleChange}
                                placeholder="https://..."
                            />
                        </label>
                    </div>
                
                    <div className="profile-form-actions">
                        <button type="submit" disabled={isSaving || !isSafeUpdatePassword(state.formData.password)}>
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