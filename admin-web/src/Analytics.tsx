import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, InputAdornment, LinearProgress, TextField, Typography } from '@mui/material';
import { useLogout } from 'react-admin';
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from './api';
import { AdminIcon, AdminPage } from './AdminUi';

function localISO(value: Date) {
    return `${value.getFullYear()}-${String(value.getMonth()+1).padStart(2,'0')}-${String(value.getDate()).padStart(2,'0')}`;
}
function rangeForDays(end: string, days: number) {
    const [year, month, day] = end.split('-').map(Number);
    const beginning = new Date(year, month - 1, day);
    beginning.setDate(beginning.getDate() - days + 1);
    return { from: localISO(beginning), to: end };
}
function shortDate(value: string) {
    const [, month, day] = value.split('-');
    return `${day}/${month}`;
}
function fullDate(value: string) {
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
}
const initialRange = rangeForDays(localISO(new Date()), 30);
type Day = {date:string; new_users:number; food_logs:number; activity_logs:number; water_ml:number; ai_requests:number};
type Report = {totals:Record<string,number>; daily:Day[]};
const series = [
    {key:'food_logs', label:'Món ăn', color:'#12a977'},
    {key:'activity_logs', label:'Vận động', color:'#4498e0'},
    {key:'ai_requests', label:'Yêu cầu AI', color:'#a165d5'},
] as const;
const metrics = [
    {key:'total_users', label:'Tổng người dùng', caption:'Tất cả tài khoản trong hệ thống', icon:'users', color:'#059a72', tint:'#dff7ef'},
    {key:'active_accounts', label:'Tài khoản đang mở', caption:'Đang có trạng thái hoạt động', icon:'shield', color:'#08a879', tint:'#e1f7ec'},
    {key:'active_users', label:'Người dùng có hoạt động trong kỳ', caption:'Có ghi nhận dữ liệu trong kỳ', icon:'robot', color:'#9e65d0', tint:'#f1e9fb'},
    {key:'food_logs', label:'Lượt ghi món ăn', caption:'Số lượt ghi món trong kỳ', icon:'food', color:'#e9a329', tint:'#fff4dc'},
    {key:'ai_requests', label:'Yêu cầu AI', caption:'Chat và phân tích ảnh trong kỳ', icon:'robot', color:'#388dda', tint:'#e6f1ff'},
] as const;
const chartCard = {p:{xs:2,md:3},borderRadius:'24px',border:'1px solid #edf0f5',boxShadow:'0 5px 18px rgba(31,48,72,.035)',overflow:'hidden'};
const fieldStyle = {
    width:{xs:'100%',sm:220},
    '& .MuiOutlinedInput-root':{borderRadius:'999px',backgroundColor:'#fff',pr:2},
    '& .MuiOutlinedInput-notchedOutline':{borderColor:'#edf1f4'},
    '& .MuiInputLabel-root':{color:'#697386'},
};

function DailyDataTable({days}: {days: Day[]}) {
    if (!days.length) return null;
    return <Box component="details" sx={{mt:2,color:'#667085',fontSize:13,
        '& summary':{cursor:'pointer',width:'fit-content',borderRadius:1,py:.75},
        '& summary:focus-visible':{outline:'2px solid #07956c',outlineOffset:3}}}>
        <summary>Xem bảng số liệu theo ngày</summary>
        <Box sx={{overflowX:'auto',mt:1,border:'1px solid #e9edf2',borderRadius:2,
            '& table':{borderCollapse:'collapse',width:'100%',minWidth:660},
            '& th, & td':{textAlign:'right',px:1.5,py:1.25,borderBottom:'1px solid #edf0f4',whiteSpace:'nowrap'},
            '& th:first-of-type, & td:first-of-type':{textAlign:'left'},
            '& th':{background:'#f7faf9',color:'#344054',fontWeight:600},
            '& caption':{textAlign:'left',px:1.5,py:1.25,color:'#475467'}}}>
            <table>
                <caption>Số liệu thực tế từ báo cáo đã tải</caption>
                <thead><tr><th scope="col">Ngày</th><th scope="col">Món ăn</th><th scope="col">Vận động</th><th scope="col">Yêu cầu AI</th><th scope="col">Tài khoản mới</th><th scope="col">Nước (ml)</th></tr></thead>
                <tbody>{days.map(day => <tr key={day.date}>
                    <th scope="row">{fullDate(day.date)}</th>
                    <td>{day.food_logs.toLocaleString('vi-VN')}</td><td>{day.activity_logs.toLocaleString('vi-VN')}</td>
                    <td>{day.ai_requests.toLocaleString('vi-VN')}</td><td>{day.new_users.toLocaleString('vi-VN')}</td>
                    <td>{day.water_ml.toLocaleString('vi-VN')}</td>
                </tr>)}</tbody>
            </table>
        </Box>
    </Box>;
}

