sc delete BestX
C:\SoftSolutions\BestX\bestx_engine\bin\BestX.exe install BestX C:\SoftSolutions\BestX\bestx_engine\bin\startup.bat
C:\SoftSolutions\BestX\bestx_engine\bin\BestX.exe set BestX description "BestX"
C:\SoftSolutions\BestX\bestx_engine\bin\BestX.exe set BestX start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\bestx_engine\bin\BestX.exe set BestX AppStopMethodConsole 30000

sc delete TradeStacBloomberg
C:\SoftSolutions\BestX\gateways\tradestac\bloomberg\TradeStacBloomberg.exe install TradeStacBloomberg C:\SoftSolutions\BestX\gateways\tradestac\bloomberg\startup.bat
C:\SoftSolutions\BestX\gateways\tradestac\bloomberg\TradeStacBloomberg.exe set TradeStacBloomberg description "TradeStacBloomberg"
C:\SoftSolutions\BestX\gateways\tradestac\bloomberg\TradeStacBloomberg.exe set TradeStacBloomberg start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\gateways\tradestac\bloomberg\TradeStacBloomberg.exe set TradeStacBloomberg AppStopMethodConsole 30000

sc delete TradeStacMarketaxess
C:\SoftSolutions\BestX\gateways\tradestac\marketaxess\bin\TradeStacMarketaxess.exe install TradeStacMarketaxess C:\SoftSolutions\BestX\gateways\tradestac\marketaxess\bin\startup.bat
C:\SoftSolutions\BestX\gateways\tradestac\marketaxess\bin\TradeStacMarketaxess.exe set TradeStacMarketaxess description "TradeStacMarketaxess"
C:\SoftSolutions\BestX\gateways\tradestac\marketaxess\bin\TradeStacMarketaxess.exe set TradeStacMarketaxess start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\gateways\tradestac\marketaxess\bin\TradeStacMarketaxess.exe set TradeStacMarketaxess AppStopMethodConsole 30000

sc delete TradeStacTradeweb
C:\SoftSolutions\BestX\gateways\tradestac\tradeweb\TradeStacTradeweb.exe install TradeStacTradeweb C:\SoftSolutions\BestX\gateways\tradestac\tradeweb\startup.bat
C:\SoftSolutions\BestX\gateways\tradestac\tradeweb\TradeStacTradeweb.exe set TradeStacTradeweb description "TradeStacTradeweb"
C:\SoftSolutions\BestX\gateways\tradestac\tradeweb\TradeStacTradeweb.exe set TradeStacTradeweb start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\gateways\tradestac\tradeweb\TradeStacTradeweb.exe set TradeStacTradeweb AppStopMethodConsole 30000

sc delete FixGatewayOMS1
C:\SoftSolutions\BestX\services\FixGatewayOMS1\FixGatewayOMS1.exe install FixGatewayOMS1 C:\SoftSolutions\BestX\services\FixGatewayOMS1\XT2FixGateway-x86-v110_xp-MTDLL-R.exe
C:\SoftSolutions\BestX\services\FixGatewayOMS1\FixGatewayOMS1.exe set FixGatewayOMS1 description "FixGatewayOMS1"
C:\SoftSolutions\BestX\services\FixGatewayOMS1\FixGatewayOMS1.exe set FixGatewayOMS1 start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\services\FixGatewayOMS1\FixGatewayOMS1.exe set FixGatewayOMS1 AppStopMethodConsole 30000


sc delete SODLoader
C:\SoftSolutions\BestX\services\SODLoader\SODLoader.exe install SODLoader C:\SoftSolutions\BestX\services\SODLoader\startup.bat
C:\SoftSolutions\BestX\services\SODLoader\SODLoader.exe set SODLoader description "SODLoader"
C:\SoftSolutions\BestX\services\SODLoader\SODLoader.exe set SODLoader start SERVICE_DEMAND_START
C:\SoftSolutions\BestX\services\SODLoader\SODLoader.exe set SODLoader AppStopMethodConsole 30000

pause