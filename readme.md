Prepared ![](images/media/image1.png){width="6.829863298337708in" height="4.452430008748906in"}by: Alfred Lopez, Lead Full-Stack Engineer

November 23, 2020

> The image on the cover page depicts observers observing *quantum entanglement* with *qubits* (represented by *Bloch spheres*)*.* The idea is though user sessions are distributed, each instance of a session, shares the same state and lifecycle as the other.

***[NOTE: This project is not entirely buildable or deployable. It is purposely missing key configuration/deployment files. This is for demonstration only, and to showcase my development work. Though, with a little work, and insight from the documentation, it can be "made whole".]***


## 

## Objective

The purpose of this document is to introduce the Global Session Management Library, as a discussion point and recommendation as a solution that can be implemented organization-wide.

## Goals

The primary goal is understanding of the GSML, the problems it tries to solve, and current implementation, and, secondary, to incite ideas from the readers (as stakeholders) regarding new uses and reuse in other projects or tooling.

Introduction

The intent of the Global Session Management Library (GSML) is to bind a user session across a distributed set of applications with no need for session synchronization from the applications' perspectives. Each application can manage the user session as if it were their own. Any effects a session incurs in one application, are reflected in all instances of that distributed session. GSML currently exist for the Java platform, but can be targeted to other platforms such as JavaScript, iOS, Android, and other native platforms like Linux, macOS, and Windows, from a single source code set. This is made possible by the use of Kotlin as the implementation language. Benefits of using Kotlin include, safer, more concise code, very light-weight threads in the form of coroutines, and the ability to apply multiple-inheritance strategies, as well as producing Domain-Specific Language constructs that can be combined with regular code.

Overall Architecture

Conceptually, GSML is comprised of three major components: a client-side component for receiving and managing tokens, a messaging broker, and a token issuing service. The latter is typically an independent identity provider not necessarily associated with the GSML. An application is instrumented with a TokenManager which handles all communications and token validations on behalf of the application. The TokenManager alerts the application via events which the application can query and react to. GSML is not opinionated and allows the application to interpret and implement the meaning of each event as it sees fit.

In the preceding diagram, the *Application Space* depicts the possibly many instances of the TokenManager. Each application will have an instance of the TokenManager per session. Each TokenManager sends and receives events via a message bus. Each part of the diagram is detailed below.

Token Manager

The TokenManager is the class the host application (hereto known as *the application*) will interact with. It has a simple interface so it imposes (in a good way) a simple integration pattern, as depicted in the following diagram:

This is the basic use of the TokenManager; as a *gatekeeper*. A token is received by the application, and the application uses the TokenManager to validate it as well as gather information about it, without needing to know anything about its format. Therefore, there's no need specialized code to handle different token types. *Access* and *Identity* tokens are conceptual. In addition to interacting with the TokenManager, the application can also elect to subscribe to TokenManager events, by implementing an *EventListener*. Though this is optional, it is highly recommended as the application will react as events happen, as opposed to "polling" the TokenManager for the current state.

As the name implies, TokenManager manages the lifecycle of a token set. This set comprises of a *access*, *identity*, and *refresh* token. Applications query the TokenManager for validity and not the token itself. A *valid* state is defined as valid token formats, time-to-live, and signature.

Message Bus

The *message bus* is also conceptual. This can be implemented as an independent enterprise service bus (ESB) like MuleSoft's **Mule** or **Apache Camel**; stream processors like **Apache Kafka**; or using distributed grids like **Hazelcast**, **Infinispan**, or **redis**. In short, anything that supports a publish/subscribe model. GSML incorporates a simple implementation of this out of the box that currently interacts with the **Security Utilities** back-end. Integrating with pub/sub systems, requires implementing the *EventBroadcaster* and *BroadcastListener* interfaces. We are planning on providing implementations of these interfaces for a few popular ones, though, as of this writing, no decision, as to which ones, have been made.

TOKEN ISSUER

*Token issuers* are merely identity managers. They are labeled as such because that's the only functionality that TokenManager needs. Since TokenManager is not opinionated, as mentioned, token issuers are inherently trusted, so all issued tokens are trusted until the issuing system says otherwise.

Key COMPONENTS

Token Manager

Despite its simple interface, TokenManager performs certain token functions once it has a token set, so instantiating a TokenManager is independent of creating tokens. TokenManagers manage one token set at a time, but can manage many tokens during the lifetime of the application. However, as we shall see, in an application that is session-oriented (web application, for example), it is suggested to use/associate a session with one TokenManager. TokenManagers are lightweight in nature, even in their threading model, which is implemented using *coroutines*[^1]*.* TokenManager can be viewed as a component, from the application's perspective.

