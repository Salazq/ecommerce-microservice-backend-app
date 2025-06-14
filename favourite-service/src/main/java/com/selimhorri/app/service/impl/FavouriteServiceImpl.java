package com.selimhorri.app.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {
	
	private final FavouriteRepository favouriteRepository;
	private final RestTemplate restTemplate;
	private static final String USER_SERVICE = "userService";
	private static final String PRODUCT_SERVICE = "productService";
	
	@Override
	public List<FavouriteDto> findAll() {
		log.info("*** FavouriteDto List, service; fetch all favourites *");
		return this.favouriteRepository.findAll()
				.stream()
					.map(FavouriteMappingHelper::map)
					.map(this::enrichFavouriteDtoWithUserDetails)
					.map(this::enrichFavouriteDtoWithProductDetails)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public FavouriteDto findById(final FavouriteId favouriteId) {
		log.info("*** FavouriteDto, service; fetch favourite by id *");
		return this.favouriteRepository.findById(favouriteId)
				.map(FavouriteMappingHelper::map)
				.map(this::enrichFavouriteDtoWithUserDetails)
				.map(this::enrichFavouriteDtoWithProductDetails)
				.orElseThrow(() -> new FavouriteNotFoundException(
						String.format("Favourite with id: [%s] not found!", favouriteId)));
	}

	@CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackUserDto")
	protected FavouriteDto enrichFavouriteDtoWithUserDetails(FavouriteDto f) {
		try {
			f.setUserDto(this.restTemplate
					.getForObject(AppConstant.DiscoveredDomainsApi
							.USER_SERVICE_API_URL + "/" + f.getUserId(), UserDto.class));
		} catch (Exception e) {
			log.error("Error calling userService for userId {}: {}", f.getUserId(), e.getMessage());
			// Fallback will be called by Resilience4j, but as a safeguard or for direct calls:
			return fallbackUserDto(f, e);
		}
		return f;
	}

	@CircuitBreaker(name = PRODUCT_SERVICE, fallbackMethod = "fallbackProductDto")
	protected FavouriteDto enrichFavouriteDtoWithProductDetails(FavouriteDto f) {
		try {
			f.setProductDto(this.restTemplate
					.getForObject(AppConstant.DiscoveredDomainsApi
							.PRODUCT_SERVICE_API_URL + "/" + f.getProductId(), ProductDto.class));
		} catch (Exception e) {
			log.error("Error calling productService for productId {}: {}", f.getProductId(), e.getMessage());
			// Fallback will be called by Resilience4j, but as a safeguard or for direct calls:
			return fallbackProductDto(f, e);
		}
		return f;
	}
	
	@Override
	public FavouriteDto save(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}
	
	@Override
	public FavouriteDto update(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}
	
	@Override
	public void deleteById(final FavouriteId favouriteId) {
		this.favouriteRepository.deleteById(favouriteId);
	}
	
	// Fallback methods
	public FavouriteDto fallbackUserDto(FavouriteDto favouriteDto, Throwable t) {
		log.error("Fallback for userService: Error calling userService for userId {}: {}", favouriteDto.getUserId(), t.getMessage());
		favouriteDto.setUserDto(new UserDto()); // Default UserDto
		return favouriteDto;
	}

	public FavouriteDto fallbackProductDto(FavouriteDto favouriteDto, Throwable t) {
		log.error("Fallback for productService: Error calling productService for productId {}: {}", favouriteDto.getProductId(), t.getMessage());
		favouriteDto.setProductDto(new ProductDto()); // Default ProductDto
		return favouriteDto;
	}

	// Fallback for findAll if needed, though individual enrichment fallbacks are more granular
	public List<FavouriteDto> fallbackFindAllFavourites(Throwable t) {
		log.error("Error calling external services from findAll favourites: {}", t.getMessage());
		// Return an empty list or cached data, with default user/product details
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(new UserDto()); 
					f.setProductDto(new ProductDto()); 
					return f;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	// Fallback for findById if needed
	public FavouriteDto fallbackFindFavouriteById(FavouriteId favouriteId, Throwable t) {
		log.error("Error calling external services for favouriteId {}: {}", favouriteId, t.getMessage());
		return this.favouriteRepository.findById(favouriteId)
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(new UserDto()); 
					f.setProductDto(new ProductDto()); 
					return f;
				})
				.orElseThrow(() -> new FavouriteNotFoundException(String
						.format("Favourite with id: %s not found (fallback due to service error: %s)", favouriteId, t.getMessage())));
	}
	
}










