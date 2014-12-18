					
					==========
					README.TXT
					==========

				        by Sarath R.
=================
Chain-Replication
=================

Bank chain replication project for CSE535-Asynchronous Systems

===============
LANGUAGES USED:
===============
1) Java

=============================
(1)IMPLEMENTATION IN JAVA
=============================

============
INSTRUCTIONS
============
1) Setup Activemq framework to handle message passing using JMX. Binaries for Linux are included under /ChainReplication (Java)/apache-activemq-5.9.1/. More instructions at http://activemq.apache.org/
2) Start the Activemq broker service
3) Run Spawner.class's main to test using the configuration file @ /ChainReplication (Java)/src/log4j.properties. (Provide location of config file as first argument)
4) Run TestMain.class's main to test the framework. (Provide location of config file as first argument)

=======
DESIGN
=======
Files under org.* provide the basic ChainReplication framework, which is extended by files in com.* to get the extended the functionality for the bank problem.

==========
MAIN FILES
==========
.
-rw------- 1 xxxx xxxx 2358 Nov 13 20:35 log4j.properties 		#CONFIGURATION FILE

com/bank:
total 28
-rw------- 1 xxxx xxxx  8766 Nov 13 21:13 BankClient.java		#BANK CLIENT
-rw------- 1 xxxx xxxx 11968 Nov 13 16:56 BankServer.java		#BANK SERVER
-rw------- 1 xxxx xxxx  3646 Nov 13 20:34 Spawner.java			#STARTS CLIENTS(THREADS) AND SERVERS(SEPARATE JVMs)
-rw-r--r-- 1 xxxx xxxx 12777 Nov 24 17:01 XBankMaster.java		#FT BANK MASTER
-rw-r--r-- 1 xxxx xxxx 12389 Nov 24 17:01 XBankServer.java		#FT BANK SERVER

com/test:
total 68
-rw------- 1 xxxx xxxx 4940 Nov 11 22:50 FrameworkTestClass.java	#TEST CLASS TO CHECK BASE FRAMEWORK
-rw------- 1 xxxx xxxx 4734 Nov 13 20:50 TestMain.java				#TEST CLASS TO CHECK BANK FRAMEWORK
-rw-r--r-- 1 xxxx xxxx  695 Nov 24 17:01 SimpleClient.java
-rw-r--r-- 1 xxxx xxxx  916 Nov 24 17:01 SimpleServer.java
-rw-r--r-- 1 xxxx xxxx  397 Nov 24 17:01 testobject.java

org/actors:
total 18
-rw------- 1 xxxx xxxx 10198 Nov 13 19:55 BasicChainReplicationServer.java 	#BASE FRAMEWORK CLASS FOR SERVER FOR ANY CHAIN REPLICATION
-rw------- 1 xxxx xxxx    76 Nov 11 22:50 Client.java				#BASE FRAMEWORK CLASS FOR CLIENT ANY CHAIN REPLICATION
-rw------- 1 xxxx xxxx  1040 Nov 11 23:24 Identifier.java			#BASE FRAMEWORK CLASS FOR IDENTIFIER FOR ANY CLASS
-rw------- 1 xxxx xxxx    74 Nov 11 22:50 Master.java				#BASE FRAMEWORK MASTER FOR CLIENT ANY CHAIN REPLICATION
-rw------- 1 xxxx xxxx   529 Nov 11 23:24 Messages.java				#BASE FRAMEWORK EXTERNALISING MESSAGES

org/commons:
total 21
-rw------- 1 xxxx xxxx 3399 Nov 13 16:52 BasicChainReplicationMessage.java	#BASE FRAMEWORK MESSAGE FOR CLIENT ANY CHAIN REPLICATION
-rw------- 1 xxxx xxxx 8038 Nov 13 20:32 Comm.java				#BASE FRAMEWORK COMMUNICATION CLASS
-rw------- 1 xxxx xxxx  283 Nov 11 23:09 CommType.java				#ENUM OF COMMUNICATION TYPES
-rw------- 1 xxxx xxxx  679 Nov 13 19:43 Constants.java				#CONSTANTS
-rw------- 1 xxxx xxxx  944 Nov 11 22:54 IComm.java				#INTERFACE DEFINING COMM CLASS
-rwxrwxrwx 1 xxxx xxxx 1117 Nov 24 17:01 BasicChainReplicationIdentifier.java #BASE FRAMEWORK IDENTIFIER FOR CLIENT ANY CHAIN REPLICATION
-rw-r--r-- 1 xxxx xxxx 5090 Nov 24 17:01 customComm.java		#CUSTOM BASE FRAMEWORK COMMUNICATION CLASS
-rw-r--r-- 1 xxxx xxxx  237 Nov 24 17:01 ServerState.java		#ENUM FOR SERVER STATE

org/utilities:
total 4
-rw------- 1 xxxx xxxx 1116 Nov 12 21:41 CUtils.java				#COMMON UTILITY PROVIDER FOR THREADING, HASHING

====
LOGS
====
Logs of some tests done can be found at /Logs

========
LICENSE
========
Uses Apache ActiveMQ released under the Apache 2.0 License. Visit http://activemq.apache.org/ for more details