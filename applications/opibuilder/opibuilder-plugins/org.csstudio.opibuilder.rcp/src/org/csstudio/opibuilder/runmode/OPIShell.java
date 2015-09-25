package org.csstudio.opibuilder.runmode;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.opibuilder.OPIBuilderPlugin;
import org.csstudio.opibuilder.actions.RefreshOPIAction;
import org.csstudio.opibuilder.datadefinition.NotImplementedException;
import org.csstudio.opibuilder.editparts.ExecutionMode;
import org.csstudio.opibuilder.editparts.WidgetEditPartFactory;
import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.persistence.XMLUtil;
import org.csstudio.opibuilder.util.MacrosInput;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.opibuilder.util.SingleSourceHelper;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.services.IServiceLocator;

/**
 * An OPIShell is a CS-Studio OPI presented in an SWT shell, which allows
 * more free integration with the host operating system.  In most ways
 * it behaves like an OPIView.
 *
 * All OPIShells are maintained in a static set within this class.
 *
 * In order for the OPIShell to be integrated with Eclipse functionality,
 * in particular the right-click context menu, it needs to be registered
 * against an existing IViewPart.
 *
 * @author Will Rogers, Matthew Furseman
 *
 */
public class OPIShell implements IOPIRuntime {

    private static Logger log = OPIBuilderPlugin.getLogger();
    public static final String OPI_SHELLS_CHANGED_ID = "org.csstudio.opibuilder.opiShellsChanged";
    // Cache of open OPI shells in order of opening.
    private static final Set<OPIShell> openShells = new LinkedHashSet<OPIShell>();
    // The view against which the context menu is registered.
    private IViewPart view;

    private final Image icon;
    private final Shell shell;
    private final IPath path;
    // macrosInput should not be null.  If there are no macros it should
    // be an empty MacrosInput object.
    private final MacrosInput macrosInput;
    private final ActionRegistry actionRegistry;
    private final GraphicalViewer viewer;
    private DisplayModel displayModel;

    // Private constructor means you can't open an OPIShell without adding
    // it to the cache.
    private OPIShell(Display display, IPath path, MacrosInput macrosInput) {

        this.icon = OPIBuilderPlugin
                .imageDescriptorFromPlugin(OPIBuilderPlugin.PLUGIN_ID, "icons/OPIRunner.png")
                .createImage(display);
        this.path = path;
        this.macrosInput = macrosInput;
        this.shell = new Shell(display);
        this.shell.setImage(icon);
        this.displayModel = new DisplayModel(path);
        this.displayModel.setOpiRuntime(this);
        this.actionRegistry = new ActionRegistry();

        viewer = new GraphicalViewerImpl();
        viewer.createControl(shell);
        viewer.setEditPartFactory(new WidgetEditPartFactory(ExecutionMode.RUN_MODE));
        viewer.setRootEditPart(new ScalableFreeformRootEditPart() {
            @Override
            public DragTracker getDragTracker(Request req) {
                return new DragEditPartsTracker(this);
            }
            @Override
            public boolean isSelectable() {
                return false;
            }
        });

        EditDomain editDomain = new EditDomain() {
            @Override
            public void loadDefaultTool() {
                setActiveTool(new RuntimePatchedSelectionTool());
            }
        };
        editDomain.addViewer(viewer);

        try {
            displayModel = createDisplayModel(path, macrosInput, viewer);
            setTitle();

            shell.setLayout(new FillLayout());
            shell.addShellListener(new ShellListener() {
                private boolean firstRun = true;
                public void shellIconified(ShellEvent e) {}
                public void shellDeiconified(ShellEvent e) {}
                public void shellDeactivated(ShellEvent e) {}
                public void shellClosed(ShellEvent e) {
                    // Remove this shell from the cache.
                    openShells.remove(OPIShell.this);
                    sendUpdateCommand();
                }
                public void shellActivated(ShellEvent e) {
                    if (firstRun) {
                        // Resize the shell after it's open, so we can take into account different window borders.
                        // Do this only the first time it's activated.
                        resizeToContents();
                        shell.setFocus();
                        firstRun = false;
                    }
                }
            });
            shell.addDisposeListener(new DisposeListener() {

                @Override
                public void widgetDisposed(DisposeEvent e) {
                    if (!icon.isDisposed()) icon.dispose();
                }
            });
            shell.pack();
            if (!displayModel.getLocation().equals(DisplayModel.NULL_LOCATION)) {
                shell.setLocation(displayModel.getLocation().getSWTPoint());
            }
            /*
             * Don't open the Shell here, as it causes SWT to think the window is on top when it really isn't.
             * Wait until the window is open, then call shell.setFocus() in the activated listener.
             *
             * Make some attempt at sizing the shell, sometimes a shell is not given focus and the shellActivated
             * listener callback doesn't resize the window. It's better to have something a little too large as the
             * default. Related to Eclipse bug 96700.
             */
            int windowBorderX = 30;
            int windowBorderY = 30;
            shell.setSize(displayModel.getSize().width + windowBorderX, displayModel.getSize().height + windowBorderY);
            shell.setVisible(true);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create new OPIShell.", e);
        }
    }

