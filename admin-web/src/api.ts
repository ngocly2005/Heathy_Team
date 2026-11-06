import { HttpError, type AuthProvider, type DataProvider } from 'react-admin';

const origin = new URL(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000');
if (!['http:', 'https:'].includes(origin.protocol) || origin.pathname !== '/' || origin.username || origin.password || origin.search || origin.hash) {
    throw new Error('VITE_API_BASE_URL must be a backend origin without api/v1 or credentials');
}
if (origin.protocol !== 'https:' && !import.meta.env.DEV && !['localhost', '127.0.0.1'].includes(origin.hostname)) {
    throw new Error('Use HTTPS for a remote backend');
}
const key = `apphealth-admin-v1:${origin.origin}`;
type User = { id: number; email: string; full_name: string; role: string; status: string };
type Tokens = { access_token: string; refresh_token: string };
type Session = Tokens & { user: User };
function session(): Session | null {
    try {
        const value=JSON.parse(sessionStorage.getItem(key) || 'null');
        return value && typeof value.access_token==='string' && typeof value.refresh_token==='string' &&
            Number.isSafeInteger(value.user?.id) && value.user.id>0 ? value : null;
    } catch { return null; }
}
function save(value: Session) { sessionStorage.setItem(key, JSON.stringify(value)); }
function clear() { sessionStorage.removeItem(key); }
let rotating: Promise<void> | null = null;

async function raw(path: string, init: RequestInit = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 40000);
    try {
        return await fetch(`${origin.origin}/api/v1${path}`, { ...init, redirect: 'error', credentials: 'omit', signal: controller.signal,
            headers: { 'Content-Type': 'application/json', ...init.headers } });
    } catch {
        throw new HttpError(init.method && init.method !== 'GET'
            ? 'Chưa rõ kết quả yêu cầu. Tải lại dữ liệu trước khi thao tác tiếp.' : 'Không kết nối được máy chủ.', 0);
    } finally { clearTimeout(timeout); }
}
async function json(response: Response) {
    if (response.status === 204) return null;
    const value = await response.json().catch(() => ({}));
    if (!response.ok) {
        const detail = value.detail;
        const message = typeof detail === 'string' ? detail : Array.isArray(detail)
            ? detail.map((e: { msg: string }) => e.msg).join('; ') : `Yêu cầu thất bại (${response.status})`;
        throw new HttpError(message, response.status, value);
    }
    return value;
}
async function rotate(expected: Session) {
    if (!rotating) rotating = (async () => {
        const current = session();
        if (!current || current.user.id !== expected.user.id) throw new HttpError('Phiên đã thay đổi.', 401);
        if (current.refresh_token !== expected.refresh_token) return;
        const response = await raw('/auth/refresh', { method: 'POST', body: JSON.stringify({ refresh_token: current.refresh_token }) });
        if (response.status === 401 && session()?.refresh_token === current.refresh_token) clear();
        const tokens: Tokens = await json(response);
        if (session()?.refresh_token === current.refresh_token) save({ ...current, ...tokens });
    })().finally(() => { rotating = null; });
    return rotating;
}
export async function api(path: string, init: RequestInit = {}, retry = true): Promise<any> {
    const current = session();
    if (!current) throw new HttpError('Vui lòng đăng nhập.', 401);
    const response = await raw(path, { ...init, headers: { ...init.headers, Authorization: `Bearer ${current.access_token}` } });
    if (response.status === 401 && retry) {
        await rotate(current);
        if (session()?.user.id !== current.user.id) throw new HttpError('Phiên đã thay đổi.', 401);
        return api(path, init, false);
    }
    if (response.status === 401 && session()?.access_token === current.access_token) clear();
    return json(response);
}

export const authProvider: AuthProvider = {
    login: async ({ credential }: { credential: string }) => {
        const tokens: Tokens = await json(await raw('/auth/google', { method: 'POST', body: JSON.stringify({ id_token: credential }) }));
        const user: User = await json(await raw('/auth/me', { headers: { Authorization: `Bearer ${tokens.access_token}` } }));
        if (user.role !== 'admin' || user.status !== 'active') {
            await raw('/auth/logout', { method: 'POST', body: JSON.stringify({ refresh_token: tokens.refresh_token }) }).catch(() => undefined);
            throw new Error('Tài khoản này chưa được cấp quyền quản trị.');
        }
        save({ ...tokens, user });
    },
    logout: async () => {
        const previous = session();
        clear();
        if (previous) await raw('/auth/logout', { method: 'POST', body: JSON.stringify({ refresh_token: previous.refresh_token }) }).catch(() => undefined);
        return '/login';
    },
    checkAuth: async () => { if (!session()) throw new Error('Vui lòng đăng nhập.'); },
    checkError: async error => { if ([401,403].includes(error.status)) throw error; },
    getIdentity: async () => {
        const user: User = await api('/auth/me');
        if (user.role !== 'admin') throw new HttpError('Cần quyền quản trị.', 403);
        return { id: user.id, fullName: user.full_name };
    },
    getPermissions: async () => {
        const user: User = await api('/auth/me');
        if (user.role !== 'admin') throw new HttpError('Cần quyền quản trị.',403);
        return user.role;
    },
};

const fields: Record<string, string[]> = {
    users: ['status'], foods: ['name','calories_per_100g','protein_per_100g','carb_per_100g','fat_per_100g','is_active'],
    activities: ['name','met','is_active'],
};
function payload(resource: string, data: Record<string, unknown>) {
    return JSON.stringify(Object.fromEntries((fields[resource] || []).filter(k => data[k] !== undefined).map(k => [k,data[k]])));
}
export const dataProvider: DataProvider = {
    getList: async (resource, params) => {
        const query = new URLSearchParams({ page: String(params.pagination?.page || 1), per_page: String(params.pagination?.perPage || 25),
            sort_field: params.sort?.field || 'id', sort_order: params.sort?.order || 'ASC' });
        for (const [name,value] of Object.entries(params.filter || {})) if (value !== undefined && value !== null && value !== '') query.set(name,String(value));
        const result = await api(`/admin/${resource}?${query}`);
        return { data: result.items, total: result.total };
    },
    getOne: async (resource, params) => ({ data: await api(`/admin/${resource}/${encodeURIComponent(params.id)}`) }),
    getMany: async (resource, params) => ({ data: await Promise.all(params.ids.map(id => api(`/admin/${resource}/${encodeURIComponent(id)}`))) }),
    getManyReference: async (resource, params) => dataProvider.getList(resource,{ ...params, filter: { ...params.filter, [params.target]: params.id } }),
    create: async (resource, params) => ({ data: await api(`/admin/${resource}`,{ method:'POST', body:payload(resource,params.data) }) }),
    update: async (resource, params) => ({ data: await api(`/admin/${resource}/${params.id}`,{ method:'PUT', body:payload(resource,params.data) }) }),
    updateMany: async (resource, params) => { await Promise.all(params.ids.map(id => api(`/admin/${resource}/${id}`,{ method:'PUT',body:payload(resource,params.data) }))); return {data:params.ids}; },
    delete: async (resource, params) => { await api(`/admin/${resource}/${params.id}`,{method:'DELETE'}); return { data: params.previousData || {id:params.id} as any }; },
    deleteMany: async (resource, params) => { await Promise.all(params.ids.map(id => api(`/admin/${resource}/${id}`,{method:'DELETE'}))); return { data:params.ids }; },
};
