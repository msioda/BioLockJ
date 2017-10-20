package biolockj.node;

import java.util.Map;

public interface OtuNode
{
	public void addOtu( String levelDelim, String otu );

	public Long getCount();

	public String getLine();

	public Map<String, String> getOtuMap();

	public String getSampleId();

	public void report();

	public void setCount( Long count );

	public void setLine( String line );

	public void setSampleId( final String sampleId );
}
