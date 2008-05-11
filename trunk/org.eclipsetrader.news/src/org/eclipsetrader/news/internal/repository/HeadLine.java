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

package org.eclipsetrader.news.internal.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipsetrader.core.instruments.ISecurity;
import org.eclipsetrader.news.core.IHeadLine;

@XmlRootElement(name = "headline")
@XmlType(name = "org.eclipsetrader.news.Headline")
public class HeadLine implements IHeadLine {
	@XmlAttribute(name = "date")
	@XmlJavaTypeAdapter(DateTimeAdapter.class)
	private Date date;

	private boolean recent;

	@XmlAttribute(name = "readed")
	private boolean readed;

    @XmlElement(name = "text")
	private String text;

	@XmlElement(name = "link")
	private String link;

	@XmlElement(name = "source")
	private String source;

	@XmlElementWrapper(name = "members")
    @XmlElement(name = "security")
	@XmlJavaTypeAdapter(SecurityAdapter.class)
	private List<ISecurity> members;

	public HeadLine() {
	}

	public HeadLine(Date date, String source, String text, ISecurity[] members, String link) {
	    this.date = date;
	    this.source = source;
	    this.text = text;
	    this.members = members != null ? new ArrayList<ISecurity>(Arrays.asList(members)) : null;
	    this.link = link;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#getDate()
     */
	@XmlTransient
	public Date getDate() {
    	return date;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#getSource()
     */
	@XmlTransient
	public String getSource() {
    	return source;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#getText()
     */
	@XmlTransient
	public String getText() {
    	return text;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#contains(org.eclipsetrader.core.instruments.ISecurity)
     */
	public boolean contains(ISecurity security) {
		return members.contains(security);
	}

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#getMembers()
     */
	@XmlTransient
	public ISecurity[] getMembers() {
    	return members.toArray(new ISecurity[members.size()]);
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#isReaded()
     */
	@XmlTransient
    public boolean isReaded() {
	    return readed;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#setReaded(boolean)
     */
    public void setReaded(boolean readed) {
    	this.readed = readed;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#isRecent()
     */
	@XmlTransient
    public boolean isRecent() {
	    return recent;
    }

	public void setRecent(boolean recent) {
    	this.recent = recent;
    }

	/* (non-Javadoc)
     * @see org.eclipsetrader.news.core.IHeadLine#getLink()
     */
	@XmlTransient
    public String getLink() {
    	return link;
    }

	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof IHeadLine))
    		return false;
    	IHeadLine other = (IHeadLine) obj;
    	if (link.equals(other.getLink()))
    		return true;
	    return text.equals(other.getText()) && (source == other.getSource() || (source != null && source.equals(other.getSource())));
    }

	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
	    return 3 * text.hashCode() + 7 * (source != null ? source.hashCode() : 0);
    }
}
