# AI Presentation Generator

An AI-powered presentation generation application that automatically creates structured PowerPoint presentations from a user-provided topic.

The application uses **Java, Spring Boot, AI/LLM APIs, and PowerPoint generation** to transform a simple topic into a complete presentation containing slides, text, images, diagrams, and structured content.

## 🚀 Features

* Generate presentations from a simple topic or prompt
* AI-generated slide titles and content
* Automatic slide structure and organization
* Generate relevant images for presentation slides
* Create diagrams to visually explain concepts
* Generate PowerPoint (`.pptx`) files automatically
* Customizable presentation options
* Structured AI responses for reliable slide generation
* REST API-based backend
* Separation of AI processing, presentation generation, and file handling

## 🛠️ Technologies Used

* **Java**
* **Spring Boot**
* **Spring Web**
* **AI / LLM API**
* **Apache POI / PowerPoint libraries**
* **REST APIs**
* **Maven**
* **JSON**
* **Git / GitHub**

## 🏗️ Project Architecture

```text
User
  │
  ▼
REST API
  │
  ▼
Presentation Controller
  │
  ▼
Presentation Service
  │
  ├──────────────► AI Service
  │                    │
  │                    ▼
  │              LLM / AI API
  │                    │
  │                    ▼
  │              Slide Content
  │
  ├──────────────► Image Service
  │                    │
  │                    ▼
  │              Relevant Images
  │
  ├──────────────► Diagram Service
  │                    │
  │                    ▼
  │              Generated Diagrams
  │
  ▼
PowerPoint Generator
  │
  ▼
.pptx File
```

## 🔄 Application Flow

### 1. User provides a topic

The user sends a request such as:

```text
Create a presentation about Apache Kafka
```

### 2. Request reaches the REST Controller

The Spring Boot controller receives the request and passes it to the presentation service.

```text
POST /presentation/generate
```

### 3. Presentation Service processes the request

The service coordinates the different components required to build the presentation.

It determines:

* Number of slides
* Slide topics
* Slide structure
* Content requirements
* Images
* Diagrams

### 4. AI generates the presentation content

The application sends a structured prompt to the LLM.

The model generates information such as:

```text
Slide 1 → Introduction
Slide 2 → What is Kafka?
Slide 3 → Kafka Architecture
Slide 4 → Producers and Consumers
Slide 5 → Kafka Topics and Partitions
Slide 6 → Real-world Use Cases
Slide 7 → Conclusion
```

### 5. Images are generated or retrieved

Relevant images can be associated with appropriate slides to make the presentation more visually engaging.

### 6. Diagrams are created

For technical topics, the application can generate diagrams such as:

```text
Producer
    │
    ▼
 Kafka Topic
    │
 ┌──┴──┐
 ▼     ▼
Consumer  Consumer
```

This helps explain technical concepts visually instead of relying only on text.

### 7. PowerPoint file is generated

The generated content, images, and diagrams are assembled into PowerPoint slides.

The final result is a:

```text
presentation.pptx
```

file that can be opened in Microsoft PowerPoint or compatible presentation software.

## 📂 Project Structure

A typical project structure looks like:

```text
Presentation_Generator/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.example.presentation/
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   └── PresentationController.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── PresentationService.java
│   │   │       │   ├── AIService.java
│   │   │       │   ├── ImageService.java
│   │   │       │   └── DiagramService.java
│   │   │       │
│   │   │       ├── generator/
│   │   │       │   └── PowerPointGenerator.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── Presentation.java
│   │   │       │   ├── Slide.java
│   │   │       │   └── Chunk.java
│   │   │       │
│   │   │       └── PresentationGeneratorApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
├── README.md
└── .gitignore
```

> The exact package and class names may differ depending on the current implementation.

## 🧩 Important Components

### PresentationController

Responsible for exposing REST endpoints and accepting presentation-generation requests.

Example:

```text
POST /presentation/generate
```

### PresentationService

Acts as the main coordinator.

It connects the AI, image, diagram, and PowerPoint generation components.

### AIService

Responsible for communicating with the LLM API and generating structured presentation content.

The AI configuration can include parameters such as:

```java
options.put("temperature", 0.2);
```

A lower temperature helps produce more consistent and predictable presentation content.

### ImageService

Responsible for obtaining or generating images that are relevant to individual slides.

### DiagramService

Creates visual diagrams for concepts that are better explained graphically.

### PowerPointGenerator

Converts the structured presentation data into an actual `.pptx` file.

## 📦 Data Model

The application uses objects to represent presentation information.

For example:

```java
public record Chunk(int index, String text) {
}
```

A Java `record` is useful when an object primarily represents data and does not require extensive custom behavior.

A presentation can conceptually be represented as:

```text
Presentation
    │
    ├── Slide 1
    │     ├── Title
    │     ├── Content
    │     ├── Image
    │     └── Diagram
    │
    ├── Slide 2
    │     ├── Title
    │     └── Content
    │
    └── Slide N
```

## ⚙️ Configuration

Create the required configuration values in:

```text
src/main/resources/application.properties
```

Example:

```properties
server.port=8080

# AI API configuration
ai.api.key=${AI_API_KEY}
```

Sensitive API keys should **never be committed to GitHub**.

Instead, configure them as environment variables.

Example:

### Windows

```cmd
set AI_API_KEY=your_api_key
```

### macOS/Linux

```bash
export AI_API_KEY=your_api_key
```

## ▶️ Running the Application

### 1. Clone the repository

```bash
git clone <your-repository-url>
```

### 2. Navigate to the project

```bash
cd Presentation_Generator
```

### 3. Configure the API key

Set the required environment variables.

### 4. Build the project

```bash
mvn clean install
```

### 5. Start the application

```bash
mvn spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

## 🔌 Example API Request

Example request:

```http
POST /presentation/generate
Content-Type: application/json
```

Request body:

```json
{
  "topic": "Apache Kafka",
  "slides": 7
}
```

The application processes the request and generates the presentation.

## 📤 Output

The generated presentation contains:

* Slide titles
* AI-generated content
* Relevant visual content
* Technical diagrams
* Structured layouts
* PowerPoint formatting

Example:

```text
output/
└── Apache_Kafka_Presentation.pptx
```

## 🎯 Example Use Cases

The application can be used to generate presentations for:

* Technical topics
* Software architecture
* College presentations
* Project demonstrations
* Business presentations
* Educational content
* Product explanations
* Technical interviews

Example topics:

```text
Java Spring Boot
Apache Kafka
Microservices
Cloud Computing
Artificial Intelligence
Machine Learning
Cybersecurity
Database Systems
```

## 🔮 Future Enhancements

Possible improvements include:

* User-selectable presentation themes
* Multiple PowerPoint templates
* Automatic speaker notes
* Voice-over generation
* Automatic slide animations
* Interactive charts
* More advanced diagrams
* Web-based presentation editor
* Presentation preview before download
* User authentication
* Presentation history
* Cloud storage integration
* Support for multiple AI providers

## 📚 Learning Outcomes

This project demonstrates practical experience with:

* Java application development
* Spring Boot REST APIs
* AI/LLM integration
* Prompt engineering
* JSON processing
* Object-oriented design
* File generation
* PowerPoint automation
* API integration
* Service-layer architecture
* Exception handling
* Environment-based configuration

## 👨‍💻 Author

**Badrinath Nooka**

Computer Science Graduate Student
Java | Spring Boot | AI/LLM | Full Stack Development

---

⭐ If you find this project useful, consider giving the repository a star.
