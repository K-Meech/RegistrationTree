# RegistrationTree

Registration for very large 3D images stored in the BigDataViewer HDF5 or n5 formats.

- Wrapper for BigWarp (https://github.com/saalfeldlab/bigwarp) and Elastix (https://elastix.lumc.nl/) allowing easy passing of images between the two (builds on top of Christian Tischer's elastix wrapper: https://github.com/embl-cba/elastixWrapper)

- Cropped and downsampled regions can be selected interactively to send to elastix (minimising the amount of data that must be re-written / making it easier to use with very large images).

- Registrations can be built up / viewed / compared in an arbitrary tree, allowing easy comparison of different registration options.

Currently only **affine** registrations are supported.
Time series and multichannel images are not supported.

Designed for registration of 3D images, but 2D should also be supported (not tested explicitly!)

## Installation

RegistrationTree is a Fiji plugin. If you haven't used Fiji/ImageJ before - you can download it from [the ImageJ website](https://imagej.net/Fiji).  
If you're using an existing Fiji installation, make sure it is up to date!  
Go to `Help > Update...` in the imagej menu, and select `Apply Changes`.  

### Add the RegistrationTree update site
Go to `Help > Update...` in the imagej menu, and select `Manage update sites`.  
In the new window that pops up, select `Add Unlisted Site`. This will create a new row in the table.  
Fill this row out so it looks like below:  

| Active | Name          | URL           | Host   | Directory on Host | Description |
| -------| ------------- | ------------- | ------ | ------            | ------      |
| &check;| `RegistrationTree`   | `https://sites.imagej.net/RegistrationTree/` | | |

Then click `Apply and Close`, followed by `Apply Changes`.

## User guide

A short user guide is provided in the wiki:
https://github.com/K-Meech/RegistrationTree/wiki

## Cite

If you use RegistrationTree in your work, please cite our [paper on eLife](https://elifesciences.org/articles/80899)
