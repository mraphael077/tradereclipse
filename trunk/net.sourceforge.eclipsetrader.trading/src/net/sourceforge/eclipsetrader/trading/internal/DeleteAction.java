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

package net.sourceforge.eclipsetrader.trading.internal;

import net.sourceforge.eclipsetrader.core.db.WatchlistItem;
import net.sourceforge.eclipsetrader.trading.views.WatchlistView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DeleteAction extends Action
{
    private WatchlistView view;

    public DeleteAction(WatchlistView view)
    {
        this.view = view;
        setText("&Delete");
        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        setEnabled(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run()
    {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (MessageDialog.openConfirm(window.getShell(), "Watchlist", "Do you really want to delete the selected item ?"))
        {
            WatchlistItem[] items = view.getSelection();
            for (int i = 0; i < items.length; i++)
                view.getWatchlist().getItems().remove(items[i]);
        }
    }
}
