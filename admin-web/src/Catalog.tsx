import { BooleanField, BooleanInput, Create, Datagrid, DeleteWithConfirmButton, Edit, List, NumberField,
    NumberInput, SaveButton, SimpleForm, TextField, TextInput, Toolbar, maxValue, minValue, required } from 'react-admin';
import { Box } from '@mui/material';

const filters = [<TextInput source="q" label="Tìm theo tên" alwaysOn/>, <BooleanInput source="is_active" label="Đang sử dụng"/>];
const nutrientFields = ['calories_per_100g','protein_per_100g','carb_per_100g','fat_per_100g'] as const;
const labels = ['Calo / 100 g','Protein / 100 g','Carb / 100 g','Fat / 100 g'];
function CatalogToolbar() {
    return <Toolbar sx={{display:'flex',justifyContent:'space-between'}}>
        <SaveButton label="Lưu"/><DeleteWithConfirmButton mutationMode="pessimistic" label="Ngừng sử dụng"
            confirmTitle="Ngừng sử dụng mục này?" confirmContent="Dữ liệu nhật ký đã ghi vẫn được giữ nguyên."/>
    </Toolbar>;
}
export function FoodList() {
    return <List title="Thực phẩm" filters={filters} perPage={25}>
        <Datagrid rowClick="edit" bulkActionButtons={false}><TextField source="name" label="Tên thực phẩm"/>
            {nutrientFields.map((field,i) => <NumberField key={field} source={field} label={labels[i]}/>)}
            <BooleanField source="is_active" label="Đang sử dụng"/>
        </Datagrid>
    </List>;
}
function FoodFields() {
    return <Box sx={{display:'grid',gap:1,width:'min(100%, 620px)'}}>
        <TextInput source="name" label="Tên thực phẩm" validate={[required()]} fullWidth/>
        {nutrientFields.map((field,i) => <NumberInput key={field} source={field} label={labels[i]}
            validate={[required(),minValue(0),maxValue([20000,2000,3000,2000][i])]} defaultValue={0} fullWidth/>)}
        <BooleanInput source="is_active" label="Đang sử dụng" defaultValue={true}/>
    </Box>;
}
export function FoodCreate() { return <Create title="Thêm thực phẩm"><SimpleForm><FoodFields/></SimpleForm></Create>; }
export function FoodEdit() { return <Edit title="Sửa thực phẩm" mutationMode="pessimistic"><SimpleForm toolbar={<CatalogToolbar/>}><FoodFields/></SimpleForm></Edit>; }

export function ExerciseList() {
    return <List title="Bài tập" filters={filters} perPage={25}><Datagrid rowClick="edit" bulkActionButtons={false}>
        <TextField source="name" label="Tên bài tập"/><NumberField source="met" label="MET"/><BooleanField source="is_active" label="Đang sử dụng"/>
    </Datagrid></List>;
}
function ExerciseFields() {
    return <><TextInput source="name" label="Tên bài tập" validate={[required()]} fullWidth/>
        <NumberInput source="met" label="MET" validate={[required(),minValue(0.01),maxValue(30)]}/>
        <BooleanInput source="is_active" label="Đang sử dụng" defaultValue={true}/></>;
}
export function ExerciseCreate() { return <Create title="Thêm bài tập"><SimpleForm><ExerciseFields/></SimpleForm></Create>; }
export function ExerciseEdit() { return <Edit title="Sửa bài tập" mutationMode="pessimistic"><SimpleForm toolbar={<CatalogToolbar/>}><ExerciseFields/></SimpleForm></Edit>; }
