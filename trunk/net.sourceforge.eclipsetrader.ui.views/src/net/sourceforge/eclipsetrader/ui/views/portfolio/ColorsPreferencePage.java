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
package net.sourceforge.eclipsetrader.ui.views.portfolio;

import java.util.Vector;

import net.sourceforge.eclipsetrader.ui.internal.views.Messages;
import net.sourceforge.eclipsetrader.ui.internal.views.ViewsPlugin;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Portfolio / Stockwatch colors preference page.
 * <p></p>
 * 
 * @author Marco Maccaferri
 */
public class ColorsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
  private FieldEditor[] editor;
  
  public void init(IWorkbench workbench) 
  {
    //Initialize the preference store we wish to use
    setPreferenceStore(ViewsPlugin.getDefault().getPreferenceStore());
  }

  /*
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite parent) 
  {
    Vector _v = new Vector();

    Composite entryTable = new Composite(parent, SWT.NULL);
    entryTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    entryTable.setLayout(gridLayout);

    _v.add(new ColorFieldEditor("portfolio.text_color", Messages.getString("ColorsPreferencePage.textColor"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$
    _v.add(new ColorFieldEditor("portfolio.even_row_background", Messages.getString("ColorsPreferencePage.evenRowBackground"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$
    _v.add(new ColorFieldEditor("portfolio.odd_row_background", Messages.getString("ColorsPreferencePage.oddRowBackground"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$
    _v.add(new ColorFieldEditor("portfolio.positive_value_color", Messages.getString("ColorsPreferencePage.positiveValueColor"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$
    _v.add(new ColorFieldEditor("portfolio.negative_value_color", Messages.getString("ColorsPreferencePage.negativeValueColor"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$
    _v.add(new ColorFieldEditor("portfolio.total_row_background", Messages.getString("ColorsPreferencePage.totalBackground"), entryTable));         //$NON-NLS-1$ //$NON-NLS-2$

    // Perform operations common to all field editors
    editor = new FieldEditor[_v.size()];
    for (int i = 0; i < _v.size(); i++)
    {
      editor[i] = (FieldEditor)_v.elementAt(i);
      editor[i].setPreferenceStore(getPreferenceStore());
      editor[i].load();
    }
    
    return entryTable;
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  protected void performDefaults()
  {
    for (int i = 0; i < editor.length; i++)
      editor[i].loadDefault();
  }

  /** 
   * Method declared on IPreferencePage. Save the
   * author name to the preference store.
   */
  public boolean performOk() 
  {
    for (int i = 0; i < editor.length; i++)
      editor[i].store();
    return super.performOk();
  }
}