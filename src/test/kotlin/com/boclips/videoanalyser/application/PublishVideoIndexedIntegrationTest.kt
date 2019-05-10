package com.boclips.videoanalyser.application

import com.boclips.videoanalyser.testsupport.fakes.AbstractSpringIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PublishVideoIndexedIntegrationTest(
        @Autowired val publishAnalysedVideoId: PublishVideoIndexed
) : AbstractSpringIntegrationTest() {

    @Test
    fun `publishes the id`() {
        publishAnalysedVideoId.execute("abc")

        val publishedId = messageCollector.forChannel(topics.videoIndexed()).poll()

        assertThat(publishedId.payload.toString()).isEqualTo("abc")
    }
}
