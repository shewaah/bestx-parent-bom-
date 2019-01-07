SET CP=cfg

java -Xmx512M -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=29300 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.password=false -Djava.class.path=%CP% -Djava.ext.dirs=.;lib -Djava.library.path=lib it.softsolutions.bestx.Main cs-spring.xml BESTX.properties > sysout.txt

pause
