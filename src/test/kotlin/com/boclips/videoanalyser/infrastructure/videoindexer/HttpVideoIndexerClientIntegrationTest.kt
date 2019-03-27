package com.boclips.videoanalyser.infrastructure.videoindexer

import com.boclips.videoanalyser.presentation.IndexingProgressCallbackFactory
import com.boclips.videoanalyser.testsupport.fakes.AbstractSpringIntegrationTest
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.io.Resource

class HttpVideoIndexerClientIntegrationTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var restTemplateBuilder: RestTemplateBuilder

    @Autowired
    lateinit var indexingProgressCallbackFactory: IndexingProgressCallbackFactory

    @Value("classpath:videoindexer/responses/videoUpload.json")
    lateinit var videoUploadResponseResource: Resource

    @Value("classpath:videoindexer/responses/videoIndex.json")
    lateinit var videoIndexResponseResource: Resource

    lateinit var wireMockServer: WireMockServer

    lateinit var videoIndexer: HttpVideoIndexerClient

    @BeforeEach
    fun setUp() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()
        var properties = VideoIndexerProperties(
                apiBaseUrl = wireMockServer.baseUrl(),
                accountId = "account1",
                subscriptionKey = "subs-key"
        )

        videoIndexer = HttpVideoIndexerClient(
                restTemplate = restTemplateBuilder.build(),
                properties = properties,
                indexingProgressCallbackFactory = indexingProgressCallbackFactory
        )
        wireMockServer.stubFor(get(urlPathEqualTo("/auth/northeurope/Accounts/account1/AccessToken"))
                .withHeader("Ocp-Apim-Subscription-Key", equalTo("subs-key"))
                .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("\"test-access-token\"")
                )
        )
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun submit() {
        wireMockServer.stubFor(post(urlPathEqualTo("/northeurope/Accounts/account1/Videos"))
                .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(videoUploadResponseResource.inputStream.readAllBytes())
                )
        )

        videoIndexer.submitVideo("video1", "https://cdnapisec.example.com/v/1")

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/northeurope/Accounts/account1/Videos"))
                .withQueryParam("accessToken", equalTo("test-access-token"))
                .withQueryParam("name", equalTo("video1"))
                .withQueryParam("videoUrl", equalTo("https://cdnapisec.example.com/v/1"))
                .withQueryParam("externalUrl", equalTo("https://cdnapisec.example.com/v/1"))
                .withQueryParam("externalId", equalTo("video1"))
                .withQueryParam("callbackUrl", equalTo("https://video-analyser.test-boclips.com/v1/videos/video1/check_indexing_progress"))
                .withQueryParam("language", equalTo("auto"))
                .withQueryParam("indexingPreset", equalTo("AudioOnly"))
                .withQueryParam("privacy", equalTo("Private")))
    }

    @Test
    fun getVideoIndex() {
        val videoId = "video-id-1234"
        val microsoftVideoId = "ms-id-1234"

        wireMockServer.stubFor(get(urlPathEqualTo("/northeurope/Accounts/account1/Videos/GetIdByExternalId"))
                .withQueryParam("accessToken", equalTo("test-access-token"))
                .withQueryParam("externalId", equalTo(videoId))
                .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("\"$microsoftVideoId\"")
                )
        )

        wireMockServer.stubFor(get(urlPathEqualTo("/northeurope/Accounts/account1/Videos/$microsoftVideoId"))
                .withQueryParam("accessToken", equalTo("test-access-token"))
                .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(videoIndexResponseResource.inputStream.readAllBytes())
                )
        )

        wireMockServer.stubFor(get(urlPathEqualTo("/northeurope/Accounts/account1/Videos/$microsoftVideoId/Captions"))
                .withQueryParam("accessToken", equalTo("test-access-token"))
                .withQueryParam("format", equalTo("vtt"))
                .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream").withBody("contents of vtt file".toByteArray())
                )
        )

        val response = videoIndexer.getVideoIndex(videoId)

        assertThat(response.videoId).isEqualTo(videoId)
        assertThat(response.keywords).contains("office")
        assertThat(response.vttCaptions).isEqualTo("contents of vtt file".toByteArray())
        assertThat(response.topics.map { it.name }).contains("Politics and Government")
    }
}
