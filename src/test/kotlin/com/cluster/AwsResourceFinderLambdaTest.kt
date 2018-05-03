package com.cluster

import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.FunctionConfiguration
import com.amazonaws.services.lambda.model.ListFunctionsResult
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderLambdaTest{

    @Test
    fun `should build appropriate relationships for lambda resources`(){
        val lambdaClient = Mockito.mock(AWSLambda::class.java)
        val listFunctionsResult = ListFunctionsResult().withFunctions(listOf(
                FunctionConfiguration()
                        .withFunctionArn(AwsResource.Arn.from(AwsResourceType.FUNCTION, "region", "account", "function-1").arn())
                        .withMemorySize(1024)
                        .withTimeout(5)
                        .withRuntime("java")
                        .withKMSKeyArn(AwsResource.Arn.from(AwsResourceType.KEY,"region", "account", "key-1").arn())
                        .withRole("role-1"),
                FunctionConfiguration()
                        .withFunctionArn(AwsResource.Arn.from(AwsResourceType.FUNCTION, "region", "account", "function-2").arn())
                        .withMemorySize(2046)
                        .withTimeout(10)
                        .withRuntime("node")
                        .withRole("role-2")
        ))
        Mockito.`when`(lambdaClient.listFunctions(Mockito.any())).thenReturn(listFunctionsResult)
        val resources = AwsResourceFinderLambda({ lambdaClient }).findIn("a", listOf("us-east-1"))
        assertEquals(2, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:lambda:region:account:function:function-1", this.arn.arn())
//               with(this.info as AwsResourceFinderLambda.LambdaInfo){
//                   assertEquals(1024, this.memorySize)
//                   assertEquals(5, this.timeout)
//                   assertEquals("java", this.runtime)
//               }
            }
            assertEquals(2, this.relatedArns.size)
            assertEquals("arn:aws:iam:us-east-1:a:role/role-1", this.relatedArns[0].arn())
            assertEquals("arn:aws:kms:region:account:key/key-1", this.relatedArns[1].arn())
        }
        with(resources[1]){
            with(this.resource){
                assertEquals("arn:aws:lambda:region:account:function:function-2", this.arn.arn())
//                with(this.info as AwsResourceFinderLambda.LambdaInfo){
//                    assertEquals(2046, this.memorySize)
//                    assertEquals(10, this.timeout)
//                    assertEquals("node", this.runtime)
//                }
            }
            assertEquals(1, this.relatedArns.size)
            assertEquals("arn:aws:iam:us-east-1:a:role/role-2", this.relatedArns[0].arn())
        }
    }
}