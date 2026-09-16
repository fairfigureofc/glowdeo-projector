#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <linux/videodev2.h>
#include <poll.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

/* Use the kernel ABI headers, not the prototype's hand-sized 32-bit structs. */
typedef struct {
    int fd, streaming;
    unsigned width, height, stride, count;
    void *maps[8];
    size_t lengths[8];
} Camera;

static int control(int fd, unsigned long request, void *arg) {
    int result;
    do { result = ioctl(fd, request, arg); } while (result < 0 && errno == EINTR);
    return result;
}

static void release(Camera *camera) {
    if (!camera) return;
    if (camera->streaming) {
        enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        control(camera->fd, VIDIOC_STREAMOFF, &type);
    }
    for (unsigned i = 0; i < camera->count; i++) {
        if (camera->maps[i]) munmap(camera->maps[i], camera->lengths[i]);
    }
    if (camera->fd >= 0) close(camera->fd);
    free(camera);
}

static void error(JNIEnv *env, const char *operation, int code) {
    char message[256];
    snprintf(message, sizeof(message), "%s: %s (errno %d)", operation, strerror(code), code);
    jclass exception = (*env)->FindClass(env, "java/io/IOException");
    if (exception) (*env)->ThrowNew(env, exception, message);
}

JNIEXPORT jlong JNICALL Java_com_glowdeo_player_Hy310xCamera_openNative(JNIEnv *env, jobject self) {
    (void)self;
    Camera *camera = calloc(1, sizeof(*camera));
    if (!camera) { error(env, "Allocate camera", ENOMEM); return 0; }
    camera->fd = open("/dev/video0", O_RDWR | O_NONBLOCK | O_CLOEXEC);
    const char *operation = "Open HY310X camera";
    if (camera->fd < 0) goto failed;
    struct v4l2_capability capability = {0};
    operation = "Query camera capabilities";
    if (control(camera->fd, VIDIOC_QUERYCAP, &capability) < 0) goto failed;
    unsigned caps = capability.capabilities & V4L2_CAP_DEVICE_CAPS ? capability.device_caps : capability.capabilities;
    if (!(caps & V4L2_CAP_VIDEO_CAPTURE) || !(caps & V4L2_CAP_STREAMING)) { errno = ENOTSUP; goto failed; }
    struct v4l2_format format = {.type = V4L2_BUF_TYPE_VIDEO_CAPTURE};
    operation = "Read camera format";
    if (control(camera->fd, VIDIOC_G_FMT, &format) < 0) goto failed;
    struct v4l2_pix_format *pixels = &format.fmt.pix;
    operation = "Unsupported camera format (HY310X YUYV required)";
    if (pixels->pixelformat != V4L2_PIX_FMT_YUYV || pixels->width < 2 || pixels->width > 2048 ||
        pixels->height < 2 || pixels->height > 2048 || pixels->width % 2) { errno = ENOTSUP; goto failed; }
    camera->width = pixels->width;
    camera->height = pixels->height;
    camera->stride = pixels->bytesperline ? pixels->bytesperline : pixels->width * 2;
    if (camera->stride < camera->width * 2 || camera->stride > 16384) { errno = EINVAL; goto failed; }
    struct v4l2_requestbuffers request = {.count = 3, .type = V4L2_BUF_TYPE_VIDEO_CAPTURE, .memory = V4L2_MEMORY_MMAP};
    operation = "Request camera buffers (close autofocus or other camera apps)";
    if (control(camera->fd, VIDIOC_REQBUFS, &request) < 0) goto failed;
    if (!request.count || request.count > 8) { errno = EINVAL; goto failed; }
    camera->count = request.count;
    for (unsigned i = 0; i < camera->count; i++) {
        struct v4l2_buffer buffer = {.index = i, .type = V4L2_BUF_TYPE_VIDEO_CAPTURE, .memory = V4L2_MEMORY_MMAP};
        operation = "Map camera buffer";
        if (control(camera->fd, VIDIOC_QUERYBUF, &buffer) < 0) goto failed;
        if (buffer.length < (size_t)camera->stride * camera->height || buffer.length > 32 * 1024 * 1024) {
            errno = EINVAL; goto failed;
        }
        void *map = mmap(NULL, buffer.length, PROT_READ | PROT_WRITE, MAP_SHARED, camera->fd, buffer.m.offset);
        if (map == MAP_FAILED) goto failed;
        camera->maps[i] = map;
        camera->lengths[i] = buffer.length;
        if (control(camera->fd, VIDIOC_QBUF, &buffer) < 0) goto failed;
    }
    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    operation = "Start camera";
    if (control(camera->fd, VIDIOC_STREAMON, &type) < 0) goto failed;
    camera->streaming = 1;
    return (jlong)(intptr_t)camera;
failed: {
    int code = errno;
    release(camera);
    error(env, operation, code);
    return 0;
}
}

