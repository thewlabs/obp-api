package code.dynamicResourceDoc

import code.api.cache.Caching
import code.api.util.APIUtil
import com.tesobe.CacheKeyFromArguments
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mapper._
import net.liftweb.util.Helpers.tryo
import net.liftweb.util.Props

import java.util.UUID.randomUUID
import scala.concurrent.duration.DurationInt

object MappedDynamicResourceDocProvider extends DynamicResourceDocProvider {

  private val getDynamicResourceDocTTL : Int = {
    if(Props.testMode) 0
    else if(Props.devMode) 10
    else APIUtil.getPropsValue(s"dynamicResourceDoc.cache.ttl.seconds", "40").toInt
  }

  override def getById(dynamicResourceDocId: String): Box[JsonDynamicResourceDoc] = DynamicResourceDoc
    .find(By(DynamicResourceDoc.DynamicResourceDocId, dynamicResourceDocId))
    .map(DynamicResourceDoc.getJsonDynamicResourceDoc)

  override def getByVerbAndUrl(requestVerb: String, requestUrl: String): Box[JsonDynamicResourceDoc] = DynamicResourceDoc
    .find(By(DynamicResourceDoc.RequestVerb, requestVerb), By(DynamicResourceDoc.RequestUrl, requestUrl))
    .map(DynamicResourceDoc.getJsonDynamicResourceDoc)
  
  override def getAllAndConvert[T: Manifest](transform: JsonDynamicResourceDoc => T): List[T] = {
    var cacheKey = (randomUUID().toString, randomUUID().toString, randomUUID().toString)
    CacheKeyFromArguments.buildCacheKey {
      Caching.memoizeSyncWithProvider (Some(cacheKey.toString())) (getDynamicResourceDocTTL second) {
        DynamicResourceDoc.findAll()
          .map(doc => transform(DynamicResourceDoc.getJsonDynamicResourceDoc(doc)))
      }}
  }

  override def create(entity: JsonDynamicResourceDoc): Box[JsonDynamicResourceDoc]=
    tryo {
      DynamicResourceDoc.create
      .DynamicResourceDocId(APIUtil.generateUUID())
      .PartialFunction(entity.partialFunction)
      .PartialFunctionName(entity.partialFunctionName)
      .RequestVerb(entity.requestVerb)
      .RequestUrl(entity.requestUrl)
      .Summary(entity.summary)
      .Description(entity.description)
      .ExampleRequestBody(entity.exampleRequestBody)
      .SuccessResponseBody(entity.successResponseBody)
      .ErrorResponseBodies(entity.errorResponseBodies)
      .Tags(entity.tags)
      .Roles(entity.roles)
      .MethodBody(entity.methodBody)
      .saveMe()
    }.map(DynamicResourceDoc.getJsonDynamicResourceDoc)


  override def update(entity: JsonDynamicResourceDoc): Box[JsonDynamicResourceDoc] = {
    DynamicResourceDoc.find(By(DynamicResourceDoc.DynamicResourceDocId, entity.dynamicResourceDocId.getOrElse(""))) match {
      case Full(v) =>
        tryo {
          v.PartialFunction(entity.partialFunction)
            .PartialFunctionName(entity.partialFunctionName)
            .RequestVerb(entity.requestVerb)
            .RequestUrl(entity.requestUrl)
            .Summary(entity.summary)
            .Description(entity.description)
            .ExampleRequestBody(entity.exampleRequestBody)
            .SuccessResponseBody(entity.successResponseBody)
            .ErrorResponseBodies(entity.errorResponseBodies)
            .Tags(entity.tags)
            .Roles(entity.roles)
            .MethodBody(entity.methodBody)
            .saveMe()
        }.map(DynamicResourceDoc.getJsonDynamicResourceDoc)
      case _ => Empty
    }
  }

  override def deleteById(id: String): Box[Boolean] = tryo {
    DynamicResourceDoc.bulkDelete_!!(By(DynamicResourceDoc.DynamicResourceDocId, id))
  }
}


