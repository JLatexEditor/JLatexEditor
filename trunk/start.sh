#!/bin/bash

USER_PWD=`pwd`

# if there is only one argument which is not an option then it must be a file which we could try to open directly
if [ $# == 1 ]; then
	if echo "$1" | grep -q "^[^-]"; then
		file="$1"

		# local path? => make absolute
		if echo "$file" | grep -q "^[^\/]"; then
			file="$USER_PWD/$file"
		fi

		echo "open: $file" | nc -w 1 localhost 13231 2>/dev/null && exit 0
	fi
fi

JLE_SCRIPT=$0
# JLE_SCRIPT=`readlink -f $0`
cd `dirname $JLE_SCRIPT`

LIB=""
for file in `ls lib/`
do
   LIB="$LIB:lib/$file"
done

svn up
ant clearDependentClasses
ant compile

MAC=""
if [ `uname` = "Darwin" ]
then
  MAC="-Xdock:name=JLatexEditor -Xdock:icon=dev/tex-cookie.png"
fi

java -Xmx200M $MAC "-Djlatexeditor.working_dir=$USER_PWD" -cp build/classes/$LIB jlatexeditor.JLatexEditorJFrame "$@"
