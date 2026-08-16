// sql.js fetches its SQLite-compiled-to-WASM binary at runtime via a plain relative URL
// ("sql-wasm.wasm"), but webpack only bundles assets it sees referenced through import/require,
// so this binary never makes it into the output on its own. Copy it in explicitly.
const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

const sqlJsWasm = path.join(path.dirname(require.resolve('sql.js/package.json')), 'dist', 'sql-wasm.wasm');

config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [{ from: sqlJsWasm, to: 'sql-wasm.wasm' }]
    })
);
