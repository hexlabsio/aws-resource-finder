package com.bobjamin

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.AWSKMSClient
import com.amazonaws.services.kms.model.ListKeysRequest

class AwsResourceFinderKMS(
        private val kmsClient: (region: String) -> AWSKMS = AwsConfigurator.regionClientFrom(AWSKMSClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap{ kmsResources(it, account) }
    }

    fun kmsResources(region: String, account: String): List<AwsResource.Relationships>{
        val kmsClient = kmsClient(region)
        return AwsResource.Finder
                .collectAll( { it.nextMarker } ){ kmsClient.listKeys(ListKeysRequest().withMarker(it)) }
                .flatMap { it.keys }
                .map {
                    val keyArn = AwsResource.Arn.from(it.keyArn)
                    System.out.println(keyArn.arn())
                    AwsResource.Relationships(AwsResource(keyArn, AwsResource.Info(it.keyId, AwsResourceType.KEY.type())))
                }
    }
}