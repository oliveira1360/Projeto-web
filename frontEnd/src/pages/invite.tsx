import * as React from "react";
import { Link } from "react-router";
import { createInviteToken, InviteToken } from "../services/user/userApi";
import "../../style/invite.css";

type Status = "idle" | "creating" | "success" | "error";

type State = {
    invite: InviteToken | null;
    status: Status;
    errorMessage: string;
};

type Action =
    | { type: "create_start" }
    | { type: "create_success"; payload: InviteToken }
    | { type: "create_error"; payload: string };

const initialState: State = {
    invite: null,
    status: "idle",
    errorMessage: "",
};

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "create_start":
            return { ...state, status: "creating", errorMessage: "" };
        case "create_success":
            return {
                ...state,
                status: "success",
                invite: action.payload,
                errorMessage: "",
            };
        case "create_error":
            return { ...state, status: "error", errorMessage: action.payload, invite: null };
        default:
            return state;
    }
}

function CreateInvitePage() {
    const [state, dispatch] = React.useReducer(reducer, initialState);
    const [copied, setCopied] = React.useState(false);

    const handleCreateInvite = async () => {
        dispatch({ type: "create_start" });
        try {
            const invite = await createInviteToken();
            dispatch({ type: "create_success", payload: invite });
        } catch (error) {
            console.error("Error creating invite:", error);
            dispatch({ type: "create_error", payload: "Failed to create invite. Please try again." });
        }
    };

    const handleCopyToken = async () => {
        if (state.invite?.token) {
            try {
                await navigator.clipboard.writeText(state.invite.token);
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
            } catch (error) {
                console.error("Failed to copy:", error);
            }
        }
    };

    const formatDate = (isoString: string) => {
        try {
            const date = new Date(isoString);
            return date.toLocaleString();
        } catch {
            return isoString;
        }
    };

    const isCreating = state.status === "creating";

    return (
        <div className="invite-container">
            <div className="invite-header">
                <h1>Create Invite</h1>
            </div>

            <div className="invite-nav">
                <Link to="/home">Back to Home</Link>
            </div>

            {state.errorMessage && (
                <p className="invite-error">{state.errorMessage}</p>
            )}

            <div className="invite-card">
                <div className="invite-button-container">
                    <button onClick={handleCreateInvite} disabled={isCreating}>
                        {isCreating ? "Creating..." : "Create New Invite"}
                    </button>
                </div>

                {state.status === "success" && state.invite && (
                    <div className="invite-success">
                        <h2>Invite Created Successfully!</h2>
                        <div className="invite-token-display">
                            <p className="invite-token-label">Token:</p>
                            <div className="invite-token-code-wrapper">
                                <div className="invite-token-code">
                                    {state.invite.token}
                                </div>
                                <button
                                    className="invite-copy-button"
                                    onClick={handleCopyToken}
                                    title="Copy to clipboard"
                                >
                                    {copied ? (
                                        <>
                                            <span className="copy-icon">✓</span>
                                            <span className="copy-text">Copied!</span>
                                        </>
                                    ) : (
                                        <>
                                            <span className="copy-icon">📋</span>
                                            <span className="copy-text">Copy</span>
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default CreateInvitePage;