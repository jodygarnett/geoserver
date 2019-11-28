.. _rest_services_overview:

Overview
========

GeoServer uses spring model view controller for REST related functionality.
REST API documentation is captured in OpenAPI 

References:

* `REST <https://docs.geoserver.org/latest/en/user/rest/index.html#rest>`__ (GeoServer User Manual)
* `REST API Refresh <https://github.com/geoserver/geoserver/wiki/REST-API-Refresh>`__ (GeoServer Wiki)
* `Spring Web MVC <https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc>`__ (Spring) 
* `What is OpenAPI <https://swagger.io/docs/specification/about/>`__ (Swagger)

.. note::
   
   GeoSever 2.1 and earlier used the `Restlet <http://www.restlet.org/>`__ framework.

Controllers
-----------

Controllers are the main REST request handlers, using annotations to document the requests being handled.

.. code-block:: java

   @GetMapping(
       path = "/styles/{styleName}",
       produces = {
           MediaType.APPLICATION_JSON_VALUE, 
           MediaType.APPLICATION_XML_VALUE, 
           MediaType.TEXT_HTML_VALUE})
   protected RestWrapper<StyleInfo> getStyle(
           @PathVariable String styleName) {
       return wrapObject(getStyleInternal(styleName, null), StyleInfo.class);
   }


Message Converters
------------------

Message converters are responsible for serialization and deserialization of response objects.

.. code-block:: java

   @GetMapping(
       path = "/styles/{styleName}",
       produces = {
           MediaType.APPLICATION_JSON_VALUE, 
           MediaType.APPLICATION_XML_VALUE, 
           MediaType.TEXT_HTML_VALUE})
   protected RestWrapper<StyleInfo> getStyle(
           @PathVariable String styleName) {
       return wrapObject(getStyleInternal(styleName, null), StyleInfo.class);
   }

GeoServer uses three approaches:

* XStreamXmlConverter for XML output
* XStreamJSONConverter for JSON output
* FreemarkerHtmlConverter for html output

Wrappers
--------

Wrappers are used to provide additional configuration alongside the objects returned by the controller. 

The RestController class provides `wrapObject` and `wrapList` utility methods.

Reference API Documentation
---------------------------