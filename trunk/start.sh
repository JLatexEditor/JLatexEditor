#!/bin/bash

cd `dirname $0`

svn up
ant compile
java -cp build/classes/ jlatexeditor.JLatexEditorJFrame &
