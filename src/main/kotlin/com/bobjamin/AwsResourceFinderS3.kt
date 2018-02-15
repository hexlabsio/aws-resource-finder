package com.bobjamin

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListBucketsRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient

class AwsResourceFinderS3(
        private val s3Client: (region: String) -> AmazonS3 = AwsConfigurator.regionClientFrom(AmazonS3Client.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return s3Resources()
    }

    fun s3Resources(): List<AwsResource.Relationships<S3Info>>{
        val s3Client = s3Client("")
        return AwsResource.Finder
                .clientCall{ s3Client.listBuckets() }
                .flatMap { it }
                .map {
                    val bucketArn = AwsResource.Arn.from(AwsResourceType.BUCKET, "", "", it.name)
                    System.out.println(bucketArn.arn())
                    AwsResource.Relationships(AwsResource(bucketArn, S3Info(it.name)))
                }
    }
     class S3Info(val name: String): AwsResource.Info
}