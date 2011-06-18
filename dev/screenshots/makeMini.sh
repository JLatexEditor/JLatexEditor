#!/bin/bash

name=$1
# convert "${name}.png" -resize 800x360 \( +clone -background black -shadow 80x5+5+5 \) +swap \( -background none -mosaic \) -sharpen 2 "${name}_mini.png"

convert "${name}.png" -page +10+10 -resize 800x360 \( +clone -background black -shadow 60x5+1+1 \) +swap \( -background none -mosaic \) -sharpen 2 "${name}_mini.png"
