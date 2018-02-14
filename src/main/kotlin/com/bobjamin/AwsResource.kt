package com.bobjamin

class AwsResource<out T: AwsResource.Info>(val arn: Arn, val info: T){
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
                val args = arn.split(":")
                if(args.size < 6) throw IllegalArgumentException("$arn is not a valid arn")
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

    interface Info

    data class Relationships<out T: Info>(val resource: AwsResource<T>, val relatedArns: List<Arn> = emptyList())

    interface Finder{

        fun findIn(account: String, regions: List<String>): List<Relationships<*>>

        companion object {

            fun <T> clientCall(method: () -> T) = collectAll( { null } ){ method() }

            fun <T> collectAll(nextToken: (tokenized: T) -> String?, method: (nextToken: String?) -> T): List<T> {
                val results = mutableListOf<T>()
                    return try{
                        var result = method(null)
                        results.add(result)
                        var token = nextToken(result)
                        while(token != null){
                            result = method(token)
                            results.add(result)
                            token = nextToken(result)
                        }
                        results
                    }
                    catch(e: Exception){
                        System.err.println(e)
                        if(e.message?.contains("Rate exceeded") == true){
                            Thread.sleep(4000)
                            results + collectAll(nextToken, method)
                        }
                        else emptyList()
                    }
            }
        }
    }
}

