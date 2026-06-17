[//]: # (Copyright Bosch.IO GmbH 2020)
[//]: # (This program and the accompanying materials are made)
[//]: # (available under the terms of the Eclipse Public License 2.0)
[//]: # (which is available at https://www.eclipse.org/legal/epl-2.0/)
[//]: # (SPDX-License-Identifier: EPL-2.0)
# The HTTP support library

During an sw360 client execution it is typically necessary to communicate with 
one or multiple HTTP servers. Examples include the download of artifacts from a
repository or the interaction with an SW360 instance. Custom workflow step
implementations may also contact specific servers to obtain additional 
information about the software components used by a project.

While doing HTTP requests is not rocket science, it is nevertheless 
inefficient and error prone if every component that requires this
communication comes with its own implementation. There are a number of aspects
to be taken into account:

* HTTP clients are often complex objects making use of expensive resources like
  thread pools and connection pools. Having multiple clients in a single 
  run is therefore a waste of resources, especially as HTTP clients are
  typically thread-safe and can be shared between multiple components.
* Configuring an HTTP client is not trivial; basic settings from the environment
  configuration, such as Proxy settings or timeouts, should be taken into
  account to make sure that all HTTP connections behave in a uniform way.
* Resource and error handling when sending HTTP requests can become tricky. It
  must be ensured that all resources are correctly released even in case of
  failure responses and exceptions thrown during processing.
* If many requests are to be sent - e.g. when downloading a number of 
  artifacts -, performance can be improved by making use of threading and
  parallelism. This complicates the programming model significantly though.
 
To deal with these issues, sw360 provides a simple to use HTTP client
library that can be used by all components taking part in an sw360 client execution.
The library manages a central, fully configured HTTP client that offers the
following features:

* HTTP requests can be defined in a declarative way making use of a domain
  specific language (DSL).
* The API is lambda-friendly; for instance the processing of a response can be
  done by a lambda expression.
* Resource management is handled by the library; client code does not have to
  close responses or entities, or use means like try-with-resources.
* The programming model is reactive using _CompletableFutures_ to allow for
  the efficient processing of many requests. There is, however, an easy means
  to support blocking calls as well.
* JSON processing is directly supported, as this is a standard protocol used by
  many APIs.
* Support for multipart requests that are used for instance for file upload
  operations.

The library is implemented by the _http-support_ sub module of the clients
module. It has comprehensive Javadoc which can be consulted for detail
questions. This document provides a short user's guide.

#[[##]]# Obtaining the HTTP client

The _HttpClientFactory_ interface and its default implementation 
_HttpClientFactoryImpl_ are responsible for creating and setting
up new instances of _HttpClient_. The factory interface defines a
_newHttpClient()_ method, which expects an _HttpClientConfig_ object as its
single argument.

_HttpClientConfig_ allows setting a number of properties that impact the 
behavior of the client instance. An instance with default settings is available
via the static _basicConfig()_ method. Using this as a starting point, other
properties can be set using methods starting with the prefix `with`. Such 
methods return a modified copy of the current configuration; _HttpClientConfig_
itself is immutable, and thus instances can be safely shared between different
components.

The properties that can be changed in a client configuration are the following:
* A JSON object mapper: This object is used to serialize Java objects to 
  generate the payload for JSON requests.
* Proxy settings: Requests can be routed through an HTTP proxy. For this 
  purpose, the _ProxySettings_ class exists that collects the properties of the
  proxy server. There are multiple options:
  * Users can configure a specific proxy to be used for all requests with the
    `ProxySettings.useProxy()` method.
  * Usage of a proxy can be disabled completely by using
    `ProxySettings.noProxy()`.
  * The selection of a proxy can be delegated to the default
    [ProxySelector](https://docs.oracle.com/javase/8/docs/api/java/net/ProxySelector.html)
    installed in the JVM by using `ProxySettings.defaultProxySelector()`. This
    option is the default if the client configuration does not contain any
    explicit settings.
* SSL Certificate Verification: Dynamically, ssl certificate verification can be
  disabled by setting the system property _client.access.unverified_ to true.

Below is a code fragment that shows the construction of an _HttpClientConfig_ 
instance and the creation of a new HTTP client based on this configuration. For
this example we use a very verbose configuration that sets all the properties
supported:

```
ProxySettings proxy = ProxySettings.useProxy("my.proxy.host", 8080);
ObjectMapper customMapper = new ObjectMapper()
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
HttpClientConfig clientConfig = HttpClientConfig.basicConfig()
  .withProxySettings(proxy)
  .withObjectMapper(customMapper);

HttpClientFactory clientFactory = new HttpClientFactoryImpl();
HttpClient httpClient = clientFactory.newHttpClient(clientConfig);
```

#[[##]]# The HttpClient interface

The central interface for the execution of HTTP requests is _HttpClient_. It is
a very lean interface with only a single _execute()_ method:

```
    <T> CompletableFuture<T> execute(Consumer<? super RequestBuilder> producer,
                                     ResponseProcessor<? extends T> processor);
```

The method references the following components:

* A _Consumer_ of the interface _RequestBuilder_ is passed in as the first
  parameter. This object defines the request to be executed. We will see a bit
  later how a request definition looks like with this approach.
* The second parameter is a _ResponseProcessor_. This is a generic functional
  interface that is passed a _Response_ object (representing the response 
  returned from the server) and produces a result object of a specific type.
  Here the logic is encapsulated to evaluate the data sent by the server.
* The result of _execute()_ is a _CompletableFuture_ of the result type 
  produced by the _ResponseProcessor_. This shows that requests are made in an
  asynchronous fashion. The resulting future contains either the result
  generated by the _ResponseProcessor_ or is failed exceptionally, in case of a
  connection error or if the _ResponseProcessor_ has thrown an exception.
  Using _CompletableFuture_ as return value has advantages over conventional,
  callback-based approaches; especially if the results of multiple requests are
  to be combined or if some requests depend on the results of others, the 
  ability to chain and compose futures comes in very handy.

#[[###]]# Defining requests

The _HttpClient_ interface expects that a request to be executed is defined by
an object implementing the standard _Consumer_ interface for objects of type
_RequestBuilder_. _RequestBuilder_ is a fluent builder interface that allows
setting various properties of an HTTP request:
* the URL of the server to be called
* the HTTP method, such as GET or POST
* HTTP headers
* the body of the request

The only mandatory property which must be set is the request URL. In this case,
a GET request to this URL (without any headers) is executed. The following
fragment shows an example:

```
httpClient.execute(builder ->
    builder.uri("https://www.google.com"), ...
```

Notice that it is not necessary to invoke some kind of _build()_ method on the
passed in builder object; only the desired request properties need to be
initialized. For setting headers, the builder's _header()_ method can be used:

```
httpClient.execute(builder ->
    builder.uri("https://my.server.com")
        .header("Authorization", "Basic ..."), ...
```

For requests of type POST or PUT, a request body or entity is typically
required. In addition, the concrete HTTP method to be used must be defined. The
body is constructed via a dedicated builder interface named
_RequestBodyBuilder_. Here the same approach is followed as for defining the
request as a whole: in order to set the body, the _body()_ method of
_RequestBuilder_ must be called passing in a _Consumer_ of type
_RequestBodyBuilder_. Using the body builder, the content of the request body
can be set in various flavours; for instance as a plain string:

```
httpClient.execute(builder ->
    builder.uri("https://my.endpoint.org")
        .method(RequestBuilder.Method.POST)
        .body(body -> body.string("This is the request payload.", 
             HttpConstants.CONTENT_TEXT_PLAIN)), ...
```

The second parameter of the _string()_ method is the Mime type of the content.
(Note the use of the _HttpConstants_ class which offers a bunch of constants
related to HTTP headers, content types, status codes, etc.) The HTTP method of
the request is specified using an enumeration class.

In addition to plain strings, the body of a request can be defined in other
formats as well. As many HTTP-based APIs use JSON payloads, there is a special
support for this protocol in form of the _json()_ method of
_RequestBodyBuilder_. The method is passed the object representing the payload.
It is serialized to JSON using an internal JSON object mapper, and a content
header for the MIME type _application/json_ is automatically added. The
example fragment below shows how a PUT request with JSON data can be
constructed:

```
Car myCar = new Car(...);  // object representation of payload
httpClient.execute(builder ->
    builder.uri("https://my.endpoint.org/cars/42")
        .method(RequestBuilder.Method.PUT)
        .body(body -> body.json(myCar)), ...
```

The use case to upload a file is handled by the _file()_ method of
_RequestBodyBuilder_. The method expects the path of the file to be uploaded
and a string constant with the MIME type of the file:

```
Path file = ...;  // file to be uploaded
httpClient.execute(builder ->
    builder.uri("https://my.endpoint.org/uploads")
        .method(RequestBuilder.Method.PATCH)
        .body(body -> body.file(file, HttpConstants.CONTENT_OCTET_STREAM)), ...
```

File uploads often require additional information to be transferred to the 
server, e.g. a JSON data object representing the entity to be associated with
the data contained in the file. This can be handled using _multipart requests_.
A multipart request consists of multiple body parts that can have different
content types. To create such a request, on the _RequestBuilder_ interface the
_multiPart()_ method has to be called for each part to be added. The method
expects a name for the new part and a consumer for a _RequestBodyBuilder_. The
latter is used to set the actual content of the part. For this all the 
facilities shown in the previous examples are available. The code fragment
below demonstrates the construction of a multipart request with three parts, a
JSON part, a plain text part, and a file upload part:

```
httpClient.execute(builder ->
    builder.uri(endpointUri())
        .method(RequestBuilder.Method.POST)
        .multiPart("json", body -> body.json(jsonObj))
        .multiPart("plain", body -> body.string(CONTENT, CONTENT_TEXT_PLAIN))
        .multiPart("file", body -> body.file(testFilePath, CONTENT_OCTET_STREAM)), ...
```

#[[###]]# Processing responses

The _execute()_ method of _HttpClient_ generates the request defined via the
builder and sends it to the target server. If sending fails or no response is
received - e.g. because of network or connection problems -, the
_CompletableFuture_ returned by _execute()_ fails with a corresponding 
exception. Otherwise, from the response of the server a _Response_ object is
created, and this object is passed to the _ResponseProcessor_ provided as
second parameter to _execute()_. The processor can use this information to
create a result object, which becomes the result of the _CompletableFuture_.
Alternatively, the processor can throw an _IOException_ (e.g. if the response
indicates an error); in this case, the future completes with this exception.

The _Response_ interface provides access to the well-known properties of an
HTTP response:
* The HTTP status code can be queried using the _statusCode()_ method.
* Headers set by the server are available via the methods _headerNames()_ and
  _header()_.
* The request body can be retrieved as a (potentially empty) stream via the 
  _bodyStream()_ method.
  
It is up to a concrete _ResponseProcessor_ implementation how this information
is evaluated to construct a result object. Normally, the response has to be
validated (e.g. by checking the status code or other properties), then the body
is read, and based on this a result can be generated (maybe using JSON
de-serialization). As some of these steps need to be repeated for each request,
there are some helper functions in the _HttpUtils_ class to simplify things a 
bit; they will be introduced shortly. First we give a full example of a request
execution with a response processor that just returns the response body as a 
string:

```
CompletableFuture<String> futResponse =
        httpClient.execute(builder -> builder.uri(endpointUri()), resonponse -> {
            if (!response.isSuccess()) {
                return "error";
            }
            else {
                return IOUtils.toString(response.bodyStream(), StandardCharsets.UTF_8);
            }
        }
);
```

This example uses a _ResponseProcessor_ implementation that first checks the
HTTP status and - if successful - reads the content of the response body into a
string. Note that it is not necessary to close the input stream; this is 
handled by the framework automatically.

#[[##]]# Utility functions

The _HttpUtils_ class contains a number of helper functions to simplify typical
tasks related to sending and processing of HTTP requests. In many cases, these
simplify the implementation of custom response processors or even make it
unnecessary. This section gives an overview over this functionality.

#[[###]]# Simplified GET requests

If only a simple GET request is to be executed without headers, it is not
necessary to write a lambda expression that interacts with a _RequestBuilder_.
The _HttpUtils.get()_ method provides a corresponding implementation. Its 
result can be passed as first argument to _HttpClient_'s _execute()_ method:

```
httpClient.execute(HttpUtils.get("https://my-server.com/endpoint"), ...
```

#[[###]]# Checking the response status

When processing the response of a request it is often necessary to check the
HTTP status code; only if the status indicates a successful response, further
evaluation of the response body should be done. _HttpUtils_ provides a number
of functions that handle such checks. The idea behind these functions is that
they expect a _ResponseProcessor_ as argument and return another one as result. 
They implement specific checking logic and only delegate to the passed in 
processor if the checks are successful. Thus, a custom _ResponseProcessor_ can
assume a success status and focus on the actual processing logic. Instead of
the original processor, the one returned by the functions needs to be passed to
the HTTP client.

The functions are named _checkResponse()_. There are multiple overloaded
variants; they support the following arguments:

* the original _ResponseProcessor_; as has already been explained, this
  processor is decorated with the checking logic.
* a predicate defining the checking logic. This is a function that expects a
  _Response_ object and returns a *boolean* result indicating whether the
  response is successful.
* a tag to assign a name to the request. This string appears in exceptions
  reporting failed requests. This is especially useful if requests are chained
  via _CompletableFuture_ mechanisms; it is then not always obvious, which 
  request has caused the failure.
  
There are variants of _checkResponse()_ that do not expect a predicate, but use
a default one which just checks whether the HTTP status is in the successful
range. This may be sufficient for many use cases. If more control is needed, 
the _hasStatus()_ function is a candidate: it is passed a status code, and it
returns *true* if and only if a response has exactly this status.

When detecting a failed request the functions throw an exception of type
_FailedRequestException_. This exception becomes the result of the future
returned by the HTTP client. From its properties, the caller can gain some
useful information about the failure:

* the HTTP status code
* the tag of the request (if provided)
* the body of the response; this is the content sent by the server in the
  response - depending on the server, this may contain additional information
  why the request failed

#[[###]]# JSON deserialization support

When dealing with JSON APIs server responses often have to be transformed into
Java object representations. This is possible with the _jsonResult()_ functions
of _HttpUtils_. The functions require a JSON object mapper as argument. They 
use this object to convert the entity of a response to a Java object whose type
is determined either by a class or a type reference.

Note that the _ResponseProcessor_s returned by these functions directly access
the entity content without doing checks of the status code; but they can be
combined with the _checkResponse()_ functions discussed earlier to achieve
this. Below is an example of a GET request that is converted to a model object:

```
CompletableFuture<JsonBean> response = httpClient.execute(HttpUtils.get(endpointUri()),
        HttpUtils.checkResponse(HttpUtils.jsonResult(mapper, JsonBean.class)));
```

#[[###]]# Blocking requests

The API of _HttpClient_ to execute requests is asynchronous per default, which
gives the highest flexibility and enables sending many requests efficiently.
For some simple use cases, however, the easier blocking programming model may
be a better fit. If you just want to execute a single request and have the
result available, it may be inconvenient to deal with futures.

For such cases, the _HttpUtils_ class offers the _waitFor()_ function. The
function expects a _Future_ of a specific type, waits (in a blocking manner)
for the completion of this future, and returns its result. If the future
completes with an exception, the exception is wrapped in an _IOException_ and
thrown; thus callers do not have to deal with the various exceptions thrown by
_Future.get()_.

The _waitFor()_ function offers an easy to use means to convert the 
asynchronous API for request execution to a synchronous one. The example below
shows a typical usage:

```
ResponseProcessor<JsonBean> processor = HttpUtils.jsonResult(mapper, JsonBean.class);
try {
    JsonBean result = HttpUtils.waitFor(httpClient.execute(HttpUtils.get(endpoint()), processor));
    // do something with result
} catch (IOException e) {
    // exception handling
} 
```
