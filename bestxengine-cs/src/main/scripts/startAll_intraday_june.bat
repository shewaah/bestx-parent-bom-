TITLE "Start all BestX! services"

@ECHO off
echo "START Apache Tomcat 8.5" 
NET START "Apache Tomcat 8.5"

echo "START BestX!"
NET START BestX

ECHO "START OMS1"
NET START XT2FIXGWOMS1
REM NET START XT2FIXGWOMS2

ECHO "START Bloomberg GW"
NET START TradeStacBloomberg

ECHO "START MarketAxess GW"
NET START TradeStacMarketAxess

ECHO "START Tradeweb GW"
NET START TradeStacTradeweb


timeout 20
REM connection to the GUI
ECHO "Connect GUI"
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iIB4JOperatorConsoleAdapter connect

REM restore of the status
ECHO "RESTORE ORDERS AND ORDER STATES"
timeout 360

java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iOperationRestore restoreOperationStates
timeout 360

ECHO "START CONNECTIONS TO MARKETS"
REM Market connections
REM Bloomberg
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iBloombergMarketConnection startPriceConnection
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iBloombergMarketConnection enablePriceConnection
:retrybbg
timeout 3
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -qBloombergMarketConnection PriceConnectionEnabled | findstr /I true
REM ERRORLEVEL 1 means the returned value is not true, i.e. the channel is not enabled yet
IF ERRORLEVEL 1 GOTO :retrybbg
REM Tradeweb
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iTradewebMarketConnection startPriceConnection
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iTradewebMarketConnection enablePriceConnection
:retrytwp
timeout 3
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -qTradewebMarketConnection PriceConnectionEnabled | findstr /I true
REM ERRORLEVEL 1 means the returned value is not true, i.e. the channel is not enabled yet
IF ERRORLEVEL 1 GOTO :retrytwp

java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iTradewebMarketConnection startBuySideConnection
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iTradewebMarketConnection enableBuySideConnection
:retrytwo
timeout 3
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -qTradewebMarketConnection BuySideConnectionEnabled | findstr /I true
REM ERRORLEVEL 1 means the returned value is not true, i.e. the channel is not enabled yet
IF ERRORLEVEL 1 GOTO :retrytwo
REM MarketAxess
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iMarketAxessMarketConnection startPriceConnection
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iMarketAxessMarketConnection enablePriceConnection
:retrymap
timeout 3
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -qMarketAxessMarketConnection PriceConnectionEnabled | findstr /I true
REM ERRORLEVEL 1 means the returned value is not true, i.e. the channel is not enabled yet
IF ERRORLEVEL 1 GOTO :retrymap

java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iMarketAxessMarketConnection startBuySideConnection
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iMarketAxessMarketConnection enableBuySideConnection
:retrymao
timeout 3
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -qMarketAxessMarketConnection BuySideConnectionEnabled | findstr /I true
REM ERRORLEVEL 1 means the returned value is not true, i.e. the channel is not enabled yet 
IF ERRORLEVEL 1 GOTO :retrymao

REM Order connections
ECHO "CONNECT OMS1"
java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iOms1FixCustomerConnection connect
REM java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iOms2CustomerConnection connect

REM rank
REM java -jar bxcontrol.jar -Nit.softsolutions.bestx.cs -Hlocalhost:29300 -iDealerRankingService loadRankingAndUpdate