    /**
     * In order for the right-click menu to work, this shell must be registered
     * with a view.  Register the context menu against the view.
     * Make the view the default.
     * @param view
     */
    public void registerWithView(IViewPart view) {
        this.view = view;
        actionRegistry.registerAction(new RefreshOPIAction(this));
        SingleSourceHelper.registerRCPRuntimeActions(actionRegistry, this);
        OPIRunnerContextMenuProvider contextMenuProvider = new OPIRunnerContextMenuProvider(viewer, this);
        getSite().registerContextMenu(contextMenuProvider, viewer);
        viewer.setContextMenu(contextMenuProvider);
    }

    public MacrosInput getMacrosInput() {
        return macrosInput;
    }

    public IPath getPath() {
        return path;
    }

    public void raiseToTop() {
        shell.forceFocus();
        shell.forceActive();
        shell.setFocus();
        shell.setActive();
    }

    @Override
    public boolean equals(Object o) {
        boolean equal = false;
        if (o instanceof OPIShell) {
            OPIShell opiShell = (OPIShell) o;
            equal = opiShell.getMacrosInput().equals(this.getMacrosInput());
            equal &= opiShell.getPath().equals(this.path);
        }
        return equal;
    }

    public void close() {
        shell.close();
        dispose();
    }

    private DisplayModel createDisplayModel(IPath path, MacrosInput macrosInput, GraphicalViewer viewer)
            throws Exception {
        displayModel = new DisplayModel(path);
        XMLUtil.fillDisplayModelFromInputStream(ResourceUtil.pathToInputStream(path), displayModel);
        if(macrosInput != null) {
            macrosInput = macrosInput.getCopy();
            macrosInput.getMacrosMap().putAll(displayModel.getMacrosInput().getMacrosMap());
            displayModel.setPropertyValue(AbstractContainerModel.PROP_MACROS, macrosInput);
        }
        viewer.setContents(displayModel);
        displayModel.setViewer(viewer);
        displayModel.setOpiRuntime(this);
        return displayModel;
    }

    private void setTitle() {
        if (displayModel.getName() != null && displayModel.getName().trim().length() > 0) {
            shell.setText(displayModel.getName());
        } else { // If the name doesn't exist, use the OPI path
            shell.setText(path.toString());
        }
    }

    private void resizeToContents() {
        int frameX = shell.getSize().x - shell.getClientArea().width;
        int frameY = shell.getSize().y - shell.getClientArea().height;
        shell.setSize(displayModel.getSize().width + frameX, displayModel.getSize().height + frameY);
    }

    /*************************************************************
     * Static helper methods to manage open shells.
     *************************************************************/

