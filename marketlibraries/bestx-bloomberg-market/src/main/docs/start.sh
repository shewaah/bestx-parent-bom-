#!/bin/sh

export HOSTNAME=192.168.0.183
export JDK_HOME=/usr/java/jdk1.6.0_21
export SERVER_CONFIG='-server -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms512m -Xmx1024m -XX:PermSize=128m -XX:MaxPermSize=256m'
export JMX_CONFIG='-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=3099 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.password=false'
export RMI_CONFIG='-Dexample.rmi.agent.port=8492 -javaagent:./lib/custom-agent-1.0.jar -Djava.rmi.server.hostname=192.168.0.183 '

$JDK_HOME/bin/java -Dss.app.name=bestx-cs $SERVER_CONFIG $JMX_CONFIG $RMI_CONFIG -Djava.ext.dirs=./cfg:./lib:. -jar ./lib/bestxengine-cs-3.0.1.jar cs-spring.xml BESTX.properties
