/**
Author: Aaron Yerke
Purpose: Test if all of the properties' IDs and names are present in the config file generator forms.
To use:
  1. cut and paste standard properties file into standPropsString.
  2. cut and paste wiki table into convertPropertiesWikiToMap()
  3. start webapp with npm start
  4. in config menu type missingElementsWithDescr() in javascript console.

  NOTE: Checklist options dont get IDs.
*/

function findMissingProps(){
  const standPropsString = `# Deployment path: $BLJ/resources/config/default/standard.properties
  # All 100 available properties are listed (many commented out)
  ###################################################################
  #cluster.batchCommand=
  #cluster.host=
  #cluster.jobHeader=
  #cluster.modules=
  #cluster.runJavaAsScriptModule=
  #cluster.validateParams=
  ##################################################################
  demultiplexer.barcodeCutoff=0.05
  #demultiplexer.barcodeUseReverseCompliment=
  #demultiplexer.strategy=do_not_demux
  #demultiplexer.mapping=
  ##################################################################
  #docker.deleteContainerOnExit=
  #docker.user=
  #docker.imgVersion=
  ##################################################################
  #exe.awk=
  #exe.bowtie2=
  exe.bowtie2Params=no-unal, k 60
  #exe.classifier=
  #exe.classifierParams=
  #exe.docker=
  #exe.gzip=
  #exe.java=
  #exe.kneaddata=
  #exe.kneaddataParams=
  #exe.pear=
  #exe.pearParams=
  #exe.python=
  #exe.Rscript=
  #exe.samtools=
  #exe.vsearch=
  #exe.vsearchParams=
  ##################################################################
  #input.dirPaths=
  #input.ignoreFiles=
  input.requireCompletePairs=Y
  input.suffixFw=_R1
  input.suffixRv=_R2
  #input.trimPrefix=
  #input.trimSuffix=
  ##################################################################
  kraken.classifierParams=--only-classified-output, --preload
  #kraken.db=
  #kraken2.classifierParams=
  #kraken2.db=
  ##################################################################
  mail.encryptedPassword=SlrotqvCPGsFhWkKxtpwkQ==
  mail.from=biolockj@gmail.com
  mail.smtp.auth=Y
  mail.smtp.host=smtp.gmail.com
  mail.smtp.port=25
  mail.smtp.starttls.enable=Y
  mail.to=msioda@uncc.edu
  ##################################################################
  metadata.barcodeColumn=BarcodeSequence
  metadata.columnDelim=\t
  #metadata.commentChar=
  metadata.fileNameColumn=InputFileName
  #metadata.filePath=
  metadata.nullValue=NA
  metadata.required=Y
  #metadata.useEveryRow=
  ##################################################################
  multiplexer.gzip=Y
  ##################################################################
  #project.disablePreReqModules=
  #project.disableAddPreReqModules=
  #project.copyInput=
  project.defaultModuleDemultiplexer=biolockj.module.implicit.Demultiplexer
  project.defaultModuleFastaConverter=biolockj.module.seq.AwkFastaConverter
  project.defaultModuleSeqMerger=biolockj.module.seq.PearMergeReads
  #project.defaultProps=
  #project.deleteTempFiles=
  project.downloadDir=~/projects/downloads
  project.env=local
  project.logLevel=INFO
  #project.limitDebugClasses=
  project.permissions=770
  project.userProfile=~/bash_profile
  ##################################################################
  rarefyOtuCounts.iterations=10
  rarefyOtuCounts.quantile=0.5
  rarefyOtuCounts.removeSamplesBelowQuantile=N
  rarefyOtuCounts.lowAbundantCutoff=0.01
  ##################################################################
  qiime.alphaMetrics=shannon
  qiime.removeChimeras=Y
  ##################################################################
  r.colorBase=black
  r.colorHighlight=red
  r.colorPalette=npg
  r.colorPoint=black
  r.debug=Y
  #r.nominalFields=
  #r.numericFields=
  r.pch=20
  r.plotWidth=23
  r.pvalCutoff=0.05
  r.pValFormat=%1.2g
  r.rareOtuThreshold=0.25
  #r.excludeFields=
  #r.reportFields=
  #r.saveRData=
  r.timeout=10
  ##################################################################
  r_PlotEffectSize.parametricPval=Y
  r_PlotEffectSize.useAdjustedPvals=Y
  r_PlotEffectSize.excludePvalAbove=1
  # r_PlotEffectSize.taxa=
  r_PlotEffectSize.maxNumTaxa=40
  r_PlotEffectSize.cohensD=Y
  r_PlotEffectSize.rSquared=Y
  r_PlotEffectSize.foldChange=N
  ##################################################################
  rdp.minThresholdScore=80
  ##################################################################
  removeLowOtuCounts.minOtuCount=2
  ##################################################################
  removeScarceOtuCounts.cutoffPct=0.25
  ##################################################################
  report.logBase=10
  #report.minOtuCount=
  #report.minOtuThreshold=
  report.numHits=Y
  report.numReads=Y
  report.taxonomyLevels=phylum,class,order,family,genus
  ##################################################################
  #r_PlotMds.reportFields=
  r_PlotMds.numAxis=3
  r_PlotMds.distance=bray
  ##################################################################
  seqFileValidator.requireEqualNumPairs=Y
  #seqFileValidator.seqMaxLen=
  #seqFileValidator.seqMinLen=
  ##################################################################
  r_CalculateStats.pAdjustScope=LOCAL
  r_CalculateStats.pAdjustMethod=BH
  ##################################################################
  script.batchSize=6
  script.defaultHeader=#!/bin/bash
  script.numThreads=6
  script.permissions=770
  #script.timeout=
  ##################################################################
  #slimm.db=
  #slimm.refGenomeIndex=
  ##################################################################
  #trimPrimers.filePath=
  trimPrimers.requirePrimer=Y`;
  const standPropsLines = standPropsString.split('\n');

  let missingPropsId = new Set();
  let missingPropsIdAndName = new Set();

  for (var i = 0; i < standPropsLines.length; i++) {
    console.log(standPropsLines[i]);
    let line = standPropsLines[i];
    if (line.includes('=')){
      line = line.replace('#', '')
        .trim()
        .split('=')[0];
      console.log(line);
      if (!document.getElementById(line)){
        missingPropsId.add(line);
        if (!document.getElementsByName(line)){
          missingPropsIdAndName.add(line)
        }
      }
    }
  }//end forloop
  console.log('elements absent in ID search:', missingPropsId);
  console.log('elements absent in ID and name search', missingPropsIdAndName);
  return missingPropsId;
}//end findMissingProps

