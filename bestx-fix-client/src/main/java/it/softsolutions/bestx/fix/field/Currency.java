/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */
package it.softsolutions.bestx.fix.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: Jun 11, 2012 
 * 
 **/
public enum Currency {
    Unknown("NONE"),
    AFA("AFA"),  // Afghani
    DZD("DZD"),  // Algerian Dinar
    ADP("ADP"),  // Andorran Peseta
    ARS("ARS"),  // Argentine Peso
    AMD("AMD"),  // Armenian Dram
    AWG("AWG"),  // Aruban Guilder
    AUD("AUD"),  // Australian Dollar
    AZM("AZM"),  // Azerbaijanian Manat
    BSD("BSD"),  // Bahamian Dollar
    BHD("BHD"),  // Bahraini Dinar
    THB("THB"),  // Baht
    PAB("PAB"),  // Balboa
    BBD("BBD"),  // Barbados Dollar
    BYB("BYB"),  // Belarussian Ruble
    BEF("BEF"),  // Belgian Franc
    BZD("BZD"),  // Belize Dollar
    BMD("BMD"),  // Bermudian Dollar
    VEB("VEB"),  // Bolivar
    BOB("BOB"),  // Boliviano
    BRL("BRL"),  // Brazilian Real
    BND("BND"),  // Brunei Dollar
    BIF("BIF"),  // Burundi Franc
    XOF("XOF"),  // CFA Franc BCEAO+
    XAF("XAF"),  // CFA Franc BEAC#
    XPF("XPF"),  // CFP Franc
    CAD("CAD"),  // Canadian Dollar
    CVE("CVE"),  // Cape Verde Escudo
    KYD("KYD"),  // Cayman Islands Dollar
    GHC("GHC"),  // Cedi
    CLP("CLP"),  // Chilean Peso
    COP("COP"),  // Colombian Peso
    KMF("KMF"),  // Comoro Franc
    BAM("BAM"),  // Convertible Marks
    NIO("NIO"),  // Cordoba Oro
    CRC("CRC"),  // Costa Rican Colon
    CUP("CUP"),  // Cuban Peso
    CYP("CYP"),  // Cyprus Pound
    CZK("CZK"),  // Czech Koruna
    GMD("GMD"),  // Dalasi
    DKK("DKK"),  // Danish Krone
    MKD("MKD"),  // Denar
    DEM("DEM"),  // Deutsche Mark
    DJF("DJF"),  // Djibouti Franc
    STD("STD"),  // Dobra
    DOP("DOP"),  // Dominican Peso
    VND("VND"),  // Dong
    GRD("GRD"),  // Drachma
    XCD("XCD"),  // East Caribbean Dollar
    EGP("EGP"),  // Egyptian Pound
    SVC("SVC"),  // El Salvador Colon
    ETB("ETB"),  // Ethiopian Birr
    EUR("EUR"),  // Euro
    FKP("FKP"),  // Falkland Islands Pound
    FJD("FJD"),  // Fiji Dollar
    HUF("HUF"),  // Forint
    CDF("CDF"),  // Franc Congolais
    FRF("FRF"),  // French Franc
    GIP("GIP"),  // Gibraltar Pound
    HTG("HTG"),  // Gourde
    PYG("PYG"),  // Guarani
    GNF("GNF"),  // Guinea Franc
    GWP("GWP"),  // Guinea-Bissau Peso
    GYD("GYD"),  // Guyana Dollar
    HKD("HKD"),  // Hong Kong Dollar
    UAH("UAH"),  // Hryvnia
    ISK("ISK"),  // Iceland Krona
    INR("INR"),  // Indian Rupee
    IRR("IRR"),  // Iranian Rial
    IQD("IQD"),  // Iraqi Dinar
    IEP("IEP"),  // Irish Pound
    ITL("ITL"),  // Italian Lira
    JMD("JMD"),  // Jamaican Dollar
    JOD("JOD"),  // Jordanian Dinar
    KES("KES"),  // Kenyan Shilling
    PGK("PGK"),  // Kina
    LAK("LAK"),  // Kip
    EEK("EEK"),  // Kroon
    HRK("HRK"),  // Kuna
    KWD("KWD"),  // Kuwaiti Dinar
    MWK("MWK"),  // Kwacha
    ZMK("ZMK"),  // Kwacha
    AOR("AOR"),  // Kwanza Reajustado
    MMK("MMK"),  // Kyat
    GEL("GEL"),  // Lari
    LVL("LVL"),  // Latvian Lats
    LBP("LBP"),  // Lebanese Pound
    ALL("ALL"),  // Lek
    HNL("HNL"),  // Lempira
    SLL("SLL"),  // Leone
    ROL("ROL"),  // Leu
    BGL("BGL"),  // Lev
    LRD("LRD"),  // Liberian Dollar
    LYD("LYD"),  // Libyan Dinar
    SZL("SZL"),  // Lilangeni
    LTL("LTL"),  // Lithuanian Litas
    LSL("LSL"),  // Loti
    LUF("LUF"),  // Luxembourg Franc
    MGF("MGF"),  // Malagasy Franc
    MYR("MYR"),  // Malaysian Ringgit
    MTL("MTL"),  // Maltese Lira
    TMM("TMM"),  // Manat
    FIM("FIM"),  // Markka
    MUR("MUR"),  // Mauritius Rupee
    MZM("MZM"),  // Metical
    MXN("MXN"),  // Mexican Peso
    MXV("MXV"),  // Mexican Unidad de Inversion (UDI)
    MDL("MDL"),  // Moldovan Leu
    MAD("MAD"),  // Moroccan Dirham
    BOV("BOV"),  // Mvdol
    NGN("NGN"),  // Naira
    ERN("ERN"),  // Nakfa
    NAD("NAD"),  // Namibia Dollar
    NPR("NPR"),  // Nepalese Rupee
    ANG("ANG"),  // Netherlands Antillian Guilder
    NLG("NLG"),  // Netherlands Guilder
    YUM("YUM"),  // New Dinar
    ILS("ILS"),  // New Israeli Sheqel
    AON("AON"),  // New Kwanza
    TWD("TWD"),  // New Taiwan Dollar
    ZRN("ZRN"),  // New Zaire
    NZD("NZD"),  // New Zealand Dollar
    USN("USN"),  // Next day
    BTN("BTN"),  // Ngultrum
    KPW("KPW"),  // North Korean Won
    NOK("NOK"),  // Norwegian Krone
    PEN("PEN"),  // Nuevo Sol
    MRO("MRO"),  // Ouguiya
    TOP("TOP"),  // Pa("anga
    PKR("PKR"),  // Pakistan Rupee
    MOP("MOP"),  // Pataca
    UYU("UYU"),  // Peso Uruguayo
    PHP("PHP"),  // Philippine Peso
    PTE("PTE"),  // Portuguese Escudo
    GBP("GBP"),  // Pound Sterling
    BWP("BWP"),  // Pula
    QAR("QAR"),  // Qatari Rial
    GTQ("GTQ"),  // Quetzal
    ZAR("ZAR"),  // Rand
    OMR("OMR"),  // Rial Omani
    KHR("KHR"),  // Riel
    MVR("MVR"),  // Rufiyaa
    IDR("IDR"),  // Rupiah
    RUB("RUB"),  // Russian Ruble
    RUR("RUR"),  // Russian Ruble
    RWF("RWF"),  // Rwanda Franc
    XDR("XDR"),  // SDR
    USS("USS"),  // Same day
    SAR("SAR"),  // Saudi Riyal
    ATS("ATS"),  // Schilling
    SCR("SCR"),  // Seychelles Rupee
    SGD("SGD"),  // Singapore Dollar
    SKK("SKK"),  // Slovak Koruna
    SBD("SBD"),  // Solomon Islands Dollar
    KGS("KGS"),  // Som
    SOS("SOS"),  // Somali Shilling
    ESP("ESP"),  // Spanish Peseta
    LKR("LKR"),  // Sri Lanka Rupee
    SHP("SHP"),  // St Helena Pound
    ECS("ECS"),  // Sucre
    SDD("SDD"),  // Sudanese Dinar
    SRG("SRG"),  // Surinam Guilder
    SEK("SEK"),  // Swedish Krona
    CHF("CHF"),  // Swiss Franc
    SYP("SYP"),  // Syrian Pound
    TJR("TJR"),  // Tajik Ruble
    BDT("BDT"),  // Taka
    WST("WST"),  // Tala
    TZS("TZS"),  // Tanzanian Shilling
    KZT("KZT"),  // Tenge
    TPE("TPE"),  // Timor Escudo
    SIT("SIT"),  // Tolar
    TTD("TTD"),  // Trinidad and Tobago Dollar
    MNT("MNT"),  // Tugrik
    TND("TND"),  // Tunisian Dinar
    TRL("TRL"),  // Turkish Lira
    AED("AED"),  // UAE Dirham
    USD("USD"),  // US Dollar
    UGX("UGX"),  // Uganda Shilling
    ECV("ECV"),  // Unidad de Valor Constante (UVC)
    CLF("CLF"),  // Unidades de fomento
    UZS("UZS"),  // Uzbekistan Sum
    VUV("VUV"),  // Vatu
    KRW("KRW"),  // Won
    YER("YER"),  // Yemeni Rial
    JPY("JPY"),  // Yen
    CNY("CNY"),  // Yuan Renminbi
    ZWD("ZWD"),  // Zimbabwe Dollar
    PLN("PLN"),  // Zloty
    ZAL("ZAL"),  // financial Rand
    VEF("VEF"),  // Bolivar
    TRY("TRY"),  // Turkish Lira
    ZWN("ZWN"),  // Zimbabwe Dollar
    // -- missing currencies added 
    BGN("BGN"),  // Bulgarian Lev
    GBX("GBX"),  // Pence Sterling
    ;
    
    public String getFIXValue() {
        return mFIXValue;
    }

    public static Currency getInstanceForFIXValue(String inFIXValue) {
        if(inFIXValue == null) {
            return Unknown;
        }
        Currency currency = mFIXValueMap.get(inFIXValue);
        return currency == null
                ? Unknown
                : currency;
    }

    private Currency(String inFIXValue) {
        mFIXValue = inFIXValue;
    }
    private final String mFIXValue;
    private static final Map<String, Currency> mFIXValueMap;
    static {
        Map<String, Currency> table = new HashMap<String, Currency>();
        for(Currency currency: values()) {
            table.put(currency.getFIXValue(),currency);
        }
        mFIXValueMap = Collections.unmodifiableMap(table);
    }
    
}
