docker run -v "%CD%:/srv/jekyll" -v "%CD%/vendor/bundle:/usr/local/bundle" -p 4000:4000 -it jekyll/jekyll jekyll serve %* -H 0.0.0.0
