import { test } from 'node:test';
import assert from 'node:assert/strict';
import queryString, { parse, stringify, parseUrl } from '../src/queryStringCompat.mjs';

test('Admin filters and sort survive a Unicode URL round trip', () => {
    const filter = JSON.stringify({ q: 'Cơm trắng', is_active: true });
    const encoded = stringify({ filter, page: 2, perPage: 25, sort: 'name', order: 'ASC' });
    const result = parse(encoded);
    assert.equal(result.filter, filter);
    assert.equal(result.page, '2');
    assert.equal(result.sort, 'name');
    assert.equal(queryString.parse, parse);
});
test('Malformed percent-encoding does not break Admin navigation', () => {
    const value = parseUrl('/users?filter=%E0%A4%A&sort=name');
    assert.equal(value.url, '/users');
    assert.equal(value.query.sort, 'name');
    assert.equal(typeof value.query.filter, 'string');
    assert.doesNotThrow(() => parse('q=' + '%A%'.repeat(10000)));
});
