import { useEffect, useRef, useState } from 'react';
import { useLogin } from 'react-admin';
import { Alert, Box, Button, Card, CircularProgress, Divider, TextField, Typography } from '@mui/material';
import { googleSignInOptions, type GoogleSignInConfiguration } from './googleSignIn.mjs';
import { AdminIcon, FitChatBrand, type IconName } from './AdminUi';

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
    return <Box sx={{minHeight:'100dvh',display:'grid',gridTemplateColumns:{xs:'1fr',md:'1fr 1fr'},alignItems:'center',
        gap:{xs:3,md:8},background:'radial-gradient(ellipse at 0% 0%,#daf5e9,transparent 55%),radial-gradient(ellipse at 100% 100%,#e1f5ec,transparent 45%),#f5f6fb',
        px:{xs:2,sm:5,lg:10},py:{xs:4,md:7}}}>
        <Box sx={{maxWidth:540,justifySelf:{xs:'stretch',md:'center'},width:'100%'}}>
            <FitChatBrand/>
            <Typography sx={{color:'#07825e',fontWeight:700,letterSpacing:2,fontSize:11,mt:{xs:3,md:8},textTransform:'uppercase'}}>Không gian quản trị</Typography>
            <Typography component="h1" sx={{fontSize:{xs:30,md:46},fontWeight:800,lineHeight:1.18,letterSpacing:'-1.6px',mt:2,maxWidth:510}}>
                Cùng xây dựng một cộng đồng <Box component="span" sx={{color:'#009568'}}>sống khỏe.</Box>
            </Typography>
            <Typography sx={{color:'text.secondary',fontSize:16,lineHeight:1.8,mt:3,maxWidth:445,display:{xs:'none',sm:'block'}}}>
                Quản lý dữ liệu dinh dưỡng, theo dõi hoạt động và đồng hành cùng người dùng trên hành trình sức khỏe.
            </Typography>
            <Box sx={{display:{xs:'none',md:'grid'},gridTemplateColumns:'1fr 1fr',gap:2,mt:4,maxWidth:440}}>
                {([['users','Người dùng'],['food','Thực phẩm'],['exercise','Bài tập'],['robot','Lịch sử AI']] as [IconName,string][]).map(([icon,label])=>
                    <Box key={icon} sx={{display:'flex',alignItems:'center',gap:1.5,color:'#466357',fontWeight:600,fontSize:13}}>
                        <Box sx={{display:'grid',placeItems:'center',width:38,height:38,borderRadius:'13px',bgcolor:'#ffffffa8',color:'#00a377'}}><AdminIcon name={icon} size={19}/></Box>{label}
                    </Box>)}
            </Box>
        </Box>
        <Card sx={{p:{xs:3,sm:4.5},maxWidth:510,width:'100%',justifySelf:'center',borderRadius:'30px',boxShadow:'0 15px 60px #1a57470a'}}>
            <Box sx={{display:'flex',alignItems:'center',gap:1,color:'#008b64',mb:2}}><AdminIcon name="shield" size={20}/><Typography sx={{fontSize:12,fontWeight:700}}>FITCHAT ADMIN</Typography></Box>
            <Typography component="h2" variant="h4" sx={{fontWeight:800,fontSize:{xs:27,sm:31}}}>Chào mừng trở lại!</Typography>
            <Typography color="text.secondary" sx={{mt:1.5,mb:3,fontSize:14}}>Đăng nhập bằng tài khoản đã được cấp quyền quản trị.</Typography>
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
                <Button type="submit" variant="contained" sx={{minHeight:48,mt:1}} disabled={busy || !email.trim() || !password}>Đăng nhập</Button>
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
