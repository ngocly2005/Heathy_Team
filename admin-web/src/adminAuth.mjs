import { adminAccountDeniedMessage } from './googleSignIn.mjs';

/** Shared exchange for either login method; only server-confirmed admins are stored. */
export async function loginAdmin(input, exchange, remember, onDenied) {
    const google = typeof input?.credential === 'string' && input.credential.length > 0;
    if (!google && (typeof input?.email !== 'string' || typeof input?.password !== 'string' || !input.password)) {
        throw new Error('Nhập email và mật khẩu.');
    }
    const tokens = await exchange(google ? '/auth/google' : '/auth/login', {
        method: 'POST', body: JSON.stringify(google ? { id_token: input.credential }
            : { email: input.email.trim(), password: input.password }),
    });
    let stored = false;
    try {
        if (!tokens?.access_token || !tokens?.refresh_token) throw new Error('Phiên đăng nhập chưa đầy đủ.');
        const user = await exchange('/auth/me', { headers: { Authorization: `Bearer ${tokens.access_token}` } });
        if (user.role !== 'admin' || user.status !== 'active') {
            onDenied();
            throw new Error(adminAccountDeniedMessage(user.email));
        }
        remember({ ...tokens, user });
        stored = true;
    } finally {
        if (!stored && tokens?.refresh_token) await exchange('/auth/logout', {
            method: 'POST', body: JSON.stringify({ refresh_token: tokens.refresh_token }),
        }).catch(() => undefined);
    }
}
