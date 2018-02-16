package com.bobjamin

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ListQueuesResult
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderS3Test {

    @Test
    fun `should build appropriate relationships for s3 resources`(){
        val s3Client = Mockito.mock(AmazonS3::class.java)
        val listBucketsResult = listOf(
               Bucket("bucket-1"), Bucket("bucket-2")
        )
        Mockito.`when`(s3Client.listBuckets()).thenReturn(listBucketsResult)
        val resources = AwsResourceFinderS3({ s3Client }).findIn("a", listOf("us-east-1"))
        assertEquals(2, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:s3:::bucket-1", this.arn.arn())
            }
        }
        with(resources[1]){
            with(this.resource){
                assertEquals("arn:aws:s3:::bucket-2", this.arn.arn())
            }
        }
    }
}