In the above TokenManager component diagram, we can see the exposed interface and their interactions. Notice that **isValid** is dependent on the **isValid** of the IdentityToken and the AccessToken, implying that the validity of the tokens is the responsibility of the tokens themselves. Again, this implies trust on the tokens, via trust on the token issuer. This model allows GSML to "componentize" the tokens, meaning, they can be swapped out for better ones or ones that match a particular environment, or token issuer (e.g., CIAM vs Security Utilities vs SAML, etc).

## Token Refreshing/Revoking

Though not depicted in the preceding diagram, TokenManager incorporates a *Refresh Token*. This token is long lived and it's the token that validated the session as a whole. The refresh token is used to obtain an updated identity token, once the current one expires. TokenManager monitors the expiration of its identity token and sends a message to token issuer for a refreshed one utilizing the refresh token as a validator. The refresh token is encrypted and only decryptable by the issuer. If the issuer deems it valid, then TokenManager will receive and updated identity token. Otherwise, the issuer responds with a *revoke* message. If this occurs, TokenManager broadcasts a **REVOKE** event to all listeners in the application, for them to take whatever actions necessary. Likewise, the application can invalidate the TokenManager, which would, in turn, send a **REVOKE** even to the token issuer, marking the refresh token as revoked, and causing the token issuer to broadcast the message to all TokenManager, whose job is to match their respective refresh token ids with the one sent via the event, thus, causing all said instances to send the **REVOKE** event to their respective applications.

ACCESS, IDENTITY, AND REFRESH TOKENS

All tokens implement the interface **Token**. More specialized tokens, such as **Access**, would have additional features.

The AccessToken acts as an envelope for the Identity and Refresh tokens. It is also very short lived. It is expired once received by TokenManager, though it maintains its validity, in the context of TokenManager. TokenManager's validity is set once and will not change unless it has been invalidated by the application or the token issuer. The AccessToken also provides a context: a set of key/value pairs that are used arbitrarily by interacting applications. This context's life is permanent, meaning, even if the TokenManager is invalidated, the application can still read the context values. The context is immutable, and a new context would need to be created if different or additional values are needed prior to communicating with other applications.

IdentityToken and RefreshToken have a standard feature set. In its current implementation, TokenManager expects to receive a AccessToken, and expects to extract the Identity and Refresh tokens from the AccessToken. There are use-cases outside of the original scope that differentiates or omit tokens, and those can be modeled with very small effort.

*EventBroadcaster* and *BroadcastListener*

EventBroadcaster and BroadcastListener are the pub/sub mechanism in GSML. They are represented as singletons in the application. Currently, BroadcastListener listens via a polling mechanism, using a single coroutine. All TokenManager instances in an application registers with the BroadcastListener to receive messages from the token issuer, rather than having each TokenManager instance poll the issuer themselves. Likewise, all TokenManager instances proxies their messages to the issuer via the EventBroadcaster singleton.

Environment and injector

An application will not necessarily need to interact with the Environment or Injector singletons, but they can for very specialized use-cases. Environment is responsible for reading the environment where the application is running and provide default and environment-specific configuration values to the TokenManager and accompanying classes. Injector utilizes Environment to get information regarding the components to use as specified in the environment. Here's a sequence diagram for **TokenManager**.**getAccessToken** feature to illustrate:

We decided to roll our own dependency injector, rather than use an off-the-shelf one, because it was much simpler and, since it's written in pure Kotlin, it's more compatible when targeting multiple platforms, which is one of the goals of GSML.

Session Management

Now we come to the *pièce de résistance*: ***Global Session Management*!**

One of the goals of GSML is to solve the problem of keeping a session alive in one application, while the user is interacting in another. Well, GSML doesn't *actually* do that (sorry to disappoint), but it assists in session keep-alive, by alerting the application that it needs to "touch" the session to keep it alive. This is because there's no standard way of "keeping a session alive". In JEE (formally "J2EE") application, the application container (WebSphere, Weblogic, JBoss, Tomcat, etc.) is responsible for session keep-alive, but, since there's no specification for it, each container implements this mechanism differently, and there are no "hooks" into these mechanisms. This is not to say that the application developer has absolutely no control over this. One method (and it seems to be the only one) of touching the session is to "get" the session from the user request. The application container augments this action by updating the **lastAccessTime** of the session, but this property is not accessible to the application developer, nor is it accessible to the class developer, who would, potentially, inherit from the Session class (this is one aspect of Java that I fervently contend[^2]). The user request is only accessible when there's *actually* a request made to the application. Since this is not the case, there is no other way of performing "getting the session". To mitigate this, TokenManager sends a **REFRESH** event to the application. The application would then be responsible to perform said function. One strategy (or hack) to mock this is for the application to make a request to itself. I don't advocate this approach, since it may introduce side-effects, but it is a documented hack.

