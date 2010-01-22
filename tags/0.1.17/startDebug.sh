#!/bin/bash

cd `dirname $0`

svn up
ant compile
java -Xprof -cp build/classes/ jlatexeditor.JLatexEditorJFrame "$@" > debug.log &
