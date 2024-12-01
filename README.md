# CS441 Homework3

**Name**: Thanmayi Akkala  
**Email**: takkal2@uic.edu

**UIN**: 650556907

**Video Link**: https://youtu.be/Sf0Z1QxdJRg

## Overview
This project implements a conversational agent using gRPC and a microservices architecture. The solution consists of a gRPC server that processes user queries, communicates with an AWS Lambda function via API Gateway, and generates responses based on an integrated Large Language Model (LLM). Additionally, the project includes a conversational client that interacts with the server and uses the Ollama API to generate follow-up queries, creating a seamless conversational experience. The implementation supports local testing and AWS deployment, utilizing modern frameworks such as Akka HTTP and Spray JSON for HTTP handling, ScalaPB for Protobuf support, and Ollama for local LLM interactions. Comprehensive logging, retry mechanisms, and configurable settings ensure robust performance and flexibility in various deployment environments.


### Key Classes and Objects

- **QueryServiceImpl class**: Located in `example.grpc.main`, this class implements the gRPC server logic. It processes incoming queries, forwards them to an AWS Lambda function via an API Gateway using Akka HTTP, and returns the response to the client.
- **Main.scala**: The main entry point for the project. It sets up the gRPC server, manages shutdown hooks, and includes a client for local testing of the gRPC service.
- **ConversationAgent.scala**: Implements the conversational client that interacts with the gRPC server. It uses the Ollama API for generating follow-up queries based on server responses and facilitates automated query-response flows.
- **Lambda Function**: Defined in the AWS environment, it processes queries by invoking the Amazon Bedrock LLM to generate responses and returns them via the API Gateway.
- **Configuration Files**: Includes `application.conf`, which specifies the API Gateway URL, gRPC port, and Ollama LLM configurations.
- **Service.proto**: Used to generate protobuf


### Build Instructions
This project uses **SBT** (Scala Build Tool) to manage dependencies and compile the project.

**build.sbt** includes the following dependencies:
- `com.typesafe.akka`: For gRPC server implementation and HTTP handling.
- `io.grpc`: For gRPC client and server support.
- `io.github.ollama4j`: For interacting with the Ollama LLM API.
- `com.thesamet.scalapb`: For ScalaPB (Protocol Buffers) support.
- Logging libraries like `logback` and `slf4j`.

### Running the Project

#### Prerequisites:
- **Java Version**: Ensure that **Java 11** is installed and set as your default JDK.
- **Scala Version**: The project is built with **Scala 2.13.8**.
- **SBT Version**: Use **SBT 1.x** to build and package the project.
- **ScalaPB**: Ensure ScalaPB is configured for Protobuf generation and gRPC support.


### How to Run Locally in IntelliJ

This project involves running a gRPC server and an Ollama-based conversational agent. Follow the steps below to set up and execute the project in IntelliJ, along with an explanation of how each component works.

---

#### 1. **Clone the Project**
- Open IntelliJ IDEA and clone the repository into your workspace, or manually create a new project in IntelliJ.
- Add the provided key files:
  - Place Scala source files (`*.scala`) under `src/main/scala`.
  - Place the `build.sbt` and `plugins.sbt` files in the project root directory.

---

