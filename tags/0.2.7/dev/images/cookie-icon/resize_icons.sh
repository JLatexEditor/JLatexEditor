#!/bin/bash

for size in 16 32 64 128 256; do
	convert -resize ${size}x${size} tex-cookie_512.png tex-cookie_${size}.png
done
