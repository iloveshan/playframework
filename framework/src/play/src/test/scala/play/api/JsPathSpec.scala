package play.api.libs.json

import org.specs2.mutable._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.json.Generic._
import play.api.libs.json.JsResultHelpers._

import scala.util.control.Exception._
import java.text.ParseException

object JsPathSpec extends Specification {

  "JsPath" should {
    "retrieve simple path" in {
      val obj = Json.obj( "key1" -> Json.obj("key11" -> "value11"), "key2" -> "value2")

      (JsPath \ "key1" \ "key11")(obj) must equalTo(Seq(JsString("value11")))
    }

    "retrieve 1-level recursive path" in {
      val obj = Json.obj( 
        "key1" -> Json.obj(
          "key11" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))
        ), 
        "key2" -> Json.obj(
          "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
        ),
        "key3" -> "blabla"
      )

      (JsPath \\ "tags")(obj) must equalTo(Seq(Json.arr("alpha1", "beta1", "gamma1"), Json.arr("alpha2", "beta2", "gamma2"))) 
    }

    "retrieve 2-level recursive path" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj(
            "key11" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          ),
          "key3" -> "blabla"
        ),
        "level2" -> 5
      )

      (JsPath \ "level1" \\ "tags")(obj) must equalTo(Seq(Json.arr("alpha1", "beta1", "gamma1"), Json.arr("alpha2", "beta2", "gamma2"))) 
    }

    "retrieve 2-level middle recursive path" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj(
            "key11" -> Json.obj("tags" -> Json.obj("sub" -> "alpha1"))
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.obj("sub" -> "beta2"))
          )
        ),
        "level2" -> 5
      )

      (JsPath \\ "tags" \ "sub")(obj) must equalTo(Seq(JsString("alpha1"), JsString("beta2"))) 
    }

    "retrieve simple indexed path" in {
      val obj = Json.obj( 
        "level1" -> Json.arr( 5, "alpha", true )
      )

      (JsPath \ "level1")(2)(obj) must equalTo(Seq(JsBoolean(true)))
    }

    "retrieve 2-level recursive indexed path" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj(
            "key11" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          )
        ),
        "level2" -> 5
      )

      (JsPath \ "level1" \\ "tags")(1)(obj) must equalTo(Seq(JsString("beta1"), JsString("beta2"))) 

    }

    "retrieve 2-level recursive indexed path" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.arr(
            "key11",
            Json.obj("key111" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))),
            "key12"
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          )
        ),
        "level2" -> 5
      )

      ((JsPath \ "level1" \ "key1") (1) \\ "tags")(obj) must equalTo(Seq(Json.arr("alpha1", "beta1", "gamma1"))) 

    }

    "retrieve with symbol keys" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.arr(
            "key11",
            Json.obj("key111" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))),
            "key12"
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          )
        ),
        "level2" -> 5
      )

      ((JsPath \ 'level1 \ 'key1)(1)\\'tags)(obj) must equalTo(Seq(Json.arr("alpha1", "beta1", "gamma1"))) 

    }

    "set 1-level field in simple jsobject" in {
      val obj = Json.obj("key" -> "value")

      (JsPath \ 'key ).set(obj, JsString("newvalue")) must equalTo(Json.obj("key" -> "newvalue"))
    }
  
    "set 2-level field in simple jsobject" in {
      val obj = Json.obj("key" -> Json.obj("subkey" -> "value"))
      (JsPath \ 'key \ 'subkey ).set(obj, JsString("newvalue")) must equalTo(Json.obj("key" -> Json.obj("subkey" -> "newvalue")))
    }

    "set 2-level field in complex jsobject" in {
      val obj = Json.obj("key1" -> "blabla", "key" -> Json.obj("subkey" -> "value"), "key2" -> 567)

      (JsPath \ 'key \ 'subkey ).set(obj, JsNumber(5)) must equalTo(Json.obj("key1" -> "blabla", "key" -> Json.obj("subkey" -> 5), "key2" -> 567))
    }

    "set 1-level field in simple jsarray" in {
      val obj = Json.arr("alpha", "beta", "gamma")

      val p = JsPath(1)
      p.set(obj, JsString("newvalue")) must equalTo(Json.arr("alpha", "newvalue", "gamma"))
    }

    "set 1-level field in complex jsarray" in {
      val obj = Json.obj("key" -> Json.arr("alpha", "beta", "gamma"))

      val p = (JsPath \ 'key)(1)
      p.set(obj, JsString("newvalue")) must equalTo(Json.obj("key" -> Json.arr("alpha", "newvalue", "gamma")))
    }

    "set on JsPath Root" in {
      val obj = JsNumber(5)

      JsPath.set(obj, JsString("newvalue")) must equalTo(JsString("newvalue"))
    }

    "set on JsPath recursive same level" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj(
            "key11" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          ),
          "key3" -> "blabla"
        ),
        "level2" -> 5
      )

      (JsPath \\ "tags").set(obj, JsString("newvalue")) must equalTo(Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj(
            "key11" -> Json.obj("tags" -> JsString("newvalue"))
          ), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> JsString("newvalue"))
          ),
          "key3" -> "blabla"
        ),
        "level2" -> 5
      ))
    }

    "set on JsPath recursive different level" in {
      val obj = Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1")), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))
          ),
          "key3" -> "blabla"
        ),
        "level2" -> 5
      )

      (JsPath \\ "tags").set(obj, JsString("newvalue")) must equalTo(Json.obj( 
        "level1" -> Json.obj(
          "key1" -> Json.obj("tags" -> JsString("newvalue")), 
          "key2" -> Json.obj(
            "key21" -> Json.obj("tags" -> JsString("newvalue"))
          ),
          "key3" -> "blabla"
        ),
        "level2" -> 5
      ))
    }

    "set on JsPath recursive array same level" in {
      val obj = Json.obj( 
        "level1" -> Json.arr(
          Json.obj("key1" -> Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1"))), 
          Json.obj("key2" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      )

      (JsPath \\ "tags").set(obj, JsString("newvalue")) must equalTo(Json.obj( 
        "level1" -> Json.arr(
          Json.obj("key1" -> Json.obj("tags" -> JsString("newvalue"))), 
          Json.obj("key2" -> Json.obj("tags" -> JsString("newvalue"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      ))
    }

    "set on JsPath recursive array different level" in {
      val obj = Json.obj( 
        "level1" -> Json.arr(
          Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1")), 
          Json.obj("key2" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      )

      (JsPath \\ "tags").set(obj, JsString("newvalue")) must equalTo(Json.obj( 
        "level1" -> Json.arr(
          Json.obj("tags" -> JsString("newvalue")), 
          Json.obj("key2" -> Json.obj("tags" -> JsString("newvalue"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      ))
    }

    "set on JsPath recursive array mix" in {
      val obj = Json.obj( 
        "level1" -> Json.arr(
          Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1")), 
          Json.obj("key2" -> Json.obj("tags" -> Json.arr("alpha2", "beta2", "gamma2"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      )
      (((JsPath \ 'level1)(1) \\ 'tags)(1)).set(obj, JsNumber(5)) must equalTo(Json.obj( 
        "level1" -> Json.arr(
          Json.obj("tags" -> Json.arr("alpha1", "beta1", "gamma1")), 
          Json.obj("key2" -> Json.obj("tags" -> Json.arr("alpha2", 5, "gamma2"))),
          Json.obj("key3" -> "blabla")
        ),
        "level2" -> 5
      ))
    }    

  
  }

}