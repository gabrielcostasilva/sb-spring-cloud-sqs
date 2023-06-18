package com.example.demo;

import org.springframework.stereotype.Service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Service
@RequiredArgsConstructor
public class TodoController {

    private final TodoRepository repository;
    private final SqsAsyncClient sqsAsyncClient;

    public void getTodos() {

        SqsTemplate template = 
                SqsTemplate.newTemplate(sqsAsyncClient);

        repository
            .findAll()
            .forEach(aTodo -> 
                        template.sendAsync(
                "get-todos", 
                            aTodo));

    }

    @SqsListener("new-todo")
    public void createTodo(Todo aTodo) {
        repository.save(aTodo);

        this.getTodos();

    }

}
