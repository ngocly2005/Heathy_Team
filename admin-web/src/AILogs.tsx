import { Datagrid, DateField, DateInput, FunctionField, List, NumberInput, SelectInput,
    Show, SimpleShowLayout, TextField, useRecordContext } from 'react-admin';
import { Box, Typography } from '@mui/material';

const filters = [<DateInput source="date_from" label="Từ ngày" alwaysOn/>,<DateInput source="date_to" label="Đến ngày" alwaysOn/>,
    <NumberInput source="user_id" label="ID người dùng" min={1}/>,
    <SelectInput source="kind" label="Loại" choices={[{id:'chat',name:'Chat'},{id:'photo',name:'Ảnh'}]}/>];
export function AILogList() {
    return <List title="Lịch sử AI" filters={filters} perPage={25} sort={{field:'id',order:'DESC'}}>
        <Datagrid rowClick="show" bulkActionButtons={false}>
            <TextField source="id"/><TextField source="user_id" label="User ID"/><TextField source="kind" label="Loại"/>
            <TextField source="context_date" label="Ngày dữ liệu"/><DateField source="created_at" label="Thời điểm gọi" showTime/>
            <TextField source="question" label="Câu hỏi"/>
            <FunctionField label="Trạng thái" render={(row:{status:string})=>row.status==='success'?'Thành công':'Lỗi'}/>
            <TextField source="error_code" label="Mã lỗi"/>
        </Datagrid>
    </List>;
}
function Result() {
    const row=useRecordContext();
    if(!row) return null;
    return <Box sx={{width:'100%',maxWidth:960}}>
        <Typography variant="h6">Nội dung phản hồi</Typography>
        <Box component="pre" sx={{whiteSpace:'pre-wrap',overflowWrap:'anywhere',bgcolor:'#eef6f2',p:2,borderRadius:2}}>
            {row.response ? JSON.stringify(row.response,null,2) : 'Không có phản hồi hợp lệ.'}
        </Box>
    </Box>;
}
export function AILogShow() {
    return <Show title="Chi tiết tương tác AI"><SimpleShowLayout>
        <TextField source="user_id" label="User ID"/><TextField source="kind" label="Loại"/>
        <TextField source="context_date" label="Ngày dữ liệu"/><DateField source="created_at" label="Thời điểm gọi" showTime/>
        <TextField source="question" label="Câu hỏi"/><TextField source="status" label="Trạng thái"/><TextField source="error_code" label="Mã lỗi"/>
        <Result/>
    </SimpleShowLayout></Show>;
}
