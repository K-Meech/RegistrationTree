# RegistrationTree

Registration for very large 3D images stored in the BigDataViewer HDF5 or n5 formats.

- Wrapper for BigWarp (https://github.com/saalfeldlab/bigwarp) and Elastix (https://elastix.lumc.nl/) allowing easy passing of images between the two (builds on top of Christian Tischer's elastix wrapper: https://github.com/embl-cba/elastixWrapper)

- Cropped and downsampled regions can be selected interactively to send to elastix (minimising the amount of data that must be re-written / making it easier to use with very large images).

- Registrations can be built up / viewed / compared in an arbitrary tree, allowing easy comparison of different registration options.

Currently only **affine** registrations are supported.
Time series and multichannel images are not supported.

Designed for registration of 3D images, but 2D should also be supported (not tested explicitly!)
