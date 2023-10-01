---
permalink: /targets/web/
group: targets
layout: default
title: "Web (JS)"
title_short: Web
fa-icon: fa-window-restore
priority: 20
#status: new
---

This target allows you to publish applications and games on any website.
Making it available to anyone with a browser or a mobile phone.

This target works on any web browser
supporting [WebGL](https://caniuse.com/#feat=webgl){:target="_blank",:rel="noopener"}
and [WebAudio](https://caniuse.com/#feat=audio-api){:target="_blank",:rel="noopener"} for sound,
that is almost modern web browser nowadays.

Features fast compilation time, small output size, fast startup time
and widely array of supported of devices with a single target.



## Executing

To compile, start an http-server and open a browser, use the gradle task:

```bash
./gradlew jsRun
```

## Packaging

```bash
./gradlew jsBrowserDistribution # Outputs to /build/distributions
```

You can use any HTTP server to serve the files in your browser.
For example using http-server: `npm -g install http-server` and then `hs build/distributions`.
Or using live-server: `npm -g install live-server` and then `live-server build/distributions`.
Or using Python3: change directory to `build/distributions` and then `python -m http.server`.

You can also use `./gradlew -t jsBrowserDistribution` to continuously build the JS sources and run
`hs build/distributions` in another terminal.
Here you can find a `testJs.sh` script doing exactly this for convenience.

You can run your tests using Node.JS by calling jsTest or in a headless chrome with jsTestChrome.

## Application Configuration

* The Application Icon would be rendered as a `favicon.ico`.
* The Application Title as the `<title>` tag.

## Recommendations: `runBlocking`

Remember that the JS and the Common target doesn't support blocking calls neither the `runBlocking` construct.
So when dealing with I/O you have to mark your functions as `suspend fun`.
Fortunately Korlibs are designed to be asynchronous, and reading resources is already suspending.
So you only have to propagate the suspend modifier when required and you are mostly safe here.

## Create a Docker image

To create a docker image use this Dockerfile (which is also included in the [korge-hello-world](https://github.com/korlibs/korge-hello-world){:target="_blank",:rel="noopener"} template):

```dockerfile
FROM gradle:8.2.1-jdk17-alpine as builder
WORKDIR /home/gradle/app
COPY --chown=gradle:gradle . /home/gradle/app
# Install necessary graphic and audio libraries
RUN apk add --no-cache freeglut-dev openal-soft-dev mesa-dri-gallium gcompat
RUN gradle jsBrowserDistribution

FROM node:18-alpine
WORKDIR /app
COPY --from=builder /home/gradle/app/build/distributions /app
RUN npm install -g http-server
EXPOSE 8080
CMD ["http-server", "-p", "8080"]
```

You can build the image with: `docker build -t korge-hello-world .`
To use the image with docker-compose:

```yaml
services:
  my-app:
    container_name: my-app
    build: .
    ports:
      - "8080:8080"
    restart: unless-stopped
```
