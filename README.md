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

![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-1.png)
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-2.png)
![](https://github.com/mrFlick72/saga-spike/blob/master/img/SAGA-3.png)