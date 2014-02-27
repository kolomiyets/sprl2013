package be.liir.SpRL.model;

import javax.xml.bind.annotation.XmlTransient;

@XmlTransient
public abstract class WithContent extends WithIdentifier implements Containable {


	/* (non-Javadoc)
	 * @see be.liir.SpRL.model.WithIdentifier#getId()
	 */
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see be.liir.SpRL.model.WithIdentifier#setId(java.lang.String)
	 */
	@Override
	public void setId(String value) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
