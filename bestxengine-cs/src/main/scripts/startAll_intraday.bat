TITLE "Start all BestX! services"

@ECHO off
echo "START Apache Tomcat 8.5" 
NET START Tomcat8

echo "START BestX!"
NET START BestX

ECHO "START OMS1"
NET START FixGatewayOMS1

ECHO "START Bloomberg GW"
NET START TradeStacBloomberg

ECHO "START MarketAxess GW"
NET START TradeStacMarketaxess

ECHO "START Tradeweb GW"
NET START TradeStacTradeweb

startBestxConnections.bat