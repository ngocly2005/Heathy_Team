/** ID-token sign-in options; account selection remains in Google's own UI. */
export function googleSignInOptions(clientId, callback) {
    const id = typeof clientId === 'string' ? clientId.trim() : '';
    if (!id) throw new Error('Chưa cấu hình Google Web Client ID.');
    return { client_id: id, callback, auto_select: false, button_auto_select: false, ux_mode: 'popup' };
}

/** Record application sign-out without signing the browser out of Google. */
export function disableGoogleAutoSelect(idApi) {
    try { idApi?.disableAutoSelect?.(); } catch { /* Local app logout must still finish. */ }
}

export function adminAccountDeniedMessage(email) {
    const selected = typeof email === 'string' && email.trim() ? email.trim() : 'vừa chọn';
    return `Tài khoản ${selected} chưa có quyền quản trị hoặc đang bị khóa. Hãy chọn đúng tài khoản Google đã được cấp Admin.`;
}
