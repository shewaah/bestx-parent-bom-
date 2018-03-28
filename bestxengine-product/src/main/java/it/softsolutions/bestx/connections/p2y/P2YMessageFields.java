package it.softsolutions.bestx.connections.p2y;

public class P2YMessageFields {
    public static final String P2Y_MSG_NAME = "XT2CRequestForQuantLibReq";
    public static final String P2Y_ACCRUED_RESP_NAME = "CalculateQLAccruedAmountResp";
    public static final String P2Y_NEXT_COUPON_RESP_NAME = "CalculateQLNextCouponDateResp";
    
    public static final String P2Y_LABEL_MSG_ID = "RequestID"; // input/output
    // output fields
    public static final String P2Y_LABEL_REQ_NAME = "RequestQLName";
    public static final String P2Y_LABEL_ISIN = "Isin";
    public static final String P2Y_LABEL_TICKER = "Ticker";
    public static final String P2Y_LABEL_CURRENCY = "Currency";
    public static final String P2Y_LABEL_CALENDAR_CODE = "CalendarCode";
    public static final String P2Y_LABEL_DAY_COUNT = "DayCount"; // UL
    public static final String P2Y_LABEL_DAY_COUNT_CODE = "DayCountCodeString"; // Es Act/Act
    public static final String P2Y_LABEL_COUPON = "Coupon"; // double
    public static final String P2Y_LABEL_PRINCIPAL = "Principal"; // UL
    public static final String P2Y_LABEL_MATURITY_DATE = "Maturity"; // UL YYYYMMDD
    public static final String P2Y_LABEL_ISSUE_DATE = "IssueDate"; // UL YYYYMMDD
    public static final String P2Y_LABEL_FIRST_COUPON_DATE = "FirstCouponDate"; // UL YYYYMMDD
    public static final String P2Y_LABEL_INTEREST_ACCRUAL_DATE = "InterestAccrualDate"; // UL YYYYMMDD
    public static final String P2Y_LABEL_FREQUENCY = "Frequency"; // Es 1Y
    public static final String P2Y_LABEL_END_OF_MONTH = "EndOfMonth"; // bool
    public static final String P2Y_LABEL_SETTLEMENT_DATE = "SettlementDate"; // UL YYYYMMDD
    public static final String P2Y_LABEL_EX_DIV_CALENDAR_CODE = "ExDivCalendarCode"; // same encoding as Calendar Code
    public static final String P2Y_LABEL_EX_DIV_DAYS = "ExDivDays"; //int
    //input fields
    public static final String P2Y_LABEL_ACCRUED_AMOUNT = "AccruedAmount"; // double
    public static final String P2Y_LABEL_ACCRUED_DAY_COUNT = "AccruedDayCount"; // UI
    public static final String P2Y_LABEL_ERROR_CODE = "ErrCode"; // int
    public static final String P2Y_LABEL_ERROR_MESSAGE = "ErrMsg";
    // values
    public static final String P2Y_VALUE_REQ_NAME_ACCRUED_AMOUNT = "CalculateQLAccruedAmount";
    public static final String P2Y_VALUE_REQ_NAME_NEXT_COUPON = "CalculateQLNextCoupon";
    public static final String P2Y_VALUE_REQ_NAME_INFLATION_RATIO = "CalculateInflationRatio";
    
    // inflation linked values
    public static final String P2Y_LABEL_CALCULATION_METHOD = "CalculationMethod";
    // values
    public static final String P2Y_VALUE_CALCULATION_METHOD_UK_I_L_BOND = "UK I/L BOND"; // metodo dopo il 2005
    public static final String P2Y_VALUE_CALCULATION_METHOD_INDEX_LINKED_FLOAT = "INDEX LINKED FLOAT"; // prima del 2005
    
    public static final String P2Y_LABEL_ISSUE_DATE_REFERENCE_RPI = "IssueDateReferenceRPI";
    public static final String P2Y_LABEL_CURRENT_REFERENCE_RPI = "CurrentReferenceRPI";
    
    public static final String P2Y_LABEL_ISSUE_DATE_2_MONTHS_RPI = "IssueDate2MonthsRPI";
    public static final String P2Y_LABEL_CURRENT_2_MONTHS_RPI = "Current2MonthsRPI";

    public static final String P2Y_LABEL_CALCULATED_INFLATION_RATIO = "CalculatedInflationRatio";
    public static final String P2Y_LABEL_CALCULATED_NEXT_COUPON = "CalculatedNextCoupon";
       
}
