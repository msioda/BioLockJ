package biolockj.module.parser;

import java.util.Set;
import biolockj.module.BioModule;

public interface ParserModule extends BioModule
{
	public void addParsedSample( ParsedSample sample ) throws Exception;

	public void buildOtuCountTables() throws Exception;

	public ParsedSample getParsedSample( String sampleId );

	public Set<ParsedSample> getParsedSamples();

	public void parseSamples() throws Exception;
}
