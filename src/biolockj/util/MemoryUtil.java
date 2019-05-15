/**
 * @UNCC Fodor Lab
 * @author Michael Sioda
 * @email msioda@uncc.edu
 * @date Nov 7, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

import java.text.DecimalFormat;
import biolockj.Log;

/**
 * This Java memory report utility can be used to help identify memory leaks in the application.
 */
public class MemoryUtil {
	/**
	 * Print stats on memory usage: Max, Total, ∆Total, %Free, Free, ∆Free.
	 * 
	 * @param msg Print msg with stats
	 */
	public static void reportMemoryUsage( final String msg ) {
		// Initialize by printing current values, skip delta values
		if( prevTotal == 0 && prevFree == 0 && prevTotal == 0 ) {
			reportInitMemory( msg );
			return;
		}

		final long total = Runtime.getRuntime().totalMemory();
		final long free = Runtime.getRuntime().freeMemory();

		if( total != prevTotal || free != prevFree ) {
			// reportMemoryInBytes();
			Log.debug( MemoryUtil.class,
				msg + ": => Max: " + getMaxMemoryInMiB() + ", Total: " + getTotalMemoryInMiB() + ", ∆Total: " +
					getChangeFormatted( total, prevTotal ) + ", %Free: " + getPercentageFreeFormatted() + ", Free: " +
					getFreeMemoryInMiB() + ", ∆Free: " + getChangeFormatted( free, prevFree ) );
			prevTotal = total;
			prevFree = free;
		} else Log.debug( MemoryUtil.class, msg + " no change" );
	}

	private static double bytesToMiB( final long val ) {
		return val / MEGABYTE_FACTOR;
	}

	private static String getChangeFormatted( final long prev, final long cur ) {
		final double maxMiB = bytesToMiB( prev - cur );
		return String.format( "%s %s", decimalFormat.format( maxMiB ), MIB );
	}

	private static String getFreeMemoryInMiB() {
		final double freeMiB = bytesToMiB( Runtime.getRuntime().freeMemory() );
		return String.format( "%s %s", decimalFormat.format( freeMiB ), MIB );
	}

	private static String getMaxMemoryInMiB() {
		final double maxMiB = bytesToMiB( Runtime.getRuntime().maxMemory() );
		return String.format( "%s %s", decimalFormat.format( maxMiB ), MIB );
	}

	private static double getPercentageFree() {
		return (double) ( Runtime.getRuntime().maxMemory() - getUsedMemory() ) / Runtime.getRuntime().maxMemory() * 100;
	}

	private static String getPercentageFreeFormatted() {
		final double freePercentage = getPercentageFree();
		return decimalFormat.format( freePercentage ) + "%";
	}

	private static String getTotalMemoryInMiB() {
		final double totalMiB = bytesToMiB( Runtime.getRuntime().totalMemory() );
		return String.format( "%s %s", decimalFormat.format( totalMiB ), MIB );
	}

	private static long getUsedMemory() {
		return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory();
	}

	private static void reportInitMemory( final String msg ) {
		prevFree = Runtime.getRuntime().freeMemory();
		prevTotal = Runtime.getRuntime().totalMemory();
		Log.debug( MemoryUtil.class, msg + ": => Max: " + getMaxMemoryInMiB() + ", Total: " + getTotalMemoryInMiB() +
			", %Free: " + getPercentageFreeFormatted() + ", Free: " + getFreeMemoryInMiB() );
	}

	@SuppressWarnings("unused")
	private static void reportMemoryInBytes() {
		Log.debug( MemoryUtil.class, "MAXIMUM MEMORY: " + Runtime.getRuntime().maxMemory() );
		Log.debug( MemoryUtil.class, "TOTAL MEMORY: " + Runtime.getRuntime().totalMemory() );
		Log.debug( MemoryUtil.class, "FREE  MEMORY: " + Runtime.getRuntime().freeMemory() );
		Log.debug( MemoryUtil.class, "To convert to mb, divide by: " + MEGABYTE_FACTOR );
	}

	private static final DecimalFormat decimalFormat = new DecimalFormat( "####0" );
	private static final long MEGABYTE_FACTOR = 1024L * 1024L;
	private static final String MIB = "mb";
	private static long prevFree = 0;
	private static long prevTotal = 0;
}
