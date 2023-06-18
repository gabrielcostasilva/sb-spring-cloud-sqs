package com.example.demo;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import io.awspring.cloud.sqs.operations.SqsTemplate;

@Controller
public class TodoController {

    private SqsTemplate template;

    public TodoController(SqsTemplate template) {
        this.template = template;
    }


    @GetMapping("/")
    public String getTodos(Model memory) {

        var messages = template.receiveMany(
                            "get-todos", 
                            Todo.class);

        if (messages.size() == 0) {
            memory.addAttribute(
                "todos", 
                new ArrayList<Todo>());
            
        } else {
            var todos = messages
                            .stream()
                            .map(item -> item.getPayload())
                            .collect(Collectors.toList());
                            
            memory.addAttribute("todos", todos);

        }

        return "/todo";
    }

    @PostMapping("/add")
    public String createTodo(Todo aTodo) {
        
        template.sendAsync(
                "new-todo", 
                aTodo);

        return "redirect:/";
    }

}
