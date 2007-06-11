/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
/*
 * $Id$
 */
package org.csstudio.platform.ui.internal.dataexchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.print.attribute.standard.Severity;

import org.csstudio.platform.data.IDoubleValue;
import org.csstudio.platform.data.IEnumeratedMetaData;
import org.csstudio.platform.data.IEnumeratedValue;
import org.csstudio.platform.data.IIntegerValue;
import org.csstudio.platform.data.IMetaData;
import org.csstudio.platform.data.INumericMetaData;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.IStringValue;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.data.ValueFactory;
import org.csstudio.platform.data.IValue.Quality;
import org.csstudio.platform.model.CentralItemFactory;
import org.csstudio.platform.model.IProcessVariableWithArchive;
import org.csstudio.platform.model.IProcessVariableWithSamples;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 *  * Drag-and-Drop transfer type for <code>IProcessVariableWithSample</code>.
 * <p>
 * This transfer type expects the data to transfer to implement the
 * <code>IProcessVariableWithSample</code> interface, and the resulting data
 * is provided as an array of <code>ProcessVariableWithSample</code>.
 * <p>
 * Most of this implementation is from the javadoc for ByteArrayTransfer.
 *
 *---------------------------------------------------------
 * Order of sending and reciving elements
 *  *  - Header
 *  0. PV Name [String]
 *  1. TypeID [String]
 *  2. Number of Samples [int]
 *  - IValues
 *  3. ValueTyp [enum ValueTyp]
 *  4. Format [String]
 *  -- MetaData
 *  5. MetaDataType [enum MetaData]
 *  --- Nummeric
 *  6. Precision [int]
 *  7. Units [String]
 *  8. AlarmHigh [double]
 *  9. AlarmLow [double]
 * 10. DisplayHigh [double]
 * 11. DisplayLow [double]
 * 12. WarnHigh [double]
 * 13. WarnLow [double]
 * --- Enumerte
 *  6. Size of Enumerate [int]
 *  7- All Enumerates Status elements [enum Enumerate]
 * -- Quality
 * 14.Quality Name [String]
 * 15.      -- deleted --
 * -- Serverity
 * 16.    -- deleted --
 * 17. isInvalid [boolean]
 * 18. isMajor [boolean]
 * 19. isMinor [boolean]
 * 20. isOK [boolean]
 * --
 * 21. Status [String]
 * -- Timestamp
 * 22. seconds [long]
 * 23. nanoseconds [long]
 * -- Values
 * --- Double
 * 24. size [int]
 * 25- value [double]
 * --- int
 * 24. size [int]
 * 25- value [int]
 * --- enum
 * 24. size [int]
 * 25- value [int]
 * --- String
 * 24. value [String]
 * 
 *  ----------------------------------------------------
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 06.06.2007
 */
public final class ProcessVariableWithSamplesTransfer extends ByteArrayTransfer {
    /**
     * 
     */
    private static final String TYPE_NAME = "pv_with_samples_data";
    /**
     *
     */
    private static final int TYPE_ID = registerType(TYPE_NAME);
    /**
     * 
     */
    private static ProcessVariableWithSamplesTransfer _singletonInstance = new ProcessVariableWithSamplesTransfer();

    /**
     * 
     * @author hrickens
     * @author $Author$
     * @version $Revision$
     * @since 06.06.2007
     */
     private static enum ValueTyp {
         /**
          * Double Value Typ.
          */
         Double,
         /**
          * String Value Typ.
          */
         String,
         /**
          * IValue Typ.
          */
         IValue,
         /**
          * Integer Value Typ.
          */
         Integer,
         /**
          * Enumerated Value Typ.
          */
         Enumerated
     }
    /**
     * 
     * @author hrickens
     * @author $Author$
     * @version $Revision$
     * @since 06.06.2007
     */
    private static enum MetaData {
        /**
         * Enumerate MetaData Typ. 
         */
        Enumerate,
        /**
         * Numeric MetaData Typ. 
         */
        Numeric
    }
             
    /**
     * The Byte Outputstream.
     */
    private ByteArrayOutputStream _out;
    /**
     * The Data Outputstream.
     */
    private DataOutputStream _writeOut;
    /**
     * 
     */
    private ProcessVariableWithSamplesTransfer() {
        System.out.println("PVWSTs"+TYPE_ID);
    }

