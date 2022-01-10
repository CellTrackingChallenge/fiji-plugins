/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2017-2022, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.celltrackingchallenge.fiji.plugins;

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogService;

import org.scijava.widget.FileWidget;
import java.io.File;

import net.celltrackingchallenge.measures.TrackDataCache;
import net.celltrackingchallenge.measures.CT;
import net.celltrackingchallenge.measures.TF;
import net.celltrackingchallenge.measures.BCi;
import net.celltrackingchallenge.measures.CCA;

@Plugin(type = Command.class, menuPath = "Plugins>Cell Tracking Challenge>Biological measures",
        name = "CTC_BIO", headless = true,
		  description = "Calculates biological tracking performance measures from the CTC paper.\n"
				+"The plugin assumes certain data format, please see\n"
				+"http://www.celltrackingchallenge.net/submission-of-results.html")
public class plugin_BIOmeasures implements Command
{
	//------------- GUI stuff -------------
	//
	@Parameter
	private LogService log;

	@Parameter(label = "Path to computed result folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain result files directly: mask???.tif and res_track.txt")
	private File resPath;

	@Parameter(label = "Path to ground-truth folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain folder TRA and files: TRA/man_track???.tif and TRA/man_track.txt")
	private File gtPath;

	@Parameter(label = "Number of digits used in the image filenames:", min = "1",
		description = "Set to 3 if your files are, e.g., t000.tif, or to 5 if your files are, e.g., t00021.tif")
	public int noOfDigits = 3;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterA
		= "Note that folders has to comply with certain data format, please see";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterB
		= "http://www.celltrackingchallenge.net/submission-of-results.html";


	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false,
		label = "Select measures to calculate:")
	private final String measuresHeader = "";

	@Parameter(label = "CT",
		description = "Examines how good a method is at reconstructing complete reference tracks.")
	private boolean calcCT = true;

	@Parameter(label = "TF",
		description = "Targets the longest, correctly reconstructed, continuous fraction of a reference track.")
	private boolean calcTF = true;

	@Parameter(label = "BC(i)",
		description = "Examines how good a method is at reconstructing mother-daughter relationships.")
	private boolean calcBCi = true;

	@Parameter(label = "i =", min = "0", max = "5",
		description = "Value of 'i' for which the BC(i) should be reported.")
	private int iForBCi = 2;

	@Parameter(label = "CCA",
		description = "Reflects the ability of an algorithm to discover true distribution of cell cycle lengths in a video.")
	private boolean calcCCA = true;


	//citation footer...
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = "Please, cite us:")
	private final String citationFooterA
		= "Ulman V, Maška M, Magnusson KEG, ..., Ortiz-de-Solórzano C.";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = ":")
	private final String citationFooterB
		= "An objective comparison of cell-tracking algorithms.";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false, label = ":")
	private final String citationFooterC
		= "Nature Methods. 2017. doi:10.1038/nmeth.4473";


	//hidden output values
	@Parameter(type = ItemIO.OUTPUT)
	String RESdir;
	@Parameter(type = ItemIO.OUTPUT)
	String GTdir;
	@Parameter(type = ItemIO.OUTPUT)
	String sep = "--------------------";

	@Parameter(type = ItemIO.OUTPUT)
	double CT = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double TF = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double BCi = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double CCA = -1;


	//the GUI path entry function:
	@Override
	public void run()
	{
		//saves the input paths for the final report table
		GTdir  = gtPath.getPath();
		RESdir = resPath.getPath();

		//reference on a shared object that does
		//pre-fetching of data and some common pre-calculation
		TrackDataCache cache = new TrackDataCache(log);
		cache.noOfDigits = noOfDigits;

		if (calcCT )
		{
			try {
				final CT ct = new CT(log);
				CT = ct.calculate(GTdir, RESdir, cache);
				cache = ct.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC CT measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC CT measure error: "+e.getMessage());
			}
		}

		if (calcTF )
		{
			try {
				final TF tf = new TF(log);
				TF = tf.calculate(GTdir, RESdir, cache);
				cache = tf.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC TF measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC TF measure error: "+e.getMessage());
			}
		}

		if (calcBCi)
		{
			try {
				final BCi bci = new BCi(log);
				if (calcBCi) bci.setI(iForBCi);
				BCi = bci.calculate(GTdir, RESdir, cache);
				cache = bci.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC BC(i) measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC BC(i) measure error: "+e.getMessage());
			}
		}

		if (calcCCA)
		{
			try {
				final CCA cca = new CCA(log);
				CCA = cca.calculate(GTdir, RESdir, cache);
				cache = cca.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC CCA measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC CCA measure error: "+e.getMessage());
			}
		}

		//do not report anything explicitly (unless special format for parsing is
		//desired) as ItemIO.OUTPUT will make it output automatically
	}
}
