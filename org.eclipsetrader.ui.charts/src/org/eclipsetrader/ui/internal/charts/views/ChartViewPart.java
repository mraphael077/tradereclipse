/*
 * Copyright (c) 2004-2008 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.ui.internal.charts.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipsetrader.core.charts.OHLCDataSeries;
import org.eclipsetrader.core.charts.repository.IChartTemplate;
import org.eclipsetrader.core.feed.IHistory;
import org.eclipsetrader.core.feed.TimeSpan;
import org.eclipsetrader.core.instruments.ISecurity;
import org.eclipsetrader.core.internal.charts.repository.ChartTemplate;
import org.eclipsetrader.core.repositories.IPropertyConstants;
import org.eclipsetrader.core.repositories.IRepositoryService;
import org.eclipsetrader.ui.charts.BaseChartViewer;
import org.eclipsetrader.ui.charts.ChartRowViewItem;
import org.eclipsetrader.ui.charts.ChartView;
import org.eclipsetrader.ui.charts.ChartViewer;
import org.eclipsetrader.ui.charts.IChartObject;
import org.eclipsetrader.ui.internal.charts.ChartsUIActivator;

public class ChartViewPart extends ViewPart {
	public static final String VIEW_ID = "org.eclipsetrader.ui.chart";

	public static final String K_VIEWS = "Views";
	public static final String K_URI = "uri";
	public static final String K_TEMPLATE = "template";

	public static final String K_PERIOD = "period";
	public static final String K_CUSTOM = "custom";
	public static final String K_FIRST_DATE = "first-date";
	public static final String K_LAST_DATE = "last-date";
	public static final String K_SHOW_TOOLTIPS = "show-tooltips";

	private URI uri;
	private ISecurity security;
	private IChartTemplate template;

	private BaseChartViewer viewer;
	private ChartView view;
	private IHistory history;

	private IDialogSettings dialogSettings;
	private Action cutAction;
	private Action copyAction;
	private Action pasteAction;
	private Action deleteAction;
	private Action propertiesAction;

	private PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
        	if (IPropertyConstants.BARS.equals(evt.getPropertyName()))
        		job.schedule();
        }
	};

	private Job job = new Job("ChartObject Loading") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
        	monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
            try {
            	if (history == null) {
                	IRepositoryService repositoryService = ChartsUIActivator.getDefault().getRepositoryService();
                	history = repositoryService.getHistoryFor(security);
                	PropertyChangeSupport propertyChangeSupport = (PropertyChangeSupport) history.getAdapter(PropertyChangeSupport.class);
                	if (propertyChangeSupport != null)
                		propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
            	}

            	if (view == null)
            		view = new ChartView(template);

            	IHistory activeHistory = history;

            	if (K_CUSTOM.equals(dialogSettings.get(K_PERIOD))) {
    				try {
    					Date firstDate = new SimpleDateFormat("yyyyMMdd").parse(dialogSettings.get(K_FIRST_DATE));
    					Date lastDate = new SimpleDateFormat("yyyyMMdd").parse(dialogSettings.get(K_LAST_DATE));
                		activeHistory = history.getSubset(firstDate, lastDate);
    				} catch(Exception e) {
    					// Do nothing
    				}
            	}
            	else {
            		TimeSpan timeSpan = TimeSpan.fromString(dialogSettings.get(K_PERIOD));
                	if (timeSpan != null) {
                		Calendar c = Calendar.getInstance();
                		c.setTime(history.getLast().getDate());
                		switch(timeSpan.getUnits()) {
                			case Months:
                        		c.add(Calendar.MONTH, - timeSpan.getLength());
                        		activeHistory = history.getSubset(c.getTime(), history.getLast().getDate());
                				break;
                			case Years:
                        		c.add(Calendar.YEAR, - timeSpan.getLength());
                        		activeHistory = history.getSubset(c.getTime(), history.getLast().getDate());
                				break;
                		}
                	}
            	}

            	view.setRootDataSeries(new OHLCDataSeries(history.getSecurity() != null ? history.getSecurity().getName() : "MAIN", activeHistory.getOHLC()));

    	        if (!viewer.isDisposed()) {
    				try {
    					viewer.getDisplay().asyncExec(new Runnable() {
    						public void run() {
    							if (!viewer.isDisposed()) {
    		    					BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
    		    						public void run() {
    		    							ChartRowViewItem[] rowViewItem = (ChartRowViewItem[]) view.getAdapter(ChartRowViewItem[].class);
    		    							if (rowViewItem != null) {
    		    								IChartObject[] input = new IChartObject[rowViewItem.length];
    		    								for (int i = 0; i < input.length; i++)
    		    									input[i] = (IChartObject) rowViewItem[i].getAdapter(IChartObject.class);
        		   				    			viewer.setInput(input);
    		    							}
    		    						}
    		    					});
    							}
    						}
    					});
    				} catch (SWTException e) {
    					if (e.code != SWT.ERROR_DEVICE_DISPOSED)
    						throw e;
    				}
    			}
            } catch (Exception e) {
            	Status status = new Status(Status.ERROR, ChartsUIActivator.PLUGIN_ID, "Error loading view " + getViewSite().getSecondaryId(), e);
            	ChartsUIActivator.getDefault().getLog().log(status);
            } finally {
            	monitor.done();
            }
	        return Status.OK_STATUS;
        }
	};

	public ChartViewPart() {
	}

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
     */
    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
	    super.init(site, memento);

        try {
    		dialogSettings = ChartsUIActivator.getDefault().getDialogSettings().getSection(K_VIEWS).getSection(site.getSecondaryId());
        	uri = new URI(dialogSettings.get(K_URI));

        	IRepositoryService repositoryService = ChartsUIActivator.getDefault().getRepositoryService();
        	security = repositoryService.getSecurityFromURI(uri);

			JAXBContext jaxbContext = JAXBContext.newInstance(ChartTemplate.class);
	        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	        unmarshaller.setEventHandler(new ValidationEventHandler() {
				public boolean handleEvent(ValidationEvent event) {
					System.out.println("Error validating XML: " + event.getMessage());
					if (event.getLinkedException() != null)
						event.getLinkedException().printStackTrace(System.out);
					return true;
				}
			});

        	IPath templatePath = new Path("data").append(dialogSettings.get(K_TEMPLATE)); //$NON-NLS-1$
        	InputStream stream = FileLocator.openStream(ChartsUIActivator.getDefault().getBundle(), templatePath, false);
	        template = (IChartTemplate) unmarshaller.unmarshal(stream);

        } catch (Exception e) {
        	Status status = new Status(Status.ERROR, ChartsUIActivator.PLUGIN_ID, "Error loading view " + site.getSecondaryId(), e);
        	ChartsUIActivator.getDefault().getLog().log(status);
        }

        createActions();

        IActionBars actionBars = site.getActionBars();

		actionBars.setGlobalActionHandler(cutAction.getId(), cutAction);
		actionBars.setGlobalActionHandler(copyAction.getId(), copyAction);
		actionBars.setGlobalActionHandler(pasteAction.getId(), pasteAction);
		actionBars.setGlobalActionHandler(deleteAction.getId(), deleteAction);
		actionBars.setGlobalActionHandler(propertiesAction.getId(), propertiesAction);

        IMenuManager menuManager = actionBars.getMenuManager();

        TimeSpan[] availablePeriods = new TimeSpan[] {
        		TimeSpan.years(2),
        		TimeSpan.years(1),
        		TimeSpan.months(6),
        	};
        PeriodMenu periodMenu = new PeriodMenu(site.getShell(), availablePeriods) {
            @Override
            protected void selectionChanged(TimeSpan selection) {
            	dialogSettings.put(K_PERIOD, selection != null ? selection.toString() : (String) null);
	            job.schedule();
	            super.selectionChanged(selection);
            }

            @Override
            protected void customPeriodSelection(Date firstDate, Date lastDate) {
            	dialogSettings.put(K_PERIOD, K_CUSTOM);
            	dialogSettings.put(K_FIRST_DATE, new SimpleDateFormat("yyyyMMdd").format(firstDate));
            	dialogSettings.put(K_LAST_DATE, new SimpleDateFormat("yyyyMMdd").format(lastDate));
	            job.schedule();
	            super.customPeriodSelection(firstDate, lastDate);
            }
       	};
    	if (K_CUSTOM.equals(dialogSettings.get(K_PERIOD)))
			try {
				Date beginDate = new SimpleDateFormat("yyyyMMdd").parse(dialogSettings.get(K_FIRST_DATE));
				Date endDate = new SimpleDateFormat("yyyyMMdd").parse(dialogSettings.get(K_LAST_DATE));
	    		periodMenu.setCustomSelection(beginDate, endDate);
			} catch(Exception e) {
				// Do nothing
			}
    	else {
    		TimeSpan timeSpan = TimeSpan.fromString(dialogSettings.get(K_PERIOD));
    		periodMenu.setSelection(timeSpan);
    	}

    	menuManager.add(periodMenu);
    	menuManager.add(new Separator());

    	Action showTooltipsAction = new Action("Show tooltips", Action.AS_CHECK_BOX) {
            @Override
            public void run() {
	            dialogSettings.put(K_SHOW_TOOLTIPS, isChecked());
	            viewer.setShowTooltips(isChecked());
            }
    	};
    	showTooltipsAction.setChecked(dialogSettings.get(K_SHOW_TOOLTIPS) != null && Boolean.TRUE.equals(dialogSettings.getBoolean(K_SHOW_TOOLTIPS)));
    	menuManager.add(showTooltipsAction);

        actionBars.getToolBarManager().add(new Separator("additions"));
        actionBars.updateActionBars();
    }

    protected void createActions() {
    	ISharedImages sharedImages = getViewSite().getWorkbenchWindow().getWorkbench().getSharedImages();

    	cutAction = new Action("Cut") {
            @Override
            public void run() {
            }
		};
		cutAction.setId("cut"); //$NON-NLS-1$
		cutAction.setActionDefinitionId("org.eclipse.ui.edit.cut"); //$NON-NLS-1$
		cutAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		cutAction.setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		cutAction.setEnabled(false);

        copyAction = new Action("Copy") {
            @Override
            public void run() {
            }
		};
		copyAction.setId("copy"); //$NON-NLS-1$
		copyAction.setActionDefinitionId("org.eclipse.ui.edit.copy"); //$NON-NLS-1$
		copyAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		copyAction.setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		copyAction.setEnabled(false);

        pasteAction = new Action("Paste") {
            @Override
            public void run() {
            }
		};
		pasteAction.setId("copy"); //$NON-NLS-1$
		pasteAction.setActionDefinitionId("org.eclipse.ui.edit.paste"); //$NON-NLS-1$
		pasteAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		pasteAction.setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
		pasteAction.setEnabled(false);

        deleteAction = new Action("Delete") {
            @Override
            public void run() {
            }
		};
		deleteAction.setId("delete"); //$NON-NLS-1$
		deleteAction.setActionDefinitionId("org.eclipse.ui.edit.delete"); //$NON-NLS-1$
		deleteAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		deleteAction.setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		deleteAction.setEnabled(false);

        propertiesAction = new Action("Settings") {
            @Override
            public void run() {
            }
		};
		propertiesAction.setId("settings"); //$NON-NLS-1$
		propertiesAction.setActionDefinitionId("org.eclipse.ui.edit.settings"); //$NON-NLS-1$
		propertiesAction.setEnabled(false);
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
    	viewer = new BaseChartViewer(parent, SWT.NONE);
    	//viewer.setHorizontalAxis(new DateValuesAxis());
		//((DateValuesAxis) viewer.getHorizontalAxis()).fillAvailableSpace = false;
		//viewer.setVerticalAxis(new DoubleValuesAxis());
		viewer.setHorizontalScaleVisible(true);
		viewer.setVerticalScaleVisible(true);
		//viewer.setRenderer(new ChartDocumentRenderer());
		//viewer.setContentProvider(new ChartDocumentContentProvider());

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
            	cutAction.setEnabled(!event.getSelection().isEmpty());
            	copyAction.setEnabled(!event.getSelection().isEmpty());
            	deleteAction.setEnabled(!event.getSelection().isEmpty());
            	propertiesAction.setEnabled(!event.getSelection().isEmpty());
            }
		});

		getSite().setSelectionProvider(viewer);

		MenuManager menuMgr = new MenuManager("#popupMenu", "popupMenu"); //$NON-NLS-1$ //$NON-NLS-2$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuManager) {
				menuManager.add(new Separator("top"));
				menuManager.add(cutAction);
				menuManager.add(copyAction);
				menuManager.add(pasteAction);
				menuManager.add(new Separator());
				menuManager.add(deleteAction);
				menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				menuManager.add(propertiesAction);
			}
		});
		viewer.getControl().setMenu(menuMgr.createContextMenu(viewer.getControl()));
		getSite().registerContextMenu(menuMgr, getSite().getSelectionProvider());

		if (security != null && template != null) {
			setPartName(NLS.bind("{0} - {1}", new Object[] {
					security.getName(),
					template.getName(),
				}));

			job.setName("Loading " + getPartName());
			job.setUser(false);
			job.schedule();
		}
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
    	viewer.getControl().setFocus();
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
    	if (history != null) {
        	PropertyChangeSupport propertyChangeSupport = (PropertyChangeSupport) history.getAdapter(PropertyChangeSupport.class);
        	if (propertyChangeSupport != null)
        		propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    	}

    	super.dispose();
    }

	/* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#getAdapter(java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
    	if (adapter.isAssignableFrom(ChartViewer.class))
    		return viewer;
    	if (adapter.isAssignableFrom(ChartView.class))
    		return view;
    	if (adapter.isAssignableFrom(IChartTemplate.class))
    		return template;
    	if (adapter.isAssignableFrom(ISecurity.class))
    		return security;
    	if (adapter.isAssignableFrom(IDialogSettings.class))
    		return dialogSettings;
	    return super.getAdapter(adapter);
    }
}