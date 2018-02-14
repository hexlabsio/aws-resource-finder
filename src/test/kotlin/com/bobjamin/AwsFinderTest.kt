package com.bobjamin

import org.junit.Test

class AwsFinderTest{
    @Test
    fun test(){
        AwsResourceFinderSQS().findIn("--", listOf("us-east-1"))
                .flatMap { resource -> resource.relatedArns.map { resource.resource.arn.arn() + " -> " + it.arn() } }
                .forEach{System.out.println(it)}
    }
}