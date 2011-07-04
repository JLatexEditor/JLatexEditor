goto START

:UPDATE
echo "UPDATE"
xcopy /E /Y update .
rmdir /S /Q update

:START
call .wstart.bat

IF errorlevel 255 goto UPDATE
