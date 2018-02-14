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
}