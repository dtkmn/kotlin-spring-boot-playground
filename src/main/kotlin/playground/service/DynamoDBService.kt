package playground.service

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.springframework.stereotype.Service

@Service
class DynamoDBService(

) {

    private val dynamoDB: DynamoDB
    private val processedRequestsTable: Table

    init {
        val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder
            .standard().build()
        dynamoDB = DynamoDB(client)
        processedRequestsTable = dynamoDB.getTable("processed_messages")
    }

    fun isAlreadyProcessed(messageId: String): Boolean {
        val item = processedRequestsTable.getItem("message_id", messageId)
        return item != null
    }

    fun markAsProcessed(messageId: String) {
//        val updateItemSpec = UpdateItemSpec()
//            .withPrimaryKey("message_Id", messageId)
//            .withUpdateExpression("set processed_at = :val")
//            .withValueMap(ValueMap().withLong(":val", System.currentTimeMillis()))
//            .withConditionExpression("attribute_not_exists(message_id)")
//        println("Trying to update item")
//        processedRequestsTable.updateItem(updateItemSpec)
        val item = Item().withPrimaryKey("message_id", messageId) // Ensure "message_id" matches the key name in your table
        processedRequestsTable.putItem(item)
    }


}