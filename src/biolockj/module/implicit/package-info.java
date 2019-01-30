/**
 * The modules in this package are implicitly added to pipelines as needed.<br>
 * These modules cannot be directly added to any pipeline unless overridden via project.disableImplicitModules=Y.
 * <ol>
 * <li>BioModule {@link biolockj.module.implicit.ImportMetadata} always runs as 1st module.
 * <li>BioModule {@link biolockj.module.implicit.Demultiplexer} always runs 2nd if data is multiplexed.
 * </ol>
 */
package biolockj.module.implicit;
