package com.bobjamin

class AwsResource(val arn: Arn){
    data class Arn(val service: String, val region: String, val account: String, val resource: String, val subType: String = "", val subId: String = "", val partition: String = "aws"){
        fun arn() = "arn:$partition:$service:$region:$account:$resource"
        companion object {
            fun from(arn: String): Arn{
                val args = arn.split(":")
                val resource = when(args.size){
                    6 -> args[5]
                    7 -> args[5] + ":" + args[6]
                    else -> throw IllegalArgumentException("$arn is not a valid arn")
                }
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
}

