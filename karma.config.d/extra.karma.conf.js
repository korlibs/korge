const basePath = config.basePath.replace(/\/node_modules$/, "")
config.set({
    browsers: ['ChromeHeadlessWebGPU'],
    customLaunchers: {
      ChromeHeadlessWebGPU: {
        base: 'ChromeHeadless',
        flags: [
          '--enable-unsafe-webgpu', // Enable WebGPU
          '--no-sandbox', // Optional: Helps to run Chrome in certain CI environments
          '--disable-dev-shm-usage', // Optional: Helps to avoid issues with /dev/shm partition in certain CI environments
          '--headless', // Ensures that Chrome runs in headless mode
          '--disable-gpu', // Optional: Disable hardware GPU acceleration (useful in some CI environments)
          '--remote-debugging-port=9222' // Optional: Allows remote debugging
        ]
      }
    },
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
