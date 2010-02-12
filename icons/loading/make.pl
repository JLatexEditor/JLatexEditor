#!/usr/bin/perl

$ani = "";
for($i = 0; $i < 18; $i++) {
  system("cp -f loading.tex main.tex");
  open(FILE,">>main.tex");
  print FILE "\\begin{document}\\load{$i}\\end{document}";
  close(FILE);
  system("pdflatex main.tex");
  system("convert main.pdf main$i.png");
  $ani = $ani."main$i.png ";
}

system("convert -delay 9 -loop 0 $ani animated.gif");
system("rm main*");
