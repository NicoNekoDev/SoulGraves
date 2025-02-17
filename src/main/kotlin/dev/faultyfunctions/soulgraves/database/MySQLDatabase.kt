package dev.faultyfunctions.soulgraves.database

import com.saicone.rtag.item.ItemTagStream
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.managers.DatabaseManager
import dev.faultyfunctions.soulgraves.utils.Soul
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*
import kotlin.collections.ArrayList

class MySQLDatabase private constructor(){

    // DATABASE VALUES
    private var dataSource: HikariDataSource
    private var jdbcUrl: String
    private var jdbcDriver: String
    private var username: String
    private var password: String
    private val databaseName: String = "soul_grave"

    init {
        val config = DatabaseManager.databaseConfig

        jdbcUrl = config.getString("MySQL.jdbc-url")!!
        jdbcDriver = config.getString("MySQL.jdbc-class")!!
        username = config.getString("MySQL.properties.user")!!
        password = config.getString("MySQL.properties.password")!!

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = jdbcUrl
        hikariConfig.driverClassName = jdbcDriver
        hikariConfig.username = username
        hikariConfig.password = password
        dataSource = HikariDataSource(hikariConfig)
    }

    companion object {
        val instance: MySQLDatabase by lazy { MySQLDatabase().apply {
            createTable()
            SoulGraves.plugin.logger.info("Connected to MySQL Database!")
        } }
    }

    // Create Table
    private fun createTable() {
        val connection = dataSource.connection
        val sql = "CREATE TABLE IF NOT EXISTS $databaseName (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "ownerUUID VARCHAR(255), " +
                "markerUUID VARCHAR(255), " +
                "serverName VARCHAR(255), " +
                "world VARCHAR(255), " +
                "x INT, " +
                "y INT, " +
                "z INT, " +
                "inventory TEXT, " +
                "xp INT, " +
                "expireTime BIGINT)"
        val statement = connection.prepareStatement(sql)
        try {
            statement.executeUpdate(sql)
            println("Table '$databaseName' created successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error while creating table: ${e.message}")
        } finally {
            statement.close()
            connection.close()
        }
    }


    // Save Soul to Database
    fun saveSoul(soul: Soul, serverName: String) {
        var now = System.currentTimeMillis()
        val connection = dataSource.connection
        val sql = "INSERT INTO $databaseName (ownerUUID, markerUUID, serverName, world, x, y, z, inventory, xp, expireTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        val statement = connection.prepareStatement(sql)

        statement.setString(1, soul.ownerUUID.toString())
        statement.setString(2, soul.markerUUID.toString())
        statement.setString(3, serverName)
        statement.setString(4, soul.location.world?.name)
        statement.setInt(5, soul.location.x.toInt())
        statement.setInt(6, soul.location.y.toInt())
        statement.setInt(7, soul.location.z.toInt())
        statement.setString(8, ItemTagStream.INSTANCE.listToBase64(soul.inventory))
        statement.setInt(9, soul.xp)
        statement.setLong(10, (ConfigManager.timeStable + ConfigManager.timeUnstable) * 1000L + now)

        statement.executeUpdate()
    }


    // Read Souls From Database
    fun readPlayerSouls(uuid: String) : MutableList<Soul> {
        val souls = ArrayList<Soul>()

        val connection = dataSource.connection
        val sql = "SELECT * FROM $databaseName WHERE uuid = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, uuid)

        try {
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val soul = Soul(
                    ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                    markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                    inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                    xp = resultSet.getInt("xp"),
                    location = Location(Bukkit.getWorld(resultSet.getString("world")), resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z")),
                    timeLeft = ((resultSet.getLong("expireTime") - System.currentTimeMillis()) / 1000).toInt()
                )
                souls.add(soul)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            statement.close()
            connection.close()
        }
        return souls
    }


    // Read Souls in Current Server
    fun readServerSouls(serverName: String) : MutableList<Soul> {
        val souls = ArrayList<Soul>()

        val connection = dataSource.connection
        val sql = "SELECT * FROM $databaseName WHERE serverName = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, serverName)

        try {
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val soul = Soul(
                    ownerUUID = UUID.fromString(resultSet.getString("ownerUUID")),
                    markerUUID = UUID.fromString(resultSet.getString("markerUUID")),
                    inventory = ItemTagStream.INSTANCE.listFromBase64(resultSet.getString("inventory")),
                    xp = resultSet.getInt("xp"),
                    location = Location(Bukkit.getWorld(resultSet.getString("world")), resultSet.getDouble("x"), resultSet.getDouble("y"), resultSet.getDouble("z")),
                    timeLeft = ((resultSet.getLong("expireTime") - System.currentTimeMillis()) / 1000).toInt()
                )
                souls.add(soul)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            statement.close()
            connection.close()
        }
        return souls
    }


    // Delete Soul From Database
    fun deleteSoul(soul: Soul) {
        val uuid = soul.markerUUID.toString()
        val connection = dataSource.connection
        val sql = "DELETE FROM $databaseName WHERE markerUUID = ?"
        val statement = connection.prepareStatement(sql)
        statement.setString(1, uuid)
        statement.executeUpdate()
    }

}