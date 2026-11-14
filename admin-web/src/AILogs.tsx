import { Box, Chip, Typography } from '@mui/material';
import { Datagrid, DateField, DateInput, ExportButton, FilterButton, FunctionField, List, ListButton, NumberInput,
    SelectInput, Show, ShowButton, SimpleShowLayout, TextField, TopToolbar, useRecordContext, type RaRecord } from 'react-admin';
import { AdminIcon, AdminPage, StatusBadge } from './AdminUi';

type AILogRow = RaRecord & { user_id: number; kind: string; status: string; question?: string; response?: unknown };
const filters = [
    <DateInput key="date_from" source="date_from" label="Từ ngày" alwaysOn />,
    <DateInput key="date_to" source="date_to" label="Đến ngày" alwaysOn />,
    <NumberInput key="user_id" source="user_id" label="ID người dùng" min={1} />,
    <SelectInput key="kind" source="kind" label="Loại" choices={[{ id: 'chat', name: 'Chat' }, { id: 'photo', name: 'Ảnh' }]} />,
];

function KindBadge({ kind }: { kind: string }) {
    return <Chip size="small" label={kind === 'chat' ? 'Chat' : kind === 'photo' ? 'Ảnh' : kind}
        sx={{ height: 27, fontWeight: 600, fontSize: 12, bgcolor: kind === 'chat' ? '#EEEAFB' : '#E7F3FD',
            color: kind === 'chat' ? '#7C62B6' : '#3279B5' }} />;
}

function AILogListActions() {
    return <TopToolbar><FilterButton /><ExportButton label="Xuất tối đa 100 dòng" maxResults={100} /></TopToolbar>;
}

export function AILogList() {
    return <AdminPage title="Lịch sử AI" description="Theo dõi các yêu cầu tư vấn, phân tích ảnh và kết quả xử lý." icon="robot">
        <List title="Lịch sử AI" filters={filters} actions={<AILogListActions />} perPage={25} sort={{ field: 'id', order: 'DESC' }}>
            <Datagrid rowClick="show" bulkActionButtons={false}>
                <TextField source="id" label="ID" />
                <TextField source="user_id" label="User ID" />
                <FunctionField<AILogRow> source="kind" label="Loại" render={record => <KindBadge kind={record.kind} />} />
                <TextField source="context_date" label="Ngày dữ liệu" />
                <DateField source="created_at" label="Thời điểm gọi" showTime />
                <FunctionField<AILogRow> source="question" label="Câu hỏi" render={record => <Typography title={record.question || ''}
                    sx={{ fontSize: 14, width: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{record.question || '—'}</Typography>} />
                <FunctionField<AILogRow> source="status" label="Trạng thái" render={record => <StatusBadge active={record.status === 'success'} activeLabel="Thành công" inactiveLabel="Lỗi" />} />
                <TextField source="error_code" label="Mã lỗi" emptyText="—" />
                <FunctionField label="Thao tác" sortable={false} render={() => <ShowButton label="Chi tiết" icon={<AdminIcon name="arrow" size={17} />} sx={{ minWidth: 0, bgcolor: '#E9F8F1', borderRadius: 2 }} />} />
            </Datagrid>
        </List>
    </AdminPage>;
}

function Result() {
    const row = useRecordContext<AILogRow>();
    if (!row) return null;
    return <Box sx={{ width: '100%', maxWidth: 1100, mt: 2 }}>
        <Typography component="h2" sx={{ fontSize: 18, fontWeight: 700, color: '#243043', mb: 1.5 }}>Nội dung phản hồi</Typography>
        <Box component="pre" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', bgcolor: '#F4F8F6', color: '#33443F',
            p: { xs: 2, md: 3 }, border: '1px solid #E2ECE6', borderRadius: 3, fontSize: 13, lineHeight: 1.8, maxHeight: 640, overflow: 'auto', m: 0 }}>
            {row.response ? JSON.stringify(row.response, null, 2) : 'Không có phản hồi hợp lệ.'}
        </Box>
    </Box>;
}

export function AILogShow() {
    return <AdminPage title="Chi tiết tương tác AI" description="Xem nội dung yêu cầu, phản hồi đầy đủ và thông tin xử lý." icon="robot">
        <Show title="Chi tiết tương tác AI" actions={<TopToolbar><ListButton label="Về lịch sử AI" /></TopToolbar>}>
            <SimpleShowLayout spacing={2.5}>
                <TextField source="id" label="ID tương tác" />
                <TextField source="user_id" label="User ID" />
                <FunctionField<AILogRow> source="kind" label="Loại" render={record => <KindBadge kind={record.kind} />} />
                <TextField source="context_date" label="Ngày dữ liệu" />
                <DateField source="created_at" label="Thời điểm gọi" showTime />
                <TextField source="question" label="Câu hỏi" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', maxWidth: 1100 }} />
                <FunctionField<AILogRow> source="status" label="Trạng thái" render={record => <StatusBadge active={record.status === 'success'} activeLabel="Thành công" inactiveLabel="Lỗi" />} />
                <TextField source="error_code" label="Mã lỗi" emptyText="—" />
                <Result />
            </SimpleShowLayout>
        </Show>
    </AdminPage>;
}
