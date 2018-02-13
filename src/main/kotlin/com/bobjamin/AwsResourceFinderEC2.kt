package com.bobjamin

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.Instance

class AwsResourceFinderEC2(
        private val ec2Client: (region: String) -> AmazonEC2 = AwsConfigurator.regionClientFrom(AmazonEC2Client.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return regions.flatMap{ ec2Resources(it, account) }
    }

    fun ec2Regions(): List<String> = ec2Client("").describeRegions().regions.map { it.regionName }

    fun ec2Resources(region: String, account: String): List<AwsResource.Relationships<EC2Info>>{
        val ec2Client = ec2Client(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ ec2Client.describeInstances().withNextToken(it) }
                .flatMap { it.reservations }
                .flatMap { it.instances }
                .filter { it.state.name == "running" || it.state.name == "stopped" }
                .map {
                    val instanceArn = AwsResource.Arn.from(AwsResourceType.INSTANCE, region, account, it.instanceId)
                    System.out.println(instanceArn)
                    AwsResource.Relationships(AwsResource(instanceArn, EC2Info(it.instanceType)),relatedArnsFor(it, region, account))
                }
    }

    private fun relatedArnsFor(instance: Instance, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            if(instance.imageId != null) AwsResource.Arn.from(AwsResourceType.IMAGE, region,account,instance.imageId) else null,
            if(instance.iamInstanceProfile != null) AwsResource.Arn.from(instance.iamInstanceProfile.arn) else null
    )

    data class EC2Info(val instanceType: String): AwsResource.Info
}