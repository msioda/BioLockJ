// BioLockJ v0.9.3-beta generated Nextflow Template with 49 BioModule processes
// Path: /Users/mike/git/BioLockJ/resources/docker/nextflow/nextflow_template_main.nf

aws.efs = $aws.efs
project.pipelineName = $project.pipelineName
project.pipelineDir = "${aws.efs}/${project.pipelineName}"

process AddMetadataToPathwayTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_AddMetadataToPathwayTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process AddMetadataToTaxaTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_AddMetadataToTaxaTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process AwkFastaConverter {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_AwkFastaConverter/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process BuildQiimeMapping {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_BuildQiimeMapping/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process BuildTaxaTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_BuildTaxaTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process CompileOtuCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_CompileOtuCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Demultiplexer {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Demultiplexer/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Email {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Email/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Gunzipper {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Gunzipper/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Humann2Classifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Humann2Classifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Humann2Parser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Humann2Parser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process ImportMetadata {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_ImportMetadata/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process JsonReport {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_JsonReport/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process KneadDataSanitizer {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_KneadDataSanitizer/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Kraken2Classifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Kraken2Classifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Kraken2Parser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Kraken2Parser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process KrakenClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_KrakenClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process KrakenParser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_KrakenParser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process LogTransformTaxaTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_LogTransformTaxaTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process MergeQiimeOtuTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_MergeQiimeOtuTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Metaphlan2Classifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Metaphlan2Classifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Metaphlan2Parser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Metaphlan2Parser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process Multiplexer {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_Multiplexer/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process NormalizeTaxaTables {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_NormalizeTaxaTables/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process PearMergeReads {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_PearMergeReads/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process QiimeClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_QiimeClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process QiimeClosedRefClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_QiimeClosedRefClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process QiimeDeNovoClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_QiimeDeNovoClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process QiimeOpenRefClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_QiimeOpenRefClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process QiimeParser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_QiimeParser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process R_CalculateStats {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_R_CalculateStats/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process R_PlotEffectSize {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_R_PlotEffectSize/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process R_PlotMds {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_R_PlotMds/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process R_PlotOtus {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_R_PlotOtus/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process R_PlotPvalHistograms {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_R_PlotPvalHistograms/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RarefyOtuCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RarefyOtuCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RarefySeqs {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RarefySeqs/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RdpClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RdpClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RdpParser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RdpParser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RegisterNumReads {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RegisterNumReads/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RemoveLowOtuCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RemoveLowOtuCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RemoveLowPathwayCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RemoveLowPathwayCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RemoveScarceOtuCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RemoveScarceOtuCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process RemoveScarcePathwayCounts {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_RemoveScarcePathwayCounts/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process SeqFileValidator {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_SeqFileValidator/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process SlimmClassifier {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_SlimmClassifier/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process SlimmParser {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_SlimmParser/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
process TrimPrimers {
	echo 	true
	cpus 	$nextflow.cpus
	memory 	$nextflow.memory
	label 	$nextflow.dockerImage
	input:
	val worker from Channel.watchPath( '${project.pipelineDir}/**_TrimPrimers/script/*.sh' )
	"""
	#!/bin/bash
	bash ${worker}
	"""
}
