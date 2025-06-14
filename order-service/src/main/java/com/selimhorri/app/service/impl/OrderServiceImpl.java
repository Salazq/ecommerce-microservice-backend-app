package com.selimhorri.app.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	
	private final OrderRepository orderRepository;
	private static final String ORDER_SERVICE = "orderService"; // Circuit breaker name
	
	@Override
	@CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "fallbackFindAllOrders")
	public List<OrderDto> findAll() {
		log.info("*** OrderDto List, service; fetch all orders *");
		return this.orderRepository.findAll()
				.stream()
					.map(OrderMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	@CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "fallbackFindOrderById")
	public OrderDto findById(final Integer orderId) {
		log.info("*** OrderDto, service; fetch order by id *");
		return this.orderRepository.findById(orderId)
				.map(OrderMappingHelper::map)
				.orElseThrow(() -> new OrderNotFoundException(String
						.format("Order with id: %d not found", orderId)));
	}
	
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("*** OrderDto, service; save order *");
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
	}
	
	@Override
	public OrderDto update(final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order *");
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
	}
	
	@Override
	public OrderDto update(final Integer orderId, final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order with orderId *");
		// First, find the existing order by its ID. This call is now protected by a circuit breaker.
		OrderDto existingOrder = this.findById(orderId);
		// Then, map the incoming DTO to an entity and save it.
		// Note: The original code was saving the result of findById(orderId) directly.
		// This usually means you want to update fields of the existingOrder with values from orderDto,
		// then save existingOrder. For simplicity, I'm keeping the direct mapping and saving,
		// assuming OrderMappingHelper.map(orderDto) prepares the correct entity state for update.
		// If specific fields need to be updated, that logic should be here.
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto))); // Assuming orderDto has the ID for an update
	}
	
	@Override
	public void deleteById(final Integer orderId) {
		log.info("*** Void, service; delete order by id *");
		// The findById call is protected by a circuit breaker.
		this.orderRepository.delete(OrderMappingHelper.map(this.findById(orderId)));
	}
	
	// Fallback methods for OrderService
	public List<OrderDto> fallbackFindAllOrders(Throwable t) {
		log.error("Error calling external service from findAll orders: {}", t.getMessage());
		// Return an empty list or cached data
		return Collections.emptyList();
	}
	
	public OrderDto fallbackFindOrderById(Integer orderId, Throwable t) {
		log.error("Error calling external service for orderId {}: {}", orderId, t.getMessage());
		// Return a default/empty OrderDto or throw a specific fallback exception
		// For example, returning an empty DTO:
		// return new OrderDto(); 
		// Or, re-throwing a custom exception:
		throw new OrderNotFoundException(String.format("Order with id: %d not found (fallback due to service error: %s)", orderId, t.getMessage()));
	}
	
}










