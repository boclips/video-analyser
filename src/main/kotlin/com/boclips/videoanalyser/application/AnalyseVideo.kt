package com.boclips.videoanalyser.application

import com.boclips.eventtypes.Topics.VIDEOS_TO_ANALYSE_SUBSCRIPTION
import com.boclips.eventtypes.VideoToAnalyse
import com.boclips.videoanalyser.config.messaging.Topics
import com.boclips.videoanalyser.domain.VideoAnalyserService
import mu.KLogging
import org.springframework.cloud.stream.annotation.StreamListener
import org.springframework.messaging.support.MessageBuilder

class AnalyseVideo(
        private val videoAnalyserService: VideoAnalyserService,
        private val topics: Topics
) {
    companion object : KLogging()

    @StreamListener(VIDEOS_TO_ANALYSE_SUBSCRIPTION)
    fun execute(videoToAnalyse: VideoToAnalyse) {
        val videoId = videoToAnalyse.videoId

        logger.info { "Video $videoId received to analyse" }

        val alreadyAnalysed = try {
            videoAnalyserService.isAnalysed(videoId)
        } catch(e: Exception) {
            logger.warn { "Check if video $videoId is already analysed failed and will not be retried." }
            return
        }

        if(alreadyAnalysed) {
            logger.info { "Video $videoId has already been analysed. Enqueuing it to be retrieved." }
            topics.analysedVideoIds().send(MessageBuilder.withPayload(videoId).build())
            return
        }

        try {
            videoAnalyserService.submitVideo(videoId, videoToAnalyse.videoUrl)
        } catch (e: Exception) {
            logger.warn { "Submission of video $videoId to the analyser failed and will not be retried." }
        }
    }
}
