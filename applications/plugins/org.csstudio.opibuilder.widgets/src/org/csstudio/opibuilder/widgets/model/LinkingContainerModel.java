/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.widgets.model;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.opibuilder.OPIBuilderPlugin;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.model.AbstractContainerModel;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.properties.BooleanProperty;
import org.csstudio.opibuilder.properties.ComboProperty;
import org.csstudio.opibuilder.properties.FilePathProperty;
import org.csstudio.opibuilder.properties.StringProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.opibuilder.visualparts.BorderStyle;
import org.csstudio.opibuilder.widgets.Activator;
import org.csstudio.opibuilder.widgets.editparts.LinkingContainerEditpart;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.GraphicalViewer;
import org.osgi.framework.Version;

/**The model for linking container widget.
 * @author Xihui Chen
 *
 */
public class LinkingContainerModel extends AbstractContainerModel {

	/**
	 * The ID of this widget model.
	 */
	public static final String ID = "org.csstudio.opibuilder.widgets.linkingContainer"; //$NON-NLS-1$	
	
	/**
	 * Versions before this didn't have an updated resize behaviour.
	 */
	public static final Version VERSION_CHANGE_OF_RESIZE_BEHAVIOUR = new Version(4, 0, 103);

	/**
	 * How should the container behave when the OPI it is wrapping has content of a different size to the widget.
	 */
	public enum ResizeBehaviour {
		SIZE_OPI_TO_CONTAINER,
		SIZE_CONTAINER_TO_OPI,
		CROP_OPI,
		SCROLL_OPI;

		public final static String[] stringValues = {
				"Size *.opi to fit the container",
				"Size the container to fit the linked *.opi",
				"Don't resize anything, crop if *.opi too large for container",
				"Don't resize anything, add scrollbars if *.opi too large for container",
				};
	}

	/**
	 * The ID of the resource property.
	 */
	public static final String PROP_OPI_FILE = "opi_file"; //$NON-NLS-1$

	/**
	 * The name of the group container widget in the OPI file, which
	 * will be loaded if it is specified. If it is not specified, the whole
	 * OPI file will be loaded.
	 */
	public static final String PROP_GROUP_NAME = "group_name"; //$NON-NLS-1$
	
	/**
	 * The ID of the auto zoom property.
	 */
	@Deprecated
	public static final String PROP_ZOOMTOFITALL = "zoom_to_fit"; //$NON-NLS-1$
	
	/**
	 *  The ID of the auto scale property.
	 */
	@Deprecated
	public static final String PROP_AUTO_SIZE = "auto_size"; //$NON-NLS-1$
	
	/**
	 * How the widget should behave when the contents is not the same size as the widget.
	 */
	public static final String PROP_RESIZE_BEHAVIOUR = "resize_behaviour"; //$NON-NLS-1$

	/**
	 * The default value of the height property.
	 */
	private static final int DEFAULT_HEIGHT = 200;

	/**
	 * The default value of the width property.
	 */
	private static final int DEFAULT_WIDTH = 200;

	/**
	 * The geographical size of the children.
	 */
	private Dimension childrenGeoSize = null;
	
	/**
	 * The display Scale options of the embedded OPI.
	 */
	private DisplayModel displayModel = null;
	
	public LinkingContainerModel() {
		setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setBorderStyle(BorderStyle.LOWERED);
	}
	
