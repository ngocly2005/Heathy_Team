import { Avatar, Box, Chip, Typography } from '@mui/material';
import { Datagrid, Edit, EditButton, ExportButton, FunctionField, List, ListButton, SaveButton,
    SelectInput, SimpleForm, TextField, TextInput, Toolbar, TopToolbar, useRecordContext, type RaRecord } from 'react-admin';
import { AdminIcon, AdminPage, StatusBadge } from './AdminUi';

type UserRow = RaRecord & { full_name: string; email: string; role: string; status: string };
const statuses = [{ id: 'active', name: 'Đang hoạt động' }, { id: 'disabled', name: 'Đã khóa' }];
const filters = [
    <TextInput key="q" source="q" label="Tìm kiếm theo email hoặc tên" placeholder="Nhập email hoặc tên…" alwaysOn resettable />,
    <SelectInput key="status" source="status" label="Trạng thái" choices={statuses} emptyText="Tất cả trạng thái" alwaysOn />,
];

function initials(name: string, email: string) {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    return (parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : parts[0]?.slice(0, 2) || email.slice(0, 2)).toLocaleUpperCase('vi');
}

function UserName({ record }: { record: UserRow }) {
    return <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 190 }}>
        <Avatar sx={{ width: 44, height: 44, bgcolor: '#D6F4E9', color: '#0A8061', fontSize: 15, fontWeight: 700 }}>
            {initials(record.full_name || '', record.email || '')}
        </Avatar>
        <Typography sx={{ fontSize: 14, fontWeight: 600, color: '#1F2937' }}>{record.full_name || 'Chưa có tên'}</Typography>
    </Box>;
}

function RoleBadge({ role }: { role: string }) {
    return <Chip size="small" label={role === 'admin' ? 'Admin' : role === 'user' ? 'Người dùng' : role}
        sx={{ height: 27, px: 0.5, fontSize: 12, fontWeight: 600,
            bgcolor: role === 'admin' ? '#DDF6EB' : '#F1F3F6', color: role === 'admin' ? '#087B5C' : '#616A7B' }} />;
}

function UserListActions() {
    return <TopToolbar><ExportButton label="Xuất tối đa 100 dòng" maxResults={100} /></TopToolbar>;
}

export function UserList() {
    return <AdminPage title="Người dùng" description="Quản lý danh sách người dùng và trạng thái tài khoản trong hệ thống." icon="users">
        <List title="Người dùng" filters={filters} actions={<UserListActions />} perPage={25} sort={{ field: 'id', order: 'DESC' }}>
            <Datagrid rowClick="edit" bulkActionButtons={false} sx={{ '& .column-id': { width: 70 }, '& .column-actions': { width: 110 } }}>
                <TextField source="id" label="ID" />
                <FunctionField<UserRow> source="full_name" label="Họ tên" render={record => <UserName record={record} />} />
                <TextField source="email" label="Email" sx={{ overflowWrap: 'anywhere' }} />
                <FunctionField<UserRow> source="role" label="Quyền" render={record => <RoleBadge role={record.role} />} />
                <FunctionField<UserRow> source="status" label="Trạng thái" render={record => <StatusBadge active={record.status === 'active'} activeLabel="Đang hoạt động" inactiveLabel="Đã khóa" />} />
                <FunctionField label="Thao tác" sortable={false} render={() => <EditButton label="Sửa" icon={<AdminIcon name="edit" size={17} />} sx={{ minWidth: 0, bgcolor: '#E9F8F1', borderRadius: 2 }} />} />
            </Datagrid>
        </List>
    </AdminPage>;
}

function UserSummary() {
    const record = useRecordContext<UserRow>();
    if (!record) return null;
    return <Box sx={{ width: '100%', mb: 2.5, pb: 2.5, borderBottom: '1px solid #EDF0F4', display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <UserName record={record} />
        <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography sx={{ color: '#677184', fontSize: 14, overflowWrap: 'anywhere' }}>{record.email}</Typography>
            <Typography sx={{ color: '#8891A0', fontSize: 12, mt: 0.5 }}>ID: {record.id}</Typography>
        </Box>
        <RoleBadge role={record.role} />
    </Box>;
}

export function UserEdit() {
    return <AdminPage title="Trạng thái người dùng" description="Xem tài khoản và cập nhật trạng thái hoạt động." icon="users">
        <Edit title="Trạng thái người dùng" mutationMode="pessimistic"
            actions={<TopToolbar><ListButton label="Về danh sách" /></TopToolbar>}>
            <SimpleForm toolbar={<Toolbar><SaveButton label="Lưu thay đổi" icon={<AdminIcon name="save" size={18} />} /></Toolbar>}>
                <UserSummary />
                <SelectInput source="status" label="Trạng thái tài khoản" choices={statuses} required fullWidth sx={{ maxWidth: 520 }} />
            </SimpleForm>
        </Edit>
    </AdminPage>;
}
