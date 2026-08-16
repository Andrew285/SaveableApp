// sql.js's UMD bundle conditionally requires Node.js core modules that are never actually
// reached in a browser, but webpack 5 still tries to statically resolve them. Tell webpack
// there's nothing to polyfill instead of failing the build.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    path: false,
    fs: false,
    crypto: false
});
