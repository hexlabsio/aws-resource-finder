package com.bobjamin

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.ListSubscriptionsRequest
import com.amazonaws.services.sns.model.ListTopicsRequest

class AwsResourceFinderSNS(
        private val snsClient: (region: String) -> AmazonSNS = AwsConfigurator.regionClientFrom(AmazonSNSClient.builder())
) : AwsResource.Finder {

    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<SnsInfo>> {
        return regions.flatMap { mergeResources(snsSubScriptions(it), snsResources(it)) }
    }

    fun mergeResources(res1: List<AwsResource.Relationships<SnsInfo>>, res2: List<AwsResource.Relationships<SnsInfo>>) =
            res2.fold(res1) {acc, elem ->
                if(acc.filter { it.resource.arn == elem.resource.arn }.isEmpty()) acc + elem
                else acc
            }

    fun snsResources(region: String): List<AwsResource.Relationships<SnsInfo>> {
        val snsClient = snsClient(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ snsClient.listTopics(ListTopicsRequest().withNextToken(it)) }
                .flatMap { it.topics }
                .map {
                    val topArn = AwsResource.Arn.from(it.topicArn)
                    AwsResource.Relationships(AwsResource(topArn, SnsInfo))
                }
    }

    fun snsSubScriptions(region: String): List<AwsResource.Relationships<SnsInfo>>  {
        val snsClient = snsClient(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ snsClient.listSubscriptions(ListSubscriptionsRequest().withNextToken(it)) }
                .flatMap { it.subscriptions }
                .map { AwsResource.Arn.from(it.topicArn) to AwsResource.Arn.from(it.endpoint) }
                .groupBy { it.first }.mapValues { it.value.map { it.second } }
                .map{AwsResource.Relationships(AwsResource(it.key, SnsInfo), it.value)}
    }

    object SnsInfo : AwsResource.Info
}