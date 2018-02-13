package com.bobjamin

import org.junit.Test

class AwsFinderTest{
    @Test
    fun test(){
        AwsResourceFinderEC2().findIn("--", listOf("us-east-1")).forEach{System.out.println(it)}
    }
}