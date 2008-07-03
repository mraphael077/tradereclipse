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

package org.eclipsetrader.directa.internal.core.repository;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.eclipsetrader.core.feed.IFeedIdentifier;
import org.eclipsetrader.core.feed.IFeedProperties;

@XmlRootElement(name = "list")
@XmlType(name = "org.eclipsetrader.directaworld.IdentifiersList")
public class IdentifiersList {
	private static IdentifiersList instance;

	private static String[] PROPERTIES = new String[] {
			"org.eclipsetrader.directa.symbol",
			"org.eclipsetrader.directaworld.symbol",
			"org.eclipsetrader.borsaitalia.code",
		};

    @XmlElementRef
	private List<IdentifierType> identifiers;

	public IdentifiersList() {
		instance = this;
		identifiers = new ArrayList<IdentifierType>();
	}

	public static IdentifiersList getInstance() {
    	return instance;
    }

	@XmlTransient
	public List<IdentifierType> getIdentifiers() {
    	return identifiers;
    }

	public void setIdentifiers(List<IdentifierType> identifiers) {
    	this.identifiers = identifiers;
    }

	public IdentifierType getIdentifierFor(IFeedIdentifier identifier) {
		String symbol = identifier.getSymbol();

		IFeedProperties properties = (IFeedProperties) identifier.getAdapter(IFeedProperties.class);
		if (properties != null) {
			for (int i = 0; i < PROPERTIES.length; i++) {
				if (properties.getProperty(PROPERTIES[i]) != null) {
					symbol = properties.getProperty(PROPERTIES[i]);
					break;
				}
			}
		}

		for (IdentifierType type : identifiers) {
			if (type.getSymbol().equals(symbol)) {
				type.setIdentifier(identifier);
				return type;
			}
		}

		IdentifierType type = new IdentifierType(symbol);
		type.setIdentifier(identifier);
		identifiers.add(type);
		return type;
	}

	public IdentifierType getIdentifierFor(String symbol) {
		for (IdentifierType type : identifiers) {
			if (type.getSymbol().equals(symbol))
				return type;
		}

		IdentifierType type = new IdentifierType(symbol);
		identifiers.add(type);
		return type;
	}
}