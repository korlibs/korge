const basePath = config.basePath.replace(/\/node_modules$/, "")
config.set({
    "basePath": basePath,
    "files": [
        ...config.files,
        {pattern: 'kotlin/**/*', included: false}
    ],
    "proxies": {
      '/': '/base/kotlin/'
    },
    client: {
        mocha: {
            timeout: 20000
        }
    }
})
