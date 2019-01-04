@ECHO off
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iApplicationControl shutdown
timeout 20
NET STOP XT2FIXGWOMS1
NET STOP TradeStacBloomberg
NET STOP TradeStacMarketAxess
NET STOP TradeStacTradeweb
NET STOP Tomcat8
NET STOP BestX

