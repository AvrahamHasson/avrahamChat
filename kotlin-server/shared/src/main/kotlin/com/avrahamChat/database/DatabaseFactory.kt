import com.mongodb.MongoClientSettings
import com.mongodb.ConnectionString
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

object DatabaseFactory {
    private val pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
    )

    private val clientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(com.avrahamChat.config.AppConfig.MONGO_URI))
        .codecRegistry(pojoCodecRegistry)
        .build()

    private val mongoClient = MongoClient.create(clientSettings)

    fun getDatabase(dbName: String): MongoDatabase = mongoClient.getDatabase(dbName)
}