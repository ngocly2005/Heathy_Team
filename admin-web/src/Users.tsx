import { Datagrid, Edit, FunctionField, List, SaveButton, SelectInput, SimpleForm, TextField, TextInput, Toolbar } from 'react-admin';

const statuses = [{id:'active',name:'Đang hoạt động'},{id:'disabled',name:'Đã khóa'}];
const filters = [<TextInput source="q" label="Tên hoặc email" alwaysOn/>,<SelectInput source="status" label="Trạng thái" choices={statuses} alwaysOn/>];
export function UserList() {
    return <List title="Người dùng" filters={filters} perPage={25} sort={{field:'id',order:'DESC'}}>
        <Datagrid rowClick="edit" bulkActionButtons={false}>
            <TextField source="id"/><TextField source="full_name" label="Họ tên"/><TextField source="email" label="Email"/>
            <TextField source="role" label="Quyền"/>
            <FunctionField label="Trạng thái" render={(record: {status:string}) => record.status === 'active' ? 'Đang hoạt động' : 'Đã khóa'}/>
        </Datagrid>
    </List>;
}
export function UserEdit() {
    return <Edit title="Trạng thái người dùng" mutationMode="pessimistic">
        <SimpleForm toolbar={<Toolbar><SaveButton label="Lưu"/></Toolbar>}><TextField source="full_name"/><TextField source="email"/>
            <SelectInput source="status" label="Trạng thái" choices={statuses} required/>
        </SimpleForm>
    </Edit>;
}
