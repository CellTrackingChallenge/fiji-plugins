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

import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.log.LogService;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedShortType;

import org.scijava.widget.FileWidget;
import java.io.File;
import java.nio.file.Files;

import net.celltrackingchallenge.measures.TRA;
import net.celltrackingchallenge.measures.TrackDataCache;

@Plugin(type = Command.class, menuPath = "Plugins>Tracking>AOGM: Tracking data consistency",
        name = "CTC_AOGM_consistency", headless = true,
		  description = "Checks the tracking data for consistency.\n"
				+"It is the same consistency as is used in the AOGM tracking measure.\n"
				+"The plugin assumes certain data format, please see\n"
				+"http://celltrackingchallenge.net/submission-of-results/")
public class plugin_AOGMconsistency implements Command
{
	//------------- GUI stuff -------------
	//
	@Parameter
	private LogService log;

	@Parameter(label = "Path to tracking data folder:",
		style = FileWidget.DIRECTORY_STYLE,
		description = "Path should contain result files directly, or TRA folder with the files.",
		persistKey = "ctc_sequence_folder")
	private File resPath;

	@Parameter(label = "Path contains GT or result data:",
		choices = {"RES: mask???.tif and res_track.txt",
		           "RES: mask????.tif and res_track.txt",
		           "GT: TRA/man_track???.tif and TRA/man_track.txt",
		           "GT: TRA/man_track????.tif and TRA/man_track.txt"},
		description = "Choose what naming convention is implemented in the data folder.")
	private String resPathType;

	@Parameter(label = "Do empty images check:",
		description = "Checks if no label is found in either ground-truth or result image before measuring TRA.")
	private boolean checkEmptyImages = true;

	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterA
		= "Note that folder has to comply with certain data format, please see";
	@Parameter(visibility = ItemVisibility.MESSAGE, persist = false, required = false)
	private final String pathFooterB
		= "http://celltrackingchallenge.net/submission-of-results/";


	@Parameter(type = ItemIO.OUTPUT)
	boolean consistent = false;

	static private
	final String[] inputNames = { "/res_track.txt",     "%s/mask%03d.tif",
	                              "/res_track.txt",     "%s/mask%04d.tif",
	                              "/TRA/man_track.txt", "%s/TRA/man_track%03d.tif",
	                              "/TRA/man_track.txt", "%s/TRA/man_track%04d.tif" };

	//the GUI path entry function:
	@Override
	public void run()
	{
		try {
			log.info("Input path: "+resPath);

			final TrackDataCache cache = new TrackDataCache(log);
			final TRA tra = new TRA(log);
			final int inputNamesChooser = ( resPathType.startsWith("RES") ?
			                (resPathType.indexOf("????") == -1 ? 0 : 1)
			              : (resPathType.indexOf("????") == -1 ? 2 : 3) )*2;

			//load metadata with the lineages
			cache.LoadTrackFile(resPath+inputNames[inputNamesChooser], cache.res_tracks);

			//iterate through the data folder and read files, one by one,
			//and call ClassifyLabels() for every file
			int time = 0;
			while (Files.isReadable(
				new File(String.format(inputNames[inputNamesChooser+1],resPath,time)).toPath()))
			{
				//read the image
				Img<UnsignedShortType> img
					= cache.ReadImageG16(String.format(inputNames[inputNamesChooser+1],resPath,time));

				cache.ClassifyLabels(img, img, checkEmptyImages);
				++time;

				//to be on safe side (with memory)
				img = null;
			}

			if (cache.levels.size() == 0)
				throw new IllegalArgumentException("No input image was found!");

			consistent = true;
			try {
				tra.CheckConsistency(cache.levels, cache.res_tracks, (inputNamesChooser == 4 || inputNamesChooser == 6));
			}
			catch (IllegalArgumentException e)
			{
				//report the error and set the output flag
				log.info(e.getMessage());
				consistent = false;
			}
		}
		catch (RuntimeException e) {
			log.error("AOGM problem: "+e.getMessage());
		}
		catch (Exception e) {
			log.error("AOGM error: "+e.getMessage());
		}
	}
}
