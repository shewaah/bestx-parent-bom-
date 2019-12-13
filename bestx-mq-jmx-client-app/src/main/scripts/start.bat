@echo off
rem
rem This script starts the BestX MQ JMX Application.
rem
rem artifact ref.: bestx-mq-jmx-client-app
rem script version 1.0 - 2019/12/13
rem
rem NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl
rem The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and
rem may be covered by EU, U.S. and other Foreign Patents, patents in process, and
rem are protected by trade secret or copyright law.
rem Dissemination of this information or reproduction of this material is strictly forbidden
rem unless prior written permission is obtained from SoftSolutions! srl.
rem Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt',
rem which may be part of this software package.
rem
rem Scripts vars

SET JVM_HOME=C:\Program Files\Java\jdk1.8.0_112
SET CURRENT_DIR=C:/softsolutions/bestx/tools/bestx-mq-jmx-client

set OPT_1=-server -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods
set OPT_2=-XX:PermSize=128m -XX:MaxPermSize=256m -XX:HeapDumpPath=%CURRENT_DIR%/log/dump -XX:+HeapDumpOnOutOfMemoryError -Dcom.ibm.mq.cfg.useIBMCipherMappings=false
set OPT_3=-Dss.app.name=BogusGRDLite -cp %CURRENT_DIR%\cfg;%CURRENT_DIR%\lib\*;

SET JAVA_OPTIONS= %OPT_1% %OPT_2% %OPT_3%
SET START_CLASS=it.softsolutions.bestx.mqclient.MQtest

"%JVM_HOME%\bin\java" %JAVA_OPTIONS% %START_CLASS%
pause