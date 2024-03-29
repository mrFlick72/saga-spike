# saga-spike
A spike on a full no blocking use case using saga as solution for distributed long lived transaction

# What is SAGA
SAGA is a pattern that try to approach on a long lived distributed transaction like a saga TV.
In a saga tv, only when we have saw all the saga, we can have the complete picture, while every episode is only one atomic 
piece of the whole saga.
Similarly the pattern try to manage a distributed transaction with a series of local transaction, the our set of episode, and only when we have saw all the saga, we have done all the local
 transaction of the whole distributed transaction, we therefore we have done all the transaction.
 
 # How to do a SAGA
There exist many way to implements a SAGA. Even if a point to point integration style like rest, RPC integration and so on,
 may be suitable in specific use cases, mainly for distributed transaction when have a temporal decoupling is not required,
  all the power of the pattern is in action when all the peer of the transaction are spatial and temporal decoupled, 
  in this case the messaging integration style is the king!

Just few words about the possibility of rest as integration way. It is not the evil, even a long cousin WS-BEPL use a similar approach 
with WS SOAP, in this case the purist can disagree on this words but basically I want just say that the key point here is not on how to do: 
rest, messaging or other approaches, the key point is that the most mature approach on temporal and spatial decoupled system is messaging and since that 
the pattern attack distributed long lived transaction use a messaging system is the most natural and mature way to do so.

Now that we have clarify why messaging wins over other approaches, is time to choose how proceed. Typically in a messaging system 
we can design the our application in two way: orchestration integration or a choreography integration approach. 
Basically in a choreography all the peer know how react to the messages and all the peer do the needed like a dancing choreography. 
This approach is without doubts the most free and fluid approach, but in this case the transaction flow is implementation is scattered in 
all the all the bounded contexts participants in the transaction. On the other hands Orchestration is an approach in which we have a process 
that own the transaction flow, like in WS-BEPL, old ESB, BPMN 2.0 and all runtime that basically do workload orchestration.
 In this case the approach is more ordered because all the flow is implemented in on point and even the debug in case of error is simpler.

Last but not least in a SAGA is crucial that every local transaction have to expose the `doSomething` and the compensation `undoSomething` in order to 
rollback the transaction in case of error in the middle of the process.   

# How I have implemented a SAGA
For this spike I have used probably the most simple use case, a sales order process. In this scenario I have three microservices. Every microservices expose 
the `doSomething` and the compensation `undoSomething` for all non idempotent operation, even the errors are treated like normal messages that take place on the saga as barrier to 
alert the orchestrator to start the rollback. Furthermore for this sample I have used for all the microservices a reactive approach, one of the goal is show hot to do 
all reactive and no blocking. For this the my stack is:

* Reactor
* Spring WebFlux
* Spring Data Reactive Mongo
* Spring Cloud Stream (Kafka as binder)
* Spring Integration for the orchestration
* Redis as Message Store 
* Docker in order to provide all the infrastructure for test by your self the spike with just a `docker-compose up` command. 

## Take in charge the nre sales order saga
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-1.png)
Basically the process starting from a gateway that publish a new sales order request.
 starting from this message it is published in a publish subscriber channel with two branch, the first branch return to the external the software id 
for the new sales order and return as soon as possible to the caller, while the other branch publish on kafka via spring cloud stream channel. 
The listener take the message and save the customer like a command on mongo. Then the list of goods in the original message 
was splitted, transformed for the catalog service and sent to the `Catalog-Service` like before via a kafka message with spring cloud stream.
all the rest of process before the aggregation is choreographed on the other two microservices. Even in this case 
the glue infra microservices is done by messaging wich kafka and spring cloud stream. As probably on this point is clear in this project I have used 
Spring Cloud Stream as glue between microservices it is a stream of messages that make the system as fluid as possible and spatial decoupled.   


## Take in charge the aggregation just before to commit or rollback 
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-2.png)
The result of `Inventory-Service` and `Catalog-Service` enter to the orchestrator via a spring cloud stream channel via Kafka, 
here we have one listener per possible message and those are responsible to process the message and transform the incoming messages in a uniform message 
to send on the aggregator that tanks to a router decide base on the whole aggregation if the transaction should be rollbacked or no.

It is important to note that right before the aggregation and the "commit" or "rollback" of the transaction, from the sales order point of view 
the order is on a pending status and only when the orchestrator decide to process or rollback the transaction, the sales order change status on aborted or completed.
In case of process the orchestrator send a series of commands to change the status on completed and of course save the all goods in the order even in this case like a series of commands.
Otherwise the orchestrator send a command to abort the order and send a message for every goods in error on the inventory service 
inorder to undo the reservation of the goods.

Pay attention that from the sales order point ov view we have a CQRS and Event Sourcing implementation with a command store on Mongo and In Memory Read Store. 
All the events produce commands that when applied on the in memory read store, give the sales order status, in this case the transaction is, of course, not an ACID transaction but a BASE transaction. 
I Hope that we agree that in a distributed system it is totally acceptable, remember the CAP theorem, we can not have Availability, Consistency and Partition Tolerance on the same time!

In this case the reader probably do not see where is the distribution of transaction if the sales order maintains an internal status. 
Yes it is true, but the behaviour is not decided from an internal status the outcome of the whole transaction. It depends for three component and 
the transaction span in any case three bounded context and in any case have a sales order service that is maps a Order Domain is totally acceptable that 
it will have an internal status of the order mapped on the Order Domain without Catalog and Inventory Details.    
 


## Take in charge the reserve of a goods after the process know the goods price
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-3.png)
It is the listener that take the goods from the catalog with the price and ask to reserve this goods that we know exist on the required catalog

# Conclusion

Now it is the time of conclusions! Is SAGA the silver bullet that will save us form the dark and the evil?
The answer is very simple.............. NO!

SAGA is designed to solve the problem of distributed and long lived transaction. Implements SAGA can introduce an high level of 
complexity, but if the use case required a distributed long lived transaction in which have a spatial and temporal decoupling is a must, 
SAGA may be for you.
 
Remember the previous discussion of the usage of rest or messaging: use the power true of a messaging system implies 
to pay attention on how implements request reply services. I have solved this problem returning soon the software id of the sales order and 
embracing a CQRS style domain modeling the status is totally Soft (the S of BASE) and event driven, as long as the saga is on going, the sales order status can change. All the rest of the integration pipeline 
is totally fluid and no blocking, it can seems cool but in some system it can not be suitable. It is very important that the system can be eventually consistent,
 soft in the state and Basically Available, in other words BASE. If the system required to be ACID and synchronous with strict bounded time to run the transaction the 
messaging perhaps is not the best way. Use a message integration in which we have all request reply is not a good way to use messaging, and it may implies many issues.          