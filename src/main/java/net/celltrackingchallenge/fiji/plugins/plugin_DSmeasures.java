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

import net.imagej.ops.OpService;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogLevel;
import org.scijava.log.Logger;
import org.scijava.log.LogService;

import org.scijava.widget.FileWidget;
import java.io.File;

import net.celltrackingchallenge.measures.ImgQualityDataCache;
import net.celltrackingchallenge.measures.SNR;
import net.celltrackingchallenge.measures.CR;
import net.celltrackingchallenge.measures.HETI;
import net.celltrackingchallenge.measures.HETB;
import net.celltrackingchallenge.measures.RES;
import net.celltrackingchallenge.measures.SHA;
import net.celltrackingchallenge.measures.SPA;
import net.celltrackingchallenge.measures.CHA;
import net.celltrackingchallenge.measures.OVE;
import net.celltrackingchallenge.measures.MIT;
/*
import net.celltrackingchallenge.measures.SYN;
import net.celltrackingchallenge.measures.ENTLEAV;
*/

@Plugin(type = Command.class, menuPath = "Plugins>Cell Tracking Challenge>Dataset measures",
        name = "CTC_DS", headless = true,
		  description = "Calculates dataset quality measures from the CTC paper.\n"
				+"The plugin assumes certain data format, please see\n"
				+"http://celltrackingchallenge.net/submission-of-results/")
public class plugin_DSmeasures implements Command
{
	@Parameter
	private LogService logService;
	@Parameter
	private OpService opService;

	@Parameter(label = "Path to images folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain cell image files directly: t???.tif")
	private File imgPath;

	@Parameter(label = "Number of digits used in the image filenames:", min = "1",
		description = "Set to 3 if your files are, e.g., t000.tif, or to 5 if your files are, e.g., t00021.tif")
	public int noOfDigits = 3;

	@Parameter(label = "Resolution (um/px) of the images, x-axis:",
		min = "0.0001", stepSize = "0.1",
		description = "Size of single pixel/voxel along the x-axis in micrometers.")
	double xRes = 1.0;

	@Parameter(label = "y-axis:",
		min = "0.0001", stepSize = "0.1",
		description = "Size of single pixel/voxel along the y-axis in micrometers.")
	double yRes = 1.0;

	@Parameter(label = "z-axis:",
		min = "0.0001", stepSize = "0.1",
		description = "Size of single pixel/voxel along the z-axis in micrometers.")
	double zRes = 1.0;

	@Parameter(label = "Path to annotations folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain folders BG and TRA and annotation files: "
			+ "BG/mask???.tif, TRA/man_track???.tif and man_track.txt. "
			+ "The TRA/man_track???.tif must provide realistic masks of cells (not just blobs representing centres etc.).")
	private File annPath;

	@Parameter(label = "Verbose log:")
	boolean doVerboseLogging = false;

