package com.cluster

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderAPIGatewayTest {

    @Test
    fun `should build appropriate relationships for api resources`(){
        val apiGatewayClient = Mockito.mock(AmazonApiGateway::class.java)


        val listRestAPIsResult = GetRestApisResult().withItems(listOf(
                RestApi().withId("api1").withName("my-api")
        ))
        Mockito.`when`(apiGatewayClient.getRestApis(Mockito.any())).thenReturn(listRestAPIsResult)

        val listResourcesResult = GetResourcesResult().withItems(listOf(
                Resource().withId("path1")
                        .withPath("/path1")
                        .withResourceMethods(mapOf(
                                Pair("method1", Method().withHttpMethod("GET").withMethodIntegration(Integration().withType("AWS_PROXY").withUri("arn:aws::lambda:us-east-1:account:function:firstlambda"))),
                                Pair("method2", Method().withHttpMethod("POST").withMethodIntegration(Integration().withType("AWS_PROXY").withUri("arn:aws::lambda:us-east-1:account:function:secondlambda"))))),
                Resource().withId("path2")
                        .withPath("/path2")
                        .withResourceMethods(mapOf(
                                Pair("method3", Method().withHttpMethod("GET").withMethodIntegration(Integration().withType("AWS_PROXY").withUri("arn:aws::lambda:us-east-1:account:function:thirdlambda"))),
                                Pair("method4", Method().withHttpMethod("POST").withMethodIntegration(Integration().withType("AWS_PROXY").withUri("arn:aws::lambda:us-east-1:account:function:thirdlambda")))))
        ))
        Mockito.`when`(apiGatewayClient.getResources(Mockito.any())).thenReturn(listResourcesResult)



        val resources = AwsResourceFinderAPIGateway({ apiGatewayClient }).findIn("--", listOf("us-east-1"))

        assertEquals(7, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1", this.arn.arn())
                assertEquals("my-api", this.info.title)
                assertEquals("AWS::ApiGateway::RestApi", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(2, this.size)
                assertEquals(listOf("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1","arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2"), this.map { it.arn()})
            }
        }
        with(resources[1]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1", this.arn.arn())
                assertEquals("/path1", this.info.title)
                assertEquals("AWS::ApiGateway::Resource", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(2, this.size)
                assertEquals(listOf("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1/methods/GET","arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1/methods/POST"), this.map { it.arn()})
            }
        }

        with(resources[2]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2", this.arn.arn())
                assertEquals("/path2", this.info.title)
                assertEquals("AWS::ApiGateway::Resource", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(2, this.size)
                assertEquals(listOf("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2/methods/GET","arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2/methods/POST"), this.map { it.arn()})
            }
        }

        with(resources[3]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1/methods/GET", this.arn.arn())
                assertEquals("GET", this.info.title)
                assertEquals("AWS::ApiGateway::Method", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(1, this.size)
                assertEquals(listOf("arn:aws:::lambda:us-east-1:account:function:firstlambda"), this.map { it.arn()})
            }
        }

        with(resources[4]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path1/methods/POST", this.arn.arn())
                assertEquals("POST", this.info.title)
                assertEquals("AWS::ApiGateway::Method", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(1, this.size)
                assertEquals(listOf("arn:aws:::lambda:us-east-1:account:function:secondlambda"), this.map { it.arn()})
            }
        }

        with(resources[5]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2/methods/GET", this.arn.arn())
                assertEquals("GET", this.info.title)
                assertEquals("AWS::ApiGateway::Method", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(1, this.size)
                assertEquals(listOf("arn:aws:::lambda:us-east-1:account:function:thirdlambda"), this.map { it.arn()})
            }
        }

        with(resources[6]){
            with(this.resource){
                assertEquals("arn:aws:apigateway:us-east-1::/restapis/api1/resources/path2/methods/POST", this.arn.arn())
                assertEquals("POST", this.info.title)
                assertEquals("AWS::ApiGateway::Method", this.info.type)
            }
            with(this.relatedArns) {
                assertEquals(1, this.size)
                assertEquals(listOf("arn:aws:::lambda:us-east-1:account:function:thirdlambda"), this.map { it.arn()})
            }
        }
    }
}