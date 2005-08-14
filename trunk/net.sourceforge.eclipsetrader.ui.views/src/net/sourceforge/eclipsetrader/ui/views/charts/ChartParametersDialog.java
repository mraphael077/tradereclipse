/*******************************************************************************
 * Copyright (c) 2004-2005 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 *******************************************************************************/
package net.sourceforge.eclipsetrader.ui.views.charts;

import net.sourceforge.eclipsetrader.ui.internal.views.Messages;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;


/**
 * @author Marco
 */
public class ChartParametersDialog extends TitleAreaDialog
{
  public final static int SELECTED_ZONE = 1;
  public final static int BELOW_SELECTED_ZONE = 2;
  public final static int ABOVE_SELECTED_ZONE = 3;
  public static final int NEW_CHART = 1;
  public static final int EDIT_CHART = 2;
  private int type = NEW_CHART;
  protected int position = SELECTED_ZONE;
  private Composite composite;
//  private Text chartName;
  private Button selectedZone;
  private Button newZone;
  private ColorSelector colorSelector;
  private IChartConfigurer chartConfigurer;
  
  public ChartParametersDialog(IChartConfigurer chartConfigurer)
  {
    super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
    this.chartConfigurer = chartConfigurer;
  }
  
  /**
   * @see org.eclipse.jface.window.Window#configureShell(Shell)
   */
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.getString("ChartParametersDialog.title") + chartConfigurer.getName()); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  protected Control createDialogArea(Composite parent)
  {
    composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));
    composite.setLayout(new GridLayout(2, false));
    
    Label label = new Label(composite, SWT.NONE);
    label.setText(Messages.getString("ChartParametersDialog.name")); //$NON-NLS-1$
    label.setLayoutData(new GridData(125, SWT.DEFAULT));
    Text text = new Text(composite, SWT.BORDER);
    text.setData("name"); //$NON-NLS-1$
    text.setText(chartConfigurer.getName());
    text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL|GridData.HORIZONTAL_ALIGN_FILL));
    
    label = new Label(composite, SWT.NONE);
    label.setText(Messages.getString("ChartParametersDialog.color")); //$NON-NLS-1$
    colorSelector = new ColorSelector(composite);
    colorSelector.getButton().setData("color"); //$NON-NLS-1$
    colorSelector.setColorValue(chartConfigurer.getColorParameter("color")); //$NON-NLS-1$

    // Adds the parameters specific to the given chart
    if (chartConfigurer != null)
      chartConfigurer.createContents(composite);
    
    if (type == NEW_CHART)
    {
      Group group = new Group(composite, SWT.NONE);
      group.setText(Messages.getString("ChartParametersDialog.insertIndicator")); //$NON-NLS-1$
      group.setLayoutData(new GridData(GridData.FILL_BOTH));
      group.setLayout(new GridLayout(1, false));
      
      selectedZone = new Button(group, SWT.RADIO);
      selectedZone.setText(Messages.getString("ChartParametersDialog.selectedZone")); //$NON-NLS-1$
      selectedZone.setSelection(true);
      newZone = new Button(group, SWT.RADIO);
      newZone.setText(Messages.getString("ChartParametersDialog.newZone")); //$NON-NLS-1$
    }

    return super.createDialogArea(parent);
  }

  /**
   * Override this method to provide custom parameters configuration.
   * <p></p>
   */
//  public abstract void createPartControl(Composite parent);

  /**
   * Open the dialog for a new chart.
   * <p></p>
   */
  public int open()
  {
    create();

    setTitle(chartConfigurer.getName());
    setMessage(Messages.getString("ChartParametersDialog.message") + chartConfigurer.getName()); //$NON-NLS-1$
    
    return super.open();
  }

  /**
   * Open the dialog for editing.
   * <p></p>
   */
  public int openEdit()
  {
    type = EDIT_CHART;
    return open();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  protected void okPressed()
  {
    if (type == NEW_CHART)
    {
      if (selectedZone.getSelection() == true)
        position = SELECTED_ZONE;
      else if (newZone.getSelection() == true)
        position = BELOW_SELECTED_ZONE;
    }

    RGB rgb = colorSelector.getColorValue(); 
    chartConfigurer.setParameter("color", String.valueOf(rgb.red) + "," + String.valueOf(rgb.green) + "," + String.valueOf(rgb.blue)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    
    Control[] c = composite.getChildren();
    for (int i = 0; i < c.length; i++)
    {
      if (c[i].getData() == null || !(c[i].getData() instanceof String))
        continue;
      if (c[i] instanceof Text)
        chartConfigurer.setParameter((String)c[i].getData(), ((Text)c[i]).getText());
      else if (c[i] instanceof Combo)
        chartConfigurer.setParameter((String)c[i].getData(), String.valueOf(((Combo)c[i]).getSelectionIndex()));
    }

    super.okPressed();
  }

  /**
   * Method to return the position field.<br>
   *
   * @return Returns the position.
   */
  public int getPosition()
  {
    return position;
  }
  /**
   * Method to set the position field.<br>
   * 
   * @param position The position to set.
   */
  public void setPosition(int position)
  {
    this.position = position;
  }
}