package com.bobjamin

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.*
import junit.framework.TestCase.*
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Matchers.eq
import org.mockito.Mockito

class AwsResourceFinderECSTest {

    @Test
    fun `should build appropriate relationships for ecs resources`(){
        val ecsClient = Mockito.mock(AmazonECS::class.java)
        val c1 = "arn:aws:ecs:eu-west-1:a:cluster/default"
        val i1 =  "arn:aws:ecs:eu-west-1:a:container-instance/427420384093-242730487230"
        val listClusterResult = ListClustersResult().withClusterArns(listOf(
              c1
        ))
        val listContainerInstancesResult = ListContainerInstancesResult().withContainerInstanceArns(listOf(
               i1
        ))
        val describeContainerInstancesResult = DescribeContainerInstancesResult().withContainerInstances(listOf(ContainerInstance().withEc2InstanceId("i-123456789")))
        Mockito.`when`(ecsClient.listClusters(any())).thenReturn(listClusterResult)
        Mockito.`when`(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult)
        Mockito.`when`(ecsClient.describeContainerInstances(eq(DescribeContainerInstancesRequest().withCluster(c1).withContainerInstances(listOf(i1)))))
                .thenReturn(describeContainerInstancesResult)
        val resources = AwsResourceFinderECS({ ecsClient }).findIn("a", listOf("eu-west-1"))
        assertEquals(1, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:ecs:eu-west-1:a:cluster/default", this.arn.arn())
            }
            assertEquals(1, this.relatedArns.size)
            with(this.relatedArns){
                assertEquals("arn:aws:ec2:eu-west-1:a:instance/i-123456789", this[0].arn())
            }
        }
    }
}