package com.bobjamin

import com.amazonaws.services.ec2.AmazonEC2Client
import org.junit.Test

class AwsFinderTest{
    @Test
    fun test(){
        AwsConfigurator.defaultClient(AmazonEC2Client.builder()).describeRegions().regions.forEach{System.out.println(it)}
    }
}