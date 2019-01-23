package biolockj;

import java.io.File;
import java.util.List;

public class Branch
{
	public Branch( final String branchType, final List<String> ids )
	{
		this.ids = ids;
		this.branchType = branchType;
	}

	public String getBranchType()
	{
		return branchType;
	}

	public List<String> getIds()
	{
		return ids;
	}

	public String getStatus()
	{
		return status;
	}

	private String branchType = null;

	private List<String> ids = null;

	private final String status = null;

	/**
	 * Branch type {@value #CLASSIFIER_TYPE}<br>
	 * 1st module reads input sequence files<br>
	 * 1st module outputs classifier specific output files<br>
	 */
	public static final String CLASSIFIER_TYPE = "classifier";

	/**
	 * Branch type {@value #OTU_COUNT_TYPE} is associated with modules reads input files that pass
	 * {@link biolockj.util.OtuUtil#isOtuFile(File)}
	 */
	public static final String OTU_COUNT_TYPE = "otu_count";
	/**
	 * Internal {@link biolockj.Config} String property: {@value #R_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} if input files are some type of count table merged
	 * with the metadata such as those output by {@link biolockj.module.report.taxa.AddMetadataToTaxaTables}. These
	 * files can be input into any {@biolockj.module.r.R_Module}.
	 */
	public static final String R_TYPE = "R";
	public static final String REPORT_TYPE = "report";

	/**
	 * Internal {@link biolockj.Config} String property: {@value #SEQ_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} for sequence input files.
	 */
	public static final String SEQ_TYPE = "seq";
	/**
	 * Internal {@link biolockj.Config} String property: {@value #STATS_TYPE}<br>
	 * Set as the value of {@value #INTERNAL_PIPELINE_INPUT_TYPES} if input files are tables of statistics such as those
	 * output by {@link biolockj.module.r.CalculateStats}.
	 */
	public static final String STATS_TYPE = "stats";
	/**
	 * The 1st module of type {@value #TAXA_COUNT_TYPE} reads input files that pass
	 * {@link biolockj.util.OtuUtil#isOtuFile(File)}
	 */
	public static final String TAXA_COUNT_TYPE = "taxa_count";
}
