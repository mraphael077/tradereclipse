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

package org.eclipsetrader.directa.internal.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipsetrader.core.feed.FeedIdentifier;
import org.eclipsetrader.core.feed.IFeedIdentifier;
import org.eclipsetrader.core.feed.IFeedProperties;
import org.eclipsetrader.core.instruments.ISecurity;
import org.eclipsetrader.core.instruments.Security;
import org.eclipsetrader.core.repositories.IRepositoryService;
import org.eclipsetrader.core.trading.BrokerException;
import org.eclipsetrader.core.trading.IBroker;
import org.eclipsetrader.core.trading.IOrder;
import org.eclipsetrader.core.trading.IOrderChangeListener;
import org.eclipsetrader.core.trading.IOrderMonitor;
import org.eclipsetrader.core.trading.IOrderRoute;
import org.eclipsetrader.core.trading.IOrderSide;
import org.eclipsetrader.core.trading.IOrderType;
import org.eclipsetrader.core.trading.IOrderValidity;
import org.eclipsetrader.core.trading.OrderChangeEvent;
import org.eclipsetrader.core.trading.OrderDelta;
import org.eclipsetrader.directa.internal.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BrokerConnector implements IBroker, IExecutableExtension, IExecutableExtensionFactory {
	public static final IOrderRoute Immediate = new OrderRoute("1", "immed");
	public static final IOrderRoute MTA = new OrderRoute("2", "MTA");
	public static final IOrderRoute CloseMTA = new OrderRoute("4", "clos-MTA");
	public static final IOrderRoute AfterHours = new OrderRoute("5", "AfterHours");
	public static final IOrderRoute Open = new OrderRoute("7", "open//");

	public static final IOrderValidity Valid30Days = new OrderValidity("30days", "30 Days");

	private static BrokerConnector instance;

	private String id;
	private String name;

	private Set<OrderMonitor> orders;
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

	private Thread thread;
	private Runnable updateRunnable = new Runnable() {
        public void run() {
            orders = new HashSet<OrderMonitor>();

            for (;;) {
            	synchronized(thread) {
            		try {
            			updateOrders();
    	                thread.wait(60 * 1000);
                    } catch (InterruptedException e) {
    	                break;
                    }
            	}
        	}
        }
	};

	public BrokerConnector() {
	}

	public static BrokerConnector getInstance() {
		if (instance == null)
			instance = new BrokerConnector();
		return instance;
	}

	/* (non-Javadoc)
     * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
     */
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
    	id = config.getAttribute("id");
    	name = config.getAttribute("name");
    }

	/* (non-Javadoc)
     * @see org.eclipse.core.runtime.IExecutableExtensionFactory#create()
     */
    public Object create() throws CoreException {
		if (instance == null)
			instance = this;
	    return instance;
    }

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#connect()
     */
    public void connect() {
    	if ("".equals(WebConnector.getInstance().getUser()))
    		WebConnector.getInstance().login();

    	if (thread == null || !thread.isAlive()) {
    		thread = new Thread(updateRunnable, getName() + " - Orders Monitor");
        	thread.start();
    	}
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#disconnect()
     */
    public void disconnect() {
    	if (thread != null) {
    		try {
        		thread.interrupt();
	            thread.join(30 * 1000);
            } catch (InterruptedException e) {
	            // Do nothing
            }
    		thread = null;
    	}
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#canTrade(org.eclipsetrader.core.instruments.ISecurity)
     */
    public boolean canTrade(ISecurity security) {
		IFeedIdentifier identifier = security.getIdentifier();
		if (identifier == null)
			return false;

		IFeedProperties properties = (IFeedProperties) identifier.getAdapter(IFeedProperties.class);
		if (properties != null) {
			for (int p = 0; p < WebConnector.PROPERTIES.length; p++) {
				if (properties.getProperty(WebConnector.PROPERTIES[p]) != null)
					return true;
			}
		}

	    return false;
    }

	protected String getSecurityFeedSymbol(ISecurity security) {
		IFeedIdentifier identifier = security.getIdentifier();
		if (identifier == null)
			return null;

		IFeedProperties properties = (IFeedProperties) identifier.getAdapter(IFeedProperties.class);
		if (properties != null) {
			for (int p = 0; p < WebConnector.PROPERTIES.length; p++) {
				if (properties.getProperty(WebConnector.PROPERTIES[p]) != null)
					return properties.getProperty(WebConnector.PROPERTIES[p]);
			}
		}

		return null;
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#getSecurityFromSymbol(java.lang.String)
     */
    public ISecurity getSecurityFromSymbol(String symbol) {
		ISecurity security = null;

		if (Activator.getDefault() != null) {
			BundleContext context = Activator.getDefault().getBundle().getBundleContext();
			ServiceReference serviceReference = context.getServiceReference(IRepositoryService.class.getName());
			if (serviceReference != null) {
				IRepositoryService service = (IRepositoryService) context.getService(serviceReference);

				ISecurity[] securities = service.getSecurities();
				for (int i = 0; i < securities.length; i++) {
					String feedSymbol = getSecurityFeedSymbol(securities[i]);
					if (feedSymbol != null && feedSymbol.equals(symbol)) {
						security = securities[i];
						break;
					}
				}

				context.ungetService(serviceReference);
			}
		}

		if (security == null)
			security = new Security("Unknown", new FeedIdentifier(symbol, null));

		return security;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#prepareOrder(org.eclipsetrader.core.trading.IOrder)
     */
    public IOrderMonitor prepareOrder(IOrder order) throws BrokerException {
		if (order.getType() != IOrderType.Limit && order.getType() != IOrderType.Market)
			throw new BrokerException("Invalid order type, must be Limit or Market");
		if (order.getSide() != IOrderSide.Buy && order.getSide() != IOrderSide.Sell)
			throw new BrokerException("Invalid order side, must be Buy or Sell");
		if (order.getValidity() != IOrderValidity.Day && order.getValidity() != Valid30Days)
			throw new BrokerException("Invalid order validity, must be Day or 30 Days");

		OrderMonitor tracker = new OrderMonitor(WebConnector.getInstance(), this, order);
		orders.add(tracker);
		fireUpdateNotifications(new OrderDelta[] {
				new OrderDelta(OrderDelta.KIND_ADDED, tracker),
			});

		synchronized(thread) {
			thread.notifyAll();
		}

		return tracker;
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getAllowedSides()
	 */
	public IOrderSide[] getAllowedSides() {
		return new IOrderSide[] {
				IOrderSide.Buy,
				IOrderSide.Sell,
			};
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getAllowedTypes()
	 */
	public IOrderType[] getAllowedTypes() {
		return new IOrderType[] {
				IOrderType.Limit,
				IOrderType.Market,
			};
	}

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getAllowedValidity()
	 */
	public IOrderValidity[] getAllowedValidity() {
		return new IOrderValidity[] {
				IOrderValidity.Day,
				Valid30Days,
			};
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#getAllowedRoutes()
     */
    public IOrderRoute[] getAllowedRoutes() {
	    return new IOrderRoute[] {
    			BrokerConnector.Immediate,
    			BrokerConnector.MTA,
    			BrokerConnector.CloseMTA,
    			BrokerConnector.Open,
    			BrokerConnector.AfterHours,
    		};
    }

	/* (non-Javadoc)
	 * @see org.eclipsetrader.core.trading.IBroker#getOrders()
	 */
	public IOrderMonitor[] getOrders() {
		if (orders == null)
			return new IOrderMonitor[0];
		return orders.toArray(new IOrderMonitor[orders.size()]);
	}

	public void updateOrders() {
		List<OrderDelta> deltas = new ArrayList<OrderDelta>();

		synchronized(orders) {
			WebConnector.getInstance().updateOrders();

			Collection<OrderMonitor> repositoryOrder = WebConnector.getInstance().getOrders();
	        for (OrderMonitor order : repositoryOrder) {
	        	if (!orders.contains(order)) {
	        		orders.add(order);
	        		deltas.add(new OrderDelta(OrderDelta.KIND_ADDED, order));
	        	}
	        	else
	        		deltas.add(new OrderDelta(OrderDelta.KIND_UPDATED, order));
	        }
			for (Iterator<OrderMonitor> iter = orders.iterator(); iter.hasNext(); ) {
				OrderMonitor order = iter.next();
	        	if (!repositoryOrder.contains(order)) {
	        		iter.remove();
	        		deltas.add(new OrderDelta(OrderDelta.KIND_REMOVED, order));
	        	}
	        }
		}

		if (deltas.size() != 0)
			fireUpdateNotifications(deltas.toArray(new OrderDelta[deltas.size()]));
	}

	public void wakeupOrdersUpdateThread() {
		synchronized(thread) {
			thread.notifyAll();
		}
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#addOrderChangeListener(org.eclipsetrader.core.trading.IOrderChangeListener)
     */
    public void addOrderChangeListener(IOrderChangeListener listener) {
		listeners.add(listener);
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.core.trading.IBroker#removeOrderChangeListener(org.eclipsetrader.core.trading.IOrderChangeListener)
     */
    public void removeOrderChangeListener(IOrderChangeListener listener) {
		listeners.remove(listener);
    }

    protected void fireUpdateNotifications(OrderDelta[] deltas) {
    	if (deltas.length != 0) {
    		OrderChangeEvent event = new OrderChangeEvent(this, deltas);
        	Object[] l = listeners.getListeners();
    		for (int i = 0; i < l.length; i++) {
    			try {
    				((IOrderChangeListener) l[i]).orderChanged(event);
    			} catch(Throwable e) {
        			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, 0, "Error running listener", e); //$NON-NLS-1$
        			Activator.log(status);
    			}
    		}
    	}
    }
}
