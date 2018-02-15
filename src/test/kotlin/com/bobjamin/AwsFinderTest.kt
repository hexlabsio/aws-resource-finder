package com.bobjamin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Test

class AwsFinderTest{
    @Test
    fun test(){
        val items = getList()
        val nodeTree = NodeBuilder.buildFrom(items)
        System.out.println(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(nodeTree))

        items.forEach{ resource -> resource.relatedArns.forEach{ System.out.println(resource.resource.arn.arn() + " -> " + it.arn()) }}
//            .groupBy { it.resource.arn.service }
//            .map { it.key to it.value.flatMap { it.relatedArns.map { it.service + it.subType } }.toSet() }

//        items.forEach{ a -> a.second.forEach{System.out.println(a.first + " -> " + it)}}
    }

    fun getList(): List<AwsResource.Relationships<*>>{
        val account = "--"
        val regions = listOf("us-east-1")
        return AwsResourceFinderEC2().findIn(account, regions)+
                AwsResourceFinderLambda().findIn(account, regions)
       // return AwsResourceFinderS3().findIn("", emptyList())
//        return AwsResourceFinderEC2().findIn(account, regions) +
//                AwsResourceFinderKMS().findIn(account, regions) +
//                AwsResourceFinderLambda().findIn(account, regions) +
//                AwsResourceFinderIAM().findIn(account, regions) +
//                AwsResourceFinderSQS().findIn(account, regions)
    }
}