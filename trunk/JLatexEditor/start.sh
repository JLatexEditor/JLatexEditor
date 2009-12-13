#!/bin/bash
svn up
ant compile
java -cp build/classes/ jlatexeditor.JLatexEditorJFrame &