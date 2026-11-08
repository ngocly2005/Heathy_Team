import { useEffect, useRef, useState } from 'react';
import { useLogin } from 'react-admin';
import { Alert, Box, Card, CircularProgress, Typography } from '@mui/material';
import { googleSignInOptions, type GoogleSignInConfiguration } from './googleSignIn.mjs';

declare global {
    interface Window {
        google?: { accounts: { id: {
            initialize(options: GoogleSignInConfiguration): void;
            disableAutoSelect(): void;
            renderButton(element: HTMLElement, options: { theme: string; size: string; text: string; locale: string }): void;
        } } };
    }
}
export function GoogleLogin() {
    const login = useLogin();
    const target = useRef<HTMLDivElement>(null);
    const submitting = useRef(false);
    const [error,setError] = useState('');
    const [busy,setBusy] = useState(false);
    useEffect(() => {
        const clientId = import.meta.env.VITE_GOOGLE_WEB_CLIENT_ID?.trim();
        if (!clientId) { setError('Đăng nhập Google chưa được cấu hình. Liên hệ người quản trị hệ thống.'); return; }
        let initialized = false;
        let elapsed = 0;
        const timer = setInterval(() => {
            elapsed += 200;
            const googleId = window.google?.accounts?.id;
            if (googleId && target.current && !initialized) {
                initialized = true;
                googleId.initialize(googleSignInOptions(clientId, async reply => {
                    if (submitting.current) return;
                    submitting.current = true;
                    setBusy(true); setError('');
                    try { await login({credential:reply.credential}); }
                    catch (e) { setError(e instanceof Error ? e.message : 'Không đăng nhập được.'); }
                    finally { submitting.current = false; setBusy(false); }
                }));
                target.current.replaceChildren();
                googleId.renderButton(target.current,{theme:'outline',size:'large',text:'signin_with',locale:'vi'});
                clearInterval(timer);
            } else if (elapsed >= 20000) { setError('Không tải được đăng nhập Google. Kiểm tra kết nối và tải lại trang.'); clearInterval(timer); }
        },200);
        return () => clearInterval(timer);
    },[login]);
    return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#edf7f2',p:2}}>
        <Card sx={{p:5,maxWidth:440,width:'100%',borderRadius:4}}>
            <Typography variant="h4" sx={{fontWeight:750}} color="primary">AppHealth</Typography>
            <Typography variant="h6" sx={{mt:1}}>Cổng quản trị</Typography>
            <Typography color="text.secondary" sx={{my:3}}>Đăng nhập bằng tài khoản Google đã được cấp quyền quản trị.</Typography>
            {import.meta.env.DEV && <Alert severity="info" sx={{mb:2}}>
                Địa chỉ web cần đăng ký trong Google Cloud:
                <Box component="code" sx={{display:'block',overflowWrap:'anywhere',my:1}}>{window.location.origin}</Box>
                Thêm địa chỉ này vào Authorized JavaScript origins của Web OAuth client đang dùng.
            </Alert>}
            {error && <Alert severity="error" sx={{mb:2}}>{error}</Alert>}
            {busy ? <CircularProgress/> : null}
            <Box ref={target} aria-busy={busy} sx={{pointerEvents:busy?'none':'auto',opacity:busy?0.6:1}}/>
            <Typography variant="body2" color="text.secondary" sx={{mt:2}}>
                Muốn đổi tài khoản, chọn “Sử dụng tài khoản khác” trong cửa sổ Google.
                Quyền Admin được kiểm tra theo tài khoản bạn chọn.
            </Typography>
        </Card>
    </Box>;
}
