import io.grpc.Server
import io.grpc.ServerBuilder

class EchoServer(
    private val port: Int,
    val server: Server) {

    constructor(port: Int) : this(serverBuilder = ServerBuilder.forPort(port), port = port)

    constructor(
        serverBuilder: ServerBuilder<*>,
        port: Int
    ) : this(
        port = port,
        server = serverBuilder.addService(EchoService()).build()
    )

    fun start() {
        server.start()
        println("Server started, listening on $port")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val port = 8980
            val server = EchoServer(port)
            server.start()
        }
    }
}