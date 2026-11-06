import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, LinearProgress, TextField, Typography } from '@mui/material';
import { useLogout } from 'react-admin';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from './api';

function localISO(value: Date) {
    return `${value.getFullYear()}-${String(value.getMonth()+1).padStart(2,'0')}-${String(value.getDate()).padStart(2,'0')}`;
}
const today = new Date();
const start = new Date(today); start.setDate(start.getDate()-29);
type Day = {date:string; new_users:number; food_logs:number; activity_logs:number; water_ml:number; ai_requests:number};
type Report = {totals:Record<string,number>; daily:Day[]};

export function Analytics() {
    const [from,setFrom] = useState(localISO(start));
    const [to,setTo] = useState(localISO(today));
    const [range,setRange] = useState({from,to});
    const [report,setReport] = useState<Report | null>(null);
    const [error,setError] = useState('');
    const [loading,setLoading] = useState(false);
    const logout = useLogout();
    useEffect(() => {
        let active = true;
        setLoading(true); setError('');
        api(`/admin/analytics?date_from=${range.from}&date_to=${range.to}`)
            .then(data => { if(active) setReport(data); })
            .catch(e => { if(active) { setError(e.message || 'Không tải được báo cáo.'); if([401,403].includes(e.status)) void logout(); } })
            .finally(() => { if(active) setLoading(false); });
        return () => { active = false; };
    },[range,logout]);
    return <Box sx={{p:{xs:1,md:3},display:'grid',gap:3}}>
        <Typography variant="h4" sx={{fontWeight:700}}>Tổng quan AppHealth</Typography>
        <Box component="form" onSubmit={e => {e.preventDefault();setRange({from,to});}} sx={{display:'flex',gap:2,flexWrap:'wrap'}}>
            <TextField type="date" label="Từ ngày" value={from} onChange={e=>setFrom(e.target.value)} slotProps={{inputLabel:{shrink:true}}} required/>
            <TextField type="date" label="Đến ngày" value={to} onChange={e=>setTo(e.target.value)} slotProps={{inputLabel:{shrink:true}}} required/>
            <Button type="submit" variant="contained" disabled={loading || !from || !to || from>to}>Xem báo cáo</Button>
        </Box>
        {loading && <LinearProgress/>}{error && <Alert severity="error">{error}</Alert>}
        <Box sx={{display:'grid',gridTemplateColumns:'repeat(auto-fit,minmax(170px,1fr))',gap:2}}>
            {[['total_users','Tổng người dùng'],['active_accounts','Tài khoản đang mở'],['active_users','Người dùng có hoạt động trong kỳ'],['food_logs','Lượt ghi món ăn'],['ai_requests','Yêu cầu AI']].map(([key,label]) =>
                <Card key={key} sx={{p:3}}><Typography color="text.secondary">{label}</Typography><Typography variant="h4">{report?.totals[key]?.toLocaleString('vi-VN') ?? '—'}</Typography></Card>)}
        </Box>
        <Card sx={{p:3}}>
            <Typography variant="h6">Hoạt động theo ngày dữ liệu</Typography>
            <Box sx={{height:320,width:'100%',mt:2}}><ResponsiveContainer width="100%" height="100%">
                <LineChart data={report?.daily || []}><CartesianGrid strokeDasharray="3 3"/><XAxis dataKey="date"/><YAxis allowDecimals={false}/><Tooltip/><Legend/>
                    <Line type="monotone" dataKey="food_logs" name="Món ăn" stroke="#119b72"/>
                    <Line type="monotone" dataKey="activity_logs" name="Vận động" stroke="#4895ef"/>
                    <Line type="monotone" dataKey="ai_requests" name="Yêu cầu AI" stroke="#e9a329"/>
                </LineChart>
            </ResponsiveContainer></Box>
        </Card>
        <Card sx={{p:3}}><Typography variant="h6">Tài khoản mới và lượng nước ghi nhận</Typography>
            <Box sx={{height:280,width:'100%',mt:2}}><ResponsiveContainer width="100%" height="100%">
                <LineChart data={report?.daily || []}><CartesianGrid strokeDasharray="3 3"/><XAxis dataKey="date"/>
                    <YAxis yAxisId="users" allowDecimals={false}/><YAxis yAxisId="water" orientation="right"/><Tooltip/><Legend/>
                    <Line yAxisId="users" dataKey="new_users" name="Tài khoản mới" stroke="#119b72"/>
                    <Line yAxisId="water" dataKey="water_ml" name="Nước (ml)" stroke="#4895ef"/>
                </LineChart>
            </ResponsiveContainer></Box>
        </Card>
    </Box>;
}
