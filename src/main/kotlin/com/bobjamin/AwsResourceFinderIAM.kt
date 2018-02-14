package com.bobjamin

import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Volume
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest
import com.amazonaws.services.identitymanagement.model.ListRolesRequest
import com.amazonaws.services.identitymanagement.model.Role
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URLDecoder

class AwsResourceFinderIAM(
        private val iamClient: (region: String) -> AmazonIdentityManagement = AwsConfigurator.regionClientFrom(AmazonIdentityManagementClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships<*>> {
        return regions.flatMap{ iamResources(it) }
    }

    fun iamResources(region: String): List<AwsResource.Relationships<IAMInfo>>{
        val iamClient = iamClient(region)
        return AwsResource.Finder
                .collectAll( { it.marker } ){ iamClient.listRoles(ListRolesRequest().withMarker(it)) }
                .flatMap { it.roles }
                .map {
                    val roleArn = AwsResource.Arn.from(it.arn)
                    System.out.println(roleArn.arn())
                    AwsResource.Relationships(AwsResource(roleArn, IAMInfo(it.path)),relatedArnsFor(iamClient, it))
                }
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
                    .map { jacksonMapper.readValue<IamPolicy>(URLDecoder.decode(it.policyDocument, "UTF-8")) }
                    .flatMap { it.Statement.filter { it.Effect == "Allow" } }
                    .map {
                        ResourceActions(
                                if (it.Resource.isTextual) listOf(it.Resource.asText()) else it.Resource.map { it.asText() },
                                if (it.Action.isTextual) listOf(it.Action.asText()) else it.Action.map { it.asText() }
                        )
                    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class IamPolicy(val Version: String = "", val Statement: List<IamPolicyStatement>)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class IamPolicyStatement(val Action: JsonNode, val Resource: JsonNode, val Effect: String)
    data class ResourceActions(val resources: List<String>, val actions: List<String>)
    data class IAMInfo(val path: String): AwsResource.Info

    companion object {
        private val jacksonMapper = jacksonObjectMapper()
    }
}