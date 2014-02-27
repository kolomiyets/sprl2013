package be.liir.SpRL.model;

import java.math.BigInteger;

public interface Markable {
	public BigInteger getStart();
	public void setStart(BigInteger bigInteger);

	public BigInteger getEnd();
	public void setEnd(BigInteger bigInteger);

	public String getText();
	public void setText(String text);

}
