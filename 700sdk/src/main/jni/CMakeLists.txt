# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)
#cmake_minimum_required(VERSION 3.22.1)


set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/libs/${ANDROID_ABI})

#添加子目录，将自动找到子目录中的CMakeLists.txt
ADD_SUBDIRECTORY(${PROJECT_SOURCE_DIR}/sigleEcg)
ADD_SUBDIRECTORY(${PROJECT_SOURCE_DIR}/offline)
ADD_SUBDIRECTORY(${PROJECT_SOURCE_DIR}/online)
