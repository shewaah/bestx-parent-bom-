echo "Operation|Order.CONN|Order.Validation|PriceService.Out|Order.Execution_|Order.ExecutionReport_CONN" > BestX-Statistics.txt
for operation in `fgrep "Order.CONN" $1 | cut -d- -f4 | cut -d= -f2`
do
	opLoop=1
	row="$operation"
	list="Order.CONN Order.Validation PriceService.Out Order.Execution_ Order.ExecutionReport_CONN"
	for key in $list
	do
		result=`fgrep "$operation" $1 | grep "$key" | cut -d' ' -f2`
		#echo $result
		row="$row|"`echo $result | cut -d' ' -f1`
		#echo $row
	done
	echo $row >> BestX-Statistics.txt
done
