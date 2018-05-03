package com.cluster

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.ListQueuesResult
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderSQSTest {

    @Test
    fun `should build appropriate relationships for sqs resources`(){
        val sqsClient = Mockito.mock(AmazonSQS::class.java)
        val listQueuesResult = ListQueuesResult().withQueueUrls(listOf(
               "http://sqs.us-east-2.amazonaws.com/123456789012/MyQueue",
                "http://sqs.us-east-2.amazonaws.com/123456789012/MyQueue2.fifo"
        ))
        Mockito.`when`(sqsClient.listQueues()).thenReturn(listQueuesResult)
        val resources = AwsResourceFinderSQS({ sqsClient }).findIn("a", listOf("us-east-1"))
        assertEquals(2, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:sqs:us-east-2:123456789012:MyQueue", this.arn.arn())
            }
        }
        with(resources[1]){
            with(this.resource){
                assertEquals("arn:aws:sqs:us-east-2:123456789012:MyQueue2", this.arn.arn())
            }
        }
    }
}