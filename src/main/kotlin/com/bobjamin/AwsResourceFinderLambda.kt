package com.bobjamin

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClient
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.ListFunctionsRequest

class AwsResourceFinderLambda(
        private val lambdaClient: (region: String) -> AWSLambda = AwsConfigurator.regionClientFrom(AWSLambdaClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return regions.flatMap{ lambdaResources(it, account) }
    }

    fun lambdaResources(region: String, account: String): List<AwsResource.Relationships<LambdaInfo>>{
        val lambdaClient = lambdaClient(region)
        return AwsResource.Finder
                .collectAll( { it.nextMarker } ){ lambdaClient.listFunctions(ListFunctionsRequest().withMarker(it)) }
                .flatMap { it.functions }
                .map {
                    val functionArn = AwsResource.Arn.from(it.functionArn)
                    System.out.println(functionArn.arn())
                    AwsResource.Relationships(AwsResource(functionArn, LambdaInfo(it.runtime, it.timeout, it.memorySize)),relatedArnsFor(it, region, account))
                }
    }

    private fun relatedArnsFor(function: FunctionConfiguration, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            function.role?.let { AwsResource.Arn.from(AwsResourceType.ROLE, region,account, it) },
            function.kmsKeyArn?.let { AwsResource.Arn.from(function.kmsKeyArn) }
    )

    data class LambdaInfo(val runtime: String, val timeout: Int, val memorySize: Int): AwsResource.Info
}