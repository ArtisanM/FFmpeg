#!/bin/bash

# ndk版本r17c   ffmpeg 4.0  ffmpeg源码需要修改才能编译通过： aaccoder.c里的B0改为b0   libavcodec/hevc_mvs.c中的B0改为b0  libavcodec/opus_pvq.c中的B0改为b0

# ndk环境
export NDK=/Users/mo/Documents/work/Android/ndk
export SYSROOT=$NDK/platforms/android-18/arch-arm
export TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/darwin-x86_64
CPU=arm-v7a
ISYSROOT=$NDK/sysroot
ASM=$ISYSROOT/usr/include/arm-linux-androideabi

# 要保存动态库的目录，这里保存在源码根目录下的android/armv7-a
export PREFIX=$(pwd)/android/$CPU
ADDI_CFLAGS="-marm -L/usr/local/lib"

function build_android
{
    echo "开始编译ffmpeg"

    ./configure \
        --target-os=linux \
        --prefix=$PREFIX \
        --disable-everything \
        --disable-shared \
        --disable-cuvid \
        --disable-dxva2 \
        --disable-ffnvcodec \
        --disable-d3d11va \
        --disable-nvdec \
        --disable-nvenc \
        --disable-vaapi \
        --disable-vdpau \
        --disable-videotoolbox \
        --disable-audiotoolbox \
        --disable-amf \
        --disable-doc \
        --disable-stripping \
        --disable-ffmpeg \
        --disable-ffplay \
        --disable-ffprobe \
        --disable-avdevice \
        --disable-swscale \
        --disable-encoders \
        --disable-decoders \
        --disable-hwaccels \
        --disable-parsers \
        --disable-postproc \
        --disable-indevs \
        --disable-outdevs \
        --disable-protocols \
        --disable-filters \
        --disable-bsfs \
        --disable-doc \
        --disable-indevs \
        --disable-debug \
        --disable-outdevs \
        --disable-network \
        --disable-runtime-cpudetect \
        --enable-protocol=file \
        --enable-cross-compile \
        --enable-static \
        --enable-neon \
        --enable-small \
        --enable-muxer=mp3,wav,adts,ipod,mov,mp4,pcm_s16le \
        --enable-demuxer=aac,ac3,amr*,pcm*,flac,eac3,mp3,wav,ape,mov,mpegps \
        --enable-encoder=aac,pcm_s16le \
        --enable-decoder=aac,pcm*,mp3*,adpcm* \
        --enable-decoder=ac3 \
        --enable-decoder=alac \
        --enable-decoder=alac_at \
        --enable-decoder=amr_nb_at \
        --enable-decoder=amrnb \
        --enable-decoder=amrwb \
        --enable-decoder=ape \
        --enable-decoder=aptx \
        --enable-decoder=aptx_hd \
        --enable-decoder=libilbc \
        --enable-decoder=libopencore_amrnb \
        --enable-decoder=libopencore_amrwb \
        --enable-decoder=libopus \
        --enable-decoder=libvorbis \
        --enable-decoder=mlp \
        --enable-decoder=eac3 \
        --enable-decoder=eac3_at \
        --enable-decoder=vmdaudio \
        --enable-decoder=wavpack \
        --enable-decoder=flac \
        --enable-decoder=wmav1 \
        --enable-decoder=wmav2 \
        --enable-parser=aac \
        --enable-parser=ac3 \
        --enable-parser=flac \
        --enable-parser=opus \
        --enable-parser=vorbis \
        --enable-parser=mpegaudio \
        --enable-bsf=aac_adtstoasc \
        --enable-bsf=chomp \
        --enable-bsf=dca_core \
        --enable-bsf=dump_extradata \
        --enable-bsf=eac3_core \
        --enable-bsf=extract_extradata \
        --enable-bsf=filter_units \
        --enable-bsf=mp3_header_decompress \
        --enable-bsf=null \
        --enable-bsf=remove_extradata \
        --enable-filter=acopy \
        --enable-filter=acompressor \
        --enable-filter=acontrast \
        --enable-filter=acrossfade \
        --enable-filter=acrusher \
        --enable-filter=sofalizer \
        --enable-filter=ladspa \
        --enable-filter=abitscope \
        --enable-filter=adelay \
        --enable-filter=stereotools \
        --enable-filter=highpass \
        --enable-filter=hilbert \
        --enable-filter=adrawgraph \
        --enable-filter=aecho \
        --enable-filter=flanger \
        --enable-filter=aemphasis \
        --enable-filter=join \
        --enable-filter=abench \
        --enable-filter=aeval \
        --enable-filter=headphone \
        --enable-filter=aevalsrc \
        --enable-filter=afade \
        --enable-filter=hdcd \
        --enable-filter=haas \
        --enable-filter=afftfilt \
        --enable-filter=afifo \
        --enable-filter=afir \
        --enable-filter=stereowiden \
        --enable-filter=superequalizer \
        --enable-filter=surround \
        --enable-filter=treble \
        --enable-filter=tremolo \
        --enable-filter=firequalizer \
        --enable-filter=vibrato \
        --enable-filter=aformat \
        --enable-filter=ahistogram \
        --enable-filter=aiir \
        --enable-filter=ainterleave \
        --enable-filter=alimiter \
        --enable-filter=allpass \
        --enable-filter=volume \
        --enable-filter=volumedetect \
        --enable-filter=aloop \
        --enable-filter=amerge \
        --enable-filter=crystalizer \
        --enable-filter=ametadata \
        --enable-filter=amix \
        --enable-filter=amovie \
        --enable-filter=dcshift \
        --enable-filter=drmeter \
        --enable-filter=dynaudnorm \
        --enable-filter=replaygain \
        --enable-filter=resample \
        --enable-filter=rubberband \
        --enable-filter=anoisesrc \
        --enable-filter=anull \
        --enable-filter=anullsink \
        --enable-filter=crossfeed \
        --enable-filter=anullsrc \
        --enable-filter=extrastereo \
        --enable-filter=apad \
        --enable-filter=aperms \
        --enable-filter=aphasemeter \
        --enable-filter=aphaser \
        --enable-filter=sidechaincompress \
        --enable-filter=ebur128 \
        --enable-filter=equalizer \
        --enable-filter=sidechaingate \
        --enable-filter=silencedetect \
        --enable-filter=silenceremove \
        --enable-filter=sine \
        --enable-filter=arealtime \
        --enable-filter=asendcmd \
        --enable-filter=asetnsamples \
        --enable-filter=asetpts \
        --enable-filter=asetrate \
        --enable-filter=earwax \
        --enable-filter=asettb \
        --enable-filter=ashowinfo \
        --enable-filter=asidedata \
        --enable-filter=asplit \
        --enable-filter=atempo \
        --enable-filter=atrim \
        --enable-filter=avectorscope \
        --enable-filter=bandpass \
        --enable-filter=bandreject \
        --enable-filter=biquad \
        --enable-filter=channelmap \
        --enable-filter=chorus \
        --enable-filter=compensationdelay \
        --enable-filter=concat \
        --enable-filter=compand \
        --enable-filter=channelsplit \
        --enable-filter=bs2b \
        --enable-filter=bass \
        --enable-filter=loudnorm \
        --enable-filter=lowpass \
        --enable-filter=lv2 \
        --enable-filter=mcompand \
        --enable-filter=pan \
        --enable-filter=astreamselect \
        --enable-filter=astats \
        --enable-filter=aselect \
        --enable-filter=areverse \
        --enable-filter=aresample \
        --enable-filter=apulsator \
        --enable-filter=anequalizer \
        --enable-filter=agate \
        --cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
        --arch=arm \
        --sysroot=$SYSROOT \
        --extra-cflags="-I$ASM -I/usr/local/include -isysroot $ISYSROOT -D__ANDROID_API__=18 -U_FILE_OFFSET_BITS -Os -fPIC -DANDROID -Wno-deprecated -mfloat-abi=softfp -marm" \
        --extra-ldflags="$ADDI_LDFLAGS" \
        $ADDITIONAL_CONFIGURE_FLAG

    make clean

    make -j16
    make install

    # 打包
    $TOOLCHAIN/bin/arm-linux-androideabi-ld \
        -rpath-link=$SYSROOT/usr/lib \
        -L$SYSROOT/usr/lib \
        -L$PREFIX/lib \
        -soname libffmpeg.so -shared -nostdlib -Bsymbolic --whole-archive --no-undefined -o \
        $PREFIX/libffmpeg.so \
        libavcodec/libavcodec.a \
        libavfilter/libavfilter.a \
        libavformat/libavformat.a \
        libavutil/libavutil.a \
        libswresample/libswresample.a \
        -lc -lm -lz -ldl -llog --dynamic-linker=/system/bin/linker \
        $TOOLCHAIN/lib/gcc/arm-linux-androideabi/4.9.x/libgcc.a
 
    # strip 精简文件
    $TOOLCHAIN/bin/arm-linux-androideabi-strip  $PREFIX/libffmpeg.so

    echo "编译结束！"
}

build_android