	@Override
	protected void configureProperties() {
		
		addProperty(new FilePathProperty(PROP_OPI_FILE, "OPI File",
				WidgetPropertyCategory.Behavior, new Path(""), //$NON-NLS-1$
				new String[] { OPIBuilderPlugin.OPI_FILE_EXTENSION}));
		
		addProperty(new StringProperty(PROP_GROUP_NAME, "Group Name",
				WidgetPropertyCategory.Behavior, "")); //$NON-NLS-1$
		
		addProperty(new BooleanProperty(PROP_ZOOMTOFITALL, "Zoom to Fit", WidgetPropertyCategory.Display, true));
		setPropertyVisibleAndSavable(PROP_ZOOMTOFITALL, false, false);

		addProperty(new BooleanProperty(PROP_AUTO_SIZE, "Auto Size", WidgetPropertyCategory.Display, true));
		setPropertyVisibleAndSavable(PROP_AUTO_SIZE, false, false);

		addProperty(new ComboProperty(PROP_RESIZE_BEHAVIOUR, "Resize Behaviour",
				WidgetPropertyCategory.Display, ResizeBehaviour.stringValues, ResizeBehaviour.SIZE_OPI_TO_CONTAINER.ordinal()));
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	/**
	 * Return the target resource.
	 * 
	 * @return The target resource.
	 */
	public IPath getOPIFilePath() {
		IPath absolutePath = (IPath) getProperty(PROP_OPI_FILE).getPropertyValue();
		if(absolutePath != null && !absolutePath.isEmpty() && !absolutePath.isAbsolute())
			absolutePath = ResourceUtil.buildAbsolutePath(this, absolutePath);
		return absolutePath;
	}
	
	public void setOPIFilePath(String path){
		setPropertyValue(PROP_OPI_FILE, new Path(path));
	}

	/**
	 * Returns the auto zoom state.
	 * @return the auto zoom state
	 */
	public boolean isAutoFit() {
		return (int)getProperty(PROP_RESIZE_BEHAVIOUR).getPropertyValue() == ResizeBehaviour.SIZE_OPI_TO_CONTAINER.ordinal();
	}
	
	public boolean isAutoSize() {
		return (int)getProperty(PROP_RESIZE_BEHAVIOUR).getPropertyValue() == ResizeBehaviour.SIZE_CONTAINER_TO_OPI.ordinal();
	}

	public boolean isShowScrollBars() {
		return (int)getProperty(PROP_RESIZE_BEHAVIOUR).getPropertyValue() == ResizeBehaviour.SCROLL_OPI.ordinal();
	}
	
	public String getGroupName(){
		return (String)getPropertyValue(PROP_GROUP_NAME);
	}
	
	
	@Override
	public List<AbstractWidgetModel> getChildren() {
		//Linking container should have "no" children. 
		//Its children should be dynamically loaded from opi file.
		return new LinkedList<AbstractWidgetModel>();
	}
	
	@Override
	public boolean isChildrenOperationAllowable() {
		return false;
	}
	
	@Override
	public void scale(double widthRatio, double heightRatio) {
		super.scale(widthRatio, heightRatio);
		if(!isAutoFit())
			scaleChildren();
		
	}

	@Override
	public void processVersionDifference(org.osgi.framework.Version boyVersionOnFile) {
		super.processVersionDifference(boyVersionOnFile);
		if(boyVersionOnFile.compareTo(VERSION_CHANGE_OF_RESIZE_BEHAVIOUR) < 0) {
			Activator.getLogger().log(Level.CONFIG, "Converting linking container to new style of resizing behaviour.");
			if((Boolean)getPropertyValue(PROP_AUTO_SIZE)) {
				setPropertyValue(PROP_RESIZE_BEHAVIOUR, ResizeBehaviour.SIZE_CONTAINER_TO_OPI.ordinal());
			} else if((Boolean)getPropertyValue(PROP_ZOOMTOFITALL)) {
				setPropertyValue(PROP_RESIZE_BEHAVIOUR, ResizeBehaviour.SIZE_OPI_TO_CONTAINER.ordinal());
			} else {
				setPropertyValue(PROP_RESIZE_BEHAVIOUR, ResizeBehaviour.SCROLL_OPI.ordinal());
			}
		}
	};

	/**
	 * Scale its children. 
	 */
	public void scaleChildren() {
		if(isAutoFit())
			return;
		//The linking container model doesn't hold its children actually, so it 
		// has to ask editpart to get its children.
		GraphicalViewer viewer = getRootDisplayModel().getViewer();
		if(viewer == null)
			return;
		LinkingContainerEditpart editpart = 
				(LinkingContainerEditpart) viewer.
				getEditPartRegistry().
				get(this);
		Dimension size = getSize();
		double newWidthRatio = size.width/(double)getOriginSize().width;
		double newHeightRatio = size.height/(double)getOriginSize().height;
		boolean allowScale = true;
		if(displayModel != null){
			allowScale = displayModel.getDisplayScaleData().isAutoScaleWidgets();
			if(allowScale){
				int minWidth = displayModel.getDisplayScaleData()
						.getMinimumWidth();

				if (minWidth < 0) {
					minWidth = displayModel.getWidth();
				}
				int minHeight = displayModel.getDisplayScaleData()
						.getMinimumHeight();
				if (minHeight < 0) {
					minHeight = displayModel.getHeight();
				}
				if (getWidth() * newWidthRatio < minWidth)
					newWidthRatio = minWidth / (double) getOriginSize().width;
				if (getHeight() * newHeightRatio < minHeight)
					newHeightRatio = minHeight
							/ (double) getOriginSize().height;
			}
			
		}
		if(allowScale)
			for(Object child : editpart.getChildren())
				((AbstractBaseEditPart)child).getWidgetModel().scale(newWidthRatio, newHeightRatio);
	}
	
	@Override
	public Dimension getOriginSize() {
		if(childrenGeoSize == null)			
			return super.getOriginSize();
		else
			return childrenGeoSize;
	}
	
	public void setChildrenGeoSize(Dimension childrenGeoSize) {
		this.childrenGeoSize = childrenGeoSize;
	}
	
	/**Set the display model of the loaded opi.
	 * @param displayModel
	 */
	public void setDisplayModel(DisplayModel displayModel) {
		this.displayModel = displayModel;
	}
	
	/**
	 * @return display model of the loaded opi. null if no opi has been loaded.
	 */
	public DisplayModel getDisplayModel() {
		return displayModel;
	}
}
