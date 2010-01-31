goto START

:UPDATE
echo "UPDATE"
xcopy /E /Y update .
rmdir /S /Q update

:START
.wstart.bat

IF errorlevel 255 goto UPDATE
