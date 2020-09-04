REM Copyright 1997-2016 SoftSolutions! srl
REM All Rights Reserved.
REM
REM This script starts the BestX application.
REM
REM artifact ref.: BestX
REM script version 1.0 - 2016/03/10
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


SET JDK_HOME=C:\Program Files\Java\jre1.8.0_181

SET CURRENT_DIR=C:\SoftSolutions\BestX\bestx_engine
SET JMX_PORT=29300

SET OUT=%CURRENT_DIR%\log\stdout.log
SET ERR=%CURRENT_DIR%\log\stderr.log

SET START_CLASS=it.softsolutions.bestx.Main
SET ARGS=cs-spring.xml BESTX.properties

set OPT_1=-server -Dss.app.name=BestX -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1024m -Xmx4096m -XX:PermSize=128m -XX:MaxPermSize=256m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.password=false
set OPT_2=-cp %CURRENT_DIR%\lib\*;%CURRENT_DIR%\cfg -Duser.timezone=CET -Dlogback.configurationFile=cfg\logback.xml -Dcom.ibm.mq.cfg.useIBMCipherMappings=false

REM Required by CS
set JAVA_AGENT_DIR=C:\PROGRA~1\AppDynamics\JavaAgent_v4.4.3
set OPT_EXT=-javaagent:%JAVA_AGENT_DIR%\javaagent.jar -Dappdynamics.agent.nodeName=BestX_chwu9004529 -Dappdynamics.agent.tierName=BestX -Dappdynamics.agent.applicationName=24212_BestX_UAT

"%JDK_HOME%\bin\java" %OPT_1% %OPT_2% %OPT_EXT% %START_CLASS% %ARGS%



