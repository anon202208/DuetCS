Package.describe({
  summary: 'CSS minifier',
  version: '1.6.1'
});

Npm.depends({
  postcss: '8.4.16',
  cssnano: '4.1.11'
});

Package.onUse(function (api) {
  api.use('ecmascript');
  api.mainModule('minifier.js', 'server');
  api.export('CssTools');
});

Package.onTest(function (api) {
  api.use('ecmascript');
  api.use('tinytest');
  api.addFiles([
    'minifier-tests.js',
    'urlrewriting-tests.js'
  ], 'server');
});
