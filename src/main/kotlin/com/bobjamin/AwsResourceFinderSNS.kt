package com.bobjamin

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.ListSubscriptionsRequest
import com.amazonaws.services.sns.model.ListTopicsRequest

class AwsResourceFinderSNS(
        private val snsClient: (region: String) -> AmazonSNS = AwsConfigurator.regionClientFrom(AmazonSNSClient.builder())
) : AwsResource.Finder {

    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap {
            val client = snsClient(it)
            mergeResources(snsSubScriptions(client), snsResources(client))
        }
    }

    fun mergeResources(res1: List<AwsResource.Relationships>, res2: List<AwsResource.Relationships>) =
            res2.fold(res1) {acc, elem ->
                if(acc.filter { it.resource.arn == elem.resource.arn }.isEmpty()) acc + elem
                else acc
            }

    fun snsResources(snsClient: AmazonSNS): List<AwsResource.Relationships> {
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ snsClient.listTopics(ListTopicsRequest().withNextToken(it)) }
                .flatMap { it.topics }
                .map {
                    val topArn = AwsResource.Arn.from(it.topicArn)
                    AwsResource.Relationships(AwsResource(topArn, AwsResource.Info(topArn.resource, AwsResourceType.TOPIC.type())))
                }
    }

    fun snsSubScriptions(snsClient: AmazonSNS): List<AwsResource.Relationships>  {
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ snsClient.listSubscriptions(ListSubscriptionsRequest().withNextToken(it)) }
                .flatMap { it.subscriptions }
                .filter { listOf("lambda", "sqs").contains(it.protocol) }
                .map { AwsResource.Arn.from(it.topicArn) to AwsResource.Arn.from(it.endpoint) }
                .groupBy { it.first }.mapValues { it.value.map { it.second } }
                .map{AwsResource.Relationships(AwsResource(it.key, AwsResource.Info(it.key.resource, AwsResourceType.TOPIC.type())), it.value)}
    }

}