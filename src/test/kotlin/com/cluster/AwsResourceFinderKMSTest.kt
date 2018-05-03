package com.cluster

import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.KeyListEntry
import com.amazonaws.services.kms.model.ListKeysResult
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.Mockito

class AwsResourceFinderKMSTest {

    @Test
    fun `should build appropriate relationships for kms resources`(){
        val kmsClient = Mockito.mock(AWSKMS::class.java)
        val listKeysResult = ListKeysResult().withKeys(listOf(
                KeyListEntry().withKeyArn(AwsResource.Arn.from(AwsResourceType.KEY,"r", "a", "key-id").arn())
        ))
        Mockito.`when`(kmsClient.listKeys(Mockito.any())).thenReturn(listKeysResult)
        val resources = AwsResourceFinderKMS({ kmsClient }).findIn("a", listOf("us-east-1"))
        assertEquals(1, resources.size)
        with(resources[0]){
            with(this.resource){
                assertEquals("arn:aws:kms:r:a:key/key-id", this.arn.arn())
            }
        }
    }
}