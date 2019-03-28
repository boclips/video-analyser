package com.boclips.videoanalyser.presentation

import com.boclips.videoanalyser.testsupport.fakes.AbstractSpringIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class VideosControllerTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var indexingProgressCallbackFactory: IndexingProgressCallbackFactory

    @Test
    @WithMockUser
    fun analyse() {
        mockMvc.perform(post("/v1/videos/123/analyse").param("videoUrl", "http://videos.com/v1").contentType(APPLICATION_JSON))
                .andExpect(status().isAccepted)

        assertThat(fakeVideoIndexer.submittedVideo("123")).isEqualTo("http://videos.com/v1")
    }

    @Test
    fun `analyse does not submit the video when request is not authorised`() {
        mockMvc.perform(post("/v1/videos/123/analyse").param("videoUrl", "http://videos.com/v1").contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized)

        assertThat(fakeVideoIndexer.submittedVideo("123")).isNull()
    }

    @Test
    fun `update process`() {
        val callback = indexingProgressCallbackFactory.forVideo("123")

        mockMvc.perform(post("$callback?id=msid&state=Processed"))
                .andExpect(status().isOk)
    }
}
