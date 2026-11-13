type User = { id: number; email: string; full_name: string; role: string; status: string };
type Session = { access_token: string; refresh_token: string; user: User };
export function loginAdmin(input: { credential?: string; email?: string; password?: string },
    exchange: (path: string, init?: RequestInit) => Promise<any>,
    remember: (session: Session) => void, onDenied: () => void): Promise<void>;
