package com.boclips.videoanalyser.infrastructure.videoindexer

import com.boclips.videoanalyser.infrastructure.videoindexer.resources.VideoResource

interface VideoIndexer {

    fun submitVideo(videoId: String, videoUrl: String)
    fun getVideoIndex(videoId: String): VideoResource
}