#### 2. **Generate Protobuf Files**
- Before running the project, compile the code to generate the required Protobuf files for gRPC. This ensures that all service definitions are available for communication.
  ```bash
  sbt compile

#### 3. **Lambda Function**

The gRPC server communicates with an AWS Lambda function via an API Gateway. Follow these steps to ensure the Lambda function is properly deployed and tested:

#### Deployment
1. **Deploy the Lambda Function**:
   - Use the AWS Management Console, AWS CLI, or AWS Serverless Application Model (SAM) to deploy the Lambda function.
   - Ensure the function uses Amazon Bedrock for natural language processing.

2. **Configure the API Gateway**:
   - Set up an API Gateway to trigger the Lambda function.
   - In this project the API Gateway endpoint is: https://udqasoaqmk.execute-api.us-east-2.amazonaws.com/prod/query
   - This can be tested on postman if the lamda is accessible.

#### Testing
1. **Test the Lambda Function Independently**:
   - Use tools like Postman or curl to send HTTP POST requests directly to the API Gateway URL.
   - Verify the response contains the processed query result.

2. **Verify Integration with the gRPC Server**:
   - Start the gRPC server (`sbt run`) and ensure it correctly forwards queries to the Lambda function.
   - Monitor server logs to confirm the Lambda responses are being processed and sent back to the client.

#### Notes
- Ensure the Lambda function has the necessary IAM permissions to use Amazon Bedrock.
- The API Gateway URL must be accessible, and the Lambda function should be deployed in the same region as the Bedrock model for optimal performance.


#### 4. Start the gRPC Server

To run the gRPC server, use the following command:
  sbt compile

Alternatively, you can directly specify the server entry point, this is general command you can also put query with it:

```bash
sbt "runMain example.grpc.main.Main"
```
This starts the server on localhost:50051, where it listens for incoming queries. Ensure this step is completed before running the conversational agent or client, as the agent depends on the server for communication.
#### 5. Run the Ollama-Based Conversational Agent

After running the main the gRPC server, execute the conversational agent with an initial query.

The conversational agent will:

Send the query to the gRPC server.
Process the server's response using Ollama to generate the next query.
Continue the conversation loop until the termination condition (e.g., receiving "END") is met.

### Deploying on EC2
Create an EC2 instance and then connect it using ec2 connecting instance
#### Running the Application

##### **Before Logging In**
Upload the compiled JAR file to your EC2 instance using the `scp` command:

```bash
scp -i "D:\Downloads\huhu.pem" "D:\IdeaProjects\gRPC_project\target\scala-2.13\Bedrock-GRPC-Client-assembly-0.1.0-SNAPSHOT.jar" ec2-user@ec2-18-223-196-96.us-east-2.compute.amazonaws.com:/home/ec2-user/
```
Logging In to EC2
Log in to your EC2 instance using the ssh command:

```bash
ssh -i "D:\Downloads\huhu.pem" ec2-user@ec2-18-223-196-96.us-east-2.compute.amazonaws.com
```

Running the gRPC Server
Start the gRPC server on the EC2 instance using the java command:

```bash
Copy code
java -cp Bedrock-GRPC-Client-assembly-0.1.0-SNAPSHOT.jar example.grpc.main.Main
```

### Scala Unit/Integration Tests:
The tests are under in src/tests/scala. These can be run using sbt test at once or sbt.
It can be run using the scala test or by passing the files individually like: sbt "testOnly *Word2VecMapperTest"
More detailed in this docs: https://docs.google.com/document/d/1CsSLDK4hZqzr5Y7--g8d4cAiiCtesisuCnXA9J8Bxn8/edit?usp=sharing
### Output Explanation:
The API Gateway example by sending query through body
![image](https://github.com/user-attachments/assets/8a5a5b04-c301-4ebf-8be8-c08a37ab9289)

Using GRPC :

![image](https://github.com/user-attachments/assets/790b0ddc-d435-4c04-8bee-6c2046c3daa3)


The conversational Model :

Response from Bedrock for the query what is life:

![image](https://github.com/user-attachments/assets/355e1a65-7f0f-49ae-a68d-11082b15c559)
Then it is recieved by the conversational agent:
![image](https://github.com/user-attachments/assets/2c4030a2-a21f-410a-8a14-469ce964d22d)

then the ollama generates a conversation based on it:

![image](https://github.com/user-attachments/assets/5e1b11dc-059a-4f6b-9c0c-645098c5e650)
![image](https://github.com/user-attachments/assets/af2d298e-0def-4c1a-8ba5-dddbea295333)

and then bedrock replies:

![image](https://github.com/user-attachments/assets/08c9261b-e798-40be-ab1c-81eec5efb33e)

and the process goes on until termination condition.














