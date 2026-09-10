# OpenCV Test App

An Android app for trying out OpenCV image-processing algorithms on the phone,
from the camera or the gallery. Built as a sandbox while learning the OpenCV
Android SDK.

## Contents

- [Overview](#overview)
- [Algorithms](#algorithms)
  - [Canny edge detection](#canny-edge-detection)
  - [Document scanner](#document-scanner)
- [Project layout](#project-layout)
- [Building](#building)
- [Notes](#notes)
- [License](#license)

## Overview

The main screen has a spinner to pick an algorithm and a toolbar to load an image
from the **camera** or the **gallery**. OpenCV is loaded at runtime
(`OpenCVLoader`, static init from the bundled `openCVLibrary320` module).

- **Platform:** Android, `compileSdk 25`, `minSdk 15`
- **OpenCV:** 3.2.0 (Java bindings + native `jniLibs` for all ABIs)
- Originally an Eclipse ADT project, imported to Gradle.

## Algorithms

### Canny edge detection

Two variants:

- **Canny Photo** &mdash; load a still image, convert to grayscale, run
  `Imgproc.Canny(gray, out, 50, 150)` on a background thread, show the edge map.
- **Canny Video** &mdash; the same, per frame, on the live camera feed
  (`CvCameraViewListener2.onCameraFrame`).

### Document scanner

`Scanner.java` finds a page in a photo and flattens it to a rectangle:

1. Downscale to ~320&times;240 and Gaussian blur.
2. **Find the page outline** &mdash; grayscale &rarr; `Canny(50, 150)` &rarr;
   `findContours` &rarr; keep the largest-area contour (assumed to be the page).
3. **Find the corners** &mdash; `HoughLinesP` on that contour, then every pairwise
   line intersection that falls inside the frame; deduplicate near-identical
   points.
4. Reduce to four corners (the four farthest from the centroid), sort them
   top-left / top-right / bottom-right / bottom-left.
5. **Warp** &mdash; `getPerspectiveTransform` + `warpPerspective` onto a
   rectangle sized from the detected edge lengths.

If fewer than four corners are found it returns *"Cannot detect perfect
corners"* and shows the corner points it did find. An alternative page-vs-background
segmentation using 2-cluster k-means (`binaryImageKMeans`, pick the cluster
closest to pure white) is included but commented out.

## Project layout

```
TestAlgorithm/
├── app/src/main/java/com/example/testalgorithm/
│   ├── MainActivity.java        menu, camera/gallery, algorithm spinner
│   ├── CannyPhotoActivity.java  Canny on a still image
│   ├── CannyVideoActivity.java  Canny on the live camera
│   ├── ScannerActivity.java     runs the document scanner
│   └── Scanner.java             the scanner pipeline (contours, Hough, warp)
├── app/src/main/jniLibs/        OpenCV 3.2.0 native libs (committed)
└── openCVLibrary320/            OpenCV 3.2.0 Android module (committed)
```

## Building

Open `TestAlgorithm/` in Android Studio and run on a device (the app needs a
camera and storage permission). The OpenCV SDK is vendored, so no manifest
`OpenCV Manager` install is required.

## Notes

- Targets an old SDK (25) and OpenCV 3.2.0; the OpenCV Android API is largely
  unchanged in 4.x but the Gradle/SDK setup would need updating.
- `jniLibs/` and `openCVLibrary320/build/` are committed &mdash; large, and worth
  moving to a dependency or `.gitignore`.
- `Scanner`'s corner detection is brittle on cluttered backgrounds; a
  `approxPolyDP` on the largest contour is usually more robust than Hough-line
  intersections.

## License

No license file is included yet (the author's other repositories use
[MIT](https://opensource.org/licenses/MIT)). Note that the vendored OpenCV
libraries carry their own 3-clause BSD license.
