package com.bobjamin

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClient
import com.amazonaws.services.apigateway.model.*

class AwsResourceFinderAPIGateway(
        private val apiGatewayClient: (region: String) -> AmazonApiGateway = AwsConfigurator.regionClientFrom(AmazonApiGatewayClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> = regions.flatMap{ apiResources(it)
    }

    fun apiResources(region: String): List<AwsResource.Relationships<APIInfo>> {
        val apiGateway = apiGatewayClient(region)
        val blah = AwsResource.Finder
                .collectAll( { it.position }) { apiGateway.getRestApis(GetRestApisRequest().withPosition(it))}
                .flatMap { it.items }
                .map { Pair(it, apiGateway.getResources(GetResourcesRequest().withRestApiId(it.id).withEmbed("methods"))) }
                .flatMap { it.second.items.map { item ->  Pair(it.first, item) } }
                .filter { it.second.resourceMethods != null }
                .flatMap { it.second.resourceMethods.map { method -> Pair(it.first, Pair(it.second, method.value.methodIntegration)) } }
                .filter { it.second.second.type == "AWS_PROXY" }
                .map { AwsResource.Relationships(
                        AwsResource(apiGatewayArn(region, it.second.first.path), APIInfo(it.second.second.httpMethod)),
                        listOf(lambdaArnFromInvocationURI(it.second.second.uri)))}

        println("hi $blah")

        return emptyList()
    }
    data class APIInfo(val methodType: String): AwsResource.Info

    companion object {

        private fun apiGatewayArn(region: String, resource: String) = AwsResource.Arn.from("arn:aws:apigateway:$region::$resource")
        private fun lambdaArnFromInvocationURI(uri: String): AwsResource.Arn {
            val lambdaArn = ("arn:aws:" + uri.substringAfterLast("arn:aws")).substringBefore("/invocations")
            return AwsResource.Arn.from(lambdaArn)
        }
    }
}