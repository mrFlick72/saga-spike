# saga-spike
A spike on a full no blocking use case using saga as solution for distributed long lived transaction

# What is SAGA
SAGA is a pattern that try to approach on a long lived distributed transaction like a saga TV.
In a saga tv, only when we have saw all the saga, we can have the complete picture, while every episode are only one atomic 
piece of the whole saga.
Similarly the pattern try to manage a distributed transaction like a SAGA TV in which we have a 
series of local transaction, the our set of episode, and only when we have saw all the saga, we have done all the local
 transaction of the whole distributed transaction, we have done all the transaction.
 
 # How to do a SAGA
There exist many way to implements a SAGA, even if a point to point like rest, rcp integration and so on, may be suitable in specific use case,
mainly for distributed transaction when have a temporal decoupling is not required, all the power of the pattern is in action 
when all the pear of the transaction are spatial and temporal decoupled, in this case the messaging integration style is the king!

Just few words about the possibility of rest as integration way. It is not the evil, even a long cousin WS-BEPEL use a similar approach 
with WS SOAP, in this case the purist can disagree on this words but basically I want just to say that the key point is not on how to do: 
rest, messaging or other approach, the key point is that the most mature approach on temporal decoupled system is messaging and since that 
the pattern attack distributed but even long lived transaction use a messaging system is the most natural and mature way to do so.

Now that we have clarify why messaging win over other approach is time to choose how proceed. Typically in a messaging system 
we can design the our application in two way using a orchestration integration or a choreography integration approach. 
Basically in a choreography all the pear know how react to the message and all the pear do the needed like a dancing choreography. 
This approach is without doubts the most free and fluid approach, but in this case the transaction flow is implementation is scattered in 
all the all the bounded contexts participants in the transaction. On the other hands Orchestration is an approach in which we have a process 
that own the transaction flow. In this case the approach is more ordered because all the flow is implementated in on point and even the debug 
in case of error is simpler.

Last but not least in a SAGA is crucial that every local transaction have to expose the `doSomething` and the compensation `undoSomething` in order to 
rollback the transaction in case of error in the middle of the process.   

# How I have implemented a SAGA
For this spike I have used probably the most simple use case, a sales order process. In this scenario I have three microservices every microservices expose 
the `doSomething` and the compensation `undoSomething` even the error are treated like normal messages that take place on the saga as barrier to 
alert the orchestrator to start the rollback. For this sample I have used for all the microservices a reactive approach, one of the goal is show hot to do 
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
Basically the process starting from the new sales order request from the first branch return to the external the software id 
for the new sales order and return while in the other branch the system publish on kafka via spring cloud stream. 
The listener take the message and save the customer like a command on mongo then the list of goods in the original message 
was splitted, transformed for the catalog service and is send to the `Catalog-Service` via a kafka message with spring cloud stream.
all the rest of teh process before the aggreagation is choreographed on the other two microservices. Even in this case 
the glue infra microservices is done by messaging wich kafka and spring cloud stream. 


## Take in charge the aggregation just before to commit or rollback 
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-2.png)
The result of `Inventory-Service` and `Catalog-Service` enter to the orchestrator via a spring cloud stream channel via Kafka, 
here one listener per possible message is responsable to process the message and transorm the incoming message in a uniform message 
to send on the aggregator that tanks to a router decide base on the whole aggregation if the transaction should be rollbacked or no.
It is important to note that right before the aggreagation and the "commit" or rollback of the transaction from the sales order point of view 
the order is on a pending status and only when the orchestrator decide to process or rolback the trnsation the sales order change status on abborted or completed.
In case of process the orchestrator send a command to change the status on completed and save the all goods in the order.
Otherwise the orchestrator send a command to abort the order and send a message for every goods in error on the inventory service 
to unreserve the goods.

Pay attention that from the sals order point ov view we have a CQRS and event Sourcing implementation all the events produce 
commands that when applied on the in memory event store give the sales order status. In this case the transacion is not an ACID transactio 
but a BASE transaction, but in a distributed system it is totally acceptable in my opinion, remember the CAP theorem, we can not have 
Availability, Consistency and Partition Tolerance on the same time!

The attentive reader probably do not see where is the distribution of transaction if the sales order maintains an internal status. 
Yes it is true, but the behaviour is not decided from an internal status the outcome of the whole transaction depends for three component and 
the transaction span in any case three bounded context 
 


## Take in charge the reserve of a goods after the process know the goods price
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-3.png)
It is the listener that take the goods from the catalog with the price and ask to reserve this goods that we know exist on the required catalog

#Conclusion
Is SAGA the silver bullet that will save us form the dark and the evil?
The answer is very simple and it is NO!

SAGA is designed to solve a distributed and long lived transaction. Implement SAGA can introduce an high level of 
complexity but if the use case required a spatial and temporal decoupling SAGA may be for you. Remember the previous discussion 
of the usage of rest or messaging, use the tre power of a messaging system implies to pay attention how implements request reply 
service. I have solved this problem returning soon the software id of the sales order then the status is totally Soft (the S of BASE),
and as soon as the saga is on going the sales order can change all the rest of the integration pipeline is totally fluid and no blocking, it 
can seems cool but in some system it can be not suitable it is very important that the system can be eventually consistent, soft in the state and 
Basically Available, in other words BASE. If the system required to be ACID and synchronous with strict bounded time to run the transaction the 
messaging perhaps is not the best way use all request reply message integration is not a good way to use messaging.         