package com.example.demo;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class ShopkartApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopkartApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(CategoryRepository categoryRepository, ProductRepository productRepository) {
		return args -> {
			List<String> categoriesToSeed = Arrays.asList(
					"Shirts", "Pants", "Accessories", "Mobiles", "Mobile Accessories", "Beauty", "Appliances", "Books", "Food"
			);

			for (String catName : categoriesToSeed) {
				if (categoryRepository.findByCategoryName(catName).isEmpty()) {
					Category category = new Category();
					category.setCategoryName(catName);
					categoryRepository.save(category);
					System.out.println("Seeded category: " + catName);
				}
			}

			// Map product Maggie Masala (ID 12) -> Food
			Optional<Product> maggieOpt = productRepository.findById(12);
			if (maggieOpt.isPresent()) {
				Product maggie = maggieOpt.get();
				Optional<Category> foodCat = categoryRepository.findByCategoryName("Food");
				if (foodCat.isPresent()) {
					maggie.setCategory(foodCat.get());
					productRepository.save(maggie);
				}
			}

			// Map product MacBook Air M3 (ID 11) -> Mobiles
			Optional<Product> macbookOpt = productRepository.findById(11);
			if (macbookOpt.isPresent()) {
				Product macbook = macbookOpt.get();
				Optional<Category> mobilesCat = categoryRepository.findByCategoryName("Mobiles");
				if (mobilesCat.isPresent()) {
					macbook.setCategory(mobilesCat.get());
					productRepository.save(macbook);
				}
			}

			// Map watches to Accessories
			Optional<Category> accCat = categoryRepository.findByCategoryName("Accessories");
			if (accCat.isPresent()) {
				List<Integer> watchIds = Arrays.asList(7, 8);
				for (Integer id : watchIds) {
					Optional<Product> watchOpt = productRepository.findById(id);
					if (watchOpt.isPresent()) {
						Product watch = watchOpt.get();
						watch.setCategory(accCat.get());
						productRepository.save(watch);
					}
				}
			}

			// Map bags to Accessories
			if (accCat.isPresent()) {
				List<Integer> bagIds = Arrays.asList(9, 10);
				for (Integer id : bagIds) {
					Optional<Product> bagOpt = productRepository.findById(id);
					if (bagOpt.isPresent()) {
						Product bag = bagOpt.get();
						bag.setCategory(accCat.get());
						productRepository.save(bag);
					}
				}
			}
		};
	}

}
