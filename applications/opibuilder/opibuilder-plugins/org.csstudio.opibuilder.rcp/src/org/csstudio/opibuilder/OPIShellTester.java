package org.csstudio.opibuilder;

import org.csstudio.opibuilder.runmode.IOPIRuntime;
import org.csstudio.opibuilder.util.SingleSourceHelper;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.widgets.Shell;

public class OPIShellTester extends PropertyTester {

    /**
     * A property indicating whether the activeShell is an OPIShell (value <code>"isOPIShell"</code>).
     */
    private static final String IS_OPI_SHELL = "isOPIShell"; //$NON-NLS-1$

    @Override
    public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {

        boolean isOpiShell = false;

        if ((receiver instanceof Shell) && method.equals(IS_OPI_SHELL)) {
            Shell activeShell = (Shell)receiver;
            // getOPIShell will return null if the Shell passed as an argument is not
            // found in the managed collection of OPIShells.
            IOPIRuntime opiShell = SingleSourceHelper.getOPIShellForShell(activeShell);
            isOpiShell = (opiShell != null);
        }
        return isOpiShell;
    }

}
