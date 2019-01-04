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
package it.softsolutions.bestx.dao;


/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: davide.rossoni 
 * Creation date: 28/nov/2012 
 * 
 **/
public class InstrumentParamDaoTest {
 // TODO : la tabella InstrumentParam sembra non essere più necessaria, questi test e l'InstrumentParamDAO sono da togliere?    
    
    
//    private static InstrumentParamDao instrumentParamDao;
//    private static ClassPathXmlApplicationContext context;
//
//    @BeforeClass
//    public static void setUp() throws Exception {
//        context = new ClassPathXmlApplicationContext("cs-spring.xml");
//        instrumentParamDao = (InstrumentParamDao) context.getBean("hibernateInstrumentParamDao");
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void getInstrumentParamNullIsin() {
//        instrumentParamDao.getInstrumentParam(null, InstrumentParam.Type.SystematicInternalization, Rfq.OrderSide.BUY.name());
//    }
//    
//    @Test(expected = IllegalArgumentException.class)
//    public void getInstrumentParamNullType() {
//        instrumentParamDao.getInstrumentParam("IT0001976403", null, Rfq.OrderSide.BUY.name());
//    }
//
//    @Test
//    public void getInstrumentParamNullKey() {
//        InstrumentParam instrumentParam = instrumentParamDao.getInstrumentParam("IT0001976403", InstrumentParam.Type.SystematicInternalization, null);
//        assertNull(instrumentParam);
//    }
//    
//    @Test
//    public void getInstrumentParamInvalid() {
//        InstrumentParam instrumentParam = instrumentParamDao.getInstrumentParam("#INVALID#", InstrumentParam.Type.SystematicInternalization, null);
//        assertNull(instrumentParam);
//    }
//    
//    @Test
//    public void getInstrumentParamValue() {
//        String isin = "IT0001976403";
//        InstrumentParam.Type type = InstrumentParam.Type.SystematicInternalization;
//        String key = Rfq.OrderSide.BUY.name(); 
//        InstrumentParam instrumentParam = instrumentParamDao.getInstrumentParam(isin, type, key);
//        assertNotNull(instrumentParam);
//        assertEquals(isin, instrumentParam.getIsin());
//        assertEquals(type, instrumentParam.getType());
//        assertEquals(key, instrumentParam.getKey());
//        assertEquals("RABN", instrumentParam.getValue());
//    }
//    
//    @AfterClass
//    public static void tearDown() throws Exception {
//        context.close();
//    }
    
}
