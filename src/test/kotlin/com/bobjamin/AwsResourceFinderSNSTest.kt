package com.bobjamin

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.*
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderSNSTest {

    @Test
    fun `should build appropriate relationships for sns resources`() {
        val snsClient = Mockito.mock(AmazonSNS::class.java)
        val listSubscriptionsResult = ListSubscriptionsResult().withSubscriptions(listOf(
                Subscription().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic1")
                        .withEndpoint("arn:aws:lambda:eu-west-1:123456789012:function:lambda1")
                        .withProtocol("lambda"),
                Subscription().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic1")
                        .withEndpoint("arn:aws:lambda:eu-west-1:123456789012:function:lambda2")
                        .withProtocol("lambda"),
                Subscription().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic2")
                        .withEndpoint("arn:aws:lambda:eu-west-1:123456789012:function:lambda3")
                        .withProtocol("lambda")

        ))

        val listTopicsResult = ListTopicsResult().withTopics(listOf(
                Topic().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic1"),
                Topic().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic3"),
                Topic().withTopicArn("arn:aws:sns:eu-west-1:123456789012:MyTopic2")
        ))

        Mockito.`when`(snsClient.listSubscriptions(Mockito.any<ListSubscriptionsRequest>())).thenReturn(listSubscriptionsResult)
        Mockito.`when`(snsClient.listTopics(Mockito.any<ListTopicsRequest>())).thenReturn(listTopicsResult)
        val resources = AwsResourceFinderSNS({ snsClient }).findIn("a", listOf("eu-west-1"))
        assertEquals(3, resources.size)

        assertEquals(AwsResource.Relationships(resource = AwsResource(
                        arn = AwsResource.Arn(service = "sns",
                                            region = "eu-west-1",
                                            account = "123456789012",
                                            resource = "MyTopic1",
                                            partition = "aws"),
                                            info = AwsResource.Info("MyTopic1","AWS::SNS::Topic")),
                        relatedArns= listOf(
                            AwsResource.Arn(service = "lambda",
                                            region = "eu-west-1",
                                            account = "123456789012",
                                            partition = "aws",
                                            resource = "function:lambda1",
                                            subType = "function",
                                            subId = "lambda1"),
                            AwsResource.Arn(service = "lambda",
                                    region = "eu-west-1",
                                    account = "123456789012",
                                    partition = "aws",
                                    resource = "function:lambda2",
                                    subType = "function",
                                    subId = "lambda2"))), resources[0])


        assertEquals(AwsResource.Relationships(resource = AwsResource(
                        arn = AwsResource.Arn(service = "sns",
                                            region = "eu-west-1",
                                            account = "123456789012",
                                            resource = "MyTopic2",
                                            partition = "aws"),
                                            info = AwsResource.Info("MyTopic2","AWS::SNS::Topic")),
                        relatedArns= listOf(
                            AwsResource.Arn(service = "lambda",
                                            region = "eu-west-1",
                                            account = "123456789012",
                                            partition = "aws",
                                            resource = "function:lambda3",
                                            subType = "function",
                                            subId = "lambda3"))), resources[1])

        assertEquals(AwsResource.Relationships(resource = AwsResource(
                        arn = AwsResource.Arn(service = "sns",
                                    region = "eu-west-1",
                                    account = "123456789012",
                                    resource = "MyTopic3",
                                    partition = "aws"),
                                    info = AwsResource.Info("MyTopic3","AWS::SNS::Topic")),
                        relatedArns= emptyList()), resources[2])
    }
}



