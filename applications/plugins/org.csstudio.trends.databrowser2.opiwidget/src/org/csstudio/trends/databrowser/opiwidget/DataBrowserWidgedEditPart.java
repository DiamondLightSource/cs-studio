/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser.opiwidget;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.opibuilder.editparts.AbstractWidgetEditPart;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.properties.IWidgetPropertyChangeHandler;
import org.csstudio.trends.databrowser.ui.Controller;
import org.eclipse.core.runtime.IPath;
import org.eclipse.draw2d.IFigure;

/** EditPart that interfaces between the {@link DataBrowserWidgetFigure} visible on the screen
 *  and the {@link DataBrowserWidgedModel} that stores the persistent configuration.
 *  <p>
 *  Life cycle:
 *  <ol>
 *  <li>doCreateFigure()
 *  <li>setExecutionMode(RUN/EDIT) - Ignored
 *  <li>activate()
 *  <li>deactivate()
 *  </ol>
 *  For now, it does NOT support execution mode changes while activated.
 *
 *  @author Kay Kasemir
 */
public class DataBrowserWidgedEditPart extends AbstractWidgetEditPart
{
    /** Data Browser controller for D.B. Model and Plot, used in run mode */
    private Controller controller = null;

    /** @return Casted widget model */
    @Override
    public DataBrowserWidgedModel getWidgetModel()
    {
        return (DataBrowserWidgedModel) getModel();
    }

    /** @return Casted widget figure */
    public DataBrowserWidgetFigure getWidgetFigure()
    {
        return (DataBrowserWidgetFigure) getFigure();
    }

    /** {@inheritDoc}} */
    @Override
    protected IFigure doCreateFigure()
    {
        final DataBrowserWidgedModel model = getWidgetModel();

        // Creating the figure/UI
        final boolean running = getExecutionMode() == ExecutionMode.RUN_MODE;
        // In edit mode, display the file name
        final DataBrowserWidgetFigure gui =
            new DataBrowserWidgetFigure(running ? null : model.getPlainFilename().toString(),
                    model.isToolbarVisible());

        if (running)
        {   // In run mode, create a controller
            try
            {
                // Connect plot to model (created by OPI/GEF)
                controller = new Controller(null, model.createDataBrowserModel(),
                        gui.getDataBrowserPlot());
            }
            catch (Exception ex)
            {
                Logger.getLogger(Activator.ID).log(Level.SEVERE, "Cannot run Data Browser", ex); //$NON-NLS-1$
            }
        }

        return gui;
    }

    /** {@inheritDoc}} */
    @Override
    protected void registerPropertyChangeHandlers()
    {
        // File name
        setPropertyChangeHandler(DataBrowserWidgedModel.PROP_FILENAME, new IWidgetPropertyChangeHandler()
        {
            @Override
            public boolean handleChange(final Object oldValue, final Object newValue, final IFigure figure)
            {
                // In edit mode, display file name
                if (getExecutionMode() == ExecutionMode.EDIT_MODE)
                    getWidgetFigure().setFilename(((IPath) newValue).toString());
                return false;
            }
        });

        // File name
        setPropertyChangeHandler(DataBrowserWidgedModel.PROP_SHOW_TOOLBAR, new IWidgetPropertyChangeHandler()
        {
            @Override
            public boolean handleChange(final Object oldValue, final Object newValue, final IFigure figure)
            {
                getWidgetFigure().setToolbarVisible((Boolean) newValue);
                return false;
            }
        });

    }

    /** {@inheritDoc}} */
    @Override
    public void activate()
    {
        // In run mode, start controller, which will start model
        if (getExecutionMode() == ExecutionMode.RUN_MODE)
        {
            try
            {
                if (controller != null  &&  !controller.isRunning())
                    controller.start();
            }
            catch (Exception ex)
            {
                Logger.getLogger(Activator.ID).log(Level.SEVERE, "Cannot start Data Browser Widget", ex); //$NON-NLS-1$
            }
        }
        super.activate();
    }

    /** {@inheritDoc}} */
    @Override
    public void deactivate()
    {
        // In run mode, stop the controller, which will stop the model
        if (getExecutionMode() == ExecutionMode.RUN_MODE)
        {
            if (controller != null  &&  controller.isRunning())
                controller.stop();
        }
        super.deactivate();
    }
}
