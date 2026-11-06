import { Admin, Resource } from 'react-admin';
import { authProvider, dataProvider } from './api';
import { GoogleLogin } from './Login';
import { UserEdit, UserList } from './Users';
import { FoodList, FoodCreate, FoodEdit, ExerciseList, ExerciseCreate, ExerciseEdit } from './Catalog';
import { Analytics } from './Analytics';
import { AILogList, AILogShow } from './AILogs';

export function App() {
    return <Admin title="AppHealth Admin" requireAuth dashboard={Analytics} loginPage={GoogleLogin} authProvider={authProvider} dataProvider={dataProvider}
        theme={{palette:{primary:{main:'#119b72'},background:{default:'#f4f7f6'}},shape:{borderRadius:12}}}>
        <Resource name="users" options={{label:'Người dùng'}} list={UserList} edit={UserEdit}/>
        <Resource name="foods" options={{label:'Thực phẩm'}} list={FoodList} create={FoodCreate} edit={FoodEdit}/>
        <Resource name="activities" options={{label:'Bài tập'}} list={ExerciseList} create={ExerciseCreate} edit={ExerciseEdit}/>
        <Resource name="ai-logs" options={{label:'Lịch sử AI'}} list={AILogList} show={AILogShow}/>
    </Admin>;
}
