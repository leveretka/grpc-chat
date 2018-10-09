//package ua.nedz.demo
//
//import io.grpc.Server
//import io.grpc.inprocess.InProcessChannelBuilder
//import io.grpc.inprocess.InProcessServerBuilder
//import io.grpc.testing.GrpcCleanupRule
//import io.grpc.util.MutableHandlerRegistry
//import org.junit.After
//import org.junit.Before
//import org.junit.Rule
//import org.junit.runner.RunWith
//import org.junit.runners.JUnit4
//
//@RunWith(JUnit4::class)
//class ChatClientTest {
//
//    @get:Rule
//    val grpcCleanup = GrpcCleanupRule()
//    private val serviceRegistry = MutableHandlerRegistry()
//
//    private lateinit var client: ChatClient
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        // Generate a unique in-process server name.
//        val serverName = InProcessServerBuilder.generateName()
//        // Use a mutable service registry for later registering the service impl for each test case.
//        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
//                .fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start())
//        client = ChatClient(InProcessChannelBuilder.forName(serverName).directExecutor())
//        client!!.setTestHelper(testHelper)
//    }
//
//    @After
//    @Throws(Exception::class)
//    fun tearDown() {
//        client!!.shutdown()
//    }
//
//}