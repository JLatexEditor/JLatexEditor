all: symbols.txt

symbols.tex:
	wget http://mirrors.ctan.org/info/symbols/comprehensive/source/symbols.tex

symbols.txt: symbols.tex
	cat symbols.tex | grep '\index{' | grep see | grep texttt >symbols.txt

