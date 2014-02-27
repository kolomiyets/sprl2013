package be.liir.SpRL.model;

import javax.xml.bind.annotation.XmlTransient;

import be.liir.SpRL.jaxb.util.StringUtils;


public abstract class WithIdentifier {

	@XmlTransient
	public String ID_PREFIX;
	@XmlTransient
	public abstract String getId();
	public abstract void setId(String value);
	
	/**
	 * This method serves for setting xml trainsient fields for <code>Identifyable</code> JAXB-objects. 
	 * Put all methods you want to be invoked in this method. 
	 */
	public void afterInit(){
		setPrefix();
	}
	
	
	private void setPrefix(){
		ID_PREFIX = StringUtils.getCharPrefix(getId());
	}

	
}
