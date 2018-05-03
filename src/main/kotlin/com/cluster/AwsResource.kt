package com.cluster

data class AwsResource(val arn: Arn, val info: Info){
    data class Arn(val service: String, val region: String, val account: String, val resource: String, val subType: String = "", val subId: String = "", val partition: String = "aws"){
        fun arn() = "arn:$partition:$service:$region:$account:$resource"
        companion object {

            fun from(resourceType: AwsResourceType, region: String, account: String, resourceId: String) =
                    Arn(
                            service = resourceType.service.service,
                            region = if(resourceType.hasRegion) region else "",
                            account = if(resourceType.hasAccount) account else "",
                            resource = "${resourceType.resource}${resourceType.arnSeparator}$resourceId",
                            subType = resourceType.resource,
                            subId = if(resourceType.resource.isNotEmpty()) resourceId else ""
                    )

            fun from(arn: String): Arn{
                val args = arn.split(":").toMutableList()
                if(args.size in 2..5 && arn.endsWith("*")){
                    (args.size..5).forEach{ args.add("*") }
                }
                if(args.size < 6){
                    throw IllegalArgumentException("$arn is not a valid arn")
                }
                val resource = (6 until args.size).fold(args[5], { s, i -> "$s:${args[i]}" })
                val subType = subTypeFrom(resource)
                val subId = if(subType != null) resource.substring(subType.resource.length + 1) else ""
                val hasRegion = subType?.hasRegion ?: true
                val hasAccount = subType?.hasAccount ?: true
                return Arn(args[2], if(hasRegion) args[3]else "", if(hasAccount) args[4] else "", resource, subType?.resource ?: "", subId)
            }

            private fun subTypeFrom(resource: String): AwsResourceType?{
                fun subtype(delimiter: Char): AwsResourceType?{
                    val potentialSubType = resource.substringBefore(delimiter)
                    return AwsResourceType.values().find { it.resource == potentialSubType }
                }
                return when{
                    resource.contains('/') -> subtype('/')
                    resource.contains(':') -> subtype(':')
                    else -> null
                }
            }
        }
    }

    data class Info(val title: String,val type: String,val otherProperties: Map<String, String> = emptyMap(), val size: Double = 1.0)

    data class Relationships(val resource: AwsResource, val relatedArns: List<Arn> = emptyList())

    interface Finder{

        fun findIn(account: String, regions: List<String>): List<Relationships>

        companion object {
            fun <T : Any> clientCall(method: () -> T) = collectAll({ null }, { method() })

            fun <T : Any> collectAll(nextToken: (T) -> String?, nextBatch: (String?) -> T): List<T> {
                fun genNext(lRes: T?): T? {
                    val token = lRes?.let { nextToken(it) }
                    return token?.let { safeNextBatch { nextBatch(it) } }
                }
                return generateSequence(safeNextBatch { nextBatch(null) }) { genNext(it) }
                        .takeWhile { true }.toList()
            }


            fun <T> safeNextBatch(nextBatch: () -> T): T? =
                    try {
                        nextBatch()
                    } catch (e: Exception) {
                        System.err.println(e)
                        if (e.message?.contains("Rate exceeded") == true) {
                            Thread.sleep(4000)
                            safeNextBatch(nextBatch)
                        } else null
                    }
        }
    }
}

