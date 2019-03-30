package com.boclips.videoanalyser.application

import com.boclips.eventtypes.VideoToAnalyse
import com.boclips.videoanalyser.config.messaging.Subscriptions
import com.boclips.videoanalyser.domain.VideoAnalyserService
import com.boclips.videoanalyser.testsupport.fakes.AbstractSpringIntegrationTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.support.MessageBuilder
import java.lang.RuntimeException

class AnalyseVideoIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var subscriptions: Subscriptions

    lateinit var videoToAnalyse: VideoToAnalyse

    @BeforeEach
    fun setUp() {
        videoToAnalyse = VideoToAnalyse.builder()
                .videoId("1")
                .videoUrl("http://vid.eo/1.mp4")
                .build()
    }

    @Test
    fun `videos to analyse published as events are submitted to video indexer`() {
        subscriptions.videosToAnalyse().send(MessageBuilder.withPayload(videoToAnalyse).build())

        assertThat(fakeVideoIndexer.submittedVideo("1")).isEqualTo("http://vid.eo/1.mp4")
    }

    @Test
    fun `video indexer exceptions are handled`() {
        val videoAnalyserService = mock<VideoAnalyserService>()

        whenever(videoAnalyserService.submitVideo(any(), any())).thenThrow(RuntimeException("something went wrong"))

        val analyseVideo = AnalyseVideo(videoAnalyserService)

        assertThatCode { analyseVideo.execute(videoToAnalyse) }.doesNotThrowAnyException()
    }
}
