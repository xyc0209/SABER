### SABER: A MAPE-K-based Self-Adaptive Framework for Microservice Bad Smell Refactoring





### The detection algorithms of Analyzer component:

| MBS                                                          | Detection                                                    |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| Mega Service (MS)                                            | We introduce a rule-based detection algorithm from the MARS tool for identifying Mega Service, which determines the presence of this anti-pattern based on the number of lines of code. |
| Nano Service (NS)                                            | We introduce the detection algorithm for Nano Service from MARS, which is a rule-based detection algorithm that determines the presence of this smell by evaluating the number of code lines and files. |
| Sharing Persistence (SP) and Inappropriate Service Intimacy (ISI) | We develop a database configuration extraction algorithm that systematically parses each service's configuration files to obtain database connection parameters (IP, port, credentials) and schema details. For SP detection, we perform pairwise comparison of database connection strings across services to identify shared database instances. ISI detection is achieved by analyzing the number of distinct databases accessed by individual services. The Analyzer module outputs detected anomalies along with the complete entity sets from affected services' persistence layers to support refactoring decisions. |
| No API-Gateway (NAG)                                         | Traverse all service modules to examine whether their build configurations contain gateway-related dependencies (e.g., spring-cloud-starter-gateway) and verify if their application configuration files include routing definition items (e.g., spring.cloud.gateway.routes). The existence of a gateway service is then determined through comprehensive validation of these criteria. |
| No Service Discovery (NSD)                                   | For the detection of NSD, traverse the dependency management files of each service and check whether service discovery-related dependencies (such as Nacos or Eureka) are included. If not, the service is identified as having the NSD smell. |
| Hardcode Endpoints (HE)                                      | To detect HE, all Java files within each service are scanned.  IP:PORT strings are extracted from class member variables, local variables, and method arguments, and their presence is determined using regular expression matching. |
| Unnecessary Settings  (US)                                   | For US detection, the Analyzer component first traverses all service modules to extract configuration parameters and determine if their values is default values. It subsequently traverses class files within each service and performs final judgment based on parameter usage. |
| No API Versioning (NAV)                                      | For the detection of NAV, each service is traversed to extract the URLs from the controller class and the methods, which are then concatenated. Regular expressions are used to match and determine the presence of NAV. |


