package model

import ca.polymtl.log4900.api._
import lookup._
import eval.Insight

import com.twitter.util.Future
import com.twitter.finagle._
import thrift.ThriftServerFramedCodec
import thrift.ThriftClientFramedCodec
import builder.ClientBuilder
import builder.ServerBuilder
import builder.Server

import org.apache.thrift.protocol.TBinaryProtocol
import java.net.InetSocketAddress

object LookupService {
	var server = Option.empty[Server]
	def start() {
		val protocol = new TBinaryProtocol.Factory()
		val serverService = new Lookup.FinagledService(new LookupImpl, protocol)
		val address = new InetSocketAddress(Config.host, Config.port)

		val s = ServerBuilder()
			.codec(ThriftServerFramedCodec())
			.name("lookup-service")
			.bindTo(address)
			.build(serverService)

		server = Some(s)
	}

	def stop(){
		server.map(_.close())
	}
}

object Registry {
	def getEval: Option[Insight.FinagledClient] = evals.headOption
	def add(eval: Insight.FinagledClient): Unit = {
		evals = eval :: evals
	}
	private var evals = List.empty[Insight.FinagledClient]	
}

class LookupImpl extends Lookup.FutureIface {
	def register(info: ServiceInfo): Future[Unit] = {
		println(s"registering service $info")
		val service = ClientBuilder()
			.hosts(s"${info.host}:${info.port}")
		    .codec(ThriftClientFramedCodec())
		    .hostConnectionLimit(1)
		    .build()

		info.name match {
			case "eval" => Registry.add(new Insight.FinagledClient(service))
		}
		Future.value(())
	}
}