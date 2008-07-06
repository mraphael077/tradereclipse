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

package org.eclipsetrader.core.trading;

public class OrderDelta {
	public static final int KIND_ADDED = 1;
	public static final int KIND_REMOVED = 2;
	public static final int KIND_UPDATED = 3;

	private int kind;
	private IOrder order;

	public OrderDelta(int kind, IOrder order) {
		this.kind = kind;
		this.order = order;
	}

	public int getKind() {
    	return kind;
    }

	public IOrder getOrder() {
    	return order;
    }
}
