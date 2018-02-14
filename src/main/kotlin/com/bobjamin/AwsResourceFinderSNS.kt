package com.bobjamin

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient

class AwsResourceFinderSNS(
        private val snsClient: (region: String) -> AmazonSNS = AwsConfigurator.regionClientFrom(AmazonSNSClient.builder())
) : AwsResource.Finder {
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<SnsInfo>> {
        return regions.flatMap { snsResources(it) }
    }

    fun snsResources(region: String): List<AwsResource.Relationships<SnsInfo>> {
        val snsClient = snsClient(region)
        return AwsResource.Finder
                .clientCall { snsClient.listTopics() }
                .flatMap { it.topics }
                .map {
                    val topArn = AwsResource.Arn.from(it.topicArn)
                    AwsResource.Relationships(AwsResource(topArn, SnsInfo()))
                }
    }

    class SnsInfo : AwsResource.Info
}