Welcome
-------
This is a repository with Java source codes of the [Fiji](http://fiji.sc) plugins related to the [Cell Tracking Challenge](http://www.celltrackingchallenge.net).
The plugins make GUI-accessible [measures](https://github.com/CellTrackingChallenge/measures) for quantitative evaluation of biomedical tracking in general.
In particular, one can find here:

* Technical (developer-oriented) tracking and segmentation measures: TRA, SEG, DET
* Biological (user-oriented) measures: CT, TF, BC(i), CCA
* Dataset quality measures: SNR, CR, Hetb, Heti, Res, Sha, Spa, Cha, Ove, Mit
* Tracking accuracy evaluation with general [Acyclic Oriented Graphs Measure (AOGM)](http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0144959)

The binaries of the technical measures can be downloaded from the [official challenge website](http://www.celltrackingchallenge.net).
The recommended method, however, is to install all plugins (and measures) via the Fiji update mechanism, see below.
The Fiji update system mirrors the most recent versions of the measures and tools, and regularly checks for their newer versions.

Owing to the capabilities of Fiji, it is possible to call the plugins also from command line, in a batch mode.

The tools were developed and the page is maintained by [Vladimír Ulman](http://www.fi.muni.cz/~xulman/).

See also [the CTC measures repository](https://github.com/CellTrackingChallenge/measures).


Enabling update site in a new or existing Fiji installation:
------------------------------------------------------------
1. Open Fiji
1. Click menus: 'Help' -> 'Update...'
1. Click 'Manage update sites' in the opened 'ImageJ Updater' dialog
1. Mark the 'CellTrackingChallenge' checkbox
1. Click 'Close' to close the dialog
1. Click 'Apply changes' in the updater dialog window


Notes
------
Once installed, one can find the tools in the Fiji, in the _Plugins_ menu
(and in the _Cell Tracking Challenge_, _Segmentation_ and _Tracking_ sub-menus).

Contact (ulman při fi.muni.cz) for help on how to use it, or how to do the batch mode processing.
