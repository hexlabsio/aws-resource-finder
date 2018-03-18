package com.bobjamin

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.*

class AwsResourceFinderDynamoDB(
        private val dynamoDbClient: (region: String) -> AmazonDynamoDB = AwsConfigurator.regionClientFrom(AmazonDynamoDBClient.builder())
): AwsResource.Finder{
    override fun findIn(account: String, regions: List<String>): List<AwsResource.Relationships> {
        return regions.flatMap{ dynamoDbResources(it, account)  }
    }
    fun dynamoDbResources(region: String, account: String): List<AwsResource.Relationships>{
        val dynamoDbClient = dynamoDbClient(region)
        return AwsResource.Finder
                .collectAll( { it.lastEvaluatedTableName } ){ dynamoDbClient.listTables(ListTablesRequest().withExclusiveStartTableName(it)) }
                .flatMap { it.tableNames }
                .map {
                    val tableArn = AwsResource.Arn.from(AwsResourceType.TABLE, region, account, it)
                    System.out.println(tableArn.arn())
                    AwsResource.Relationships(
                            AwsResource(
                                    tableArn,
                                    AwsResource.Info(it, AwsResourceType.TABLE.type(),dynamoPropertiesFrom(it),sizeFrom(it))
                            )
                    )
                }
    }

    fun dynamoPropertiesFrom(tableName: String): Map<String, String>{
        return mapOf(
                "Table Name" to tableName
        )
    }
    fun sizeFrom(tableName: String): Double{
        return 1.0
    }

}