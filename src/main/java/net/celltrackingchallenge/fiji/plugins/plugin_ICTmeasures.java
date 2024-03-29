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

import net.celltrackingchallenge.measures.TrackDataCache;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogService;

import org.scijava.widget.FileWidget;
import java.io.File;

import net.celltrackingchallenge.measures.SEG;
import net.celltrackingchallenge.measures.DET;
import net.celltrackingchallenge.measures.TRA;

@Plugin(type = Command.class, menuPath = "Plugins>Cell Tracking Challenge>Technical measures",
        name = "CTC_ICT", headless = true,
		  description = "Calculates technical tracking performance measures from the CTC paper.\n"
				+"The plugin assumes certain data format, please see\n"
				+"http://celltrackingchallenge.net/submission-of-results/")
public class plugin_ICTmeasures implements Command
{
	//------------- GUI stuff -------------
	//
	@Parameter
	private LogService log;

	@Parameter(label = "Path to computed result folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain result files directly: mask???.tif and res_track.txt",
		persistKey = "ctc_res_folder")
	private File resPath;

	@Parameter(label = "Path to ground-truth folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain folders SEG, TRA and files: SEG/man_seg*.tif, TRA/man_track???.tif and TRA/man_track.txt",
		persistKey = "ctc_gt_folder")
	private File gtPath;

	@Parameter(label = "Number of digits used in the image filenames:", min = "1",
		description = "Set to 3 if your files are, e.g., t000.tif, or to 5 if your files are, e.g., t00021.tif")
	public int noOfDigits = 3;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterA
		= "Note that folders has to comply with certain data format, please see";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterB
		= "http://celltrackingchallenge.net/submission-of-results/";


	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false,
		label = "Select measures to calculate:")
	private final String measuresHeader = "";

	@Parameter(label = "SEG",
		description = "Quantifies the amount of overlap between the reference annotations and the computed segmentation.")
	private boolean calcSEG = true;

	@Parameter(label = "TRA",
		description = "Evaluates the ability of an algorithm to track cells in time.")
	private boolean calcTRA = true;

	@Parameter(label = "DET",
			description = "Evaluates the ability of an algorithm to detect (without tracking) cells.")
	private boolean calcDET = true;


	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false,
		label = "Select optional preferences:")
	private final String optionsHeader = "";

	@Parameter(label = "Do verbose logging",
		description = "Besides reporting the measure value itself, it also reports measurement details that lead to this value.")
	private boolean optionVerboseLogging = true;

	@Parameter(label = "Do consistency check",
		description = "Checks multiple consistency-oriented criteria on both input and GT data before measuring TRA.")
	private boolean optionConsistency = true;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String moreOptionsNote = "Note that the Segmentation and Tracking Fiji menus offer these measures with more options.";


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


	//hidden output values
	@Parameter(type = ItemIO.OUTPUT)
	String RESdir;
	@Parameter(type = ItemIO.OUTPUT)
	String GTdir;
	@Parameter(type = ItemIO.OUTPUT)
	String sep = "--------------------";

	@Parameter(type = ItemIO.OUTPUT)
	double SEG = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double DET = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double TRA = -1;


	//the GUI path entry function:
	@Override
	public void run()
	{
		//saves the input paths for the final report table
		GTdir  = gtPath.getPath();
		RESdir = resPath.getPath();

		if (calcSEG)
		{
			try {
				final SEG seg = new SEG(log);
				seg.doLogReports = optionVerboseLogging;
				seg.noOfDigits = noOfDigits;
				SEG = seg.calculate(GTdir, RESdir);
			}
			catch (RuntimeException e) {
				log.error("CTC SEG measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC SEG measure error: "+e.getMessage());
			}
		}

		TrackDataCache tradetCache = null;
		if (calcTRA)
		{
			try {
				final TRA tra = new TRA(log);
				tra.doConsistencyCheck = optionConsistency;
				tra.doLogReports = optionVerboseLogging;
				tra.noOfDigits = noOfDigits;
				TRA = tra.calculate(GTdir, RESdir);
				tradetCache = tra.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC TRA measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC TRA measure error: "+e.getMessage());
			}
		}

		if (calcDET)
		{
			try {
				final DET det = new DET(log);
				det.doLogReports = optionVerboseLogging;
				det.noOfDigits = noOfDigits;
				DET = det.calculate(GTdir, RESdir, tradetCache);
			}
			catch (RuntimeException e) {
				log.error("CTC DET measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC DET measure error: "+e.getMessage());
			}
		}

		//do not report anything explicitly (unless special format for parsing is
		//desired) as ItemIO.OUTPUT will make it output automatically
	}
}
