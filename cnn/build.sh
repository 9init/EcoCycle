#!/bin/bash

echo "Start building"
env GOARCH=arm GOARM=7 go build -ldflags="-s -w" -o armeabi-v7a main.go
env GOARCH=arm64 GOARM64=8a go build -ldflags="-s -w" -o arm64-v8a main.go

mkdir -p to-Android
mv armeabi-v7a to-Android
mv arm64-v8a to-Android

if [ -f "trained-Data.json" ]; then
    echo "archiving files"
    cp trained-Data.json to-Android
    cd to-Android
    rm -rf data.tgz
    tar -czf data.tgz *
    echo "done"
fi
