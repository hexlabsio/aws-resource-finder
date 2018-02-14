package com.bobjamin

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient

class AwsResourceFinderSQS(
        private val sqsClient: (region: String) -> AmazonSQS = AwsConfigurator.regionClientFrom(AmazonSQSClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return regions.flatMap{ sqsResources(it) }
    }

    fun sqsResources(region: String): List<AwsResource.Relationships<SqsInfo>>{
        val sqsClient = sqsClient(region)
        return AwsResource.Finder
                .clientCall{ sqsClient.listQueues() }
                .flatMap { it.queueUrls }
                .map {
                    val fifo = it.endsWith(".fifo")
                    val queueName = it.substringAfterLast('/').let { if(fifo)it.substringBeforeLast('.') else it }
                    val account= it.substringBeforeLast('/').substringAfterLast('/')
                    val region = it.substringAfter('.').substringBefore('.')
                    val queueArn = AwsResource.Arn.from(AwsResourceType.QUEUE, region, account, queueName)
                    System.out.println(queueArn.arn())
                    AwsResource.Relationships(AwsResource(queueArn, SqsInfo(fifo)))
                }
    }
     class SqsInfo(val fifo: Boolean): AwsResource.Info
}