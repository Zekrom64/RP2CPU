@echo off
set asmfile=%1
set asmfilename=%asmfile:~0,-5%
64tass %asmfile% -b --m65el02 -o %asmfilename%.img