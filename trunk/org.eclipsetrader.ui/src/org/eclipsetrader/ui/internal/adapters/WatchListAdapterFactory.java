/*
 * Copyright (c) 2004-2011 Marco Maccaferri and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Maccaferri - initial API and implementation
 */

package org.eclipsetrader.ui.internal.adapters;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipsetrader.core.views.IWatchList;
import org.eclipsetrader.ui.UIConstants;
import org.eclipsetrader.ui.internal.UIActivator;

public class WatchListAdapterFactory implements IAdapterFactory {

    private Image icon;

    private LabelProvider labelProvider = new LabelProvider() {

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
         */
        @Override
        public Image getImage(Object element) {
            if (element instanceof IAdaptable) {
                element = ((IAdaptable) element).getAdapter(IWatchList.class);
            }
            if (element instanceof IWatchList) {
                return icon;
            }
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
         */
        @Override
        public String getText(Object element) {
            if (element instanceof IAdaptable) {
                element = ((IAdaptable) element).getAdapter(IWatchList.class);
            }
            if (element instanceof IWatchList) {
                return ((IWatchList) element).getName();
            }
            return null;
        }
    };

    public WatchListAdapterFactory() {
        if (UIActivator.getDefault() != null) {
            icon = UIActivator.getDefault().getImageRegistry().get(UIConstants.REPOSITORY_OBJECT);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adaptableObject instanceof IWatchList) {
            if (adapterType.isAssignableFrom(labelProvider.getClass())) {
                return labelProvider;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Class[] getAdapterList() {
        return new Class[] {
            ILabelProvider.class,
        };
    }
}
