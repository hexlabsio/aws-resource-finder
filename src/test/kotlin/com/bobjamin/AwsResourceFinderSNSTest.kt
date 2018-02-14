package com.bobjamin

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.ListTopicsResult
import com.amazonaws.services.sns.model.Topic
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderSNSTest {

    @Test
    fun `should build appropriate relationships for sns resources`() {
        val snsClient = Mockito.mock(AmazonSNS::class.java)
        val listTopicsResult = ListTopicsResult().withTopics(listOf(
                Topic().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic"),
                Topic().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic:subscriptionid")
        ))
        Mockito.`when`(snsClient.listTopics()).thenReturn(listTopicsResult)
        val resources = AwsResourceFinderSNS({ snsClient }).findIn("a", listOf("us-east-1"))
        assertEquals(2, resources.size)
        with(resources[0]) {
            with(this.resource) {
                assertEquals("arn:aws:sns:eu-west-1:123456789012:MyTopic", this.arn.arn())
            }
        }
        with(resources[1]) {
            with(this.resource) {
                assertEquals("arn:aws:sns:eu-west-1:123456789012:MyTopic:subscriptionid", this.arn.arn())
            }
        }
    }
}