    /**
     * This is the only way to create an OPIShell
     */
    public static void openOPIShell(IPath path, MacrosInput macrosInput) {
        if (macrosInput == null) {
            macrosInput = new MacrosInput(new LinkedHashMap<String, String>(), false);
        }
        try {
            boolean alreadyOpen = false;
            for (OPIShell opiShell : openShells) {
                if (opiShell.getPath().equals(path) && opiShell.getMacrosInput().equals(macrosInput)) {
                    opiShell.raiseToTop();
                    alreadyOpen = true;
                    break;
                }
            }
            if (!alreadyOpen) {
                OPIShell os = new OPIShell(Display.getCurrent(), path, macrosInput);
                openShells.add(os);
                sendUpdateCommand();
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to open OPI shell", e);
        }

    }

    /**
     * Close all open OPIShells.  Use getAllShells() for a copy
     * of the set, to avoid removing items during iteration.
     */
    public static void closeAll() {
        for (OPIShell s : getAllShells()) {
            s.close();
        }
    }

    /** Search the cache of open OPIShells to find a match for the
     *  input Shell object.
     *
     *     Return associated OPIShell or Null if none found
     */
    public static OPIShell getOPIShellForShell(final Shell target) {
        OPIShell foundShell = null;
        if (target != null) {
            for (OPIShell os : openShells) {
                if (os.shell == target) {
                    foundShell = os;
                    break;
                }
            }
        }
        return foundShell;
    }

    /**
     * Return a copy of the set of open shells.  Returning the same
     * instance may lead to problems when closing shells.
     * @return a copy of the set of open shells.
     */
    public static Set<OPIShell> getAllShells() {
        return new LinkedHashSet<OPIShell>(openShells);
    }

    /**
     * Alert whoever is listening that a new OPIShell has been created.
     */
    private static void sendUpdateCommand() {
        IServiceLocator serviceLocator = PlatformUI.getWorkbench();
        ICommandService commandService = (ICommandService) serviceLocator.getService(ICommandService.class);
        try {
            Command command = commandService.getCommand(OPI_SHELLS_CHANGED_ID);
            command.executeWithChecks(new ExecutionEvent());
        } catch (ExecutionException | NotHandledException | NotEnabledException | NotDefinedException e) {
            log.log(Level.WARNING, "Failed to send OPI shells changed command", e);
        }
    }

    /********************************************
     * Partial implementation of IOPIRuntime
     ********************************************/
    @Override
    public void addPropertyListener(IPropertyListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void createPartControl(Composite parent) {
        throw new NotImplementedException();
    }

    @Override
    public void dispose() {
        shell.dispose();
        actionRegistry.dispose();
    }

    @Override
    public IWorkbenchPartSite getSite() {
        if (view != null) {
            return view.getSite();
        } else {
            return null;
        }
    }

    @Override
    public String getTitle() {
        return shell.getText();
    }

    @Override
    public Image getTitleImage() {
        throw new NotImplementedException();
    }

    @Override
    public String getTitleToolTip() {
        return shell.getToolTipText();
    }

    @Override
    public void removePropertyListener(IPropertyListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void setFocus() {
        throw new NotImplementedException();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == ActionRegistry.class)
            return this.actionRegistry;
        if (adapter == GraphicalViewer.class)
            return this.viewer;
        return null;
    }

    @Override
    public void setWorkbenchPartName(String name) {
        throw new NotImplementedException();
    }

    @Override
    public void setOPIInput(IEditorInput input) throws PartInitException {
        try {
            IPath path = null;
            if (input instanceof IFileEditorInput) {
                path = ((IFileEditorInput) input).getFile().getFullPath();
            } else if (input instanceof RunnerInput) {
                path = ((RunnerInput) input).getPath();
            }
            MacrosInput macrosInput = displayModel.getMacrosInput();
            GraphicalViewer viewer = displayModel.getViewer();
            displayModel = createDisplayModel(path, macrosInput, viewer);
            setTitle();
            resizeToContents();
        } catch (Exception e) {
            OPIBuilderPlugin.getLogger().log(Level.WARNING, "Failed to replace OPIShell contents.", e);
        }
    }

    @Override
    public IEditorInput getOPIInput() {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(displayModel.getOpiFilePath());
        return new FileEditorInput(file);
    }

    @Override
    public DisplayModel getDisplayModel() {
        return displayModel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(OPIShell.class, macrosInput, path);
    }

}
