/*
 * Copyright (c) 2004-2006 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package net.sourceforge.eclipsetrader.core.ui.wizards;

import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.eclipsetrader.core.CorePlugin;
import net.sourceforge.eclipsetrader.core.db.Security;
import net.sourceforge.eclipsetrader.core.db.feed.FeedSource;
import net.sourceforge.eclipsetrader.core.ui.preferences.IntradayDataOptions;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 */
public class SecurityWizard extends Wizard
{
    public static String WINDOW_TITLE = "New Security Wizard";
    public static String EDIT_WINDOW_TITLE = "Security Edit Wizard";
    private Security security;
    private SecurityPage securityPage;
    private QuoteFeedPage quoteFeedPage;
    private HistoryFeedPage historyFeedPage;
    private IntradayDataOptions options = new IntradayDataOptions();

    public SecurityWizard()
    {
    }

    public Security open()
    {
        setWindowTitle(WINDOW_TITLE);
        WizardDialog dlg = create(null);
        dlg.open();
        return this.security;
    }
    
    public WizardDialog create(Security security)
    {
        this.security = security;
        
        securityPage = new SecurityPage(security);
        addPage(securityPage);
        quoteFeedPage = new QuoteFeedPage(security);
        addPage(quoteFeedPage);
        historyFeedPage = new HistoryFeedPage(security);
        addPage(historyFeedPage);
        
        WizardPage page = new WizardPage("") {
            public void createControl(Composite parent)
            {
                setControl(options.createControls(parent, SecurityWizard.this.security));
            }
        };
        page.setTitle("Intraday Charts");
        page.setDescription("Set the options to automatically build intraday charts");
        addPage(page);

        WizardDialog dlg = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), this);
        dlg.create();
        
        return dlg;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#canFinish()
     */
    public boolean canFinish()
    {
        return securityPage.isPageComplete();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    public boolean performFinish()
    {
        if (security == null)
            security = new Security();
        
        security.setCode(securityPage.getCode());
        security.setDescription(securityPage.getSecurityDescription());
        security.setCurrency(securityPage.getCurrency());
        
        if (quoteFeedPage.getId() != null)
        {
            FeedSource feed = new FeedSource();
            feed.setId(quoteFeedPage.getId());
            feed.setSymbol(quoteFeedPage.getSymbol());
            security.setQuoteFeed(feed);
        }
        else
            security.setQuoteFeed(null);
        
        if (quoteFeedPage.getLevel2Id() != null)
        {
            FeedSource feed = new FeedSource();
            feed.setId(quoteFeedPage.getLevel2Id());
            feed.setSymbol(quoteFeedPage.getLevel2Symbol());
            security.setLevel2Feed(feed);
        }
        else
            security.setLevel2Feed(null);
        
        if (historyFeedPage.getId() != null)
        {
            FeedSource feed = new FeedSource();
            feed.setId(historyFeedPage.getId());
            feed.setSymbol(historyFeedPage.getSymbol());
            security.setHistoryFeed(feed);
        }
        else
            security.setHistoryFeed(null);

        options.saveSettings(security);
        
        CorePlugin.getRepository().save(security);
        return true;
    }

    static void updateFeedExchanges(String type, Combo combo, FeedSource feed)
    {
        combo.removeAll();
        combo.add("");

        if (feed != null)
        {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = registry.getExtensionPoint(CorePlugin.FEED_EXTENSION_POINT);
            if (extensionPoint != null)
            {
                java.util.List plugins = Arrays.asList(extensionPoint.getConfigurationElements());
                for (Iterator iter = plugins.iterator(); iter.hasNext(); )
                {
                    IConfigurationElement element = (IConfigurationElement)iter.next();
                    if (!element.getAttribute("id").equals(feed.getId())) //$NON-NLS-1$
                        continue;
                    
                    IConfigurationElement[] children = element.getChildren();
                    for (int i = 0; i < children.length; i++)
                    {
                        if (children[i].getName().equals(type)) //$NON-NLS-1$
                        {
                            IConfigurationElement[] exchanges = children[i].getChildren();
                            for (int x = 0; x < exchanges.length; x++)
                            {
                                if (exchanges[x].getName().equals("exchange")) //$NON-NLS-1$
                                {
                                    String id = exchanges[x].getAttribute("id");
                                    combo.setData(String.valueOf(combo.getItemCount()), id); //$NON-NLS-1$
                                    combo.add(exchanges[x].getAttribute("name")); //$NON-NLS-1$
                                    if (id.equals(feed.getExchange()))
                                        combo.select(combo.getItemCount() - 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        combo.setEnabled(combo.getItemCount() > 1);
    }

    static void updateFeedExchanges(String type, Combo combo, String feed)
    {
        combo.removeAll();
        combo.add("");

        if (feed != null)
        {
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = registry.getExtensionPoint(CorePlugin.FEED_EXTENSION_POINT);
            if (extensionPoint != null)
            {
                java.util.List plugins = Arrays.asList(extensionPoint.getConfigurationElements());
                for (Iterator iter = plugins.iterator(); iter.hasNext(); )
                {
                    IConfigurationElement element = (IConfigurationElement)iter.next();
                    if (!element.getAttribute("id").equals(feed)) //$NON-NLS-1$
                        continue;
                    
                    IConfigurationElement[] children = element.getChildren();
                    for (int i = 0; i < children.length; i++)
                    {
                        if (children[i].getName().equals(type)) //$NON-NLS-1$
                        {
                            IConfigurationElement[] exchanges = children[i].getChildren();
                            for (int x = 0; x < exchanges.length; x++)
                            {
                                if (exchanges[x].getName().equals("exchange")) //$NON-NLS-1$
                                {
                                    String id = exchanges[x].getAttribute("id");
                                    combo.setData(String.valueOf(combo.getItemCount()), id); //$NON-NLS-1$
                                    combo.add(exchanges[x].getAttribute("name")); //$NON-NLS-1$
                                }
                            }
                        }
                    }
                }
            }
        }
        
        combo.setEnabled(combo.getItemCount() > 1);
    }
}
