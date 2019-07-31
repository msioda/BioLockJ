/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Mar 03, 2019
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.exception;

import java.io.File;

/**
 * SequnceFormatException is thrown if errors occur processing fasta/fastq sequence files.
 */
public class SequnceFormatException extends BioLockJException {

	/**
	 * Create standard error to throw for fasta/fastq sequence file parsing errors.
	 *
	 * @param seqFile fasta/fastq sequence file (possibly compressed)
	 * @param msg Exception message details
	 */
	public SequnceFormatException( final File seqFile, final String msg ) {
		super( "Error processing SEQ file: " + seqFile.getAbsolutePath() + " --> " + msg );
	}

	/**
	 * Create standard error to throw for fasta/fastq sequence file parsing errors.
	 *
	 * @param msg Exception message details
	 */
	public SequnceFormatException( final String msg ) {
		super( msg );
	}

	private static final long serialVersionUID = -1092861177411734130L;
}
