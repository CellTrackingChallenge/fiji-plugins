/*-
 * #%L
 * CTC-Fiji-plugins
 * %%
 * Copyright (C) 2017 - 2023 Vladimír Ulman
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.celltrackingchallenge.fiji.plugins;

import net.celltrackingchallenge.measures.util.BgMaskCreator;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.Logger;
import org.scijava.log.LogService;
import org.scijava.widget.FileWidget;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import net.celltrackingchallenge.measures.util.NumberSequenceHandler;

@Plugin(type = Command.class, menuPath = "Plugins>Cell Tracking Challenge>Create BG Masks",
        name = "CTC_BG", headless = true,
        description = "Creates mandatory aux images to calculate dataset quality measures from the CTC paper.\n"
				+"The plugin assumes certain data format, please see\n"
				+"http://celltrackingchallenge.net/submission-of-results/")
public class plugin_CreateBGMask implements Command
{
	@Parameter
	private LogService logService;

	@Parameter(label = "Path to annotations folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path must contain folder TRA and annotation TRA/man_track???.tif files. "
			+ "Path may contain BG folder. Files BG/mask???.tif will be created at the path. "
			+ "The TRA/man_track???.tif must provide realistic masks of cells (not just blobs representing centres etc.).")
	private File annPath;

	@Parameter(label = "Number of digits used in the image filenames:", min = "1",
		description = "Set to 3 if your files are, e.g., t000.tif, or to 5 if your files are, e.g., t00021.tif")
	public int noOfDigits = 3;

	@Parameter(label = "Do only these timepoints (e.g. 1-9,23,25):",
		description = "Comma separated list of numbers or intervals, interval is number-hyphen-number.",
		validater = "timePointsStrValidator")
	private String fileIdxStr = "";

	@SuppressWarnings("unused")
	private void timePointsStrValidator()
	{
		//check the string is parse-able
		NumberSequenceHandler.toSet(fileIdxStr,null);
	}

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterA
			= "Note that folders has to comply with certain data format, please see";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterB
			= "http://celltrackingchallenge.net/submission-of-results/";

	@Parameter(label = "Post processing erosion, pixel radius:", min = "0",
		description = "Set to 0 to disable the post-processing.")
	int widthOfPostprocessingErosion = 0;

	@Parameter(label = "Create one BG mask over all timepoints:",
		description = "Unchecked: BG mask adapted individually to each timepoint. "
			+"Checked: BG mask adapted to an union of all FG masks across all timepoints.")
	boolean doOneMask = false;

	//citation footer...
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = "Please, cite us:")
	private final String citationFooterA
		= "Maška M, Ulman V, Delgado-Rodriguez P, ..., Ortiz-de-Solórzano C.";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = ":")
	private final String citationFooterB
		= "The Cell Tracking Challenge: 10 years of objective benchmarking";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = ":")
	private final String citationFooterC
		= "Nature Methods. 2023. doi:10.1038/s41592-023-01879-y";


	//the GUI path entry function:
	@Override
	public void run()
	{
		final Logger log = logService.subLogger("DatasetMeasures.CreateBG");

		final BgMaskCreator.Builder b = new BgMaskCreator.Builder()
				.setupForCTC(Paths.get(annPath.getAbsolutePath()),noOfDigits,widthOfPostprocessingErosion)
				.forTheseTimepointsOnly(NumberSequenceHandler.toSet(fileIdxStr))
				.setSciJavaLogger(log);
		if (doOneMask) b.setupToFindOneMaskValidForAllTimepoints();
		else b.setupToCreateIndividualMaskForEachTimepoint();

		try {
			b.build().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
