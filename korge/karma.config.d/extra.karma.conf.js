const basePath = config.basePath.replace(/\/node_modules$/, "");

config.set({
    basePath: basePath,
    files: [
        ...config.files,
        { pattern: 'kotlin/**/*', included: false, served: true, watched: false }
    ],
    proxies: {
        '/': '/base/kotlin/'
    },
    client: {
        mocha: {
            timeout: 20000  // adjust as needed
        }
    }
});