export function Analytics() {
    const [from,setFrom] = useState(initialRange.from);
    const [to,setTo] = useState(initialRange.to);
    const [range,setRange] = useState(initialRange);
    const [report,setReport] = useState<Report | null>(null);
    const [error,setError] = useState('');
    const [loading,setLoading] = useState(false);
    const logout = useLogout();
    useEffect(() => {
        let active = true;
        setLoading(true); setError(''); setReport(null);
        api(`/admin/analytics?date_from=${range.from}&date_to=${range.to}`)
            .then(data => { if(active) setReport(data); })
            .catch(e => { if(active) { setError(e.message || 'Không tải được báo cáo.'); if([401,403].includes(e.status)) void logout(); } })
            .finally(() => { if(active) setLoading(false); });
        return () => { active = false; };
    },[range,logout]);
    const choosePreset = (days: number) => {
        const next = rangeForDays(range.to, days);
        setFrom(next.from); setTo(next.to); setRange(next);
    };
    const activePreset = [7,30,90].find(days => rangeForDays(range.to, days).from === range.from);
    const daily = report?.daily || [];
    const noActivity = report && daily.length > 0 && daily.every(day => day.food_logs === 0 && day.activity_logs === 0 && day.ai_requests === 0);
    const noAccountsOrWater = report && daily.length > 0 && daily.every(day => day.new_users === 0 && day.water_ml === 0);
    const dateCaption = `${fullDate(range.from)} – ${fullDate(range.to)}`;
    const emptyChart = <Box sx={{height:280,display:'grid',placeItems:'center',textAlign:'center',color:'#697386',px:2}}>
        <Typography variant="body2">{loading ? 'Đang tải số liệu báo cáo…' : error ? 'Chưa có báo cáo để hiển thị. Hãy thử tải lại.' : 'Chưa có số liệu trong khoảng ngày đã chọn.'}</Typography>
    </Box>;
    return <AdminPage title="Tổng quan" description="Theo dõi hoạt động và dữ liệu sức khỏe trên hệ thống." icon="dashboard">
        <Box component="form" onSubmit={e => {e.preventDefault();setRange({from,to});}}
            sx={{display:'flex',alignItems:'center',gap:1.75,flexWrap:'wrap'}}>
            <TextField type="date" label="Từ ngày" value={from} onChange={e=>setFrom(e.target.value)} size="small"
                slotProps={{inputLabel:{shrink:true},input:{startAdornment:<InputAdornment position="start"><AdminIcon name="calendar" size={21}/></InputAdornment>}}} required sx={fieldStyle}/>
            <TextField type="date" label="Đến ngày" value={to} onChange={e=>setTo(e.target.value)} size="small"
                slotProps={{inputLabel:{shrink:true},input:{startAdornment:<InputAdornment position="start"><AdminIcon name="calendar" size={21}/></InputAdornment>}}} required sx={fieldStyle}/>
            <Button type="submit" variant="contained" disabled={loading || !from || !to || from>to} startIcon={<AdminIcon name="chart" size={20}/>}
                sx={{borderRadius:'999px',px:3,py:1.45,fontWeight:700,boxShadow:'0 4px 12px #08a97920',minWidth:180,width:{xs:'100%',sm:'auto'}}}>Xem báo cáo</Button>
        </Box>
        {loading && <LinearProgress aria-label="Đang tải báo cáo" sx={{mb:2,borderRadius:2}}/>}
        {error && <Alert severity="error" sx={{mb:2,borderRadius:3}}>{error}</Alert>}
        <Box aria-busy={loading} sx={{display:'grid',gridTemplateColumns:{xs:'1fr',sm:'repeat(2,minmax(0,1fr))',lg:'repeat(5,minmax(0,1fr))'},gap:1.5}}>
            {metrics.map(metric => <Card key={metric.key} sx={{p:2.25,minHeight:155,borderRadius:'22px',border:'1px solid #eef1f5',boxShadow:'0 4px 15px rgba(31,48,72,.035)',position:'relative',overflow:'hidden'}}>
                <Box sx={{display:'flex',alignItems:'flex-start',gap:1.5,position:'relative',zIndex:1}}>
                    <Box sx={{display:'grid',placeItems:'center',width:48,height:48,flexShrink:0,borderRadius:'50%',color:metric.color,background:metric.tint}}>
                        <AdminIcon name={metric.icon} size={27}/>
                    </Box>
                    <Box sx={{minWidth:0}}>
                        <Typography sx={{fontSize:13,lineHeight:1.5,color:'#293142',minHeight:39}}>{metric.label}</Typography>
                        <Typography sx={{fontSize:30,lineHeight:1.25,fontWeight:700,color:'#152238',mt:.25,fontVariantNumeric:'tabular-nums',overflowWrap:'anywhere'}}>
                            {report?.totals[metric.key]?.toLocaleString('vi-VN') ?? '—'}
                        </Typography>
                    </Box>
                </Box>
                <Typography sx={{fontSize:11,lineHeight:1.6,color:'#6e7787',mt:1.5,position:'relative',zIndex:1}}>{metric.caption}</Typography>
                <Box aria-hidden sx={{position:'absolute',right:-12,bottom:-20,width:86,height:72,borderRadius:'45% 55% 0 0',background:`linear-gradient(135deg,transparent,${metric.tint})`,transform:'rotate(-20deg)'}}/>
            </Card>)}
        </Box>
        <Card sx={chartCard}>
            <Box sx={{display:'flex',alignItems:{xs:'flex-start',md:'center'},justifyContent:'space-between',gap:2,flexWrap:'wrap'}}>
                <Box sx={{display:'flex',alignItems:'center',gap:1.25}}>
                    <Box sx={{color:'#08a779',display:'flex'}}><AdminIcon name="chart" size={25}/></Box>
                    <Typography component="h2" sx={{fontWeight:700,fontSize:{xs:16,md:18},color:'#1d2939'}}>Hoạt động theo ngày dữ liệu</Typography>
                </Box>
                <Box sx={{display:'flex',gap:2,alignItems:'center',flexWrap:'wrap'}}>
                    <Box sx={{display:'flex',gap:1.75,flexWrap:'wrap'}}>{series.map(item => <Box key={item.key} sx={{display:'flex',alignItems:'center',gap:.65}}>
                        <Box component="span" aria-hidden sx={{width:9,height:9,borderRadius:'50%',background:item.color}}/>
                        <Typography sx={{fontSize:12,color:'#4b5565'}}>{item.label}</Typography>
                    </Box>)}</Box>
                    <Box role="group" aria-label="Chọn số ngày báo cáo, kết thúc tại ngày đến đã áp dụng" sx={{display:'flex',gap:.25,border:'1px solid #edf0f4',borderRadius:'999px',p:.35}}>
                        {[7,30,90].map(days => <Button key={days} size="small" onClick={()=>choosePreset(days)} disabled={loading} aria-pressed={activePreset === days}
                            sx={{borderRadius:'999px',px:1.5,minWidth:65,fontSize:12,color:activePreset === days ? '#fff' : '#536071',background:activePreset === days ? '#08aa79' : 'transparent',
                                '&:hover':{background:activePreset === days ? '#078b65' : '#edf8f3'}}}>{days} ngày</Button>)}
                    </Box>
                </Box>
            </Box>
            <Typography sx={{fontSize:12,color:'#7a8494',mt:1}}>{dateCaption} · Số lượt ghi nhận mỗi ngày</Typography>
            {noActivity && <Typography role="status" sx={{fontSize:13,color:'#697386',mt:2}}>Chưa ghi nhận món ăn, vận động hoặc yêu cầu AI trong kỳ này.</Typography>}
            {report && daily.length > 0 ? <Box sx={{height:{xs:270,md:310},width:'100%',minWidth:0,mt:2}}>
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart accessibilityLayer data={daily} margin={{top:12,right:16,left:-18,bottom:4}} aria-label={`Hoạt động theo ngày, ${dateCaption}. Dùng các phím mũi tên để đọc số liệu.`}>
                        <CartesianGrid strokeDasharray="0" stroke="#edf0f4" vertical={false}/>
                        <XAxis dataKey="date" tickFormatter={shortDate} minTickGap={30} tickLine={false} axisLine={{stroke:'#dfe5eb'}} tick={{fontSize:11,fill:'#748092'}} dy={8}/>
                        <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{fontSize:11,fill:'#748092'}}/>
                        <Tooltip labelFormatter={label=>fullDate(String(label))} contentStyle={{border:'1px solid #e9edf3',borderRadius:14,boxShadow:'0 6px 24px #26385214',fontSize:12}}/>
                        {series.map(item => <Line key={item.key} type="monotone" dataKey={item.key} name={item.label} stroke={item.color} strokeWidth={2.5}
                            dot={daily.length <= 31 ? {r:3,strokeWidth:0,fill:item.color} : false} activeDot={{r:5}} isAnimationActive={false}/>)}
                    </LineChart>
                </ResponsiveContainer>
            </Box> : emptyChart}
            <DailyDataTable days={daily}/>
        </Card>
        <Card sx={chartCard}>
            <Box sx={{display:'flex',alignItems:'center',gap:1.25}}>
                <Box sx={{color:'#08a779',display:'flex'}}><AdminIcon name="users" size={25}/></Box>
                <Typography component="h2" sx={{fontWeight:700,fontSize:{xs:16,md:18},color:'#1d2939'}}>Tài khoản mới và lượng nước ghi nhận</Typography>
            </Box>
            <Typography sx={{fontSize:12,color:'#7a8494',mt:1}}>{dateCaption} · Hai trục tương ứng với số tài khoản và lượng nước (ml)</Typography>
            {noAccountsOrWater && <Typography role="status" sx={{fontSize:13,color:'#697386',mt:2}}>Chưa có tài khoản mới hoặc lượng nước được ghi nhận trong kỳ này.</Typography>}
            {report && daily.length > 0 ? <Box sx={{height:{xs:270,md:280},width:'100%',minWidth:0,mt:2}}>
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart accessibilityLayer data={daily} margin={{top:12,right:0,left:-18,bottom:4}} aria-label={`Tài khoản mới và lượng nước theo ngày, ${dateCaption}. Bảng số liệu ở biểu đồ phía trên.`}>
                        <CartesianGrid stroke="#edf0f4" vertical={false}/>
                        <XAxis dataKey="date" tickFormatter={shortDate} minTickGap={30} tickLine={false} axisLine={{stroke:'#dfe5eb'}} tick={{fontSize:11,fill:'#748092'}} dy={8}/>
                        <YAxis yAxisId="users" allowDecimals={false} tickLine={false} axisLine={false} tick={{fontSize:11,fill:'#159977'}}/>
                        <YAxis yAxisId="water" orientation="right" tickLine={false} axisLine={false} tick={{fontSize:11,fill:'#398dcc'}}/>
                        <Tooltip labelFormatter={label=>fullDate(String(label))} contentStyle={{border:'1px solid #e9edf3',borderRadius:14,fontSize:12}}/>
                        <Legend iconType="circle" iconSize={8} wrapperStyle={{fontSize:12,paddingTop:15}}/>
                        <Line yAxisId="users" type="monotone" dataKey="new_users" name="Tài khoản mới (trục trái)" stroke="#12a977" strokeWidth={2.5} dot={false} activeDot={{r:5}} isAnimationActive={false}/>
                        <Line yAxisId="water" type="monotone" dataKey="water_ml" name="Nước, ml (trục phải)" stroke="#4498e0" strokeWidth={2.5} dot={false} activeDot={{r:5}} isAnimationActive={false}/>
                    </LineChart>
                </ResponsiveContainer>
            </Box> : emptyChart}
        </Card>
    </AdminPage>;
}
