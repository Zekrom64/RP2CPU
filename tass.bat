set asmfile=%1
set asmfilename=%asmfile:~0,-5%
64tass %asmfile% -b -Wall --m65el02 -o %asmfilename%.img