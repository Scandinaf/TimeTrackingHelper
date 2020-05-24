# Time tracking helper

## Description
A utility that ideally should minimize the process of mapping between time spent and tasks.
It is important to understand that this is only a tool and due to the fact that the data sources
(there are only two Outlook, Jira) are quite limited there is a possibility of discrepancies with reality, inaccuracies.

## Preparations
* 12 openJDK and later.
* Configure the configuration for use. File - **application.conf**.

## How does this system work???

There are two categories of statuses, major and minor:
* Major - basic statuses, which are given the main amount of time (For the developer - "In Progress").
* Minor - third-party statuses, can take some time during the day (for the developer - "Ready For Review"). By default, it is equal to the minimum time interval.

On the basis of the obtained data, a graph of the time spent is constructed.

The minimum time interval is 15 minutes.

The daily limit is 8 hours.

P.s. If your tasks have not reached the limit, then the scaling takes place, the time spent on the task * 1.1 (**10 percent**).

A few examples:
* Task #1 started yesterday and hasn't finished yet.
 Task #1 = daily limit

## Main commands
    1. sbt run
    2. sbt clean;compile;test
    
## Stack
* Scala 2.12
* Cats
* FS2
* Sttp
* Mockito
