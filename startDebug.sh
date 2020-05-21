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

cd `dirname $0`

git pull origin master
ant clearDependentClasses
ant compile

# rename debug.log / error.log to last_debug.log / last_error.log
for f in debug error; do
	if [ -e $f.log ]; then
		mv $f.log last_$f.log
	fi
done

LIB=""
for file in `ls lib/`
do
   LIB="$LIB:lib/$file"
done

MAC=""
if [ `uname` = "Darwin" ]
then
  MAC="-Xdock:name=JLatexEditor -Xdock:icon=src/images/icon_32.png"
fi

java -Xmx200M $MAC "-Djlatexeditor.working_dir=$USER_PWD" -cp build/classes/$LIB jlatexeditor.JLatexEditorJFrame "$@" > debug.log 2> error.log &
