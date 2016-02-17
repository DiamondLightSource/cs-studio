/*******************************************************************************
 * Copyright (c) 2013 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.simplepv;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.diirt.vtype.VType;
import org.diirt.vtype.ValueFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** JUnit test for VTypeHelper methods
 *
 *  @author Nick Battam
 */
@RunWith(Parameterized.class)
public class VTypeHelperTest
{

    private VType testValue;
    private VTypeHelperBean expectedData;

    public VTypeHelperTest(VType value, VTypeHelperBean data) {
        this.testValue = value;
        this.expectedData = data;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> vtypeInstances() {
       return Arrays.asList(new Object[][] {
          { null,
               new VTypeHelperBean(BasicDataType.UNKNOWN, Double.NaN, true) },
          { ValueFactory.newVByte(new Byte("4"),
                   ValueFactory.alarmNone(),
                   ValueFactory.timeNow(),
                   ValueFactory.displayNone()),
               new VTypeHelperBean(BasicDataType.BYTE, 4.0, true)},
          { ValueFactory.newVDouble(1.0),
               new VTypeHelperBean(BasicDataType.DOUBLE, 1.0, true)},
          { ValueFactory.newVEnum(1, Arrays.asList(new String[] {"zero", "one"}),
                  ValueFactory.alarmNone(),
                  ValueFactory.timeNow()),
               new VTypeHelperBean(BasicDataType.ENUM, 1.0, true) },
          { ValueFactory.newVFloat(0.5f,
                  ValueFactory.alarmNone(),
                  ValueFactory.timeNow(),
                  ValueFactory.displayNone()),
               new VTypeHelperBean(BasicDataType.FLOAT, 0.5, true) },
          { ValueFactory.newVInt(42,
                  ValueFactory.alarmNone(),
                  ValueFactory.timeNow(),
                  ValueFactory.displayNone()),
               new VTypeHelperBean(BasicDataType.INT, 42.0, true) },
          { ValueFactory.newVShort(new Short("21"),
                  ValueFactory.alarmNone(),
                  ValueFactory.timeNow(),
                  ValueFactory.displayNone()),
              new VTypeHelperBean(BasicDataType.SHORT, 21.0, true) },
          { ValueFactory.newVString("test",
                  ValueFactory.alarmNone(),
                  ValueFactory.timeNow()),
              new VTypeHelperBean(BasicDataType.STRING, Double.NaN, true) },
       });
    }

    @Test
    public void getBasicTypeDataTest() {
        assertThat(VTypeHelper.getBasicDataType(testValue), is(expectedData.btype));
    }

    @Test
    public void getDoubleReturnsDoubleReprOfValueIfNotSTRINGorUNKNOWN() {
        if (expectedData.btype != BasicDataType.UNKNOWN &&
                expectedData.btype != BasicDataType.STRING) {
            assertThat(VTypeHelper.getDouble(testValue), is(expectedData.dval));
        }
    }

    @Test
    public void getDoubleReturnsNaNIfTypeUNKNOWN() {
        if (expectedData.btype == BasicDataType.UNKNOWN) {
            assertThat(VTypeHelper.getDouble(testValue), is(Double.NaN));
        }
    }

    @Test
    public void getDoubleReturnsNaNIfTypeSTRING() {
        if (expectedData.btype == BasicDataType.STRING) {
            assertThat(VTypeHelper.getDouble(testValue), is(Double.NaN));
        }
    }

    @Test
    public void getSizeIsOneForScalarOtherwiseArrayLength() {
        if (expectedData.isScalar) {
            assertThat(VTypeHelper.getSize(testValue), is(1));
        }
    }

}
