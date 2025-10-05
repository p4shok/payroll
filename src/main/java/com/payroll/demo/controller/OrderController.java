package com.payroll.demo.controller;

import com.payroll.demo.exception.OrderNotFoundException;
import com.payroll.demo.model.Order;
import com.payroll.demo.model.assembler.OrderModelAssembler;
import com.payroll.demo.model.domain.Status;
import com.payroll.demo.model.repository.OrderRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name="orders_controller")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderModelAssembler orderModelAssembler;

    public OrderController(OrderRepository orderRepository, OrderModelAssembler orderModelAssembler) {
        this.orderRepository = orderRepository;
        this.orderModelAssembler = orderModelAssembler;
    }

    @GetMapping("/orders")
    public CollectionModel<EntityModel<Order>> getAll() {
        List<EntityModel<Order>> orders = orderRepository.findAll().stream()
                .map(orderModelAssembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(orders,
                linkTo(methodOn(OrderController.class).getAll()).withSelfRel());
    }

    @GetMapping("/orders/{id}")
    public EntityModel<Order> one(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return orderModelAssembler.toModel(order);
    }

    @PostMapping("/orders")
    public ResponseEntity<EntityModel<Order>> newOrder(@RequestBody Order order) {
        order.setStatus(Status.IN_PROGRESS);
        Order newOrder = orderRepository.save(order);

        return ResponseEntity
                .created(linkTo(methodOn(OrderController.class).one(newOrder.getId())).toUri())
                .body(orderModelAssembler.toModel(newOrder));
    }

    @DeleteMapping("/orders/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if(order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.CANCELLED);
            return ResponseEntity.ok(orderModelAssembler.toModel(orderRepository.save(order)));
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create()
                        .withTitle("method not allowed")
                        .withDetail("You can't cancel an order that is in the " + order.getStatus() + " status"));
    }

    @PutMapping("/orders/{id}")
    public ResponseEntity<?> complete(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if(order.getStatus() == Status.IN_PROGRESS) {
            order.setStatus(Status.CANCELLED);
            return ResponseEntity.ok(orderModelAssembler.toModel(orderRepository.save(order)));
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                .body(Problem.create()
                        .withTitle("method not allowed")
                        .withDetail("You can't complete an order that is in the " + order.getStatus() + " status"));
    }
}
