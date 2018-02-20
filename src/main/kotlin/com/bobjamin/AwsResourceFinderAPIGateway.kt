package com.bobjamin

import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClient
import com.amazonaws.services.apigateway.model.*

class AwsResourceFinderAPIGateway(
        private val apiGatewayClient: (region: String) -> AmazonApiGateway = AwsConfigurator.regionClientFrom(AmazonApiGatewayClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> = regions.flatMap{ apiResources(it)
    }

    fun apiResources(region: String): List<AwsResource.Relationships> {
        val apiGateway = apiGatewayClient(region)

        val apis = AwsResource.Finder
            .collectAll( { it.position }) { apiGateway.getRestApis(GetRestApisRequest().withPosition(it))}
            .flatMap { it.items }

        val resources = apis
            .map { Pair(it, apiGateway.getResources(GetResourcesRequest().withRestApiId(it.id).withEmbed("methods"))) }
            .flatMap { it.second.items.map { item ->  Pair(it.first, item) } }
            .filter { it.second.resourceMethods != null }

        val apiRelationships = resources
            .groupBy { it.first.id }
            .map { AwsResource.Relationships(
                    AwsResource(apiArn(region, it.key), AwsResource.Info(it.value.first().first.name, AwsResourceType.REST_API.type(), emptyMap())),
                    it.value.map { resource -> resourceArn(region, it.key, resource.second.id)}
            )}


        val resourceRelationships = resources
            .map { AwsResource.Relationships (AwsResource(resourceArn(region, it.first.id, it.second.id), AwsResource.Info(it.second.path, AwsResourceType.API_RESOURCE.type(), emptyMap())),
                    it.second.resourceMethods.map { method -> methodArn(region, it.first.id, it.second.id, method.value.httpMethod)})
            }

        val methodRelationships = resources
            .flatMap { it.second.resourceMethods.map { method -> Pair(it.first, Pair(it.second, method)) } }
            .filter { it.second.second.value.methodIntegration.type == "AWS_PROXY" }
            .map { AwsResource.Relationships(
                    AwsResource(methodArn(region, it.first.id, it.second.first.id, it.second.second.value.httpMethod), AwsResource.Info(it.second.second.value.httpMethod, AwsResourceType.API_METHOD.type(), emptyMap())),
                    listOf(lambdaArnFromInvocationURI(it.second.second.value.methodIntegration.uri)))}

        return apiRelationships + resourceRelationships + methodRelationships
    }


    companion object {
        private fun apiArn(region: String, apiId: String) = AwsResource.Arn.from("arn:aws:apigateway:$region::/restapis/$apiId")
        private fun resourceArn(region: String, apiId: String, resourceId: String) = AwsResource.Arn.from("arn:aws:apigateway:$region::/restapis/$apiId/resources/$resourceId")
        private fun methodArn(region: String, apiId: String, resourceId: String, methodType: String) = AwsResource.Arn.from("arn:aws:apigateway:$region::/restapis/$apiId/resources/$resourceId/methods/$methodType")
        private fun lambdaArnFromInvocationURI(uri: String): AwsResource.Arn {
            val lambdaArn = ("arn:aws:" + uri.substringAfterLast("arn:aws")).substringBefore("/invocations")
            return AwsResource.Arn.from(lambdaArn)
        }
    }
}