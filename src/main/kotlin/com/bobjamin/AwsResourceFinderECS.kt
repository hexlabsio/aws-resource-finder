package com.bobjamin

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest

class AwsResourceFinderECS(
        private val ecsClient: (region: String) -> AmazonECS = AwsConfigurator.regionClientFrom(AmazonECSClient.builder())
): AwsResource.Finder {
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap { ecsResources(it, account) }
    }

    fun ecsResources(region: String, account: String): List<AwsResource.Relationships> {
        val ecsClient = ecsClient(region)
        return AwsResource.Finder
                .clientCall { ecsClient.describeClusters() }
                .flatMap { it.clusters }
                .map { it.clusterArn }
                .map { cluster ->
                    System.out.println(cluster)
                    AwsResource.Relationships(
                            AwsResource(
                                    AwsResource.Arn.from(cluster),
                                    AwsResource.Info("cluster", AwsResourceType.CLUSTER.type())
                            ), relatedArnsFor(cluster, region, account)
                    )
                }
    }

    fun relatedArnsFor(cluster: String, region: String, account: String): List<AwsResource.Arn>{
        val ecsClient = ecsClient(region)
        return AwsResource
                .Finder
                .clientCall { ecsClient.describeContainerInstances(DescribeContainerInstancesRequest().withCluster(cluster)) }
                .flatMap { it.containerInstances }
                .map { AwsResource.Arn.from(AwsResourceType.INSTANCE, region, account, it.ec2InstanceId) }
    }
}