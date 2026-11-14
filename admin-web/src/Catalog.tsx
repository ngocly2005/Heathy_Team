import { Box, Typography } from '@mui/material';
import { BooleanInput, Create, CreateButton, Datagrid, DeleteWithConfirmButton, Edit, EditButton, ExportButton,
    FunctionField, List, ListButton, NullableBooleanInput, NumberField, NumberInput, SaveButton,
    SimpleForm, TextInput, Toolbar, TopToolbar, maxValue, minValue, required, type RaRecord } from 'react-admin';
import { AdminIcon, AdminPage, StatusBadge } from './AdminUi';

type CatalogRow = RaRecord & { name: string; is_active: boolean };
const filters = [
    <TextInput key="q" source="q" label="Tìm kiếm theo tên" placeholder="Nhập tên cần tìm…" alwaysOn resettable />,
    <NullableBooleanInput key="is_active" source="is_active" label="Trạng thái" trueLabel="Đang sử dụng" falseLabel="Ngừng sử dụng" nullLabel="Tất cả trạng thái" alwaysOn />,
];
const nutrientFields = ['calories_per_100g', 'protein_per_100g', 'carb_per_100g', 'fat_per_100g'] as const;
const labels = ['Calo / 100 g', 'Protein / 100 g', 'Carb / 100 g', 'Fat / 100 g'];
const formGrid = { display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' }, gap: 2, width: '100%', maxWidth: 760 };

function CatalogListActions({ createLabel }: { createLabel: string }) {
    return <TopToolbar>
        <ExportButton label="Xuất tối đa 100 dòng" maxResults={100} />
        <CreateButton label={createLabel} variant="contained" icon={<AdminIcon name="plus" size={18} />} />
    </TopToolbar>;
}

function CatalogToolbar() {
    return <Toolbar sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
        <SaveButton label="Lưu thay đổi" icon={<AdminIcon name="save" size={18} />} />
        <DeleteWithConfirmButton mutationMode="pessimistic" label="Ngừng sử dụng"
            confirmTitle="Ngừng sử dụng mục này?" confirmContent="Dữ liệu nhật ký đã ghi vẫn được giữ nguyên." />
    </Toolbar>;
}

function CatalogName({ record, exercise = false }: { record: CatalogRow; exercise?: boolean }) {
    return <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', minWidth: 200 }}>
        <Box sx={{ display: 'grid', placeItems: 'center', flexShrink: 0, width: 42, height: 42, borderRadius: 2.5,
            bgcolor: exercise ? '#ECEBFE' : '#E6F6ED', color: exercise ? '#8070BA' : '#13956B' }}>
            <AdminIcon name={exercise ? 'exercise' : 'food'} size={22} />
        </Box>
        <Typography sx={{ fontSize: 14, fontWeight: 600, color: '#1F2937' }}>{record.name}</Typography>
    </Box>;
}

export function FoodList() {
    return <AdminPage title="Thực phẩm" description="Quản lý danh mục thực phẩm và giá trị dinh dưỡng trên 100 gram." icon="food">
        <List title="Thực phẩm" filters={filters} actions={<CatalogListActions createLabel="Thêm thực phẩm" />} perPage={25}>
            <Datagrid rowClick="edit" bulkActionButtons={false}>
                <FunctionField<CatalogRow> source="name" label="Tên thực phẩm" render={record => <CatalogName record={record} />} />
                {nutrientFields.map((field, i) => <NumberField key={field} source={field} label={labels[i]} />)}
                <FunctionField<CatalogRow> source="is_active" label="Trạng thái" render={record => <StatusBadge active={record.is_active} activeLabel="Đang sử dụng" inactiveLabel="Ngừng sử dụng" />} />
                <FunctionField label="Thao tác" sortable={false} render={() => <EditButton label="Sửa" icon={<AdminIcon name="edit" size={17} />} sx={{ minWidth: 0, bgcolor: '#E9F8F1', borderRadius: 2 }} />} />
            </Datagrid>
        </List>
    </AdminPage>;
}

function FoodFields() {
    return <Box sx={formGrid}>
        <TextInput source="name" label="Tên thực phẩm" validate={[required()]} fullWidth sx={{ gridColumn: '1 / -1' }} />
        {nutrientFields.map((field, i) => <NumberInput key={field} source={field} label={labels[i]}
            validate={[required(), minValue(0), maxValue([20000, 2000, 3000, 2000][i])]} defaultValue={0} fullWidth />)}
        <BooleanInput source="is_active" label="Đang sử dụng" defaultValue={true} sx={{ gridColumn: '1 / -1' }} />
    </Box>;
}

export function FoodCreate() {
    return <AdminPage title="Thêm thực phẩm" description="Thêm tên và giá trị dinh dưỡng của thực phẩm vào danh mục chung." icon="food">
        <Create title="Thêm thực phẩm" actions={<TopToolbar><ListButton label="Về danh sách" /></TopToolbar>}>
            <SimpleForm toolbar={<Toolbar><SaveButton label="Lưu thực phẩm" icon={<AdminIcon name="save" size={18} />} /></Toolbar>}><FoodFields /></SimpleForm>
        </Create>
    </AdminPage>;
}

export function FoodEdit() {
    return <AdminPage title="Sửa thực phẩm" description="Cập nhật thông tin và trạng thái sử dụng của thực phẩm." icon="food">
        <Edit title="Sửa thực phẩm" mutationMode="pessimistic" actions={<TopToolbar><ListButton label="Về danh sách" /></TopToolbar>}>
            <SimpleForm toolbar={<CatalogToolbar />}><FoodFields /></SimpleForm>
        </Edit>
    </AdminPage>;
}

export function ExerciseList() {
    return <AdminPage title="Bài tập" description="Quản lý danh mục vận động và hệ số tiêu thụ năng lượng MET." icon="exercise">
        <List title="Bài tập" filters={filters} actions={<CatalogListActions createLabel="Thêm bài tập" />} perPage={25}>
            <Datagrid rowClick="edit" bulkActionButtons={false}>
                <FunctionField<CatalogRow> source="name" label="Tên bài tập" render={record => <CatalogName record={record} exercise />} />
                <NumberField source="met" label="MET" />
                <FunctionField<CatalogRow> source="is_active" label="Trạng thái" render={record => <StatusBadge active={record.is_active} activeLabel="Đang sử dụng" inactiveLabel="Ngừng sử dụng" />} />
                <FunctionField label="Thao tác" sortable={false} render={() => <EditButton label="Sửa" icon={<AdminIcon name="edit" size={17} />} sx={{ minWidth: 0, bgcolor: '#E9F8F1', borderRadius: 2 }} />} />
            </Datagrid>
        </List>
    </AdminPage>;
}

function ExerciseFields() {
    return <Box sx={formGrid}>
        <TextInput source="name" label="Tên bài tập" validate={[required()]} fullWidth sx={{ gridColumn: '1 / -1' }} />
        <NumberInput source="met" label="Hệ số MET" validate={[required(), minValue(0.01), maxValue(30)]} fullWidth />
        <BooleanInput source="is_active" label="Đang sử dụng" defaultValue={true} sx={{ gridColumn: '1 / -1' }} />
    </Box>;
}

export function ExerciseCreate() {
    return <AdminPage title="Thêm bài tập" description="Bổ sung hoạt động và hệ số MET vào danh mục vận động." icon="exercise">
        <Create title="Thêm bài tập" actions={<TopToolbar><ListButton label="Về danh sách" /></TopToolbar>}>
            <SimpleForm toolbar={<Toolbar><SaveButton label="Lưu bài tập" icon={<AdminIcon name="save" size={18} />} /></Toolbar>}><ExerciseFields /></SimpleForm>
        </Create>
    </AdminPage>;
}

export function ExerciseEdit() {
    return <AdminPage title="Sửa bài tập" description="Cập nhật hoạt động, hệ số MET và trạng thái sử dụng." icon="exercise">
        <Edit title="Sửa bài tập" mutationMode="pessimistic" actions={<TopToolbar><ListButton label="Về danh sách" /></TopToolbar>}>
            <SimpleForm toolbar={<CatalogToolbar />}><ExerciseFields /></SimpleForm>
        </Edit>
    </AdminPage>;
}
