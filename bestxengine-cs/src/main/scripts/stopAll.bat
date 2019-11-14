@ECHO off
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iApplicationControl shutdown
timeout 20
NET STOP FixGatewayOMS1
NET STOP TradeStacBloomberg
NET STOP TradeStacMarketaxess
NET STOP TradeStacTradeweb
NET STOP BestX
NET STOP Tomcat8