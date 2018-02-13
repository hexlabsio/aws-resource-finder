package com.bobjamin

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Volume

class AwsResourceFinderEC2(
        private val ec2Client: (region: String) -> AmazonEC2 = AwsConfigurator.regionClientFrom(AmazonEC2Client.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return regions.flatMap{ ec2Resources(it, account) + ebsResources(it, account) }
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
                    System.out.println(instanceArn.arn())
                    AwsResource.Relationships(AwsResource(instanceArn, EC2Info(it.instanceType)),relatedArnsFor(it, region, account))
                }
    }

    fun ebsResources(region: String, account: String): List<AwsResource.Relationships<EBSInfo>>{
        val ec2Client = ec2Client(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ ec2Client.describeVolumes().withNextToken(it) }
                .flatMap { it.volumes }
                .map {
                    val volumeArn = AwsResource.Arn.from(AwsResourceType.VOLUME, region, account, it.volumeId)
                    System.out.println(volumeArn.arn())
                    AwsResource.Relationships(AwsResource(volumeArn, EBSInfo(it.availabilityZone, it.encrypted, it.size, it.state, it.volumeType)),relatedArnsFor(it, region, account))
                }
    }

    private fun relatedArnsFor(instance: Instance, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            if(instance.imageId != null) AwsResource.Arn.from(AwsResourceType.IMAGE, region,account,instance.imageId) else null,
            if(instance.iamInstanceProfile != null) AwsResource.Arn.from(instance.iamInstanceProfile.arn) else null
    )

    private fun relatedArnsFor(volume: Volume, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            if(volume.kmsKeyId != null) AwsResource.Arn.from(volume.kmsKeyId) else null
    ) + volume.attachments.filter { it.state == "attached" }.map { AwsResource.Arn.from(AwsResourceType.INSTANCE, region, account, it.instanceId) }

    data class EC2Info(val instanceType: String): AwsResource.Info
    data class EBSInfo(val availabilityZone: String, val encrypted: Boolean, val size: Int, val state: String, val type: String): AwsResource.Info
}