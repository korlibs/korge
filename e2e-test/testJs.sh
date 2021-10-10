#!/usr/bin/env bash

trap killgroup SIGINT

killgroup(){
  echo killing...
  kill 0
}

hs build/web &
./gradlew jsWeb -t &
wait