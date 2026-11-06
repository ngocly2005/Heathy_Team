import { useEffect, useRef, useState } from 'react';
import { useLogin } from 'react-admin';
import { Alert, Box, Card, CircularProgress, Typography } from '@mui/material';

declare global {
    interface Window {
        google?: { accounts: { id: {
            initialize(options: { client_id: string; callback: (reply: { credential: string }) => void; auto_select: boolean }): void;
            renderButton(element: HTMLElement, options: { theme: string; size: string; text: string; locale: string }): void;
        } } };
    }
}
export function GoogleLogin() {
    const login = useLogin();
    const target = useRef<HTMLDivElement>(null);
    const [error,setError] = useState('');
    const [busy,setBusy] = useState(false);
    useEffect(() => {
        const clientId = import.meta.env.VITE_GOOGLE_WEB_CLIENT_ID;
        if (!clientId) { setError('Đăng nhập Google chưa được cấu hình. Liên hệ người quản trị hệ thống.'); return; }
        let initialized = false;
        let elapsed = 0;
        const timer = setInterval(() => {
            elapsed += 200;
            if (window.google && target.current && !initialized) {
                initialized = true;
                window.google.accounts.id.initialize({ client_id:clientId, auto_select:false, callback: async reply => {
                    setBusy(true); setError('');
                    try { await login({credential:reply.credential}); }
                    catch (e) { setError(e instanceof Error ? e.message : 'Không đăng nhập được.'); }
                    finally { setBusy(false); }
                } });
                window.google.accounts.id.renderButton(target.current,{theme:'outline',size:'large',text:'continue_with',locale:'vi'});
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
            {error && <Alert severity="error" sx={{mb:2}}>{error}</Alert>}
            {busy ? <CircularProgress/> : null}<div ref={target}/>
        </Card>
    </Box>;
}
