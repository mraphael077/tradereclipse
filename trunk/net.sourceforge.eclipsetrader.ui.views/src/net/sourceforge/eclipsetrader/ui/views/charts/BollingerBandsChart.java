/*******************************************************************************
 * Copyright (c) 2004-2005 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 *     Stefan S. Stratigakos - Original Qtstalker code
 *******************************************************************************/
package net.sourceforge.eclipsetrader.ui.views.charts;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.eclipsetrader.IChartData;
import net.sourceforge.eclipsetrader.ui.internal.views.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Marco
 */
public class BollingerBandsChart extends ChartPlotter implements IChartConfigurer
{
  private static final String PLUGIN_ID = "net.sourceforge.eclipsetrader.charts.bollinger"; //$NON-NLS-1$
  private int period = 20;
  private int deviations = 2;
  private int type = AverageChart.EXPONENTIAL;
  private List bbu = new ArrayList();
  private List bbl = new ArrayList();
  
  public BollingerBandsChart()
  {
    setName(Messages.getString("BollingerBandsChart.label")); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#getId()
   */
  public String getId()
  {
    return PLUGIN_ID;
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#getDescription()
   */
  public String getDescription()
  {
    return getName();
  }
  
  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#setData(net.sourceforge.eclipsetrader.IChartData[])
   */
  public void setData(IChartData[] data)
  {
    super.setData(data);
    
    bbu = new ArrayList();
    bbl = new ArrayList();

    if (data != null && data.length != 0)
    {
      List sma = AverageChart.getMA(data, type, period); 
      int smaLoop = sma.size() - 1;

      if (sma.size() >= period * 2)
      {
        int inputLoop = data.length - 1;
        while (inputLoop >= period && smaLoop >= period)
        {
          int count;
          double t2 = 0;
          for (count = 0, t2 = 0; count < period; count++)
          {
            double t = data[inputLoop - count].getClosePrice() - ((Double)sma.get(smaLoop)).doubleValue();
            t2 = t2 + (t * t);
          }

          double t = Math.sqrt(t2 / period);

          bbu.add(0, new Double( ((Double)sma.get(smaLoop)).doubleValue() + (deviations * t) )); // upper band
          bbl.add(0, new Double( ((Double)sma.get(smaLoop)).doubleValue() - (deviations * t) )); // lower band

          inputLoop--;
          smaLoop--;
        }
      }
      
      updateMinMax(bbu);
      updateMinMax(bbl);
    }
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#paintChart(GC gc, int width, int height)
   */
  public void paintChart(GC gc, int width, int height)
  {
    super.paintChart(gc, width, height);
    drawLine(bbu, gc, height, chartData.length - bbu.size());
    drawLine(bbl, gc, height, chartData.length - bbl.size());
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#paintScale(GC gc, int width, int height)
   */
  public void paintScale(GC gc, int width, int height)
  {
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#setParameter(String name, String value)
   */
  public void setParameter(String name, String value)
  {
    if (name.equalsIgnoreCase("period") == true) //$NON-NLS-1$
      period = Integer.parseInt(value);
    else if (name.equalsIgnoreCase("deviations") == true) //$NON-NLS-1$
      deviations = Integer.parseInt(value);
    else if (name.equalsIgnoreCase("type") == true) //$NON-NLS-1$
      type = Integer.parseInt(value);
    super.setParameter(name, value);
  }

  
  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.ui.views.charts.IChartPlotter#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent)
  {
    Label label = new Label(parent, SWT.NONE);
    label.setText(Messages.getString("BollingerBandsChart.periods")); //$NON-NLS-1$
    Text text = new Text(parent, SWT.BORDER);
    text.setData("period"); //$NON-NLS-1$
    text.setText(String.valueOf(period));
    text.setLayoutData(new GridData(25, SWT.DEFAULT));
    
    label = new Label(parent, SWT.NONE);
    label.setText(Messages.getString("BollingerBandsChart.deviations")); //$NON-NLS-1$
    text = new Text(parent, SWT.BORDER);
    text.setData("deviations"); //$NON-NLS-1$
    text.setText(String.valueOf(deviations));
    text.setLayoutData(new GridData(25, SWT.DEFAULT));

    AverageChart.addParameters(parent, Messages.getString("BollingerBandsChart.smoothingType"), "type", type); //$NON-NLS-1$ //$NON-NLS-2$

    return parent;
  }
}