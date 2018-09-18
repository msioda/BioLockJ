/**
 * This package contains {@link biolockj.module.BioModule}s that are implicitly added to QIIME pipeline as needed.
 * <ul>
 * <li>BioModule {@link biolockj.module.implicit.qiime.BuildQiimeMapping} is added to every QIIME pipeline immediately
 * before the configured QIIME classifier module to convert the metadata file into a valid QIIME mapping file.
 * <li>BioModule {@link biolockj.module.implicit.qiime.MergeOtuTables} is added after
 * {@link biolockj.module.classifier.r16s.QiimeClosedRefClassifier} if the samples are configured to be classified in
 * parallel batches.
 * </ul>
 */
package biolockj.module.implicit.qiime;
