import test from 'node:test';
import assert from 'node:assert/strict';
import { loginAdmin } from '../src/adminAuth.mjs';

function backend(user = { id: 42, email: 'admin@example.com', role: 'admin', status: 'active' }) {
    const requests = [];
    let saved;
    let denied = false;
    return { requests, get saved() { return saved; }, get denied() { return denied; },
        store: value => { saved = value; }, deny: () => { denied = true; },
        exchange: async (path, init) => {
            requests.push({ path, ...init });
            if (path === '/auth/me') { assert.equal(init.headers.Authorization, 'Bearer access'); return user; }
            return { access_token: 'access', refresh_token: 'refresh' };
        } };
}

test('password admin login stores the server identity, never the password', async () => {
    const be = backend();
    await loginAdmin({ email: ' admin@example.com ', password: ' exact password ' }, be.exchange, be.store, be.deny);
    assert.equal(be.requests[0].path, '/auth/login');
    assert.deepEqual(JSON.parse(be.requests[0].body), { email: 'admin@example.com', password: ' exact password ' });
    assert.equal(be.saved.user.id, 42);
    assert.ok(!JSON.stringify(be.saved).includes(' exact password '));
    assert.equal(be.requests.length, 2);
});

test('Google continues using the same admin/session check', async () => {
    const be = backend();
    await loginAdmin({ credential: 'google-proof' }, be.exchange, be.store, be.deny);
    assert.equal(be.requests[0].path, '/auth/google');
    assert.deepEqual(JSON.parse(be.requests[0].body), { id_token: 'google-proof' });
    assert.equal(be.saved.user.role, 'admin');
});

for (const user of [{ email: 'user@example.com', role: 'user', status: 'active' },
    { email: 'blocked@example.com', role: 'admin', status: 'disabled' }]) {
    test(`rejects ${user.email} and revokes the unused refresh token`, async () => {
        const be = backend(user);
        await assert.rejects(loginAdmin({ email: user.email, password: 'password' }, be.exchange, be.store, be.deny),
            { message: new RegExp(user.email.replace('.', '\\.')) });
        assert.equal(be.saved, undefined);
        assert.equal(be.denied, true);
        assert.equal(be.requests.at(-1).path, '/auth/logout');
    });
}

test('failed identity or local session persistence revokes issued refresh tokens', async () => {
    for (const failure of ['identity', 'storage']) {
        const be = backend();
        const exchange = async (path, init) => {
            if (path === '/auth/me' && failure === 'identity') throw new Error('identity failed');
            return be.exchange(path, init);
        };
        await assert.rejects(loginAdmin({ email: 'admin@example.com', password: 'password' }, exchange,
            () => { throw new Error('storage failed'); }, be.deny));
        assert.equal(be.requests.at(-1).path, '/auth/logout');
        assert.equal(be.saved, undefined);
    }
});
