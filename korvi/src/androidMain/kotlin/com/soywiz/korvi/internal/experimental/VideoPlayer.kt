package com.soywiz.korvi.internal.experimental

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import com.soywiz.korio.file.VfsFile
import com.soywiz.korvi.internal.toMediaDataSource
import java.io.IOException

/**
 * Plays the video track from a movie file to a Surface.
 *
 * TODO: needs more advanced shuttle controls (pause/resume, skip)
 */
@Suppress("ConstantConditionIf")
class VideoPlayer(private val file: VfsFile, val androidContext: Context, private val decoderCallback: ((frame: Image, player: VideoPlayer)->Unit)? = null) {

    companion object {

        private val TAG: String = VideoPlayer::class.java.toString()
        private const val TIMEOUT_USEC = 10000
        private const val MSG_PLAY_STOPPED = 0
        //private const val VERBOSE = true
        private const val VERBOSE = false

        /**
         * Selects the video track, if any.
         *
         * @return the track index, or -1 if no video track is found.
         */
        private fun selectTrack(extractor: MediaExtractor): Int {
            // Select the first video track we find, ignore the rest.
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    if (VERBOSE) {
                        Log.d(
                            TAG,
                            "Extractor selected track $i ($mime): $format"
                        )
                    }
                    return i
                }
            }
            return -1
        }
    }

    // Declare this here to reduce allocations.
    private val mBufferInfo = MediaCodec.BufferInfo()

    // May be set/read by different threads.
    @Volatile
    private var mIsStopRequested = false
    private var mLoop = false

    /**
     * Returns the width, in pixels, of the video.
     */
    var videoWidth = 0

    /**
     * Returns the height, in pixels, of the video.
     */
    var videoHeight = 0

    /**
     * Interface to be implemented by class that manages playback UI.
     *
     *
     * Callback methods will be invoked on the UI thread.
     */
    interface PlayerFeedback {
        fun playbackStopped()
    }

    /**
     * Callback invoked when rendering video frames.  The MoviePlayer client must
     * provide one of these.
     */
    interface FrameCallback {
        /**
         * Called immediately before the frame is rendered.
         * @param presentationTimeUsec The desired presentation time, in microseconds.
         */
        fun preRender(presentationTimeUsec: Long)

        /**
         * Called immediately after the frame render call returns.  The frame may not have
         * actually been rendered yet.
         * TODO: is this actually useful?
         */
        fun postRender()

        /**
         * Called after the last frame of a looped movie has been rendered.  This allows the
         * callback to adjust its expectations of the next presentation time stamp.
         */
        fun loopReset()
    }

    init {

        // Pop the file open and pull out the video characteristics.
        // TODO: consider leaving the extractor open.  Should be able to just seek back to
        //       the start after each iteration of play.  Need to rearrange the API a bit --
        //       currently play() is taking an all-in-one open+work+release approach.
        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor()
            setDataSource(extractor)

            val trackIndex =
                selectTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found in XXXXXXXX")
            }
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            videoWidth = format.getInteger(MediaFormat.KEY_WIDTH)
            videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
            if (VERBOSE) {
                Log.d(
                    TAG,
                    "Video size is " + videoWidth + "x" + videoHeight
                )
            }
        } finally {
            extractor?.release()
        }
    }

    private fun setDataSource(extractor: MediaExtractor) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extractor.setDataSource(file.toMediaDataSource(androidContext))
        } else {
            TODO()
        }

    }

    /**
     * Sets the loop mode.  If true, playback will loop forever.
     */
    fun setLoopMode(loopMode: Boolean) {
        mLoop = loopMode
    }

    /**
     * Asks the player to stop.  Returns without waiting for playback to halt.
     *
     *
     * Called from arbitrary thread.
     */
    fun requestStop() {
        mIsStopRequested = true
    }

    /**
     * Decodes the video stream, sending frames to the surface.
     *
     * Does not return until video playback is complete, or we get a "stop" signal from
     * frameCallback.
     */
    @Throws(IOException::class)
    fun play() {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null

        try {
            extractor = MediaExtractor()
            setDataSource(extractor)

            val trackIndex =
                selectTrack(extractor)
            if (trackIndex < 0) {
                throw RuntimeException("No video track found")
            }

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)

            // Create a MediaCodec decoder, and configure it with the MediaFormat from the
            // extractor.  It's very important to use the format from the extractor because
            // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
            val mime = format.getString(MediaFormat.KEY_MIME)
            decoder = MediaCodec.createDecoderByType(mime ?: "")
            decoder.configure(format, null, null, 0)
            decoder.start()
            doExtract(extractor, trackIndex, decoder,
                SpeedControlCallback()
            )

        } finally {
            // release everything we grabbed
            decoder?.stop()
            decoder?.release()
            extractor?.release()
        }
    }

    /**
     * Work loop.  We execute here until we run out of video or are told to stop.
     */
    private fun doExtract(
        extractor: MediaExtractor, trackIndex: Int, decoder: MediaCodec,
        frameCallback: FrameCallback?
    ) {
        // We need to strike a balance between providing input and reading output that
        // operates efficiently without delays on the output side.
        //
        // To avoid delays on the output side, we need to keep the codec's input buffers
        // fed.  There can be significant latency between submitting frame N to the decoder
        // and receiving frame N on the output, so we need to stay ahead of the game.
        //
        // Many video decoders seem to want several frames of video before they start
        // producing output -- one implementation wanted four before it appeared to
        // configure itself.  We need to provide a bunch of input frames up front, and try
        // to keep the queue full as we go.
        //
        // (Note it's possible for the encoded data to be written to the stream out of order,
        // so we can't generally submit a single frame and wait for it to appear.)
        //
        // We can't just fixate on the input side though.  If we spend too much time trying
        // to stuff the input, we might miss a presentation deadline.  At 60Hz we have 16.7ms
        // between frames, so sleeping for 10ms would eat up a significant fraction of the
        // time allowed.  (Most video is at 30Hz or less, so for most content we'll have
        // significantly longer.)  Waiting for output is okay, but sleeping on availability
        // of input buffers is unwise if we need to be providing output on a regular schedule.
        //
        //
        // In some situations, startup latency may be a concern.  To minimize startup time,
        // we'd want to stuff the input full as quickly as possible.  This turns out to be
        // somewhat complicated, as the codec may still be starting up and will refuse to
        // accept input.  Removing the timeout from dequeueInputBuffer() results in spinning
        // on the CPU.
        //
        // If you have tight startup latency requirements, it would probably be best to
        // "prime the pump" with a sequence of frames that aren't actually shown (e.g.
        // grab the first 10 NAL units and shove them through, then rewind to the start of
        // the first key frame).
        //
        // The actual latency seems to depend on strongly on the nature of the video (e.g.
        // resolution).
        //
        //
        // One conceptually nice approach is to loop on the input side to ensure that the codec
        // always has all the input it can handle.  After submitting a buffer, we immediately
        // check to see if it will accept another.  We can use a short timeout so we don't
        // miss a presentation deadline.  On the output side we only check once, with a longer
        // timeout, then return to the outer loop to see if the codec is hungry for more input.
        //
        // In practice, every call to check for available buffers involves a lot of message-
        // passing between threads and processes.  Setting a very brief timeout doesn't
        // exactly work because the overhead required to determine that no buffer is available
        // is substantial.  On one device, the "clever" approach caused significantly greater
        // and more highly variable startup latency.
        //
        // The code below takes a very simple-minded approach that works, but carries a risk
        // of occasionally running out of output.  A more sophisticated approach might
        // detect an output timeout and use that as a signal to try to enqueue several input
        // buffers on the next iteration.
        //
        // If you want to experiment, set the VERBOSE flag to true and watch the behavior
        // in logcat.  Use "logcat -v threadtime" to see sub-second timing.
        val decoderInputBuffers = decoder.inputBuffers
        var inputChunk = 0
        var firstInputTimeNsec: Long = -1
        var outputDone = false
        var inputDone = false
        while (!outputDone) {
            if (mIsStopRequested) {
                Log.d(TAG, "Stop requested")
                return
            }

            // Feed more data to the decoder.
            if (!inputDone) {
                val inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC.toLong())
                if (inputBufIndex >= 0) {
                    if (firstInputTimeNsec == -1L) {
                        firstInputTimeNsec = System.nanoTime()
                    }
                    val inputBuf = decoderInputBuffers[inputBufIndex]
                    // Read the sample data into the ByteBuffer.  This neither respects nor
                    // updates inputBuf's position, limit, etc.
                    val chunkSize = extractor.readSampleData(inputBuf, 0)
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, 0, 0L,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                        if (VERBOSE) Log.d(
                            TAG,
                            "sent input EOS"
                        )
                    } else {
                        if (extractor.sampleTrackIndex != trackIndex) {
                            Log.w(
                                TAG,
                                "WEIRD: got sample from track ${extractor.sampleTrackIndex}, expected $trackIndex"
                            )
                        }
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(
                            inputBufIndex, 0, chunkSize,
                            presentationTimeUs, 0 /*flags*/
                        )
                        if (VERBOSE) {
                            Log.d(TAG, "submitted frame $inputChunk to dec, size=$chunkSize")
                        }
                        inputChunk++
                        extractor.advance()
                    }
                } else {
                    if (VERBOSE) Log.d(
                        TAG, "input buffer not available")
                }
            }
            if (!outputDone) {
                val decoderStatus =
                    decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC.toLong())
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(
                        TAG,
                        "no output from decoder available"
                    )
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE) Log.d(
                        TAG,
                        "decoder output buffers changed"
                    )
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    if (VERBOSE) Log.d(
                        TAG,
                        "decoder output format changed: $newFormat"
                    )
                } else if (decoderStatus < 0) {
                    throw RuntimeException(
                        "unexpected result from decoder.dequeueOutputBuffer: " +
                                decoderStatus
                    )
                } else { // decoderStatus >= 0
                    if (firstInputTimeNsec != 0L) {
                        // Log the delay from the first buffer of input to the first buffer
                        // of output.
                        val nowNsec = System.nanoTime()
                        Log.d(
                            TAG,
                            "startup lag " + (nowNsec - firstInputTimeNsec) / 1000000.0 + " ms"
                        )
                        firstInputTimeNsec = 0
                    }
                    var doLoop = false
                    if (VERBOSE) Log.d(
                        TAG,
                        "surface decoder given buffer " + decoderStatus +
                                " (size=" + mBufferInfo.size + ")"
                    )
                    if (mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        if (VERBOSE) Log.d(
                            TAG,
                            "output EOS"
                        )
                        if (mLoop) {
                            doLoop = true
                        } else {
                            outputDone = true
                        }
                    }
                    val doRender = mBufferInfo.size != 0

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        decoderCallback?.invoke(decoder.getOutputImage(decoderStatus)!!, this)
                    } else {
                        //decoderCallback?.invoke(decoderInputBuffers)
                        error("Requires Lollipop (Android >= 5.0) API 21")
                    }

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  We can't control when it
                    // appears on-screen, but we can manage the pace at which we release
                    // the buffers.
                    if (doRender && frameCallback != null) {
                        frameCallback.preRender(mBufferInfo.presentationTimeUs)
                    }
                    decoder.releaseOutputBuffer(decoderStatus, doRender)
                    if (doRender && frameCallback != null) {
                        frameCallback.postRender()
                    }
                    if (doLoop) {
                        Log.d(TAG, "Reached EOS, looping")
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                        inputDone = false
                        decoder.flush() // reset decoder state
                        frameCallback?.loopReset()
                    }
                }
            }
        }
    }

    /**
     * Thread helper for video playback.
     *
     * The PlayerFeedback callbacks will execute on the thread that creates the object,
     * assuming that thread has a looper.  Otherwise, they will execute on the main looper.
     */
    class PlayTask(
        private val mPlayer: VideoPlayer,
        private val mFeedback: PlayerFeedback
    ) :
        Runnable {
        private var mDoLoop = false
        private var mThread: Thread? = null
        private val mLocalHandler: LocalHandler
        private val mStopLock = Object()
        private var mStopped = false

        /**
         * Sets the loop mode.  If true, playback will loop forever.
         */
        fun setLoopMode(loopMode: Boolean) {
            mDoLoop = loopMode
        }

        /**
         * Creates a new thread, and starts execution of the player.
         */
        fun execute() {
            mPlayer.setLoopMode(mDoLoop)
            mThread = Thread(this, "Movie Player").apply {
                start()
            }
        }

        /**
         * Requests that the player stop.
         *
         * Called from arbitrary thread.
         */
        fun requestStop() {
            mPlayer.requestStop()
        }

        /**
         * Wait for the player to stop.
         *
         * Called from any thread other than the PlayTask thread.
         */
        fun waitForStop() {
            synchronized(mStopLock) {
                while (!mStopped) {
                    try {
                        mStopLock.wait()
                    } catch (ie: InterruptedException) {
                        // discard
                    }
                }
            }
        }

        override fun run() {
            try {
                mPlayer.play()
            } catch (ioe: IOException) {
                throw RuntimeException(ioe)
            } finally {
                // tell anybody waiting on us that we're done
                synchronized(mStopLock) {
                    mStopped = true
                    mStopLock.notifyAll()
                }

                // Send message through Handler so it runs on the right thread.
                mLocalHandler.sendMessage(
                    mLocalHandler.obtainMessage(MSG_PLAY_STOPPED, mFeedback)
                )
            }
        }

        private class LocalHandler : Handler() {
            override fun handleMessage(msg: Message) {
                when (val what = msg.what) {
                    MSG_PLAY_STOPPED -> {
                        val fb = msg.obj as PlayerFeedback
                        fb.playbackStopped()
                    }
                    else -> throw RuntimeException("Unknown msg $what")
                }
            }
        }

        /**
         * Prepares new PlayTask.
         *
         * @param player The player object, configured with control and output.
         * @param feedback UI feedback object.
         */
        init {
            mLocalHandler =
                LocalHandler()
        }
    }

    private class SpeedControlCallback :
        FrameCallback {

        companion object {
            private val TAG: String = VideoPlayer::class.java.toString()
            private const val CHECK_SLEEP_TIME = false
            private const val ONE_MILLION = 1000000L
        }

        private var mPrevPresentUsec: Long = 0
        private var mPrevMonoUsec: Long = 0
        private var mFixedFrameDurationUsec: Long = 0
        private var mLoopReset = false

        /**
         * Sets a fixed playback rate.  If set, this will ignore the presentation time stamp
         * in the video file.  Must be called before playback thread starts.
         */
        fun setFixedPlaybackRate(fps: Int) {
            mFixedFrameDurationUsec = ONE_MILLION / fps
        }

        // runs on decode thread
        override fun preRender(presentationTimeUsec: Long) {
            // For the first frame, we grab the presentation time from the video
            // and the current monotonic clock time.  For subsequent frames, we
            // sleep for a bit to try to ensure that we're rendering frames at the
            // pace dictated by the video stream.
            //
            // If the frame rate is faster than vsync we should be dropping frames.  On
            // Android 4.4 this may not be happening.
            if (mPrevMonoUsec == 0L) {
                // Latch current values, then return immediately.
                mPrevMonoUsec = System.nanoTime() / 1000
                mPrevPresentUsec = presentationTimeUsec
            } else {
                // Compute the desired time delta between the previous frame and this frame.
                var frameDelta: Long
                if (mLoopReset) {
                    // We don't get an indication of how long the last frame should appear
                    // on-screen, so we just throw a reasonable value in.  We could probably
                    // do better by using a previous frame duration or some sort of average;
                    // for now we just use 30fps.
                    mPrevPresentUsec =
                        presentationTimeUsec - ONE_MILLION / 30
                    mLoopReset = false
                }
                frameDelta = if (mFixedFrameDurationUsec != 0L) {
                    // Caller requested a fixed frame rate.  Ignore PTS.
                    mFixedFrameDurationUsec
                } else {
                    presentationTimeUsec - mPrevPresentUsec
                }// Sleep until it's time to wake up.  To be responsive to "stop" commands
                // we're going to wake up every half a second even if the sleep is supposed
                // to be longer (which should be rare).  The alternative would be
                // to interrupt the thread, but that requires more work.
                //
                // The precision of the sleep call varies widely from one device to another;
                // we may wake early or late.  Different devices will have a minimum possible
                // sleep time. If we're within 100us of the target time, we'll probably
                // overshoot if we try to sleep, so just go ahead and continue on.

                // Advance times using calculated time values, not the post-sleep monotonic
                // clock time, to avoid drifting.
                // /*&& mState == RUNNING*/
                // when we want to wake up
                when {
                    frameDelta < 0 -> {
                        Log.w(TAG, "Weird, video times went backward")
                        frameDelta = 0
                    }
                    frameDelta == 0L -> {
                        // This suggests a possible bug in movie generation.
                        Log.i(TAG, "Warning: current frame and previous frame had same timestamp")
                    }
                    frameDelta > 10 * ONE_MILLION -> {
                        // Inter-frame times could be arbitrarily long.  For this player, we want
                        // to alert the developer that their movie might have issues (maybe they
                        // accidentally output timestamps in nsec rather than usec).
                        Log.i(TAG, "Inter-frame pause was ${frameDelta / ONE_MILLION}sec, capping at 5 sec")
                        frameDelta = 5 * ONE_MILLION
                    }
                }

                val desiredUsec = mPrevMonoUsec + frameDelta // when we want to wake up
                var nowUsec = System.nanoTime() / 1000
                while (nowUsec < desiredUsec - 100 /*&& mState == RUNNING*/) {
                    // Sleep until it's time to wake up.  To be responsive to "stop" commands
                    // we're going to wake up every half a second even if the sleep is supposed
                    // to be longer (which should be rare).  The alternative would be
                    // to interrupt the thread, but that requires more work.
                    //
                    // The precision of the sleep call varies widely from one device to another;
                    // we may wake early or late.  Different devices will have a minimum possible
                    // sleep time. If we're within 100us of the target time, we'll probably
                    // overshoot if we try to sleep, so just go ahead and continue on.
                    var sleepTimeUsec = desiredUsec - nowUsec
                    if (sleepTimeUsec > 500000) {
                        sleepTimeUsec = 500000
                    }
                    try {
                        if (CHECK_SLEEP_TIME) {
                            val startNsec = System.nanoTime()
                            Thread.sleep(
                                sleepTimeUsec / 1000,
                                (sleepTimeUsec % 1000).toInt() * 1000
                            )
                            val actualSleepNsec = System.nanoTime() - startNsec
                            Log.d(
                                TAG,
                                "sleep=$sleepTimeUsec actual=${actualSleepNsec / 1000} diff=${Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)} (usec)"
                            )
                        } else {
                            Thread.sleep(
                                sleepTimeUsec / 1000,
                                (sleepTimeUsec % 1000).toInt() * 1000
                            )
                        }
                    } catch (ie: InterruptedException) {
                    }
                    nowUsec = System.nanoTime() / 1000
                }

                // Advance times using calculated time values, not the post-sleep monotonic
                // clock time, to avoid drifting.
                mPrevMonoUsec += frameDelta
                mPrevPresentUsec += frameDelta
            }
        }

        // runs on decode thread
        override fun postRender() {}
        override fun loopReset() {
            mLoopReset = true
        }
    }
}
