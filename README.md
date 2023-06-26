# EXPLORING SQS WITH SPRING CLOUD AWS
This project uses the [SQS feature](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0/reference/html/index.html#sqs-integration) of [Spring Cloud for AWS](https://spring.io/projects/spring-cloud-aws) to implementing asynchronous communication between front- and back-end.

> If you need to understand the application, checkout the original [to-do project](https://github.com/gabrielcostasilva/java-todo.git).

## Overview
This project decouples front- and back-end by using [Amazon SQS](https://aws.amazon.com/sqs/) as a messaging system. 

### Front-end
If the front-end find `Todo`s in the SQS queue, it loads them to show in the GUI, like so: 

```java
(...)

@GetMapping("/")
public String getTodos(Model memory) {

    var messages = template.receiveMany(
                        "get-todos", 
                        Todo.class); // (1) 

    if (messages.size() == 0) {
        memory.addAttribute(
            "todos", 
            new ArrayList<Todo>()); // (2)
        
    } else {
        var todos = messages
                        .stream()
                        .map(item -> item.getPayload())
                        .collect(Collectors.toList()); // (3)
                        
        memory.addAttribute("todos", todos);

    }

    return "/todo";
}

(...)   
```
1. Checks the `get-todos` queue
2. There is no `Todo` to load, load an empty list
3. Loads `Todo`s from the queue

When creating a new `Todo`, the front-end just send a message to the queue with `Todo` data, like so:

```java
@PostMapping("/add")
public String createTodo(Todo aTodo) {
    
    template.sendAsync(
            "new-todo", 
            aTodo); // (1)

    return "redirect:/";
}   
```

1. Sends `Todo` data to the `new-todo` queue as a message.

### Back-end
A listener awaits for new messages, like so:

```java
@SqsListener("new-todo") // (1)
public void createTodo(Todo aTodo) { // (2)
    repository.save(aTodo); // (3)

    this.getTodos(); // (4)

}
```

1. Awaits for new messages from the `new-todo` queue;
2. Gets the message and transforms it into a `Todo`;
3. Saves `Todo` in the database;
4. Populates existing `Todo` list in the _result queue_.

The _result queue_ stores all `Todo`s in the database, like so:

```java
public void getTodos() {

    SqsTemplate template = 
            SqsTemplate.newTemplate(sqsAsyncClient); // (1)

    repository
        .findAll()
        .forEach(aTodo -> 
                    template.sendAsync(
                    "get-todos", 
                    aTodo)); // (2)

}
```

1. Creates a _template_ that uses a _SQS Client_;
2. Saves all `Todo`s in the `get-todos` queue as a set of messages

> Notice that the `Todo` list is updated only after a new `Todo` is created.

## The Framework
The Spring Cloud for AWS uses traditional Spring mechanisms, like dependency injection and annotations. These well-known mechanisms make Spring Cloud easier to use than [AWS SQS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_sqs_code_examples.html).

Both [front-](./front/src/main/java/com/example/demo/SQSConfiguration.java) and [back-end](./back/src/main/java/com/example/demo/SQSConfiguration.java) have a _configuration class_. The configuration class exposes the SQS Client or Template as a Spring _Bean_. Therefore, it can be injected into Spring managed classes.

The code below shows the configuration class for the front-end project.

```java
@Configuration
public class SQSConfiguration {
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder().build();
    }

    @Bean
    public SqsTemplate sqsTemplate() {
        return SqsTemplate.newTemplate(sqsAsyncClient());
    }
}
```

Thus, it can be injected, like the code below shows:

```java
@Controller
public class TodoController {

    private SqsTemplate template;

    public TodoController(SqsTemplate template) {
        this.template = template;
    }

    (...)

    @PostMapping("/add")
    public String createTodo(Todo aTodo) {
        
        template.sendAsync(
                "new-todo", 
                aTodo);

        return "redirect:/";
    }

}
```

## Dependencies
In addition to the `io.awspring.cloud.spring-cloud-aws-starter-sqs` dependency, the dependency management for `io.awspring.cloud.spring-cloud-aws-dependencies` is also necessary.

## Running the Project
1. Create two SQS queues: `new-todo` and `get-todos`
2. Create credentials for CLI access with AWS IAM, and set writing and reading permissions 
3. Configure your AWS credentials for _aws cli_ with `aws configure`
4. Start the back-end project with `mvn spring-boot:run`
5. Start the front-end project with `mvn spring-boot:run`
6. Open your [localhost](http://localhost:8090) address on port 8090 with your favorite browser and run some tests
