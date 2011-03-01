#!/bin/bash

USER_PWD=`pwd`

# if there is only one argument which is not an option then it must be a file which we could try to open directly
if [ $# == 1 ]; then
	if echo "$1" | grep -q "^[^-]"; then
		file="$1"

		# local path? => make absolute
    if echo "main.tex" | grep -q "^[^\/]"; then
      file="$USER_PWD/$file"
		fi

		echo "open: $file" | nc -w 1 localhost 13231 2>/dev/null && exit 0
	fi
fi

cd `dirname $0`

svn up
ant compile
java -Xmx200M "-Djlatexeditor.working_dir=$USER_PWD" -cp build/classes/ jlatexeditor.JLatexEditorJFrame "$@"
