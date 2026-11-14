import type { ReactNode } from 'react';
import { Box, Typography } from '@mui/material';
import { resourceStyles } from './theme';

export type IconName = 'dashboard' | 'users' | 'food' | 'exercise' | 'robot' | 'settings' | 'logout' |
    'menu' | 'search' | 'edit' | 'arrow' | 'check' | 'close' | 'leaf' | 'plus' | 'save' | 'refresh' |
    'clock' | 'filter' | 'document' | 'shield' | 'water' | 'chart' | 'calendar';

const paths: Record<IconName, ReactNode> = {
    dashboard: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
    users: <><circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6m2 3a6 6 0 0 1 3 5v2"/></>,
    food: <><path d="M4 3v7m4-7v7M6 3v18m-2-11a2 2 0 0 0 4 0M17 3c-3 3-4 6-4 10h5M18 3v18"/></>,
    exercise: <><path d="m6 6 12 12M3 7l4-4m-3 8 7-7m2 16 7-7m-3 8 4-4"/></>,
    robot: <><rect x="3" y="7" width="18" height="13" rx="4"/><path d="M12 3v4M1 12v4m22-4v4M8 16h8"/><circle cx="8" cy="12" r=".8"/><circle cx="16" cy="12" r=".8"/></>,
    settings: <><path d="m9 3-.5 3-2 1-3-.5-2 4 2.5 2V15l-1 2 3 3 3-1 2 1 1 2h4l1-3 2-1 2 .5 2-4-2-2v-2l1-2-3-3-3 1-2-1-1-2Z"/><circle cx="12" cy="12" r="3"/></>,
    logout: <><path d="M9 4H4v16h5m4-12 4 4-4 4m-6-4h14"/></>,
    menu: <path d="M4 6h16M4 12h16M4 18h16"/>,
    search: <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></>,
    edit: <><path d="m14 4 6 6M4 20l5-1L21 7l-4-4L5 15l-1 5Zm8 0h9"/></>,
    arrow: <path d="m9 5 7 7-7 7"/>,
    check: <path d="m5 12 4 4L20 5"/>,
    close: <path d="m6 6 12 12M6 18 18 6"/>,
    leaf: <><path d="M20 3C10 2 3 6 4 13c1 7 12 7 16-10ZM4 21l10-11"/></>,
    plus: <path d="M12 4v16M4 12h16"/>,
    save: <><path d="M4 3h13l4 4v14H3V3Zm3 0v6h10V3M7 21v-8h10v8"/></>,
    refresh: <><path d="M20 7v5h-5M4 17v-5h5M6 7a7 7 0 0 1 12-2l2 2M4 17l2 2a7 7 0 0 0 12-2"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/></>,
    filter: <path d="M3 4h18l-7 8v7l-4 2v-9L3 4Z"/>,
    document: <><path d="M14 3H5v18h14V8l-5-5v5h5M8 12h8m-8 4h6"/></>,
    shield: <><path d="m12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6l9-4Z"/><path d="m8 12 3 3 5-6"/></>,
    water: <path d="M12 2S4 11 4 16a8 8 0 0 0 16 0c0-5-8-14-8-14Z"/>,
    chart: <><path d="M4 20V10m7 10V4m7 16V7M2 22h20"/></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M7 2v6m10-6v6M3 11h18m-13 4h1m6 0h1m-8 3h1"/></>,
};

export function AdminIcon({ name, size = 22 }: { name: IconName; size?: number }) {
    return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
        strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">{paths[name]}</svg>;
}

export function FitChatBrand({ compact = false }: { compact?: boolean }) {
    return <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', color: '#172336', minWidth: 0 }}>
        <svg width="48" height="52" viewBox="0 0 48 52" aria-hidden="true" focusable="false">
            <path d="M24 48V25M24 34 13 22m11 8 12-15" fill="none" stroke="#00875f" strokeWidth="3" strokeLinecap="round"/>
            <path d="M24 30C6 31 3 16 3 10c14 0 22 7 21 20Z" fill="#33c695"/>
            <path d="M25 27C23 10 35 2 46 2c0 16-9 25-21 25Z" fill="#0ca772"/>
            <circle cx="21" cy="10" r="5" fill="#16ad7b"/>
        </svg>
        {!compact && <Box><Typography sx={{ fontSize: 25, fontWeight: 800, letterSpacing: '-1px', lineHeight: 1.05 }}>FitChat</Typography>
            <Typography sx={{ fontSize: 10, color: '#657081', mt: .5 }}>Healthier · Happier</Typography></Box>}
    </Box>;
}

export function AdminPage({ title, description, icon, actions, children }: {
    title: string; description: string; icon?: IconName; actions?: ReactNode; children: ReactNode;
}) {
    return <Box className="fit-page" sx={{ ...resourceStyles, width: '100%', minWidth: 0, p: { xs: 2, sm: 3, lg: 4 }, display: 'grid', gridTemplateColumns: 'minmax(0, 1fr)', gap: 3 }}>
        <Box component="header" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', minWidth: 0 }}>
                {icon && <Box sx={{ display: { xs: 'none', sm: 'grid' }, placeItems: 'center', width: 64, height: 64, borderRadius: '22px', bgcolor: '#def6ed', color: '#048b65', flexShrink: 0 }}><AdminIcon name={icon} size={30}/></Box>}
                <Box sx={{ minWidth: 0 }}><Typography component="h1" variant="h4" sx={{ fontWeight: 800, letterSpacing: '-1.1px', lineHeight: 1.22 }}>{title}</Typography>
                    <Typography sx={{ color: 'text.secondary', mt: .8, fontSize: { xs: 13, md: 15 } }}>{description}</Typography></Box>
            </Box>
            {actions}
        </Box>
        {children}
    </Box>;
}

export function StatusBadge({ active, activeLabel = 'Đang hoạt động', inactiveLabel = 'Đã khóa' }: {
    active: boolean; activeLabel?: string; inactiveLabel?: string;
}) {
    return <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: .8, px: 1.4, py: .65,
        borderRadius: '30px', fontSize: 12, fontWeight: 600, bgcolor: active ? '#e3f6ed' : '#fff0f1',
        color: active ? '#087d5a' : '#b34254', whiteSpace: 'nowrap' }}>
        <Box component="span" sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: 'currentColor', flexShrink: 0 }}/>
        {active ? activeLabel : inactiveLabel}
    </Box>;
}