static int clamp(int value) { return value < 0 ? 0 : value > 255 ? 255 : value; }
static jint color(int y, int u, int v) {
    int c = y - 16, d = u - 128, e = v - 128;
    return (jint)(0xff000000u | (unsigned)clamp((298*c+409*e+128)>>8)<<16 |
                  (unsigned)clamp((298*c-100*d-208*e+128)>>8)<<8 | (unsigned)clamp((298*c+516*d+128)>>8));
}
static int64_t milliseconds(void) {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (int64_t)now.tv_sec * 1000 + now.tv_nsec / 1000000;
}

JNIEXPORT jintArray JNICALL Java_com_glowdeo_player_Hy310xCamera_captureNative(JNIEnv *env, jobject self, jlong handle) {
    (void)self;
    Camera *camera = (Camera *)(intptr_t)handle;
    if (!camera) { error(env, "Camera is closed", EBADF); return NULL; }
    int64_t deadline = milliseconds() + 6000;
    /* Drain all queued buffers after a pattern change, then sample a fresh frame. */
    for (int frame = 0; frame < 6; ) {
        int wait = (int)(deadline - milliseconds());
        if (wait <= 0) { error(env, "Camera frame timed out", ETIMEDOUT); return NULL; }
        struct pollfd p = {.fd = camera->fd, .events = POLLIN};
        int ready = poll(&p, 1, wait);
        if (ready < 0 && errno == EINTR) continue;
        if (ready <= 0) { error(env, "Camera frame timed out", ready ? errno : ETIMEDOUT); return NULL; }
        struct v4l2_buffer buffer = {.type = V4L2_BUF_TYPE_VIDEO_CAPTURE, .memory = V4L2_MEMORY_MMAP};
        if (control(camera->fd, VIDIOC_DQBUF, &buffer) < 0) {
            if (errno == EAGAIN) continue;
            error(env, "Read camera frame", errno); return NULL;
        }
        if (buffer.index >= camera->count || buffer.bytesused > camera->lengths[buffer.index] ||
            buffer.bytesused < (size_t)camera->stride * (camera->height - 1) + camera->width * 2) {
            error(env, "Invalid camera frame size", EIO); return NULL;
        }
        frame++;
        jintArray output = NULL;
        if (frame == 6) {
            size_t count = (size_t)camera->width * camera->height + 2;
            jint *rgb = malloc(count * sizeof(jint));
            if (!rgb) { error(env, "Allocate frame", ENOMEM); return NULL; }
            rgb[0] = (jint)camera->width; rgb[1] = (jint)camera->height;
            const unsigned char *base = camera->maps[buffer.index];
            for (unsigned y = 0; y < camera->height; y++) {
                const unsigned char *row = base + y * camera->stride;
                for (unsigned x = 0; x < camera->width; x += 2) {
                    const unsigned char *s = row + x * 2;
                    rgb[2 + y*camera->width + x] = color(s[0], s[1], s[3]);
                    rgb[3 + y*camera->width + x] = color(s[2], s[1], s[3]);
                }
            }
            output = (*env)->NewIntArray(env, (jsize)count);
            if (output) (*env)->SetIntArrayRegion(env, output, 0, (jsize)count, rgb);
            free(rgb);
        }
        if (control(camera->fd, VIDIOC_QBUF, &buffer) < 0) {
            error(env, "Requeue camera frame", errno); return NULL;
        }
        if (frame == 6) return output;
    }
    return NULL;
}

JNIEXPORT void JNICALL Java_com_glowdeo_player_Hy310xCamera_closeNative(JNIEnv *env, jobject self, jlong handle) {
    (void)env; (void)self;
    release((Camera *)(intptr_t)handle);
}
