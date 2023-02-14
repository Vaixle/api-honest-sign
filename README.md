# api-honest-sign
Test task for SELSUP

[![Project Status: WIP – Initial development is in progress, but there has not yet been a stable, usable release suitable for the public.](https://www.repostatus.org/badges/latest/inactive.svg)](https://www.repostatus.org/#inactive)
[![GitHub](https://img.shields.io/github/license/vaixle/api-honest-sign)](https://github.com/Vaixle/api-honest-sign/blob/main/LICENSE)

## Conntents:

- [Main Technologies](#Main-technologies) 
- [Task](#Task) 
    - [Requirments](#Requirments) 
- [Solution](#Solution)

### Main-technologies

| **Backend**  |[![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=java&logoColor=white)](https://dev.java/) |
|:------------:|:------------:|
| **Build tool**  |![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white) |

### Task
You need to implement in Java (you can use version 11) a class to work with the Honest Mark API.

#### Requirments
- The class must be thread-safe and support a limit on the number of requests to the API. The limit is specified in the constructor as the number of requests in a certain time interval. 

Example:

`public com.vaixle.CrptApi(TimeUnit timeUnit, int requestLimit)`

`timeUnit` – indicates the time interval - second, minute, etc.

`requestLimit` – is a positive value that defines the maximum number of requests in this time interval.

- If the limit is exceeded, the request must be blocked so that it does not exceed the maximum number of requests to the API and continue execution when the limit is not exceeded.

- The only method to be implemented is the Creation of a document to put into circulation the goods produced in the RF. Document and signature must be passed to the method in the form of Java object and string respectively.

- When implementing you can use HTTP client libraries, JSON serialization. The implementation should be as convenient as possible for future functionality expansion.

- The solution should be designed as one file com.vaixle.CrptApi.java. All additional classes that are used must be internal.

### Solution
[click](https://github.com/Vaixle/api-honest-sign/blob/main/src/main/java/com/vaixle/CrptApi.java)



