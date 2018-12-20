# 指定CMake编译器的最低版本
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

# 添加头文件路径
include_directories(
        ./src/main/cpp
        ./src/main/cpp/ffmpeg
        ./src/main/cpp/include
)

# 定义源码所在目录
aux_source_directory(./src/main/cpp SRC)
aux_source_directory(./src/main/cpp/ffmpeg SRC_FFMPEG)

# 将 SRC_FFMPEG 添加到 SRC 中
list(APPEND SRC ${SRC_FFMPEG})

# 如果需要使用系统预构建库，可以使用该方法来查找，比如这里的log库
find_library( # 该变量保存所要关联库的路径
        log-lib
        # 需要关联的库名称
        log )

find_library( android-lib
        android )

# 设置ffmpeg库所在路径的目录
set(distribution_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})


# 导入ffmpeg
add_library( libffmpeg
        SHARED
        # 表示是导入第三方库
        IMPORTED )

# 编译一个ffmpeg-cmd库
add_library(  ffmpeg-cmd   # 库名称
        SHARED             # 库类型
        ${SRC})            # 编译进库的源码

# 指定ffmpeg的路径
set_target_properties( libffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libffmpeg.so)


# 指定需要关联的库
target_link_libraries( ffmpeg-cmd
        libffmpeg
        -landroid     # native_window
        -ljnigraphics # bitmap
        -lOpenSLES    # openSLES
        ${log-lib} )
