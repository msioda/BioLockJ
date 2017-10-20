BioLockJ v1.0 supports RDP, QIIME, KRAKEN, MetPhlAn, and SLIMM classifiers & includes a suite of customizable sequence preparation and report modules.   Any Java class implementing the BioModule interface can be included in a BioLockJ pipeline.  Simply tag the Java class names with the **#BioModule** indicator and modules will be executed in the order listed in the configuration file.    

## Why use BioLockJ?
*  Organize your analysis by centralizing configuration parameters and external dependencies in a single file
*  Reproducing analysis is a simple as re-executing your configuraiton file.
*  BioLockJ generated bash scripts save time and avoid scripting errors
*  Switch between local and clustered Linux environments by updating a single configuration property
*  Simplify interactions with your high performance computing cluster
*  The standardized OTU table format facilitates direct comparisons between classifiers

![alt text](https://github.com/mikesioda/BioLockJ/blob/master/doc/img/BioLockJ_Flowchart.png "BioLockJ System Diagram")

## Sample Pipeline
| BioModule | Description | 
| :--- | :--- | 
| biolockj.module.Metadata | Import, format, and validate metadata | 
| biolockJ.module.seq.Trimmer | Remove sequence primers |  
| biolockj.module.classifier.r16s.RdpClassifier | Classify 16S sequences with RDP |  
| biolockj.module.parser.r16s.RdpParser | Parse RDP output to generate relative abundance tables |  
| biolockj.module.report.RReport | Generate univariate summary statistics, histograms, & boxplots with R to find statistically significant metadata-OTU relationships |  
| biolockj.module.Email | Receive an email summary when your pipeline is finished that includes a single UNIX command to download the R script, OTU abundance tables, & summary reports  |  

## Installation
BioLockJ is deployed as a single JAR file, which can be executed by Java.
  * Required software: Java, R, R packages (Kendall & Coin) 
  * BioLockJ can be deployed with as few as one classifier, the rest are optional  
  * Unused classifiers and their dependencies do not need to be installed

## Launching BioLockJ
  * Execute ./run.sh <path_to_config> to start a BioLockJ pipeline
