import { AppBar, Layout, Logout, Menu, UserMenu, useGetIdentity, useSidebarState,
    type AppBarProps, type LayoutProps, type MenuProps } from 'react-admin';
import { Avatar, Box, Divider, Typography } from '@mui/material';
import { AdminIcon, FitChatBrand } from './AdminUi';

function AccountMenu() {
    const { data } = useGetIdentity();
    const initials = (data?.fullName || 'Admin').trim().split(/\s+/).slice(-2).map((part: string) => part[0]).join('').toUpperCase();
    return <UserMenu label="Tài khoản quản trị" icon={<Avatar sx={{ width: 36, height: 36, bgcolor: '#d5f5e8', color: '#00835f', fontSize: 14, fontWeight: 700 }}>{initials}</Avatar>}>
        <Logout icon={<AdminIcon name="logout"/>}/>
    </UserMenu>;
}

function FitChatAppBar(props: AppBarProps) {
    const [open] = useSidebarState();
    return <AppBar {...props} color="inherit" elevation={0} userMenu={<AccountMenu/>} sx={{
        left: { xs: 0, sm: open ? 260 : 72 }, width: { xs: '100%', sm: `calc(100% - ${open ? 260 : 72}px)` },
        borderBottom: '1px solid #edf0f4', bgcolor: '#ffffffed', backdropFilter: 'blur(12px)',
        '& .RaAppBar-toolbar': { height: { xs: 64, sm: 72 }, minHeight: { xs: 64, sm: 72 }, px: { xs: 1, sm: 2.5 } },
        '& .RaUserMenu-userButton': { color: '#263345', fontWeight: 500, gap: 1 },
    }}>
        <Box sx={{ display: { xs: 'block', sm: 'none' }, '& svg': { width: 32, height: 38 }, '& p:first-of-type': { fontSize: 20 } }}><FitChatBrand/></Box>
        <Box sx={{ flex: 1 }}/>
    </AppBar>;
}

function FitChatMenu(props: MenuProps) {
    const [open] = useSidebarState();
    return <Menu {...props} sx={{ mt: 0, px: open ? 2 : 1, py: 0, minHeight: '100dvh', bgcolor: '#fff', width: open ? 260 : 72,
        '& .RaMenuItemLink-root': { borderRadius: '18px', minHeight: 54, mb: .8, px: open ? 2 : 1.5, fontSize: 15, color: '#70798b',
            '& .MuiListItemIcon-root': { minWidth: 38, color: 'inherit' },
            '&.RaMenuItemLink-active': { bgcolor: '#e1f5ed', color: '#00855f', fontWeight: 700,
                '&::before': { content: '""', position: 'absolute', left: 0, top: 15, bottom: 15, width: 3, bgcolor: '#00a679', borderRadius: '0 4px 4px 0' } },
            '&:hover': { bgcolor: '#f0faf6', color: '#00855f' } },
    }}>
        <Box component="li" role="presentation" sx={{ minHeight: 118, display: 'flex', alignItems: 'center', px: open ? 1.5 : .5, mb: 1 }}><FitChatBrand compact={!open}/></Box>
        <Menu.Item to="/" primaryText="Dashboard" leftIcon={<AdminIcon name="dashboard"/>}/>
        <Menu.Item to="/users" primaryText="Người dùng" leftIcon={<AdminIcon name="users"/>}/>
        <Menu.Item to="/foods" primaryText="Thực phẩm" leftIcon={<AdminIcon name="food"/>}/>
        <Menu.Item to="/activities" primaryText="Bài tập" leftIcon={<AdminIcon name="exercise"/>}/>
        <Menu.Item to="/ai-logs" primaryText="Lịch sử AI" leftIcon={<AdminIcon name="robot"/>}/>
        <Divider component="li" sx={{ my: 2 }}/>
        <Logout icon={<AdminIcon name="logout"/>} sx={{ minHeight: 50, borderRadius: '16px', color: '#6d7789', px: open ? 2 : 1.5 }}/>
        {open && <Box component="li" role="presentation" sx={{ position: 'relative', overflow: 'hidden', borderRadius: '24px', background: 'linear-gradient(130deg,#effbf6,#dbf4e8)', mt: 5, mb: 3, p: 2.5, minHeight: 160 }}>
            <Typography sx={{ color: '#087854', fontWeight: 750, fontSize: 16 }}>Sống khỏe</Typography>
            <Typography sx={{ fontWeight: 600, fontSize: 14, maxWidth: 120, mt: .5 }}>cùng FitChat!</Typography>
            <Box sx={{ mt: 2, color: '#22ac83' }}><AdminIcon name="leaf" size={26}/></Box>
            <Box sx={{ position: 'absolute', right: -17, bottom: -15, transform: 'rotate(-15deg)', opacity: .25, color: '#1eae76' }}><AdminIcon name="leaf" size={125}/></Box>
        </Box>}
    </Menu>;
}

export function FitChatLayout(props: LayoutProps) {
    return <Layout {...props} appBar={FitChatAppBar} menu={FitChatMenu} appBarAlwaysOn sx={{
        minWidth: 0,
        '& .RaLayout-appFrame': { mt: { xs: '64px', sm: '72px' }, minWidth: 0 },
        '& .RaLayout-contentWithSidebar': { minWidth: 0 },
        '& .RaLayout-content': { p: 0, minWidth: 0, position: 'relative', background: 'radial-gradient(ellipse at 95% -8%,#dff5ecaa 0%,transparent 38%),radial-gradient(ellipse at 0% 105%,#e4f6ee77 0%,transparent 35%),#f5f6fb' },
        '& .RaSidebar-docked': { height: '100vh' },
        '& .RaSidebar-fixed': { top: 0, height: '100dvh', borderRight: '1px solid #edf0f4', bgcolor: '#fff', zIndex: 1200 },
        '& .RaSidebar-paper': { border: 0 },
    }}/>;
}
