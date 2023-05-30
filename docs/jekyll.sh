#!/usr/bin/env bash
docker run -v "$PWD:/srv/jekyll" -v "$PWD/vendor/bundle:/usr/local/bundle" -p 4000:4000 -it jekyll/jekyll jekyll serve $* -H 0.0.0.0

