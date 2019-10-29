package org.stepik.android.view.video_player.ui.service

import android.app.Notification
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import org.stepic.droid.R
import org.stepik.android.view.video_player.model.VideoPlayerData
import org.stepik.android.view.video_player.ui.adapter.VideoPlayerMediaDescriptionAdapter
import org.stepik.android.view.video_player.ui.receiver.HeadphonesReceiver
import org.stepik.android.view.video_player.ui.receiver.InternetConnectionReceiverCompat

class VideoPlayerForegroundService : Service() {
    companion object {
        private const val EXTRA_VIDEO_PLAYER_DATA = "video_player_data"

        private const val PLAYER_CHANNEL_ID = "playback"
        private const val PLAYER_NOTIFICATION_ID = 21313

        private const val MEDIA_SESSION_TAG = "stepik_video"

        private const val BACK_BUFFER_DURATION_MS = 60 * 1000

        fun createIntent(context: Context, videoPlayerData: VideoPlayerData): Intent =
            Intent(context, VideoPlayerForegroundService::class.java)
                .putExtra(EXTRA_VIDEO_PLAYER_DATA, videoPlayerData)

        fun createBindingIntent(context: Context): Intent =
            Intent(context, VideoPlayerForegroundService::class.java)
    }

    private var player: SimpleExoPlayer? = null
    private lateinit var playerNotificationManager: PlayerNotificationManager

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var videoPlayerMediaDescriptionAdapter: VideoPlayerMediaDescriptionAdapter

    private var videoPlayerData: VideoPlayerData? = null

    private val headphonesReceiver =
        HeadphonesReceiver { player?.playWhenReady = false }

    private val internetConnectionReceiverCompat =
        InternetConnectionReceiverCompat {
            val player = this.player ?: return@InternetConnectionReceiverCompat
            val videoPlayerData = this.videoPlayerData ?: return@InternetConnectionReceiverCompat

            if (player.playbackState == Player.STATE_IDLE) {
                setPlayerData(videoPlayerData)
            }
        }

    private val mediaButtonReceiver =
        MediaButtonReceiver()

    override fun onCreate() {
        internetConnectionReceiverCompat.registerReceiver(this)
        createPlayer()
    }

    override fun onBind(intent: Intent?): IBinder? =
        VideoPlayerBinder(player)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setPlayerData(intent?.getParcelableExtra(EXTRA_VIDEO_PLAYER_DATA))
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    private fun createPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MOVIE)
            .build()

        videoPlayerMediaDescriptionAdapter = VideoPlayerMediaDescriptionAdapter(this)

        val loadControl = DefaultLoadControl.Builder()
            .setBackBuffer(BACK_BUFFER_DURATION_MS, true)
            .createDefaultLoadControl()

        player = ExoPlayerFactory
            .newSimpleInstance(this, DefaultRenderersFactory(this), DefaultTrackSelector(), loadControl)
            .apply {
                playWhenReady = true
                setAudioAttributes(audioAttributes, true)
            }

        val notificationListener =
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationCancelled(notificationId: Int) {
                    stopSelf()
                }

                override fun onNotificationStarted(notificationId: Int, notification: Notification?) {
                    startForeground(notificationId, notification)
                }
            }

        playerNotificationManager = PlayerNotificationManager
            .createWithNotificationChannel(
                this,
                PLAYER_CHANNEL_ID,
                R.string.video_player_control_notification_channel_name,
                R.string.video_player_control_notification_channel_description,
                PLAYER_NOTIFICATION_ID,
                videoPlayerMediaDescriptionAdapter,
                notificationListener
            )

        playerNotificationManager.setSmallIcon(R.drawable.ic_player_notification)
        playerNotificationManager.setUseStopAction(false)
        playerNotificationManager.setPlayer(player)

        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG, ComponentName(this, MediaButtonReceiver::class.java), null)
        mediaSession.isActive = true

        registerReceiver(mediaButtonReceiver, IntentFilter(Intent.ACTION_MEDIA_BUTTON))

        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)

        registerReceiver(headphonesReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    private fun setPlayerData(videoPlayerData: VideoPlayerData?) {
        val player = this.player
            ?: return

        val position =
            if (this.videoPlayerData?.videoId != videoPlayerData?.videoId) {
                videoPlayerData?.videoTimestamp ?: 0
            } else {
                player.currentPosition
            }

        val playWhenReady =
            this.videoPlayerData?.videoId != videoPlayerData?.videoId ||
            player.playWhenReady

        if (videoPlayerData != null) {
            if (this.videoPlayerData?.videoUrl != videoPlayerData.videoUrl || player.playbackState == Player.STATE_IDLE) {
                val mediaSource = getMediaSource(videoPlayerData)
                player.prepare(mediaSource)
            }

            player.playbackParameters = PlaybackParameters(videoPlayerData.videoPlaybackRate.rateFloat, 1f)
            player.seekTo(position)
            player.playWhenReady = playWhenReady
        }

        videoPlayerMediaDescriptionAdapter.videoPlayerMediaData = videoPlayerData?.mediaData
        playerNotificationManager.invalidate()

        this.videoPlayerData = videoPlayerData
    }

    private fun getMediaSource(videoPlayerData: VideoPlayerData): MediaSource {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(this).build()
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)), bandwidthMeter)

        return ProgressiveMediaSource
            .Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(videoPlayerData.videoUrl))
    }

    private fun releasePlayer() {
        unregisterReceiver(headphonesReceiver)
        unregisterReceiver(mediaButtonReceiver)

        mediaSession.release()
        mediaSessionConnector.setPlayer(null)

        playerNotificationManager.setPlayer(null)
        player?.release()
        player = null
    }

    override fun onDestroy() {
        releasePlayer()
        internetConnectionReceiverCompat.unregisterReceiver(this)
        super.onDestroy()
    }

    class VideoPlayerBinder(private val player: ExoPlayer?) : Binder() {
        fun getPlayer(): ExoPlayer? =
            player
    }
}