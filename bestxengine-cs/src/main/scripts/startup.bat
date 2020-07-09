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

SET JVM_HOME=C:\Program Files\Java\jre1.8.0_181
SET CURRENT_DIR=C:\SoftSolutions\BestX\bestx_engine
SET DESC="BestX"
SET LOG_DIR=%CURRENT_DIR%\log

SET JMX_PORT=29300

SET HOSTNAME=localhost

SET OUT=%CURRENT_DIR%\log\stdout.log
SET ERR=%CURRENT_DIR%\log\stderr.log

SET START_CLASS=it.softsolutions.bestx.Main
SET STOP_CLASS=it.softsolutions.bestx.controller.Main
set CLASSPATH=%CURRENT_DIR%\lib\*

SET OPT_1= -server -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1024m -Xmx2048m -XX:PermSize=128m -XX:MaxPermSize=256m
SET OPT_2=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.password=false
SET OPT_3=-cp %CURRENT_DIR%\lib\*;%CURRENT_DIR%\cfg -Djava.library.path=lib; -Duser.timezone=CET
SET OPT_4=-Dlogback.configurationFile=cfg\logback.xml
SET OPT_5=-Dcom.ibm.mq.cfg.useIBMCipherMappings=false
SET STOP_PARAMS=-Nit.softsolutions.bestx.cs -Hlocalhost:%JMX_PORT% -iApplicationControl shutdown
SET JAVA_OPTIONS= %OPT_1% %OPT_2% %OPT_3% %OPT_4% %OPT_5%
ECHO %JAVA_OPTIONS%
SET ARGS=cs-spring.xml BESTX.properties

"%JVM_HOME%\bin\java" %JAVA_OPTIONS% %START_CLASS% %ARGS%
