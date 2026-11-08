export type GoogleCredentialReply = { credential: string };
export type GoogleSignInConfiguration = {
    client_id: string;
    callback: (reply: GoogleCredentialReply) => void | Promise<void>;
    auto_select: false;
    button_auto_select: false;
    ux_mode: 'popup';
};
export function googleSignInOptions(clientId: string | undefined,
    callback: GoogleSignInConfiguration['callback']): GoogleSignInConfiguration;
export function disableGoogleAutoSelect(idApi: { disableAutoSelect?(): void } | undefined): void;
export function adminAccountDeniedMessage(email: string | undefined): string;