	@Parameter(label = "Per cell reporting:", choices = {
			"None",
			"To console, grouped by video, timepoint then cell_id",
			"To console, grouped by video, cell_id then timepoint",
			"To console with separating empty lines, grouped by video, cell_id then timepoint"
	})
	String doPerCellReporting = "None";

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterA
		= "Note that folders has to comply with certain data format, please see";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterB
		= "http://celltrackingchallenge.net/submission-of-results/";


	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false,
		label = "Select measures to calculate:")
	private final String measuresHeader = "";

	@Parameter(label = "SNR",
		description = "Evaluates the average signal to noise ratio over all annotated cells.")
	private boolean calcSNR = true;

	@Parameter(label = "CR",
		description = "Evaluates the average contrast ratio over all annotated cells.")
	private boolean calcCR = true;

	@Parameter(label = "Heti",
		description = "Evaluates the average internal signal heterogeneity of the cells.")
	private boolean calcHeti = true;

	@Parameter(label = "Hetb",
		description = "Evaluates the heterogeneity (as standard deviation) of the signal between cells.")
	private boolean calcHetb = true;

	@Parameter(label = "Res",
		description = "Evaluates the average resolution, measured as the average size of the cells in number of pixels (2D) or voxels (3D).")
	private boolean calcRes = true;

	@Parameter(label = "Sha",
		description = "Evaluates the average regularity of the cell shape, normalized between 0 (completely irregular) and 1 (perfectly regular).")
	private boolean calcSha = false;

	@Parameter(label = "Spa",
		description = "Evaluates the cell density measured as average minimum pixel (2D) or voxel (3D) distance between cells.")
	private boolean calcSpa = true;

	@Parameter(label = "Cha",
		description = "Evaluates the absolute change of the average intensity of the cells with time.")
	private boolean calcCha = true;

	@Parameter(label = "Ove",
		description = "Evaluates the average level of overlap of the cells in consecutive frames, normalized between 0 (no overlap) and 1 (complete overlap).")
	private boolean calcOve = true;

	@Parameter(label = "Mit",
		description = "Evaluates the average number of division events per frame.")
	private boolean calcMit = true;

	/*
	@Parameter(label = "Syn",
		description = "Evaluates the foo.")
	private boolean calcSyn = true;

	@Parameter(label = "EntLeav",
		description = "Evaluates the foo.")
	private boolean calcEntLeav = true;
	*/


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
	String IMGdir;
	@Parameter(type = ItemIO.OUTPUT)
	String ANNdir;
	@Parameter(type = ItemIO.OUTPUT)
	String sep = "--------------------";

	@Parameter(type = ItemIO.OUTPUT)
	double SNR = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double CR = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Heti = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Hetb = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Res = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Sha = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Spa = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Cha = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Ove = -1;

	@Parameter(type = ItemIO.OUTPUT)
	double Mit = -1;

	/*
	@Parameter(type = ItemIO.OUTPUT)
	byte Syn = -1;

	@Parameter(type = ItemIO.OUTPUT)
	byte EntLeav = -1;
	*/


	//the GUI path entry function:
	@Override
	public void run()
	{
		final Logger log = logService.subLogger("DatasetMeasures",
				doVerboseLogging ? LogLevel.TRACE : LogLevel.INFO);

		//store the resolution information
		final double[] resolution = new double[3];
		resolution[0] = xRes;
		resolution[1] = yRes;
		resolution[2] = zRes;

		//saves the input paths for the final report table
		IMGdir = imgPath.getPath();
		ANNdir = annPath.getPath();

		//reference on a shared object that does
		//pre-fetching of data and some common pre-calculation
		//
		//create an "empty" object and tell it what features we wanna calculate,
		//the first measure to be calculated will recognize that this object does not fit
		//and will make a new one that fits and will retain the flags of demanded features
		ImgQualityDataCache cache = new ImgQualityDataCache(log,opService);
		if (calcSpa) cache.doDensityPrecalculation = true;
		if (calcSha) cache.doShapePrecalculation = true;
		cache.noOfDigits = noOfDigits;

		//do the calculation and retrieve updated cache afterwards
		if (calcSNR)
		{
			try {
				final SNR snr = new SNR(log);
				SNR = snr.calculate(IMGdir, resolution, ANNdir, cache);
				cache = snr.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC SNR measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC SNR measure error: "+e.getMessage());
			}
		}

		if (calcCR)
		{
			try {
				final CR cr = new CR(log);
				CR = cr.calculate(IMGdir, resolution, ANNdir, cache);
				cache = cr.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC CR measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC CR measure error: "+e.getMessage());
			}
		}

		if (calcHeti)
		{
			try {
				final HETI heti = new HETI(log);
				Heti = heti.calculate(IMGdir, resolution, ANNdir, cache);
				cache = heti.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Heti measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Heti measure error: "+e.getMessage());
			}
		}

		if (calcHetb)
		{
			try {
				final HETB hetb = new HETB(log);
				Hetb = hetb.calculate(IMGdir, resolution, ANNdir, cache);
				cache = hetb.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Hetb measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Hetb measure error: "+e.getMessage());
			}
		}

		if (calcRes)
		{
			try {
				final RES res = new RES(log);
				Res = res.calculate(IMGdir, resolution, ANNdir, cache);
				cache = res.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Res measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Res measure error: "+e.getMessage());
			}
		}

		if (calcSha)
		{
			try {
				final SHA sha = new SHA(log,opService);
				Sha = sha.calculate(IMGdir, resolution, ANNdir, cache);
				cache = sha.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Sha measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Sha measure error: "+e.getMessage());
			}
		}

		if (calcSpa)
		{
			try {
				final SPA spa = new SPA(log);
				Spa = spa.calculate(IMGdir, resolution, ANNdir, cache);
				cache = spa.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Den measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Den measure error: "+e.getMessage());
			}
		}

		if (calcCha)
		{
			try {
				final CHA cha = new CHA(log);
				Cha = cha.calculate(IMGdir, resolution, ANNdir, cache);
				cache = cha.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Cha measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Cha measure error: "+e.getMessage());
			}
		}

		if (calcOve)
		{
			try {
				final OVE ove = new OVE(log);
				Ove = ove.calculate(IMGdir, resolution, ANNdir, cache);
				cache = ove.getCache();
			}
			catch (RuntimeException e) {
				log.error("CTC Ove measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Ove measure error: "+e.getMessage());
			}
		}

		if (calcMit)
		{
			try {
				final MIT mit = new MIT(log);
				Mit = mit.calculate(null,null, ANNdir);
			}
			catch (RuntimeException e) {
				log.error("CTC Mit measure problem: "+e.getMessage());
			}
			catch (Exception e) {
				log.error("CTC Mit measure error: "+e.getMessage());
			}
		}

		if (doPerCellReporting.startsWith("None")) return;
		if (doPerCellReporting.contains("timepoint then cell"))
		{
			System.out.println(ImgQualityDataCache.MeasuresTableRow.printHeader());
			for (ImgQualityDataCache.MeasuresTableRow row : cache.getMeasuresTable())
				System.out.println(row);
			return;
		}
		//
		//else: cell_id then timepoint
		int curId = -1;
		final boolean doSeparating = doPerCellReporting.contains("separating");
		System.out.println(ImgQualityDataCache.MeasuresTableRow.printHeader());
		for (ImgQualityDataCache.MeasuresTableRow row : cache.getMeasuresTable_GroupedByCellsThenByVideos()) {
			if (doSeparating && row.cellTraId != curId) {
				if (curId != -1) System.out.println(); //print empty line before the listing of another cell
				curId = row.cellTraId;
			}
			System.out.println(row);
		}
	}
}