But all is not lost. GSML is comprised of three packages: *core*, *jee*, and *nodejs*. JEE applications will import the *core* and *jee* packages. The *jee* package implements a specialized version of TokenManager, that responds to the **REFRESH** event and sets the session's **maxInactiveInterval** to the RefreshToken's expiry time. The rationale is RefreshTokens represent the maximum acceptable time for a session, so as long as a RefreshToken is not revoked (by the issuer or by the user's logout action), then the session is still alive.

This method is the least invasive and one with a predictable side-effect, but it's not definitive, so the application will still receive the **REFRESH** event, for further or alternative processing.

IMPLEMENTATION and integration STRATEGIES

In this section, we look at a few implementation strategies targeting the most common scenarios.

JAVA Web Application

This type of application will be the most common to integrate the GSML into, though GSML's design is not influenced by it. By "web" I mean an application that uses Servlets to accept requests, but does not, necessarily, have a front-end. The *jee* package introduces three additional classes: **WebTokenManager**, **AuthenticationFilter**, and **SUSessionManager**.

## WebTokenManager

This is a specialized version of TokenManager. In addition to the features of TokenManager, WebTokenManager simply associates itself to the current session and responds to the **REFRESH** event, as described above. It also invalidates itself when the session is destroyed or invalidated.

## AuthenticationFilter

This is an implementation of the **Filter** class in the Java Servlet specification. Its purpose is to intercept requests to the application, looking for the existence of access tokens and current sessions and making decisions as to allow the request to pass through or handle the "bad request". In a straight forward web application, AuthenticationFilter implements the "gatekeeper" approach mentioned earlier. Using this class is optional, but the application developer would then be tasked to write scaffolding/boilerplate code. This class is fully extensible and reusable, so the developer shouldn't have difficulty adapting it to their environment.

## ConcreteSessionManager

This is an implementation of the **HttpSessionListener** in the Java Servlet specification. Its purpose is to manage the lifecycle of the instances of WebTokenManager in association with each session. Again, this is an optional, fully extensible, and reusable class.

## Sample Integration of the JEE Library[^3]

Below is a suggested, minimal integration of a Login Servlet[^4] with the GSML:

The integration points are highlighted in red. With this minimal code and the **AuthenticationFilter** in place, we've implemented a simple gatekeeper. This code, however, is not "aware" of the global state of the session. For that, we would have to write code that periodically "polls" TokenManager for its state. Rather than do that, it is recommended to implement the **EventListener**:

In addition to this, you need to configure the GSML in the **web.xml** file by adding the following:

The integration code for a micro service, would follow the same pattern as the one depicted in **LoginServlet**.

NodeJS Integration

As of this writing, a NodeJS library has not been produced. There are some technical challenges that we're working through, but progress is slow due to Kotlin Multiplatform technology ~~in alpha state~~ and updates are moving too fast for the documentation to keep up. Though using bleeding edge technology is often viewed as blasphemous, I advocate this approach as this will be a huge benefit in the long run. If we had to write a library for Java, then JavaScript, and possibly for iOS or Android, if mobile needs to participate in this for some reason, then we would have duplicated efforts, and synchronization issues if a new feature needs to be introduced. With the approach we've taken, we write once, and deploy everywhere.

The plan is to have a *js* package akin to the *jee* one. The current target session technology involves ExpressJS and the proposed *js* package is to base it on ExpresJS' middleware implementation, but if a more homogenous solution is found, then that would be the direction we will take.

Current state of GSML

Currently, we have a Java library available. This is being used in our legacy systems, data services (via the **Auth-lib** library), and our React/NodeJS applications (via **ES4X**).

##Installation and Configuration

###Maven
In a Maven project, add this dependency to your POM file:

        <dependency>
            <groupId>com.asanasoft.grs</groupId>
            <artifactId>gsml-client-core</artifactId>
            <version>${gsml.version}</version>
        </dependency>

The current GSML version, as of this writing, is 0.24.0.

###Properties
GSML has several default properties files that an application can augment or fully overide. You can extract these properties from the jar file and place them in your classpath. When GSML looks for properties, your properties should be found first, if fully overriding them.
Properties files are in the form:

**[modules].properties**

If you name your properties file with the same name, you have fully overridden the property. Alternatively, you can *augment* the file or override certain properties by putting your changes with a filename like:

**[indentifier]_[module].properties**

For example, to override/augment one or more (but not all) properties in **suc.properties** for, say, your production environment, put your changes in a file named:

**PROD_suc.properties**

GSML will first load **suc.properties**, and then load **PROD_suc.properties**, provided that the **ENV** environment variable is set to **PROD**:

(linux) **export ENV=PROD**

###Logging
GSML uses the SLF4J API for logging, but doesn't provide a logging binding for it. If you need logging, use a SLF4J binding jar. 

[^1]: For an overview of coroutines, visit [[https://kotlinlang.org/docs/reference/coroutines-overview.html]{.underline}](https://kotlinlang.org/docs/reference/coroutines-overview.html)

[^2]: I cover this topic in my white paper "Software Quality:Developing Better, Cheaper Software"

[^3]: For nerds only. You may skip this section if you'd like.

[^4]: This example was taken from the Internet

