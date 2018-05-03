package com.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

class AwsFinderTest{
      // @Test
       fun test(){
           val items = getList()
           val nodeTree = NodeBuilder.buildFrom(items)
           val unlinkedNodes = nodeTree.nodes.filter { node -> nodeTree.links.find { it.uuidA == node.uuid || it.uuidB == node.uuid } == null }
           val relationshipGroups = nodeTree.links.map { it.uuidA to it.uuidB }.relatedGroups()
           val nodeTrees = relationshipGroups.map {
               val links = it.mapNotNull { pair ->
                   nodeTree.links.find { it.uuidA == pair.first && it.uuidB == pair.second }
               }
               val nodes = links.flatMap { link ->
                   nodeTree.nodes.filter { it.uuid == link.uuidA || it.uuid == link.uuidB }
               }.toSet().toList()
               NodeTree(nodes, links)
           }
           val largeTrees = nodeTrees.filter { it.nodes.size >= 10 } +
                   nodeTrees.filter { it.nodes.size < 10 }.fold(NodeTree(emptyList(), emptyList())){acc, it -> NodeTree(acc.nodes + it.nodes, acc.links + it.links)} +
                   NodeTree(unlinkedNodes, emptyList())
           File("C:\\Code\\aws-resource-finder\\target\\groups.json").writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(largeTrees))
    }

    fun getList(): List<AwsResource.Relationships>{
        val account = PropertyFinder.get("ACCOUNT") ?: "--"
        val regions = listOf("us-east-1")
        return finders().flatMap { it.findIn(account, regions) }
    }

    fun finders() = listOf(
            AwsResourceFinderECS(),
            AwsResourceFinderDynamoDB(),
            AwsResourceFinderAPIGateway(),
            AwsResourceFinderSNS(),
            AwsResourceFinderSQS(),
            AwsResourceFinderEC2(),
            AwsResourceFinderLambda(),
            AwsResourceFinderS3(),
            AwsResourceFinderKMS(),
            AwsResourceFinderIAM()
    )
}
