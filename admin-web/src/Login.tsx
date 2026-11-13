import { useEffect, useRef, useState } from 'react';
import { useLogin } from 'react-admin';
import { Alert, Box, Button, Card, CircularProgress, Divider, TextField, Typography } from '@mui/material';
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
    const [googleError,setGoogleError] = useState('');
    const [email,setEmail] = useState('');
    const [password,setPassword] = useState('');
    const [busy,setBusy] = useState(false);
    useEffect(() => {
        const clientId = import.meta.env.VITE_GOOGLE_WEB_CLIENT_ID?.trim();
        if (!clientId) { setGoogleError('Google chưa được cấu hình. Bạn có thể đăng nhập bằng email và mật khẩu đã tạo trên ứng dụng.'); return; }
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
            } else if (elapsed >= 20000) { setGoogleError('Không tải được đăng nhập Google. Bạn có thể dùng email/mật khẩu hoặc tải lại trang.'); clearInterval(timer); }
        },200);
        return () => clearInterval(timer);
    },[login]);
    return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#edf7f2',p:2}}>
        <Card sx={{p:5,maxWidth:440,width:'100%',borderRadius:4}}>
            <Typography variant="h4" sx={{fontWeight:750}} color="primary">AppHealth</Typography>
            <Typography variant="h6" sx={{mt:1}}>Cổng quản trị</Typography>
            <Typography color="text.secondary" sx={{my:3}}>Đăng nhập bằng tài khoản đã được cấp quyền quản trị. Email và Google dùng chung quyền, hồ sơ và dữ liệu.</Typography>
            <Box component="form" onSubmit={async event => {
                event.preventDefault();
                if (submitting.current) return;
                submitting.current = true; setBusy(true); setError('');
                try { await login({email,password}); setPassword(''); }
                catch (e) { setError(e instanceof Error ? e.message : 'Không đăng nhập được.'); }
                finally { submitting.current = false; setBusy(false); }
            }} sx={{display:'grid',gap:2,mb:3}}>
                <TextField label="Email" type="email" autoComplete="username" value={email} onChange={e=>setEmail(e.target.value)} disabled={busy} required/>
                <TextField label="Mật khẩu" type="password" autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} disabled={busy} required/>
                <Button type="submit" variant="contained" disabled={busy || !email.trim() || !password}>Đăng nhập</Button>
            </Box>
            <Divider sx={{mb:2}}>Hoặc dùng Google</Divider>
            {import.meta.env.DEV && <Alert severity="info" sx={{mb:2}}>
                Địa chỉ web cần đăng ký trong Google Cloud:
                <Box component="code" sx={{display:'block',overflowWrap:'anywhere',my:1}}>{window.location.origin}</Box>
                Thêm địa chỉ này vào Authorized JavaScript origins của Web OAuth client đang dùng.
            </Alert>}
            {error && <Alert severity="error" sx={{mb:2}}>{error}</Alert>}
            {googleError && <Alert severity="info" sx={{mb:2}}>{googleError}</Alert>}
            {busy ? <CircularProgress/> : null}
            <Box ref={target} aria-busy={busy} sx={{pointerEvents:busy?'none':'auto',opacity:busy?0.6:1}}/>
            <Typography variant="body2" color="text.secondary" sx={{mt:2}}>
                Muốn đổi tài khoản, chọn “Sử dụng tài khoản khác” trong cửa sổ Google.
                Quyền Admin được kiểm tra theo tài khoản bạn chọn.
            </Typography>
        </Card>
    </Box>;
}
