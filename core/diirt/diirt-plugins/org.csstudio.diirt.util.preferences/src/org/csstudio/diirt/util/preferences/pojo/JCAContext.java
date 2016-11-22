/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.diirt.util.preferences.pojo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Plain Old Java Object representing the "jcaContext" element of a
 * {@code ca.xml} file.
 * <p>
 * The core parameter for Channel Access are configured through
 * the JCA Context. This includes what implementation of JCA is used
 * (JCA-JNI or CAJ-PureJava) and the configuration parameters for those
 * implementations.</p>
 * <p>
 * By default, CAJ is used with the default CAJ configuration.
 * CAJ should, by default, honor the standard EPICS environment variables
 * to configure the client. One can still override that configuration
 * by specifying the configuration properties here. Please, refer
 * to the JCA/CAJ instructions for details on these properties.</p>
 * <p>
 * We recommend to use the CAJ (pure java) implementation, as the JCA (JNI)
 * implementation currently lacks an official maintainer.</p>
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 18 Nov 2016
 */
@XmlType( name = "JCAContext" )
public class JCAContext {

    /**
     * The possible values for the {@link #monitorMask} property.
     */
    @XmlEnum
    public enum MonitorMask {

        /**
         * Corresponds to a monitor mask on both VALUE and ALARM.
         */
        VALUE,

        /**
         * Corresponds to a monitor mask on LOG.
         */
        ARCHIVE,

        /**
         * Corresponds to a monitor mask on ALARM.
         */
        ALARM,

        /**
         * A number corresponding to the mask itself.
         */
        CUSTOM

    }

}