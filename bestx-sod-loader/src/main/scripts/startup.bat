REM Copyright 1997-2016 SoftSolutions! srl
REM All Rights Reserved.
REM
REM This script starts the BestX application.
REM
REM artifact ref.: BestXREM script version 1.0 - 2016/03/10
REM
REM NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl
REM The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and
REM may be covered by EU, U.S. and other Foreign Patents, patents in process, and
REM are protected by trade secret or copyright law.
REM Dissemination of this information or reproduction of this material is strictly forbidden
REM unless prior written permission is obtained from SoftSolutions! srl.
REM Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt',
REM which may be part of this software package.
REM
REM @ECHO OFF
@ECHO OFF

set JDK_HOME=C:\Program Files\Java\jre1.8.0_181
set SERVER_CONFIG=-XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms128m -Xmx256m 
set CURRENT_DIR%=.

start "SODLoader" "%JDK_HOME%\bin\java" -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -Dss.app.name=BestX-SOD-Loader %SERVER_CONFIG% -cp ..\cfg;..\lib\*; it.softsolutions.bestx.sod.SODLoader
