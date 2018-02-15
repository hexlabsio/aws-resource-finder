package com.bobjamin

import junit.framework.TestCase.assertEquals
import org.junit.Test

class AwsResourceTest{
    @Test
    fun `should match EC2 Instance ARN`(){
        val arn = AwsResource.Arn.from("arn:aws:ec2:region:account-id:instance/instance-id")
        assertEquals("aws", arn.partition)
        assertEquals("ec2", arn.service)
        assertEquals("region", arn.region)
        assertEquals("account-id", arn.account)
        assertEquals("instance/instance-id", arn.resource)
        assertEquals("instance", arn.subType)
        assertEquals("instance-id", arn.subId)
    }

    @Test
    fun `should match Log Group ARN`(){
        val arn = AwsResource.Arn.from("arn:aws:logs:us-east-1:123456789012:log-group:my-log-group*:log-stream:my-log-stream*")
        assertEquals("aws", arn.partition)
        assertEquals("logs", arn.service)
        assertEquals("us-east-1", arn.region)
        assertEquals("123456789012", arn.account)
        assertEquals("log-group:my-log-group*:log-stream:my-log-stream*", arn.resource)
        assertEquals("log-group", arn.subType)
        assertEquals("my-log-group*:log-stream:my-log-stream*", arn.subId)
    }

    @Test
    fun `should match * ARNs`(){
        val arn = AwsResource.Arn.from("arn:aws:rds:us-east-1:*")
        assertEquals("rds", arn.service)
        assertEquals("aws", arn.partition)
        assertEquals("us-east-1", arn.region)
        assertEquals("*", arn.account)
        assertEquals("*", arn.resource)
    }

    class Tokenizable(val nextToken: String?, val value: Int)
    @Test
    fun `should collect all results from iterable client call`(){
        val tokenList = mapOf( null to Tokenizable("a", 0), "a" to Tokenizable("b", 1), "b" to Tokenizable("c", 3), "c" to Tokenizable(null, 2))
        val responseList = AwsResource.Finder.collectAll({it!!.nextToken}){ token -> tokenList[token] }.map { it!!.value }
        assertEquals(4, responseList.size)
        assertEquals(1, responseList[1])
        assertEquals(3,  responseList[2])
        assertEquals(2,  responseList[3])
    }
}