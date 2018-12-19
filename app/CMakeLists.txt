# 指定CMake编译器的最低版本
cmake_minimum_required(VERSION 3.4.1)

# 添加头文件路径
include_directories(
        ./src/main/cpp
)


# 要求CMake根据指定的源文件生成库
add_library( # 生成的库的名称
        FFmpegUtil
        # 设置生成的库的类型
        SHARED
        # 所有需要加入到这个库的源文件
        src/main/cpp/native-lib.cpp )

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

# 指定ffmpeg的路径
set_target_properties( libffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/libffmpeg.so)


# 添加ffmpeg头文件路径
include_directories(src/main/jniLibs/include)

# 指定需要关联的库
target_link_libraries( # 目标库文件
        FFmpegUtil
        libffmpeg
        # 需要在目标库文件中使用的库
        ${log-lib}
        ${android-lib} )
