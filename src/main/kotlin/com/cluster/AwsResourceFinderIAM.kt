package com.cluster

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URLDecoder
import com.amazonaws.services.identitymanagement.model.*
import com.fasterxml.jackson.databind.module.SimpleModule



class AwsResourceFinderIAM(
        private val iamClient: (region: String) -> AmazonIdentityManagement = AwsConfigurator.regionClientFrom(AmazonIdentityManagementClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap{ iamResources(it) + instanceProfileResources(it) }
    }

    fun iamResources(region: String): List<AwsResource.Relationships>{
        val iamClient = iamClient(region)
        return AwsResource.Finder
                .collectAll( { it.marker } ){ iamClient.listRoles(ListRolesRequest().withMarker(it)) }
                .flatMap { it.roles }
                .map {
                    val roleArn = AwsResource.Arn.from(it.arn)
                    System.out.println(roleArn.arn())
                    AwsResource.Relationships(AwsResource(roleArn, AwsResource.Info(it.roleName, AwsResourceType.ROLE.type())),relatedArnsFor(iamClient, it))
                }
    }

    fun instanceProfileResources(region: String): List<AwsResource.Relationships>{
        val iamClient = iamClient(region)
        return AwsResource.Finder
                .collectAll({ it.marker }){ iamClient.listInstanceProfiles(ListInstanceProfilesRequest().withMarker(it))}
                .flatMap { it.instanceProfiles }
                .map {
                    val profileArn = AwsResource.Arn.from(it.arn)
                    System.out.println(profileArn.arn())
                    AwsResource.Relationships(AwsResource(profileArn, AwsResource.Info(it.instanceProfileName, AwsResourceType.INSTANCE_PROFILE.type())), relatedArnsFor(it))
                }
    }

    private fun relatedArnsFor(instanceProfile: InstanceProfile): List<AwsResource.Arn>{
        return instanceProfile.roles.map { AwsResource.Arn.from(it.arn) }
    }

    private fun relatedArnsFor(iamClient: AmazonIdentityManagement, role: Role): List<AwsResource.Arn>{
        return AwsResource.Finder
                .collectAll( { it.marker } ){ iamClient.listRolePolicies(ListRolePoliciesRequest().withMarker(it).withRoleName(role.roleName)) }
                .flatMap { it.policyNames.flatMap { resourceAccessListFrom(iamClient, role.roleName, it) } }
                .flatMap {
                    resourceActions ->
                    val serviceList = resourceActions.actions.groupBy { if(it == "*") it else it.substringBefore(':') }.keys
                    resourceActions.resources.flatMap {
                        when{
                            it == "*" -> serviceList.map { AwsResource.Arn(it, "", "", "*") }
                            it.startsWith("arn") ->  listOf(AwsResource.Arn.from(it))
                            else -> emptyList()
                        }
                    }
                }
    }

    private fun resourceAccessListFrom(iamClient: AmazonIdentityManagement, roleName: String, policyName: String): List<ResourceActions> =
            AwsResource.Finder
                    .clientCall { iamClient.getRolePolicy(GetRolePolicyRequest().withRoleName(roleName).withPolicyName(policyName)) }
                    .map { jacksonMapper.readValue<IamPolicy>(URLDecoder.decode(it?.policyDocument, "UTF-8")) }
                    .flatMap { it.Statement.filter { it.Effect == "Allow" } }
                    .map {
                        ResourceActions(
                                if (it.Resource.isTextual) listOf(it.Resource.asText()) else it.Resource.map { it.asText() },
                                if (it.Action.isTextual) listOf(it.Action.asText()) else it.Action.map { it.asText() }
                        )
                    }

    class IamPolicyDeserializer: JsonDeserializer<IamPolicy>(){
        override fun deserialize(parser: JsonParser, context: DeserializationContext): IamPolicy {
            val node = parser.codec.readTree<JsonNode>(parser)
            val version = if(node.has("Version")) node["Version"].textValue() else ""
            return node["Statement"].let {
                if(it.isObject){
                    IamPolicy(Version = version, Statement = listOf(parser.codec.treeToValue(it, IamPolicyStatement::class.java)))
                }
                else IamPolicy(Version = version, Statement = it.elements().asSequence().toList().map { parser.codec.treeToValue(it, IamPolicyStatement::class.java)})
            }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = IamPolicyDeserializer::class)
    data class IamPolicy(val Version: String = "", val Statement: List<IamPolicyStatement>)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class IamPolicyStatement(val Action: JsonNode, val Resource: JsonNode, val Effect: String)
    data class ResourceActions(val resources: List<String>, val actions: List<String>)


    companion object {
        private val jacksonMapper = jacksonObjectMapper()
        init {
            val module = SimpleModule()
            module.addDeserializer(IamPolicy::class.java, IamPolicyDeserializer())
            jacksonMapper.registerModule(module)
        }
    }
}