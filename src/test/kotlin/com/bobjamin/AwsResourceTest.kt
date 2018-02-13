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
}