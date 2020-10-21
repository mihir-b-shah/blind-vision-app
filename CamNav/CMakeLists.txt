#[[command is "cmake -DOpenCV_DIR="C:/Users/mihir/OpenCV/opencv/build" ."]]
cmake_minimum_required(VERSION 2.8)
project( CamNav )
find_package( OpenCV REQUIRED )
include_directories( ${OpenCV_INCLUDE_DIRS} )
add_executable( CamNav CamNav.cpp )
target_link_libraries( CamNav ${OpenCV_LIBS} )