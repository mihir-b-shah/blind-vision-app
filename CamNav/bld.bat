call vcvars64
cd build
title BuildOpenCVProject
cmake -DOpenCV_DIR="C:/Users/mihir/OpenCV/opencv/build" ..
msbuild CamNav.sln
cd Debug