package com.cluster

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.Test
import java.io.File

class AwsFinderTest{
       @Test
       fun test(){
           val nodeTree = nodeTree()
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
           File("/Users/chrisbarbour/Code/cloud-clusterer/groups.json").writeText(jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(largeTrees))
    }

    fun nodeTree():NodeTree{
        val file = File("/Users/chrisbarbour/Code/cloud-clusterer/cluster.json").readText()
       return jacksonObjectMapper().readValue(file)
    }
}
