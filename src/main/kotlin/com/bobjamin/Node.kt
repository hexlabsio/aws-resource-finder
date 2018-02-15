package com.bobjamin

import java.util.*

data class NodeTree(val nodes: List<Node>, val links: List<NodeLink>)
data class Node(val name: String, val info: Map<String, String>, val uuid: String = UUID.randomUUID().toString())
data class NodeLink(val uuidA: String, val uuidB: String, val direction: LinkDirection)
enum class LinkDirection{ FORWARD, BACKWARD, BOTH }

object NodeBuilder{
    fun buildFrom(awsResources: List<AwsResource.Relationships<*>>): NodeTree{
        val nodes = awsResources.map { Node(it.resource.arn.arn(), emptyMap()) to it.resource.arn }
        val links = awsResources.map { resource ->
            nodes.find { it.second == resource.resource.arn }!!.first to resource.relatedArns.mapNotNull { arn -> nodes.find { it.second.arn() == arn.arn() }?.first }
        }.flatMap { node -> node.second.map { NodeLink(node.first.uuid, it.uuid, LinkDirection.FORWARD) } }
        return NodeTree(nodes.map { it.first }, links)
    }
}