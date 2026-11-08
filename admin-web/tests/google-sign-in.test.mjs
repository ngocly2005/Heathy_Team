import test from 'node:test';
import assert from 'node:assert/strict';
import { googleSignInOptions, disableGoogleAutoSelect, adminAccountDeniedMessage } from '../src/googleSignIn.mjs';

test('ID-token flow disables automatic selection and keeps the provider callback', () => {
    const callback = () => {};
    const options = googleSignInOptions('  example.apps.googleusercontent.com  ', callback);
    assert.equal(options.client_id, 'example.apps.googleusercontent.com');
    assert.equal(options.callback, callback);
    assert.equal(options.auto_select, false);
    assert.equal(options.button_auto_select, false);
    assert.equal(options.ux_mode, 'popup');
    assert.equal('prompt' in options, false); // This is not the OAuth access-token API.
    assert.equal('login_hint' in options, false); // Do not force one saved account.
});

test('missing client configuration is rejected before Google initialization', () => {
    for (const value of [undefined, '', '   ']) assert.throws(() => googleSignInOptions(value, () => {}));
});

test('app logout records Google auto-select sign-out without revoking the account', () => {
    let disabled = 0;
    let revoked = 0;
    disableGoogleAutoSelect({ disableAutoSelect() { disabled++; }, revoke() { revoked++; } });
    assert.equal(disabled, 1);
    assert.equal(revoked, 0);
});

test('Google library unavailable or throwing does not prevent app logout', () => {
    assert.doesNotThrow(() => disableGoogleAutoSelect(undefined));
    assert.doesNotThrow(() => disableGoogleAutoSelect({}));
    assert.doesNotThrow(() => disableGoogleAutoSelect({ disableAutoSelect() { throw new Error('blocked'); } }));
});

test('denial identifies the account selected by Google for the user to correct it', () => {
    assert.match(adminAccountDeniedMessage('teacher@example.com'), /teacher@example\.com/);
    assert.match(adminAccountDeniedMessage(undefined), /vừa chọn/);
});
