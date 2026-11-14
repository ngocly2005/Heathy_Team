import { Admin, Resource } from 'react-admin';
import { authProvider, dataProvider } from './api';
import { GoogleLogin } from './Login';
import { UserEdit, UserList } from './Users';
import { FoodList, FoodCreate, FoodEdit, ExerciseList, ExerciseCreate, ExerciseEdit } from './Catalog';
import { Analytics } from './Analytics';
import { AILogList, AILogShow } from './AILogs';
import { FitChatLayout } from './AdminLayout';
import { fitChatTheme } from './theme';
import { uiTranslations } from './uiTranslations';

export function App() {
    return <Admin title="FitChat Admin" requireAuth dashboard={Analytics} loginPage={GoogleLogin} authProvider={authProvider} dataProvider={dataProvider}
        layout={FitChatLayout} theme={fitChatTheme} darkTheme={null} i18nProvider={uiTranslations}>
        <Resource name="users" options={{label:'Người dùng'}} list={UserList} edit={UserEdit}/>
        <Resource name="foods" options={{label:'Thực phẩm'}} list={FoodList} create={FoodCreate} edit={FoodEdit}/>
        <Resource name="activities" options={{label:'Bài tập'}} list={ExerciseList} create={ExerciseCreate} edit={ExerciseEdit}/>
        <Resource name="ai-logs" options={{label:'Lịch sử AI'}} list={AILogList} show={AILogShow}/>
    </Admin>;
}
