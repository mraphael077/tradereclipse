/*******************************************************************************
 * Copyright (c) 2004 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 *******************************************************************************/
package net.sourceforge.eclipsetrader.yahoo;

import java.util.Vector;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author Marco
 */
public class MappingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{
  FieldEditor[] editor; 
  
  public void init(IWorkbench workbench) {
    //Initialize the preference store we wish to use
    setPreferenceStore(YahooPlugin.getDefault().getPreferenceStore());
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

    _v.add(new StringFieldEditor("yahoo.suffix", "Default Suffix", 10, new Composite(entryTable, SWT.NONE)));

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