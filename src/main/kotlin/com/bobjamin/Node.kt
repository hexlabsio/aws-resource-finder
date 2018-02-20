package com.bobjamin

import java.util.*

data class NodeTree(val nodes: List<Node> = emptyList(), val links: List<NodeLink> = emptyList())
data class Node(val name: String, val info: AwsResource.Info, val uuid: String = UUID.randomUUID().toString())
data class NodeLink(val uuidA: String, val uuidB: String, val direction: LinkDirection)
enum class LinkDirection{ FORWARD, BACKWARD, BOTH }

object NodeBuilder{
    fun buildFrom(awsResources: List<AwsResource.Relationships>): NodeTree{
        val nodes = awsResources.map { Node(it.resource.arn.arn(), it.resource.info) to it.resource.arn }
        val links = awsResources.map { resource ->
            nodes.find { it.second == resource.resource.arn }!!.first to resource.relatedArns.mapNotNull { arn -> nodes.find { it.second.arn() == arn.arn() }?.first }
        }.flatMap { node -> node.second.map { NodeLink(node.first.uuid, it.uuid, LinkDirection.FORWARD) } }
        return NodeTree(nodes.map { it.first }, links)
    }

    fun groupFrom(node: Node, nodeTree: NodeTree): NodeTree{
        return if(nodeTree.nodes.isEmpty() ||nodeTree.links.isEmpty())NodeTree()
        else{
            val directLinks = nodeTree.links.filter { it.uuidA == node.uuid || it.uuidB == node.uuid }
            val linkedNodes = directLinks.mapNotNull { link -> nodeTree.nodes.find { it.uuid == (if(link.uuidA == node.uuid) link.uuidB else link.uuidA)} }
            if(directLinks.isEmpty() || linkedNodes.isEmpty()) NodeTree()
            else{
                linkedNodes.fold(Pair(NodeTree(),NodeTree(nodeTree.nodes.subtract(linkedNodes + node), nodeTree.links.subtract(directLinks)))){
                    pair, relatedNode ->
                    val nextGroup = groupFrom(relatedNode,pair.second)
                    Pair(
                            NodeTree(pair.first.nodes + nextGroup.nodes, pair.first.links + nextGroup.links),
                            NodeTree(pair.second.nodes.subtract(nextGroup.nodes), pair.second.links.subtract(nextGroup.links))
                    )
                }.first
            }
        }
    }
}

fun <E: Any> List<E>.subtract(other: List<E>): List<E> = this.filterNot { other.contains(it) }