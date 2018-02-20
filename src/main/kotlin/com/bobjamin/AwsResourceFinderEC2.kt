package com.bobjamin

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.*

class AwsResourceFinderEC2(
        private val ec2Client: (region: String) -> AmazonEC2 = AwsConfigurator.regionClientFrom(AmazonEC2Client.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap{ ec2Resources(it, account) + ebsResources(it, account) + securityGroupResources(it, account) }
    }

    fun ec2Regions(): List<String> = ec2Client("").describeRegions().regions.map { it.regionName }

    fun ec2Resources(region: String, account: String): List<AwsResource.Relationships>{
        val ec2Client = ec2Client(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ ec2Client.describeInstances(DescribeInstancesRequest().withNextToken(it)) }
                .flatMap { it.reservations }
                .flatMap { it.instances }
                .filter { it.state.name == "running" || it.state.name == "stopped" }
                .map {
                    val instanceArn = AwsResource.Arn.from(AwsResourceType.INSTANCE, region, account, it.instanceId)
                    System.out.println(instanceArn.arn())
                    AwsResource.Relationships(
                            AwsResource(
                                    instanceArn,
                                    AwsResource.Info(it.instanceId, AwsResourceType.INSTANCE.type(),ec2PropertiesFrom(it),sizeFrom(it.instanceType))
                            )
                            ,relatedArnsFor(it, region, account)
                    )
                }
    }

    fun ebsResources(region: String, account: String): List<AwsResource.Relationships>{
        val ec2Client = ec2Client(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ ec2Client.describeVolumes(DescribeVolumesRequest().withNextToken(it)) }
                .flatMap { it.volumes }
                .map {
                    val volumeArn = AwsResource.Arn.from(AwsResourceType.VOLUME, region, account, it.volumeId)
                    System.out.println(volumeArn.arn())
                    AwsResource.Relationships(
                            AwsResource(
                                    volumeArn,
                                    AwsResource.Info(it.volumeId,AwsResourceType.VOLUME.type(),ebsPropertiesFrom(it), sizeFrom(it.size))),
                            relatedArnsFor(it, region, account)
                    )
                }
    }

    fun securityGroupResources(region: String, account: String): List<AwsResource.Relationships>{
        val ec2Client = ec2Client(region)
        return AwsResource.Finder
                .collectAll( { it.nextToken } ){ ec2Client.describeSecurityGroups(DescribeSecurityGroupsRequest().withNextToken(it)) }
                .flatMap { it.securityGroups }
                .map {
                    val groupArn = AwsResource.Arn.from(AwsResourceType.SECURITY_GROUP, region, account, it.groupId)
                    System.out.println(groupArn.arn())
                    AwsResource.Relationships(
                            AwsResource(
                                    groupArn,
                                    AwsResource.Info(it.groupName,AwsResourceType.SECURITY_GROUP.type())
                            )
                    )
                }
    }

    private fun relatedArnsFor(instance: Instance, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            if(instance.imageId != null) AwsResource.Arn.from(AwsResourceType.IMAGE, region,account,instance.imageId) else null,
            if(instance.iamInstanceProfile != null) AwsResource.Arn.from(instance.iamInstanceProfile.arn) else null
    ) + instance.securityGroups.map { AwsResource.Arn.from(AwsResourceType.SECURITY_GROUP, region, account, it.groupId) }

    private fun relatedArnsFor(volume: Volume, region: String, account: String): List<AwsResource.Arn> = listOfNotNull(
            if(volume.kmsKeyId != null) AwsResource.Arn.from(volume.kmsKeyId) else null
    ) + volume.attachments.filter { it.state == "attached" }.map { AwsResource.Arn.from(AwsResourceType.INSTANCE, region, account, it.instanceId) }


    fun ec2PropertiesFrom(instance: Instance): Map<String, String>{
        return mapOf(
                "Instance Id" to instance.instanceId,
                "Type" to instance.instanceType
        )
    }
    fun ebsPropertiesFrom(volume: Volume): Map<String, String>{
        return mapOf(
                "Volume Id" to volume.volumeId,
                "Type" to volume.volumeType,
                "Encrypted" to if(volume.encrypted) "Yes" else "No"
        )
    }
    fun sizeFrom(instanceType: String): Double{
        return when{
            instanceType.contains("2xlarge") -> 1.0
            instanceType.contains("xlarge") -> 0.8
            instanceType.contains("large") -> 0.6
            instanceType.contains("medium") -> 0.5
            else -> 0.0
        }
    }
    fun sizeFrom(volumeSize: Int): Double{
        return when{
            volumeSize > 1000 -> 1.0
            else -> volumeSize / 1000.0
        }
    }

}