#!/bin/bash

cd `dirname $0`

# if there is only one argument which is not an option then it must be a file which we could try to open directly
if [ $# == 1 ]; then
	if echo "$1" | grep -q "^[^-]"; then
		echo "open: $1" | nc -w 1 localhost 13231 2>/dev/null && exit 0
	fi
fi

svn up
ant compile
java -cp build/classes/ jlatexeditor.JLatexEditorJFrame "$@"