function convertPropertiesWikiToMap() {
  const wikiTable = `cluster.batchCommand	The command to submit jobs on the cluster
cluster.classifierHeader	Job script header to define # of nodes, # of cores, RAM, walltime, etc. by the ClassifierModule. If undefined, cluster.jobHeader will be used instead
cluster.host	Cluster host address
cluster.jobHeader	Job script header to define # of nodes, # of cores, RAM, walltime, etc.
cluster.modules	List of modules to load before execution. Adds “module load” command to bash scripts
cluster.numClassifierThreads	Integer value passed to ClassifierModule if it has a number of threads parameter. If undefined, script.numThreads will be used instead Useful because classifiers often require more resources than other modules
cluster.runJavaAsScriptModule	Options: Y/N. If Y, each JavaModule will instantiate a clone of the application in direct mode on a job node via a single worker script to avoid overworking the head node where BioLockJ is deployed
cluster.validateParams	Options: Y/N. If Y, validate cluster.jobHeader/cluster.classifierHeader "ppn:" or "procs:" value matches script.numThreads/cluster.numClassifierThreads
demux.barcodeUseReverseCompliment	Options: Y/N. Use reverse compliment of metadata.barcodeColumn if demux.strategy = barcode_in_header or barcode_in_seq.
demux.strategy	Options: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux.
Set the Demultiplexer strategy. If using barcodes, they must be provided in the metadata.filePath with in column name defined by metadata.barcodeColumn.
exe.awk	Define executable awk command, if default "awk" is not included in your $PATH
exe.bowtie2	Define executable bowtie2 command, if default "bowtie2" is not included in your $PATH
exe.bowtie2Params	Optional bowtie2 parameters used by SlimmClassifier
exe.classifier	Classifier executable command
exe.classifierParams	Optional classifier parameters, excluding parameters generated by BioLockJ (input files, output files, numThreads, etc. )
exe.gzip	Define executable gzip command, if default "gzip" is not included in your $PATH
exe.java	Define executable java command, if default "java" is not included in your $PATH
exe.pear	Define executable pear command, if default "pear" is not included in your $PATH
exe.pearParams	Optional pear parameters
exe.python	Define executable python command, if default "python" is not included in your $PATH
exe.Rscript	Define executable Rscript command, if default "Rscript" is not included in your $PATH
exe.samtools	Define executable samtools command, if default "samtools" is not included in your $PATH
exe.vsearch	Define executable vsearch command, if default "vsearch" is not included in your $PATH
exe.vsearchParams	Optional vsearch parameters
input.dirPaths	List of directories containing pipeline input files
input.ignoreFiles	List of files to ignore if found in * input.dirPaths*
input.requireCompletePairs	Options: Y/N. Stop pipeline if any unpaired FW or RV read sequence file is found
input.seqMaxLen	Maximum accepted length of sequence read
input.seqMinLen	Minimum accepted length of sequence read
input.suffixFw	File name suffix to indicate a forward read
input.suffixRv	File name suffix to indicate a reverse read
input.trimPrefix	For files named by Sample ID, provide the prefix preceding the ID to trim when extracting Sample ID. For multiplexed sequences, provide any characters in the sequence header preceding the ID. For fastq, this value could be “@” if the sample ID was added to the header immediately after the "@" symbol.
input.trimSuffix	For files named by Sample ID, provide the suffix after the ID, often this is just the file extension. Do not include read direction indicators listed in input.suffixFw/input.suffixRv. For multiplexed sequences, provide 1st character in the sequence header found after every embedded Sample ID. If undefined, “_” is used as the default end-of-sample-ID delimiter.
java.timeout	Sets # minutes before direct Java module script will time out and fail
kraken.db	Path to KRAKEN database
kraken2.db	Path to 2 KRAKEN database
mail.encryptedPassword	Encrypted password from email.from account. If BioLockJ is passed a 2nd parameter (in addition to the config file), the 2nd parameter should be the clear-text password. The password will be encrypted and stored in the prop file for future use. WARNING: Base64 encryption is only a trivial roadblock for malicious users. This functionality is intended merely to keep clear-text passwords out of the configuration files and should only be used with a disposable email.from account.
mail.from	Notification emails sent from this account, provided email.encryptedPassword is valid
mail.smtp.auth	Options: Y/N. Set the SMTP authorization property
mail.smtp.host	Email SMTP Host
mail.smtp.port	Email SMTP Host
mail.smtp.starttls.enable	Options: Y/N. Set the SMTP start TLS property
mail.to	Comma-separated email recipients list
metadata.barcodeColumn	Metadata column name containing the barcode used for demultiplexing
metadata.columnDelim	Define column delimiter for metadata.filePath file, default = tab
metadata.commentChar	Define how comments are indicated in metadata.filePath file, default = ""
metadata.fileNameColumn	Column in metadata file giving file names used to identify each sample. Standard default: "InputFileName". Values should be simple names, not file paths, and unique to each sample. Using this column in the metadata overrides the use of input.trimPreifx and input.trimSuffix. For paired reads, give the forward read file and use input.suffixFw and input.suffixRv to link to the reverse file.
metadata.filePath	Metadata file path, must have unique column headers
metadata.nullValue	Define how null values are represented in metadata
metadata.required	Options: Y/N. Require every sequence file has a corresponding row in metadata file
metadata.useEveryRow	Options: Y/N. Requires every metadata row to have a corresponding sequence file
project.allowImplicitModules	Options: Y/N. If Y, implicit modules can be directly added to pipeline via the Config file
project.copyInput	Options: Y/N. If Y, copy input.dirPaths into a new directory under the project root directory
project.defaultModuleDemultiplexer	Assign module to demultiplex datasets. Default = Demultiplexer
project.defaultModuleFastaConverter	Assign module to convert fastq sequence files into fasta format when required. Default = AwkFastaConverter
project.defaultModuleSeqMerger	Assign module to merge paired reads when required. Default = PearMergeReads
project.defaultProps	Path to a default BioLockJ configuration file containing default property values that are overridden if defined in the primary configuration file
project.deleteTempFiles	Options: Y/N. If Y, delete module temp dirs after execution
project.description	description of your project/configuration file
project.downloadDir	The pipeline summary includes an scp command for the user to download the pipeline analysis if executed on a cluster server. This property defines the target directory on the users workstation to which the analysis will be downloaded.
project.env	Options: aws, cluster, local. Describes runtime environment
project.logLevel	Options: DEBUG, INFO, WARN, ERROR. Determines Java log level sensitivity
project.permissions	Set chmod -R command security bits on pipeline root directory (Ex. 770)
project.runDocker	Options: Y/N. If Y, Docker will be used to run modules.
qiime.alphaMetrics	Options listed online: scikit-bio.org
qiime.removeChimeras	Options: Y/N. If Y, remove chimeras after open or de novo OTU picking using exe.vsearch
r.colorBase	This is the base color used for labels & headings in the PDF report
r.colorHighlight	This color is used to highlight significant OTU plot titles
r.colorPoint	Sets the color of scatterplot and strip-chart plot points
r.debug	Options: Y/N. If Y, will generate R Script log files
r.excludeFields	List metadata columns to exclude from R script reports
r.nominalFields	Explicitly override default field type assignment to model as a nominal field in R
r.numericFields	Explicitly override default field type assignment to model as a numeric field in R
r.pch	Sets R plot pch parameter for PDF report
r.pvalCutoff	Sets p-value cutoff used to assign label r.colorHighlight
r.pValFormat	Sets the format used in R sprintf() function
r.rareOtuThreshold	If >1, R will filter OTUs below value provided. If <1, R will interperate the value as a percentage and discard OTUs not found in at least that percentage of samples
r.reportFields	Override field used to explicitly list metadata columns to report in the R scripts. If left undefined, all columns are reported
r.saveRData	Options: Y/N. If Y, all R script generating BioModules will save R Session data to the module output directory to a file using the extension ".RData"
r.timeout	Sets # minutes before R Script will time out and fail
r_PlotMds.numAxis	Sets # MDS axis to plot
r_PlotMds.distance	distance metric for calculating MDS (default: bray)
r_PlotMds.reportFields	Override field used to explicitly list metadata columns to build MDS plots. If left undefined, all columns are reported
r_CalculateStats.pAdjustScope	Options: GLOBAL, LOCAL, TAXA, ATTRIBUTE. Used to set the p.adjust "n" parameter for how many simultaneous p-value calculations
r_CalculateStats.pAdjustMethod	Sets the p.adjust "method" parameter
r_PlotEffectSize.parametricPval	Options: Y/N. If Y, the parametric p-value is used when determining which taxa to include in the plot and which should get a (*). If N (default), the non-parametric p-value is used.
r_PlotEffectSize.disablePvalAdjustment	Options: Y/N. If Y, the non-adjusted p-value is used when determining which taxa to include in the plot and which should get a (*). If N (default), the adjusted p-value is used.
r_PlotEffectSize.excludePvalAbove	Options: [0,1], Taxa with a p-value above this value are excluded from the plot.
r_PlotEffectSize.taxa	Override other criteria for selecting which taxa to include in the plot by specifying wich taxa should be included
r_PlotEffectSize.maxNumTaxa	Each plot is given one page. This is the maximum number of bars to include in each one-page plot.
r_PlotEffectSize.disableCohensD	Options: Y/N. If N (default), produce plots for binary attributes showing effect size calculated as Cohen's d. If Y, skip this plot type.
r_plotEffectSize.disableRSquared	Options: Y/N. If N (default), produce plots showing effect size calculated as the r-squared value. If Y, skip this plot type.
r_plotEffectSize.disableFoldChange	Options: Y/N. If N (default), produce plots for binary attributes showing the fold change. If Y, skip this plot type.
rarefyOtuCounts.iterations	Positive integer. The number of iterations to randomly select the rarefyOtuCounts.quantile of OTUs
rarefyOtuCounts.lowAbundantCutoff	Minimum percentage of samples that must contain an OTU.
rarefyOtuCounts.quantile	Quantile for rarefication. The number of OTUs/sample are ordered, all samples with more OTUs than the quantile sample are subselected without replacement until they have the same number of OTUs as the quantile sample
rarefyOtuCounts.removeSamplesBelowQuantile	Options: Y/N. If Y, all samples below the rarefyOtuCounts.quantile quantile sample are removed
rarefySeqs.max	Randomly select maximum number of sequences per sample
rarefySeqs.min	Discard samples without minimum number of sequences
rdp.minThresholdScore	Required RDP minimum threshold score for valid OTUs
removeLowOtuCounts.minOtuCount	Minimum number of OTUs allowed, if a count less that this value is found, it is set to 0.
removeScarceOtuCounts.cutoffPct	Minimum percentage of samples that must contain an OTU for it to be kept.
report.logBase	Options: 10/e. If e, use natural log (base e), otherwise use log base 10
report.minOtuCount	ParserModule ignores OTU counts below this minimum number per sample when building raw count tabes
report.minOtuThreshold	ParserModule ignores OTU counts if the total OTU count for all samples falls below this minimum number when building raw count tabes
report.numHits	Options: Y/N. If Y, and add Num_Hits to metadata
report.numReads	Options: Y/N. If Y, and add Num_Reads to metadata
report.taxonomyLevels	Options: domain, phylum, class, order, family, genus, species. Generate reports for listed taxonomy levels
script.batchSize	Number of sequence files to process per worker script
script.defaultHeader	Used to set shebang line to define scripts as bash executables, such as "#!/bin/bash"
script.numThreads	Integer value passed to any module that takes a number of threads parameter
script.permissions	Set chmod command security bits on generated scripts (Ex. 770)
slimm.db	Path to SLIMM database
slimm.refGenomeIndex	Path the bowtie2 reference genome index
trimPrimers.filePath	Path to file containing one primer sequence per line.
trimPrimers.requirePrimer	Options: Y/N. If Y, TrimPrimers will discard reads that do not include a primer sequence.`
  const wikiTableArray = wikiTable.split('\n');
  let wikiTableMap = new Map();

  for (var i = 0; i < wikiTableArray.length; i++) {
    const split = wikiTableArray[i].split('\t')
    wikiTableMap.set(split[0], split[1])
  }
  return wikiTableMap;
}

function missingElementsWithDescr(){
  let missingElements = Array.from(findMissingProps());
  let wikiMap = convertPropertiesWikiToMap();

  for (let ele of wikiMap.keys()){
    if (!missingElements.includes(ele)){
      console.log(ele);
      wikiMap.delete(ele);
      console.log(wikiMap);
    }
  }
  return wikiMap;
}