    /** @return The singleton instance of the ArchiveDataSourceTransfer. */
    public static ProcessVariableWithSamplesTransfer getInstance() {
        return _singletonInstance;
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
     */
    /**
     * @return TypeID 
     */
    @Override
    protected int[] getTypeIds() {
        return new int[] { TYPE_ID };
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
     */
    /**
     * @return Type Name
     */
    @Override
    protected String[] getTypeNames() {
        return new String[] { TYPE_NAME };
    }

    /**
     * @param object Transfer Object
     * @param transferData Transfer Data Typ 
     * 
     */
    @Override
    public void javaToNative(final Object object, final TransferData transferData) {
        if (!isSupportedType(transferData)) {
            return;
        }

        IProcessVariableWithSamples[] data;
        if (object == null) {
            return;
        }
        if (object instanceof IProcessVariableWithSamples[]){
            data = (IProcessVariableWithSamples[]) object;
        } else if (object instanceof IProcessVariableWithSamples){
            data = new IProcessVariableWithSamples[]{(IProcessVariableWithSamples) object};
        }else if(object instanceof IProcessVariableWithSamples){
            data = new IProcessVariableWithSamples[] {((IProcessVariableWithSamples) object)};
        }else if(object instanceof ArrayList){
            ArrayList al = (ArrayList) object;
            data = new IProcessVariableWithSamples[al.size()];
            for (int i=0;i<al.size();i++) {
                if (al.get(i) instanceof IProcessVariableWithSamples) {
                    IProcessVariableWithSamples pvws = (IProcessVariableWithSamples) al.get(i);
                    data[i]=pvws;
                }
            }
        }else {
            return;
        }
        _out = new ByteArrayOutputStream();
        _writeOut = new DataOutputStream(_out);
        for (IProcessVariableWithSamples samples : data) {
            IValue[] values = samples.getSamples();
            fillHeader(samples);
            for (IValue value : values) {
                ValueTyp valueTyp;
                if (value instanceof IDoubleValue) {
                    valueTyp = ValueTyp.Double;
                    fillValue(value, valueTyp);
                    IDoubleValue doubleValue = (IDoubleValue) value;
                    double[] dValues = doubleValue.getValues();
                    send(dValues.length);
                    for (double d : dValues) {
                        send(d);
                    }
                    
                }else if (value instanceof IStringValue) {
                    valueTyp = ValueTyp.String;
                    fillValue(value, valueTyp);
                    IStringValue stringValue = (IStringValue) value;
                    send(stringValue.getValue());
                }else if (value instanceof IIntegerValue) {
                    valueTyp = ValueTyp.Integer;
                    fillValue(value, valueTyp);
                    IIntegerValue integerValue = (IIntegerValue) value;
                    int[] ints = integerValue.getValues();
                    send(ints.length);
                    for (int i : ints) {
                        send(i);
                    }
                }else if (value instanceof IEnumeratedValue) {
                    valueTyp = ValueTyp.Enumerated;
                    fillValue(value, valueTyp);
                    IEnumeratedValue enumValue = (IEnumeratedValue) value;
                    int[] enums = enumValue.getValues();
                    send(enums.length);
                    for (int i : enums) {
                        send(i);
                    }
                }else {
                    valueTyp = ValueTyp.IValue;
                    fillValue(value, valueTyp);
                }
            }
            byte[] buffer = _out.toByteArray();
            try {
                _writeOut.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            super.javaToNative(buffer, transferData);
        }
    }
    
    /**
     * @param value the IValue
     * @param valueTyp Value Typ
     */
    private void fillValue(final IValue value, final ValueTyp valueTyp){
        send(valueTyp.name());
        send(value.format());
        // MetaDate
        IMetaData md = value.getMetaData();
        if (md instanceof INumericMetaData) {
            INumericMetaData nmd = (INumericMetaData) md;
            send(MetaData.Numeric.name());
            send(nmd.getPrecision());
            send(nmd.getUnits());
            send(nmd.getAlarmHigh());
            send(nmd.getAlarmLow());
            send(nmd.getDisplayHigh());
            send(nmd.getDisplayLow());
            send(nmd.getWarnHigh());
            send(nmd.getWarnLow());
        }else if (md instanceof IEnumeratedMetaData) {
            IEnumeratedMetaData emd = (IEnumeratedMetaData) md;
            send(MetaData.Enumerate.name());
            String[] states = emd.getStates();
            send(states.length);
            for (String string : states) {
                send(string);
            }
        }
        // Quality 
        Quality quality = value.getQuality();
        send(quality.name());
        // Severity
        ISeverity serv = value.getSeverity();
        send(serv.isInvalid());
        send(serv.isMajor());
        send(serv.isMinor());
        send(serv.isOK());
        //
        send(value.getStatus());
        // TimeStamp
        send(value.getTime().seconds());
        send(value.getTime().nanoseconds());
    }


    /**
     * @param samples Samples 
     */
    private void fillHeader(final IProcessVariableWithSamples samples){
        send(samples.getName());
        send(samples.getTypeId());
        send(samples.size());
    }
    
    /**
     * @param transferData recived Data
     * @return an Array of IProcessVariableWithSamples
     */
    @Override
    public Object nativeToJava(final TransferData transferData) {
        if (!isSupportedType(transferData)) {
            return null;
        }

        byte[] buffer = (byte[]) super.nativeToJava(transferData);
        if (buffer == null) {
            return null;
        }

        Vector<IProcessVariableWithSamples> received = new Vector<IProcessVariableWithSamples>();
        
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(buffer);
            DataInputStream readIn = new DataInputStream(in);
            // URL length, key, name length = 12?
            while (readIn.available() > 1) {
                String pvName = getString(readIn);
                String typeId = getString(readIn);
                int size = readIn.readInt();
                IValue[] values = new IValue[size];
                for(int i = 0; i<size;i++){ 
                    String temp = getString(readIn);
                    ValueTyp valueType = ValueTyp.valueOf(temp);
                    String format = getString(readIn);
                    MetaData metaData = MetaData.valueOf(getString(readIn));
                    INumericMetaData inmd = null;
                    IEnumeratedMetaData iemd = null;
                    switch (metaData) {
                        case Numeric:
                            int prec = readIn.readInt();
                            String units = getString(readIn);
                            double alarmHigh = readIn.readDouble();
                            double alarmLow = readIn.readDouble();
                            double dispHigh = readIn.readDouble();
                            double dispLow = readIn.readDouble();
                            double warnLow = readIn.readDouble();
                            double warnHigh = readIn.readDouble();
                            inmd =  ValueFactory.createNumericMetaData(dispLow, dispHigh, warnLow, warnHigh, alarmLow, alarmHigh, prec, units);
                            break;
                        case Enumerate:
                            String[] states = new String[readIn.readInt()];
                            for (int j=0;j<states.length;j++) {
                                states[j] = getString(readIn);
                            }
                            iemd = ValueFactory.createEnumeratedMetaData(states);
                            break;
                        default:
                            break;
                    }
                    Quality quality = Quality.valueOf(getString(readIn));
                    ISeverity severity = null;
                    if(readIn.readBoolean()){
                        severity = ValueFactory.createInvalidSeverity();
                    }
                    if(readIn.readBoolean()){
                        severity = ValueFactory.createMajorSeverity();
                    }
                    if(readIn.readBoolean()){
                        severity = ValueFactory.createMinorSeverity();
                    }
                    if(readIn.readBoolean()){
                        severity = ValueFactory.createOKSeverity();
                    }
                    String status = getString(readIn);
                    
                    ITimestamp time = TimestampFactory.createTimestamp(readIn.readLong(), readIn.readLong());
                    switch(valueType){
                        case Double:
                            int valueSize = readIn.readInt();
                            double[] dValues = new double[valueSize];
                            for(int j=0;j<valueSize;j++){
                                dValues[j] = readIn.readDouble();
                            }
                            values[i] = ValueFactory.createDoubleValue(time, severity, status, inmd, quality, dValues);
                            break;
                        case Integer:
                            valueSize = readIn.readInt();
                            int[] iValues = new int[valueSize];
                            for(int j=0;j<valueSize;j++){
                                iValues[j] = readIn.readInt();
                            }
                            values[i] = ValueFactory.createIntegerValue(time, severity, status, inmd, quality, iValues);
                            break;
                        case String:
                            values[i] = ValueFactory.createStringValue(time, severity, status, quality,getString(readIn));
                            break;
                        case Enumerated:
                            valueSize = readIn.readInt();
                            iValues = new int[valueSize];
                            for(int j=0;j<valueSize;j++){
                                iValues[j] = readIn.readInt();
                            }
                            values[i] = ValueFactory.createEnumeratedValue(time, severity, status, iemd, quality, iValues);
                            break;
                        case IValue:
                        default:
                            break;
                    }
                }
                received.add(CentralItemFactory.createProcessVariableWithSamples(pvName, values));
            }
        }catch(IOException e){
            //TODO send Logmessage
            return null;
        }
        return received.toArray(new IProcessVariableWithSamples[received.size()]);
    }
    
    /**
     * @param readIn Inputstream
     * @return String
     * @throws IOException Imputstream Exception
     */
    private String getString(final DataInputStream readIn) throws IOException {
        int size = readIn.read();
        byte[] bytes = new byte[size];
        readIn.read(bytes);
        return new String(bytes);
    }

    /**
     * @param string write to sendStream 
     */
    private void send(final String string) {
        send(string.getBytes());
    }
    /**
     * @param integer write to sendStream 
     */
    private void send(final int integer) {
        try {
            _writeOut.writeInt(integer);
        } catch (IOException e) {
            // TODO Generate a Log Message
        }
    }

    /**
     * @param lonk write to sendStream 
     */
    private void send(final long lonk) {
        try {
            _writeOut.writeLong(lonk);
        } catch (IOException e) {
            // TODO Generate a Log Message        
        }
    }

    /**
     * @param doub write to sendStream
     */
    private void send(final double doub) {
        try {
            _writeOut.writeDouble(doub);
        } catch (IOException e) {
            // TODO Generate a Log Message
        }
    }

    /**
     * @param bool write to sendStream
     */
    private void send(final boolean bool) {
        try {
            _writeOut.writeBoolean(bool);
        } catch (IOException e) {
            // TODO Generate a Log Message        
        }
    }
    
    /**
     * @param buffer write to sendStream 
     */
    private void send(final byte[] buffer) {
        try {
            _writeOut.writeInt(buffer.length);
            _writeOut.write(buffer);
        } catch (IOException e) {

        }
    }

}
