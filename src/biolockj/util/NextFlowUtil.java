/**
 * @UNCC Fodor Lab
 * @author Anthony Fodor
 * @email anthony.fodor@gmail.com
 * @date Aug 9, 2018
 * @disclaimer This code is free software; you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any
 * later version, provided that any use properly credits the author. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details at http://www.gnu.org *
 */
package biolockj.util;

/**
 * S
 */
public class NextFlowUtil
{

	public static void buildNextflowMain() throws Exception
	{

	}

	// process createprojectdir {
	//
	// echo true //enable forward stdout
	//
	// label 'image_biolockj_java_module_latest'
	// //container 'job-definition://BLJ_mjzapata2_ubuntu_latest:2'
	// cpus 4
	// memory '16 GB'
	//
	// output:
	// val true into create_complete_ch
	//
	// """
	// #!/bin/bash
	// echo "LS /efs/ before mkdir projectdir:"
	// ls -al ${params.efsdir}
	// echo ""
	//
	// aws s3 ls mytestbucketmz123
	// mkdir ${params.projectdir}
	//
	// echo "LS /efs/ after mkdir projectdir:"
	// ls -al ${params.projectdir}
	// """
	// }

}
