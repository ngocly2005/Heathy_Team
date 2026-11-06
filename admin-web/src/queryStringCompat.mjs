// react-admin uses query-string's old named exports. The maintained ESM package
// provides a default namespace. Keep both entry points and the patched URI decoder.
import queryString from 'query-string-modern';
export default queryString;
export const { parse, stringify, extract, parseUrl, stringifyUrl, pick, exclude } = queryString;
