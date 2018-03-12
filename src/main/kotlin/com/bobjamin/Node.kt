package com.bobjamin

import java.util.*

data class NodeTree(val nodes: List<Node>, val links: List<NodeLink>)
data class Node(val name: String, val info: AwsResource.Info, val uuid: String = UUID.randomUUID().toString())
data class NodeLink(val uuidA: String, val uuidB: String, val direction: LinkDirection)
enum class LinkDirection{ FORWARD, BACKWARD, BOTH }

data class Leaf<out T>(val node: T, val leaves: List<Leaf<T>>)
fun <T> Leaf<T>.relatedGroup(alreadyHit: List<Leaf<T>> = emptyList()): List<Leaf<T>>{
    return this.leaves.filter { it !in alreadyHit && it != this }.flatMap { it.relatedGroup(alreadyHit + this) }
}

fun <T> List<Pair<T,T>>.filterEither(item: T): List<Pair<T,T>> = this.filter{ it.first == item || it.second == item }
fun <T> Collection<Pair<T,T>>.containsEither(item: T): Boolean = this.find{ it.first == item || it.second == item } != null
fun <T> List<Pair<T,T>>.relatedGroups(): List<List<Pair<T,T>>> {
    val groups = mutableListOf<List<Pair<T,T>>>()
    var filteredList = this
    while(filteredList.isNotEmpty()){
        val group = filteredList.relatedGroupOf(filteredList[0].first)
        groups.add(group)
        filteredList = filteredList.filter{ !group.contains(it) }
    }
    return groups
}
fun <T> List<Pair<T,T>>.relatedGroupOf(item: T, filter: Set<Pair<T,T>> = emptySet()): List<Pair<T,T>> {
    if(this.isEmpty()) return emptyList()
    val relations = this.filterEither(item).filter { !filter.contains(it) }
    var matchedRealtionships = filter + relations
    val relationNodes = relations.map { if(it.first == item)it.second else it.first }
    relationNodes.filter { !filter.containsEither(it) }.forEach { related ->
        matchedRealtionships += this.relatedGroupOf(related, matchedRealtionships)
    }
    return matchedRealtionships.toList()
}


object NodeBuilder{
    fun buildFrom(awsResources: List<AwsResource.Relationships>): NodeTree{
        val nodes = awsResources.map { Node(it.resource.arn.arn(), it.resource.info) to it.resource.arn }
        val links = awsResources.map { resource ->
            nodes.find { it.second == resource.resource.arn }!!.first to resource.relatedArns.mapNotNull { arn -> nodes.find { it.second.arn() == arn.arn() }?.first }
        }.flatMap { node -> node.second.map { NodeLink(node.first.uuid, it.uuid, LinkDirection.FORWARD) } }
        return NodeTree(nodes.map { it.first }, links)
    }



}