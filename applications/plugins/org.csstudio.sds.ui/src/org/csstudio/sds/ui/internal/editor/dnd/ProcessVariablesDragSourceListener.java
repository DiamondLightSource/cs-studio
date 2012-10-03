package org.csstudio.sds.ui.internal.editor.dnd;

import java.util.List;

import org.csstudio.csdata.ProcessVariable;
import org.csstudio.platform.model.pvs.IProcessVariableAddress;
import org.csstudio.ui.util.dnd.SerializableItemTransfer;
import org.eclipse.gef.EditPartViewer;

/**
 * Drag source listener for SDS that provides an array of {@link ProcessVariable}.
 * 
 * @author swende
 * 
 */
public class ProcessVariablesDragSourceListener extends AbstractDragSourceListener<ProcessVariable[]> {

	public ProcessVariablesDragSourceListener(EditPartViewer viewer) {
		super(viewer, SerializableItemTransfer.getTransfer(ProcessVariable[].class));
	}

	@Override
	protected ProcessVariable[] convert(IProcessVariableAddress mainAddress, List<IProcessVariableAddress> allAddresses) {
		ProcessVariable[] result = null;

		if (!allAddresses.isEmpty()) {
			int i = 0;
			result = new ProcessVariable[allAddresses.size()];
			for (IProcessVariableAddress address : allAddresses) {
				result[i++] = new ProcessVariable(address.getProperty());
			}
		}

		return result;
	}
}