SET JVM_HOME=C:\Program Files\Java\jre1.8.0_201
SET JVM="%JVM_HOME%\bin\server\jvm.dll"
SET CURRENT_DIR=C:\SoftSolutions\BestX\bestx_engine
SET DESC="BestX"
SET LOG_DIR=%CURRENT_DIR%\log

SET JMX_PORT=29300

SET HOSTNAME=localhost

SET OUT=%CURRENT_DIR%\log\stdout.log
SET ERR=%CURRENT_DIR%\log\stderr.log

#SET OPT_1=-verbose:gc -XX:+PrintGCTimeStamps -XX:+PrintGCDetails -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1024m -Xmx2048m -XX:PermSize=128m -XX:MaxPermSize=256m
SET OPT_1=-XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms1024m -Xmx2048m -XX:PermSize=128m -XX:MaxPermSize=256m -Dcom.ibm.mq.cfg.useIBMCipherMappings=false
SET OPT_2=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=%JMX_PORT% -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.password=false
SET OPT_3=-Djava.class.path=../cfg -Djava.ext.dirs=..;../lib;"%JVM_HOME%\lib\ext"; -Djava.library.path=../lib; -Duser.timezone=CET
SET OPT_4=-Dlogback.configurationFile=../cfg/logback.xml

SET START_CLASS=it.softsolutions.bestx.Main
SET STOP_CLASS=it.softsolutions.bestx.controller.Main

SET START_PARAMS=cs-spring.xml BESTX.properties
SET STOP_PARAMS=-Nit.softsolutions.bestx.cs -Hlocalhost:%JMX_PORT% -iApplicationControl shutdown

java %OPT_1% %OPT_2% %OPT_3% %OPT_4% %START_CLASS% %START_PARAMS%


