package com.bobjamin

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest
import com.amazonaws.services.ecs.model.ListClustersRequest
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest
import com.bobjamin.AwsResource.Finder.Companion.clientCall

class AwsResourceFinderECS(
        private val ecsClient: (region: String) -> AmazonECS = AwsConfigurator.regionClientFrom(AmazonECSClient.builder())
): AwsResource.Finder {
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap { ecsResources(it, account) }
    }

    fun ecsResources(region: String, account: String): List<AwsResource.Relationships> {
        val ecsClient = ecsClient(region)
        return AwsResource.Finder
                .collectAll({it.nextToken}) { ecsClient.listClusters(ListClustersRequest().withNextToken(it)) }
                .flatMap { it.clusterArns }
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
                .collectAll({it.nextToken}) { ecsClient.listContainerInstances(ListContainerInstancesRequest().withCluster(cluster).withNextToken(it)) }
                .flatMap { it.containerInstanceArns }
                .batch(100)
                .flatMap {
                    clientCall{ecsClient.describeContainerInstances(
                            DescribeContainerInstancesRequest()
                                    .withCluster(cluster)
                                    .withContainerInstances(it)
                    )}[0].containerInstances
                }
                .map {AwsResource.Arn.from(AwsResourceType.INSTANCE,region, account, it.ec2InstanceId)}
    }

    private fun <T> Iterable<T>.batch(chunkSize: Int) =
            withIndex().                        // create index value pairs
                    groupBy { it.index / chunkSize }.   // create grouping index
                    map { it.value.map { it.value } }

}
