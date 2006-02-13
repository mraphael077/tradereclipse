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
package net.sourceforge.eclipsetrader.yahoo;

import net.sourceforge.eclipsetrader.IExtendedData;
import net.sourceforge.eclipsetrader.RealtimeChartDataProvider;
import net.sourceforge.eclipsetrader.yahoo.internal.Streamer;
import net.sourceforge.eclipsetrader.yahoo.internal.SymbolMapper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * @author Marco Maccaferri
 */
public class SnapshotDataProvider extends RealtimeChartDataProvider implements IPropertyChangeListener
{
  private Streamer streamer = Streamer.getInstance();
  protected int symbolField = 0;
  protected boolean useMapping = true;
  protected String defaultExtension = "";
  
  /**
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException
  {
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.IDataStreamer#startStreaming()
   */
  public void startStreaming()
  {
    IPreferenceStore ps = YahooPlugin.getDefault().getPreferenceStore();
    symbolField = ps.getInt("yahoo.field");
    useMapping = ps.getBoolean("yahoo.mapping");
    defaultExtension = ps.getString("yahoo.suffix");

    IExtendedData[] data = getData();
    for (int i = 0; i < data.length; i++)
    {
      String symbol = (symbolField == 0) ? data[i].getSymbol() : data[i].getTicker();
      if (useMapping == true)
      {
        symbol = SymbolMapper.getYahooSymbol(symbol);
        if (symbol.indexOf(".") == -1)
          symbol += defaultExtension;
      }
      streamer.addSymbol(symbol);
    }

    // Listen to changes to plugin settings
    ps.addPropertyChangeListener(this);
    
    streamer.addListener(this);
    streamer.start();
    super.startStreaming();
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.IDataStreamer#stopStreaming()
   */
  public void stopStreaming()
  {
    streamer.stop();
    streamer.removeListener(this);

    YahooPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);

    IExtendedData[] data = getData();
    for (int i = 0; i < data.length; i++)
    {
      String symbol = (symbolField == 0) ? data[i].getSymbol() : data[i].getTicker();
      if (useMapping == true)
      {
        symbol = SymbolMapper.getYahooSymbol(symbol);
        if (symbol.indexOf(".") == -1)
          symbol += defaultExtension;
      }
      streamer.removeSymbol(symbol);
    }

    super.stopStreaming();
  }
  
  public void update()
  {
    IExtendedData[] data = getData();
    for (int i = 0; i < data.length; i++) 
    {
      String symbol = (symbolField == 0) ? data[i].getSymbol() : data[i].getTicker();
      if (useMapping == true)
      {
        symbol = SymbolMapper.getYahooSymbol(symbol);
        if (symbol.indexOf(".") == -1)
          symbol += defaultExtension;
      }
      IExtendedData ed = streamer.getData(symbol);
      if (ed != null)
      {
        data[i].setLastPrice(ed.getLastPrice());
        data[i].setBidPrice(ed.getBidPrice());
        data[i].setBidSize(ed.getBidSize());
        data[i].setAskPrice(ed.getAskPrice());
        data[i].setAskSize(ed.getAskSize());
        data[i].setOpenPrice(ed.getOpenPrice());
        data[i].setHighPrice(ed.getHighPrice());
        data[i].setLowPrice(ed.getLowPrice());
        data[i].setClosePrice(ed.getClosePrice());
        data[i].setVolume(ed.getVolume());
        data[i].setDate(ed.getDate());
      }
    }
    
    // Signal the update to all listeners.
    fireDataUpdated();
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.DataProvider#itemAdded(java.lang.Object)
   */
  public void itemAdded(Object obj)
  {
    if (obj instanceof IExtendedData)
    {
      IExtendedData data = (IExtendedData)obj;
      String symbol = (symbolField == 0) ? data.getSymbol() : data.getTicker();
      if (useMapping == true)
      {
        symbol = SymbolMapper.getYahooSymbol(symbol);
        if (symbol.indexOf(".") == -1)
          symbol += defaultExtension;
      }
      streamer.addSymbol(symbol);
    }
    super.itemAdded(obj);
  }

  /* (non-Javadoc)
   * @see net.sourceforge.eclipsetrader.DataProvider#itemRemoved(java.lang.Object)
   */
  public void itemRemoved(Object obj)
  {
    if (obj instanceof IExtendedData)
    {
      IExtendedData data = (IExtendedData)obj;
      String symbol = (symbolField == 0) ? data.getSymbol() : data.getTicker();
      if (useMapping == true)
      {
        symbol = SymbolMapper.getYahooSymbol(symbol);
        if (symbol.indexOf(".") == -1)
          symbol += defaultExtension;
      }
      streamer.removeSymbol(symbol);
    }
    super.itemRemoved(obj);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event)
  {
    String property = event.getProperty();

    // Check if the one of the mapping preferences are changed
    if (property.equalsIgnoreCase("yahoo.mapping") == true || property.equalsIgnoreCase("yahoo.suffix") == true || property.equalsIgnoreCase("yahoo.field") == true)
    {
      // Remove the old-mapping symbols from the streamer 
      IExtendedData[] data = getData();
      for (int i = 0; i < data.length; i++)
      {
        String symbol = (symbolField == 0) ? data[i].getSymbol() : data[i].getTicker();
        if (useMapping == true)
        {
          symbol = SymbolMapper.getYahooSymbol(symbol);
          if (symbol.indexOf(".") == -1)
            symbol += defaultExtension;
        }
        streamer.removeSymbol(symbol);
      }

      // Sets the new mapping preferences
      IPreferenceStore ps = YahooPlugin.getDefault().getPreferenceStore();
      if (property.equalsIgnoreCase("yahoo.field") == true)
        symbolField = ps.getInt("yahoo.field");
      if (property.equalsIgnoreCase("yahoo.mapping") == true)
        useMapping = ps.getBoolean("yahoo.mapping");
      if (property.equalsIgnoreCase("yahoo.suffix") == true)
        defaultExtension = ps.getString("yahoo.suffix");

      // Add the new-mapped symbols to the streamer
      for (int i = 0; i < data.length; i++)
      {
        String symbol = (symbolField == 0) ? data[i].getSymbol() : data[i].getTicker();
        if (useMapping == true)
        {
          symbol = SymbolMapper.getYahooSymbol(symbol);
          if (symbol.indexOf(".") == -1)
            symbol += defaultExtension;
        }
        streamer.addSymbol(symbol);
      }
    }
  }
}