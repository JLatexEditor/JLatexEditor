#!/bin/bash

for i in `ls *.tex`; do
  file=`basename $i .tex`;
  echo $file;
  pdflatex $file.tex
  convert $file.pdf -trim -resize 24x24 $file.png
done
