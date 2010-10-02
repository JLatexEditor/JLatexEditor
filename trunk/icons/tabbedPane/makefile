
texs := $(wildcard *.tex)
pdfs := $(texs:%.tex=%.pdf)
pngs := $(texs:%.tex=%.png)

default: tabs close_button

pdfs: $(pdfs)

pngs: $(pngs)

%.pdf: %.tex
	pdflatex $*.tex
	@rm $*.aux $*.log  >/dev/null 2>&1 || true

%.ps: %.tex
	latex $*.tex
	dvips $*.dvi

tabs.png: tabs.pdf
	convert tabs.pdf +repage -trim -resize 1024x26 +repage tabs.png

tab_close.png: tab_close.ps
	echo "TODO: needed ???"

tab_close_over.png: tab_close_over.ps
	echo "TODO"

clean:
	@rm $(pdfs) $(pngs) >/dev/null 2>&1 || true

# for i in `ls *.tex`; do
#   file=`basename $i .tex`;
#   echo $file;
#   pdflatex $file.tex
#   convert $file.pdf -trim -resize 20x20 $file.png
# done

tabs: pngs
	convert tabs.png +repage -crop 20x26+0+0 tab_active_left.png
	convert tabs.png +repage -crop 1x26+25+0 tab_active.png
	convert tabs.png +repage -crop 20x26+46+0 tab_active_right.png
	convert tabs.png +repage -crop 20x26+74+0 tab_inactive_left.png
	convert tabs.png +repage -crop 1x26+115+0 tab_inactive.png
	convert tabs.png +repage -crop 20x26+122+0 tab_inactive_right.png
	convert tabs.png +repage -crop 20x26+197+0 tab_inactive_active.png
	convert tabs.png +repage -crop 20x26+247+0 tab_active_inactive.png
	convert tabs.png +repage -crop 20x26+297+0 tab_inactive_inactive.png

close_button: tab_close_over.png
